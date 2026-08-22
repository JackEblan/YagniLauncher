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
package com.eblan.launcher.feature.home.screen.pager

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.content.pm.LauncherApps
import android.content.pm.ShortcutInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.UserHandle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.eblan.launcher.domain.model.ManagedProfileResult
import com.eblan.launcher.framework.usermanager.AndroidUserManagerWrapper
import com.eblan.launcher.service.EblanNotificationListenerService
import com.eblan.launcher.ui.local.LocalAppWidgetHost
import com.eblan.launcher.ui.local.LocalLauncherApps
import com.eblan.launcher.ui.local.LocalPinItemRequest
import kotlinx.coroutines.launch

@Composable
internal fun LifecycleEffect(
    syncData: Boolean,
    userManagerWrapper: AndroidUserManagerWrapper,
    onManagedProfileResultChange: (ManagedProfileResult?) -> Unit,
    onStartSyncData: () -> Unit,
    onStatusBarNotificationsChange: (Map<String, Int>) -> Unit,
    onStopSyncData: () -> Unit,
    onPackageRemoved: (
        serialNumber: Long,
        packageName: String,
    ) -> Unit,
    onPackageAdded: (
        serialNumber: Long,
        packageName: String,
    ) -> Unit,
    onPackageChanged: (
        serialNumber: Long,
        packageName: String,
    ) -> Unit,
    onShortcutsChanged: (
        serialNumber: Long,
        packageName: String,
    ) -> Unit,
) {
    val context = LocalContext.current

    val lifecycleOwner = LocalLifecycleOwner.current

    val appWidgetHost = LocalAppWidgetHost.current

    val pinItemRequestWrapper = LocalPinItemRequest.current

    val launcherAppsWrapper = LocalLauncherApps.current

    DisposableEffect(
        key1 = lifecycleOwner,
        key2 = syncData,
    ) {
        val eblanNotificationListenerIntent =
            Intent(context, EblanNotificationListenerService::class.java)

        var shouldUnbindEblanNotificationListenerService = false

        var shouldUnRegisterManagedProfileBroadcastReceiver = false

        val eblanNotificationListenerServiceConnection =
            getEblanNotificationListenerServiceConnection(
                lifecycleOwner = lifecycleOwner,
                onStatusBarNotificationsChange = onStatusBarNotificationsChange,
            )

        val managedProfileBroadcastReceiver = getManagedProfileBroadcastReceiver(
            userManagerWrapper = userManagerWrapper,
            onStartSyncData = onStartSyncData,
            onManagedProfileResultChange = onManagedProfileResultChange,
        )

        val launcherAppsCallback = getLauncherAppsCallback(
            userManagerWrapper = userManagerWrapper,
            onPackageRemoved = onPackageRemoved,
            onPackageAdded = onPackageAdded,
            onPackageChanged = onPackageChanged,
            onShortcutsChanged = onShortcutsChanged,
        )

        val lifecycleEventObserver = LifecycleEventObserver { lifecycleOwner, event ->
            lifecycleOwner.lifecycleScope.launch {
                when (event) {
                    Lifecycle.Event.ON_START -> {
                        if (syncData && pinItemRequestWrapper.getPinItemRequest() == null) {
                            ContextCompat.registerReceiver(
                                context,
                                managedProfileBroadcastReceiver,
                                IntentFilter().apply {
                                    addAction(Intent.ACTION_MANAGED_PROFILE_AVAILABLE)
                                    addAction(Intent.ACTION_MANAGED_PROFILE_UNAVAILABLE)
                                    addAction(Intent.ACTION_MANAGED_PROFILE_REMOVED)
                                    addAction(Intent.ACTION_MANAGED_PROFILE_ADDED)
                                    addAction(Intent.ACTION_MANAGED_PROFILE_UNLOCKED)
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                                        addAction(Intent.ACTION_PROFILE_AVAILABLE)
                                        addAction(Intent.ACTION_PROFILE_UNAVAILABLE)
                                    }
                                },
                                ContextCompat.RECEIVER_NOT_EXPORTED,
                            )

                            shouldUnRegisterManagedProfileBroadcastReceiver = true

                            shouldUnbindEblanNotificationListenerService = context.bindService(
                                eblanNotificationListenerIntent,
                                eblanNotificationListenerServiceConnection,
                                Context.BIND_AUTO_CREATE,
                            )

                            launcherAppsWrapper.registerCallback(
                                callback = launcherAppsCallback,
                                handler = Handler(Looper.getMainLooper()),
                            )

                            onStartSyncData()
                        }

                        appWidgetHost.startListening()
                    }

                    Lifecycle.Event.ON_STOP -> {
                        if (syncData && pinItemRequestWrapper.getPinItemRequest() == null) {
                            if (shouldUnRegisterManagedProfileBroadcastReceiver) {
                                context.unregisterReceiver(managedProfileBroadcastReceiver)

                                shouldUnRegisterManagedProfileBroadcastReceiver = false
                            }

                            if (shouldUnbindEblanNotificationListenerService) {
                                context.unbindService(eblanNotificationListenerServiceConnection)

                                shouldUnbindEblanNotificationListenerService = false
                            }

                            launcherAppsWrapper.unregisterCallback(callback = launcherAppsCallback)

                            onStopSyncData()
                        }

                        appWidgetHost.stopListening()
                    }

                    else -> Unit
                }
            }
        }

        lifecycleOwner.lifecycle.addObserver(lifecycleEventObserver)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(lifecycleEventObserver)

            if (syncData && pinItemRequestWrapper.getPinItemRequest() == null) {
                launcherAppsWrapper.unregisterCallback(callback = launcherAppsCallback)

                onStopSyncData()
            }

            appWidgetHost.stopListening()
        }
    }
}

