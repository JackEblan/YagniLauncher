/*
 *
 *   Copyright 2023 Einstein Blanco
 *
 *   Licensed under the GNU General Public License v3.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       https://www.gnu.org/licenses/gpl-3.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package com.eblan.launcher.framework.launcherapps

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.content.pm.LauncherUserInfo
import android.content.pm.ShortcutInfo
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Process.myUserHandle
import android.os.UserHandle
import android.os.UserManager
import androidx.annotation.RequiresApi
import com.eblan.launcher.domain.common.dispatcher.Dispatcher
import com.eblan.launcher.domain.common.dispatcher.EblanDispatchers
import com.eblan.launcher.domain.framework.FileManager
import com.eblan.launcher.domain.framework.LauncherAppsWrapper
import com.eblan.launcher.domain.framework.PackageManagerWrapper
import com.eblan.launcher.domain.model.EblanUser
import com.eblan.launcher.domain.model.EblanUserType
import com.eblan.launcher.domain.model.FastLauncherAppsActivityInfo
import com.eblan.launcher.domain.model.FastLauncherAppsShortcutInfo
import com.eblan.launcher.domain.model.LauncherAppsActivityInfo
import com.eblan.launcher.domain.model.LauncherAppsEvent
import com.eblan.launcher.domain.model.LauncherAppsShortcutInfo
import com.eblan.launcher.domain.model.ShortcutConfigActivityInfo
import com.eblan.launcher.domain.model.ShortcutQueryFlag
import com.eblan.launcher.framework.imageserializer.AndroidImageSerializer
import com.eblan.launcher.framework.packagemanager.AndroidPackageManagerWrapper
import com.eblan.launcher.framework.usermanager.AndroidUserManagerWrapper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

internal class DefaultLauncherAppsWrapper @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val imageSerializer: AndroidImageSerializer,
    private val userManagerWrapper: AndroidUserManagerWrapper,
    private val fileManager: FileManager,
    private val packageManagerWrapper: PackageManagerWrapper,
    private val androidPackageManager: AndroidPackageManagerWrapper,
    @param:Dispatcher(EblanDispatchers.Default) private val defaultDispatcher: CoroutineDispatcher,
) : LauncherAppsWrapper,
    AndroidLauncherAppsWrapper {
    private val launcherApps =
        context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps

    override val hasShortcutHostPermission
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1 && launcherApps.hasShortcutHostPermission()

    override val launcherAppsEvent: Flow<LauncherAppsEvent> = callbackFlow {
        val callback = object : LauncherApps.Callback() {
            override fun onPackageRemoved(packageName: String?, user: UserHandle?) {
                if (packageName != null && user != null) {
                    trySend(
                        LauncherAppsEvent.PackageRemoved(
                            serialNumber = userManagerWrapper.getSerialNumberForUser(userHandle = user),
                            packageName = packageName,
                        ),
                    )
                }
            }

            override fun onPackageAdded(packageName: String?, user: UserHandle?) {
                if (packageName != null && user != null) {
                    trySend(
                        LauncherAppsEvent.PackageAdded(
                            serialNumber = userManagerWrapper.getSerialNumberForUser(userHandle = user),
                            packageName = packageName,
                        ),
                    )
                }
            }

            override fun onPackageChanged(packageName: String?, user: UserHandle?) {
                if (packageName != null && user != null) {
                    trySend(
                        LauncherAppsEvent.PackageChanged(
                            serialNumber = userManagerWrapper.getSerialNumberForUser(userHandle = user),
                            packageName = packageName,
                        ),
                    )
                }
            }

            override fun onPackagesAvailable(
                packageNames: Array<out String>?,
                user: UserHandle?,
                replacing: Boolean,
            ) {
                // TODO: Show installed applications
            }

            override fun onPackagesUnavailable(
                packageNames: Array<out String>?,
                user: UserHandle?,
                replacing: Boolean,
            ) {
                // TODO: Hide installed applications
            }

            override fun onShortcutsChanged(
                packageName: String,
                shortcuts: MutableList<ShortcutInfo>,
                user: UserHandle,
            ) {
                launch {
                    trySend(
                        LauncherAppsEvent.ShortcutsChanged(
                            serialNumber = userManagerWrapper.getSerialNumberForUser(userHandle = user),
                            packageName = packageName,
                            launcherAppsShortcutInfos = getShortcutsByPackageName(
                                serialNumber = userManagerWrapper.getSerialNumberForUser(
                                    userHandle = user,
                                ),
                                packageName = packageName,
                            ),
                        ),
                    )
                }
            }
        }

        launcherApps.registerCallback(callback, Handler(Looper.getMainLooper()))

        awaitClose {
            launcherApps.unregisterCallback(callback)
        }
    }.flowOn(defaultDispatcher)

    override suspend fun getActivityList(): List<LauncherAppsActivityInfo> = withContext(defaultDispatcher) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            launcherApps.profiles.filterNot { userHandle ->
                isPrivateSpaceEntryPointHidden(userHandle = userHandle)
            }.flatMap { userHandle ->
                currentCoroutineContext().ensureActive()

                launcherApps.getActivityList(null, userHandle).map { launcherActivityInfo ->
                    currentCoroutineContext().ensureActive()

                    launcherActivityInfo.toLauncherAppsActivityInfo()
                }
            }
        } else {
            launcherApps.getActivityList(null, myUserHandle()).map { launcherActivityInfo ->
                currentCoroutineContext().ensureActive()

                launcherActivityInfo.toLauncherAppsActivityInfo()
            }
        }
    }

    override suspend fun getFastActivityList(): List<FastLauncherAppsActivityInfo> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        launcherApps.profiles.filterNot { userHandle ->
            isPrivateSpaceEntryPointHidden(userHandle = userHandle)
        }.flatMap { userHandle ->
            currentCoroutineContext().ensureActive()

            launcherApps.getActivityList(null, userHandle).map { launcherActivityInfo ->
                currentCoroutineContext().ensureActive()

                launcherActivityInfo.toFastLauncherAppsActivityInfo()
            }
        }
    } else {
        launcherApps.getActivityList(null, myUserHandle()).map { launcherActivityInfo ->
            currentCoroutineContext().ensureActive()

            launcherActivityInfo.toFastLauncherAppsActivityInfo()
        }
    }

    override suspend fun getActivityList(
        serialNumber: Long,
        packageName: String,
    ): List<LauncherAppsActivityInfo> = withContext(defaultDispatcher) {
        val userHandle = userManagerWrapper.getUserForSerialNumber(serialNumber = serialNumber)

        launcherApps.getActivityList(packageName, userHandle).map { launcherActivityInfo ->
            currentCoroutineContext().ensureActive()

            launcherActivityInfo.toLauncherAppsActivityInfo()
        }
    }

    override suspend fun getFastActivityList(
        serialNumber: Long,
        packageName: String,
    ): List<FastLauncherAppsActivityInfo> = withContext(defaultDispatcher) {
        val userHandle = userManagerWrapper.getUserForSerialNumber(serialNumber = serialNumber)

        launcherApps.getActivityList(packageName, userHandle).map { launcherActivityInfo ->
            currentCoroutineContext().ensureActive()

            launcherActivityInfo.toFastLauncherAppsActivityInfo()
        }
    }

    override suspend fun getShortcuts(): List<LauncherAppsShortcutInfo>? = withContext(defaultDispatcher) {
        if (hasShortcutHostPermission) {
            val shortcutQuery = LauncherApps.ShortcutQuery().apply {
                setQueryFlags(
                    LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST or LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED,
                )
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                launcherApps.profiles.filter { userHandle ->
                    isUserAvailable(userHandle = userHandle)
                }.flatMap { userHandle ->
                    currentCoroutineContext().ensureActive()

                    launcherApps.getShortcuts(shortcutQuery, userHandle)?.map { shortcutInfo ->
                        currentCoroutineContext().ensureActive()

                        shortcutInfo.toLauncherAppsShortcutInfo()
                    } ?: emptyList()
                }
            } else {
                launcherApps.getShortcuts(shortcutQuery, myUserHandle())?.map { shortcutInfo ->
                    currentCoroutineContext().ensureActive()

                    shortcutInfo.toLauncherAppsShortcutInfo()
                }
            }
        } else {
            null
        }
    }

    override suspend fun getFastShortcuts(): List<FastLauncherAppsShortcutInfo>? = if (hasShortcutHostPermission) {
        val shortcutQuery = LauncherApps.ShortcutQuery().apply {
            setQueryFlags(
                LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST or LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED,
            )
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            launcherApps.profiles.filter { userHandle ->
                isUserAvailable(userHandle = userHandle)
            }.flatMap { userHandle ->
                currentCoroutineContext().ensureActive()

                launcherApps.getShortcuts(shortcutQuery, userHandle)?.map { shortcutInfo ->
                    currentCoroutineContext().ensureActive()

                    shortcutInfo.toFastLauncherAppsShortcutInfo()
                } ?: emptyList()
            }
        } else {
            launcherApps.getShortcuts(shortcutQuery, myUserHandle())?.map { shortcutInfo ->
                currentCoroutineContext().ensureActive()

                shortcutInfo.toFastLauncherAppsShortcutInfo()
            }
        }
    } else {
        null
    }

    override suspend fun getShortcutsByPackageName(
        serialNumber: Long,
        packageName: String,
    ): List<LauncherAppsShortcutInfo>? = withContext(defaultDispatcher) {
        val userHandle = userManagerWrapper.getUserForSerialNumber(serialNumber = serialNumber)

        if (hasShortcutHostPermission && userHandle != null) {
            val shortcutQuery = LauncherApps.ShortcutQuery().apply {
                setPackage(packageName)

                setQueryFlags(
                    LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST or LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED,
                )
            }

            launcherApps.getShortcuts(shortcutQuery, userHandle)?.map { shortcutInfo ->
                currentCoroutineContext().ensureActive()

                shortcutInfo.toLauncherAppsShortcutInfo()
            }
        } else {
            null
        }
    }

    override suspend fun getShortcutConfigActivityList(
        serialNumber: Long,
        packageName: String,
    ): List<ShortcutConfigActivityInfo> = withContext(defaultDispatcher) {
        val userHandle = userManagerWrapper.getUserForSerialNumber(serialNumber = serialNumber)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && userHandle != null) {
            launcherApps.getShortcutConfigActivityList(packageName, userHandle)
                .map { launcherActivityInfo ->
                    currentCoroutineContext().ensureActive()

                    launcherActivityInfo.toLauncherAppsActivityInfo()
                }
        } else {
            emptyList()
        }
    }

    override fun startMainActivity(
        serialNumber: Long,
        componentName: String,
        sourceBounds: Rect,
    ) {
        val userHandle = userManagerWrapper.getUserForSerialNumber(serialNumber = serialNumber)

        try {
            if (userHandle != null && isUserAvailable(userHandle = userHandle)) {
                launcherApps.startMainActivity(
                    ComponentName.unflattenFromString(componentName),
                    userHandle,
                    sourceBounds,
                    Bundle.EMPTY,
                )
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun getPinItemRequest(intent: Intent): LauncherApps.PinItemRequest = launcherApps.getPinItemRequest(intent)

    @RequiresApi(Build.VERSION_CODES.N_MR1)
    override fun startShortcut(
        serialNumber: Long,
        packageName: String,
        id: String,
        sourceBounds: Rect,
    ) {
        val userHandle = userManagerWrapper.getUserForSerialNumber(serialNumber = serialNumber)

        try {
            if (userHandle != null && isUserAvailable(userHandle = userHandle)) {
                launcherApps.startShortcut(
                    packageName,
                    id,
                    sourceBounds,
                    null,
                    userHandle,
                )
            }
        } catch (e: ActivityNotFoundException) {
            e.printStackTrace()
        }
    }

    @RequiresApi(Build.VERSION_CODES.N_MR1)
    override fun startShortcut(
        packageName: String,
        id: String,
        sourceBounds: Rect,
    ) {
        try {
            if (isUserAvailable(userHandle = myUserHandle())) {
                launcherApps.startShortcut(
                    packageName,
                    id,
                    sourceBounds,
                    null,
                    myUserHandle(),
                )
            }
        } catch (e: ActivityNotFoundException) {
            e.printStackTrace()
        }
    }

    @RequiresApi(Build.VERSION_CODES.N_MR1)
    override fun getShortcutIconDrawable(
        shortcutInfo: ShortcutInfo?,
        density: Int,
    ): Drawable? = if (shortcutInfo != null) {
        launcherApps.getShortcutIconDrawable(shortcutInfo, density)
    } else {
        null
    }

    override fun startAppDetailsActivity(
        serialNumber: Long,
        componentName: String,
        sourceBounds: Rect,
    ) {
        launcherApps.startAppDetailsActivity(
            ComponentName.unflattenFromString(componentName),
            userManagerWrapper.getUserForSerialNumber(serialNumber = serialNumber),
            sourceBounds,
            Bundle.EMPTY,
        )
    }

    override suspend fun getShortcutConfigIntent(
        serialNumber: Long,
        packageName: String,
        componentName: String,
    ): IntentSender? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || !hasShortcutHostPermission) return null

        return withContext(defaultDispatcher) {
            val userHandle = userManagerWrapper.getUserForSerialNumber(serialNumber = serialNumber)

            val launcherActivityInfo = if (userHandle != null) {
                launcherApps.getShortcutConfigActivityList(packageName, userHandle)
                    .find { launcherActivityInfo ->
                        launcherActivityInfo.componentName.flattenToString() == componentName
                    }
            } else {
                null
            }

            launcherActivityInfo?.let(launcherApps::getShortcutConfigActivityIntent)
        }
    }

    override fun getUser(serialNumber: Long): EblanUser {
        val userHandle = userManagerWrapper.getUserForSerialNumber(serialNumber = serialNumber)
            ?: return EblanUser(
                serialNumber = serialNumber,
                eblanUserType = EblanUserType.Personal,
                isPrivateSpaceEntryPointHidden = false,
            )

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            val launcherUserInfo = launcherApps.getLauncherUserInfo(userHandle) ?: return EblanUser(
                serialNumber = serialNumber,
                eblanUserType = EblanUserType.Personal,
                isPrivateSpaceEntryPointHidden = isPrivateSpaceEntryPointHidden(userHandle = userHandle),
            )

            val eblanUserType = when (launcherUserInfo.userType) {
                UserManager.USER_TYPE_PROFILE_CLONE -> EblanUserType.Clone
                UserManager.USER_TYPE_PROFILE_MANAGED -> EblanUserType.Work
                UserManager.USER_TYPE_PROFILE_PRIVATE -> EblanUserType.Private
                else -> EblanUserType.Personal
            }

            EblanUser(
                serialNumber = serialNumber,
                eblanUserType = eblanUserType,
                isPrivateSpaceEntryPointHidden = isPrivateSpaceEntryPointHidden(userHandle = userHandle),
            )
        } else {
            val eblanUserType = when {
                androidPackageManager.getUserBadgedLabel(
                    label = "",
                    userHandle = userHandle,
                ).isNotBlank() -> EblanUserType.Work

                else -> EblanUserType.Personal
            }

            EblanUser(
                serialNumber = serialNumber,
                eblanUserType = eblanUserType,
                isPrivateSpaceEntryPointHidden = isPrivateSpaceEntryPointHidden(userHandle = userHandle),
            )
        }
    }

    override fun getPrivateSpaceSettingsIntent(): IntentSender? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
        launcherApps.privateSpaceSettingsIntent
    } else {
        null
    }

    private fun isUserAvailable(userHandle: UserHandle): Boolean = userManagerWrapper.isUserRunning(userHandle = userHandle) && userManagerWrapper.isUserUnlocked(
        userHandle = userHandle,
    ) && !userManagerWrapper.isQuietModeEnabled(userHandle = userHandle)

    private fun isPrivateSpaceEntryPointHidden(userHandle: UserHandle): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            val launcherUserInfo = launcherApps.getLauncherUserInfo(userHandle) ?: return false

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
                launcherUserInfo.userConfig.getBoolean(LauncherUserInfo.PRIVATE_SPACE_ENTRYPOINT_HIDDEN)
            } else {
                false
            }
        } else {
            false
        }
    }

    private suspend fun LauncherActivityInfo.toLauncherAppsActivityInfo(): LauncherAppsActivityInfo {
        val serialNumber = userManagerWrapper.getSerialNumberForUser(userHandle = user)

        val iconKey = "$serialNumber:${componentName.flattenToString()}"

        val activityIcon = getBadgedIcon(0).let { drawable ->
            val directory = fileManager.getFilesDirectory(FileManager.ICONS_DIR)

            val file = File(
                directory,
                fileManager.getHashedFileName(name = iconKey),
            )

            imageSerializer.createDrawablePath(drawable = drawable, file = file)

            file.absolutePath
        }

        return LauncherAppsActivityInfo(
            serialNumber = serialNumber,
            componentName = componentName.flattenToString(),
            packageName = applicationInfo.packageName,
            activityIcon = activityIcon,
            activityLabel = label.toString(),
            lastUpdateTime = packageManagerWrapper.getLastUpdateTime(packageName = applicationInfo.packageName),
        )
    }

    private fun LauncherActivityInfo.toFastLauncherAppsActivityInfo(): FastLauncherAppsActivityInfo = FastLauncherAppsActivityInfo(
        serialNumber = userManagerWrapper.getSerialNumberForUser(userHandle = user),
        componentName = componentName.flattenToString(),
        packageName = applicationInfo.packageName,
        lastUpdateTime = packageManagerWrapper.getLastUpdateTime(packageName = applicationInfo.packageName),
    )

    @RequiresApi(Build.VERSION_CODES.N_MR1)
    private suspend fun ShortcutInfo.toLauncherAppsShortcutInfo(): LauncherAppsShortcutInfo {
        val serialNumber = userManagerWrapper.getSerialNumberForUser(userHandle = userHandle)

        val shortcutIconKey = "$serialNumber:$id"

        val icon = launcherApps.getShortcutBadgedIconDrawable(this, 0)?.let { drawable ->
            val directory = fileManager.getFilesDirectory(FileManager.SHORTCUTS_DIR)

            val file = File(
                directory,
                fileManager.getHashedFileName(name = shortcutIconKey),
            )

            imageSerializer.createDrawablePath(drawable = drawable, file = file)

            file.absolutePath
        }

        val shortcutQueryFlag = when {
            isPinned -> {
                ShortcutQueryFlag.Pinned
            }

            isDynamic -> {
                ShortcutQueryFlag.Dynamic
            }

            else -> {
                ShortcutQueryFlag.Manifest
            }
        }

        return LauncherAppsShortcutInfo(
            shortcutId = id,
            packageName = `package`,
            serialNumber = serialNumber,
            shortLabel = shortLabel.toString(),
            longLabel = longLabel.toString(),
            isEnabled = isEnabled,
            icon = icon,
            shortcutQueryFlag = shortcutQueryFlag,
            lastChangedTimestamp = lastChangedTimestamp,
        )
    }

    @RequiresApi(Build.VERSION_CODES.N_MR1)
    private fun ShortcutInfo.toFastLauncherAppsShortcutInfo(): FastLauncherAppsShortcutInfo = FastLauncherAppsShortcutInfo(
        packageName = `package`,
        serialNumber = userManagerWrapper.getSerialNumberForUser(userHandle = userHandle),
        lastChangedTimestamp = packageManagerWrapper.getLastUpdateTime(packageName = `package`),
    )
}