private fun getEblanNotificationListenerServiceConnection(
    lifecycleOwner: LifecycleOwner,
    onStatusBarNotificationsChange: (Map<String, Int>) -> Unit,
): ServiceConnection = object : ServiceConnection {
    override fun onServiceConnected(name: ComponentName, service: IBinder) {
        val listener =
            (service as EblanNotificationListenerService.LocalBinder).getService()

        lifecycleOwner.lifecycleScope.launch {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                listener.statusBarNotifications.collect {
                    onStatusBarNotificationsChange(it)
                }
            }
        }
    }

    override fun onServiceDisconnected(name: ComponentName) {}
}

private fun getManagedProfileBroadcastReceiver(
    userManagerWrapper: AndroidUserManagerWrapper,
    onStartSyncData: () -> Unit,
    onManagedProfileResultChange: (ManagedProfileResult?) -> Unit,
): BroadcastReceiver = object : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val userHandle = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(
                Intent.EXTRA_USER,
                UserHandle::class.java,
            )
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(Intent.EXTRA_USER)
        }

        if (userHandle != null) {
            onStartSyncData()

            onManagedProfileResultChange(
                ManagedProfileResult(
                    serialNumber = userManagerWrapper.getSerialNumberForUser(userHandle = userHandle),
                    isQuiteModeEnabled = userManagerWrapper.isQuietModeEnabled(userHandle = userHandle),
                ),
            )
        }
    }
}

private fun getLauncherAppsCallback(
    userManagerWrapper: AndroidUserManagerWrapper,
    onPackageRemoved: (
        serialNumber: Long,
        packageName: String,
    ) -> Unit,
    onPackageAdded: (
        serialNumber: Long,
        packageName: String,
    ) -> Unit,
    onPackageChanged: (
        serialNumber: Long,
        packageName: String,
    ) -> Unit,
    onShortcutsChanged: (
        serialNumber: Long,
        packageName: String,
    ) -> Unit,
): LauncherApps.Callback = object : LauncherApps.Callback() {
    override fun onPackageRemoved(packageName: String?, user: UserHandle?) {
        if (packageName != null && user != null) {
            onPackageRemoved(
                userManagerWrapper.getSerialNumberForUser(userHandle = user),
                packageName,
            )
        }
    }

    override fun onPackageAdded(packageName: String?, user: UserHandle?) {
        if (packageName != null && user != null) {
            onPackageAdded(
                userManagerWrapper.getSerialNumberForUser(userHandle = user),
                packageName,
            )
        }
    }

    override fun onPackageChanged(packageName: String?, user: UserHandle?) {
        if (packageName != null && user != null) {
            onPackageChanged(
                userManagerWrapper.getSerialNumberForUser(userHandle = user),
                packageName,
            )
        }
    }

    override fun onPackagesAvailable(
        packageNames: Array<out String>?,
        user: UserHandle?,
        replacing: Boolean,
    ) {
    }

    override fun onPackagesUnavailable(
        packageNames: Array<out String>?,
        user: UserHandle?,
        replacing: Boolean,
    ) {
    }

    override fun onShortcutsChanged(
        packageName: String,
        shortcuts: MutableList<ShortcutInfo>,
        user: UserHandle,
    ) {
        onShortcutsChanged(
            userManagerWrapper.getSerialNumberForUser(userHandle = user),
            packageName,
        )
    }
}
