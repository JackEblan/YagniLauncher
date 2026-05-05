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
package com.eblan.launcher.framework.packagemanager

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.UserHandle
import com.eblan.launcher.domain.common.Dispatcher
import com.eblan.launcher.domain.common.EblanDispatchers
import com.eblan.launcher.domain.framework.PackageManagerWrapper
import com.eblan.launcher.domain.model.PackageManagerIconPackInfo
import com.eblan.launcher.framework.imageserializer.AndroidImageSerializer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

internal class DefaultPackageManagerWrapper @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val imageSerializer: AndroidImageSerializer,
    @param:Dispatcher(EblanDispatchers.Default) private val defaultDispatcher: CoroutineDispatcher,
) : PackageManagerWrapper, AndroidPackageManagerWrapper {
    private val packageManager = context.packageManager

    override val hasSystemFeatureAppWidgets
        get() = packageManager.hasSystemFeature(PackageManager.FEATURE_APP_WIDGETS)

    override suspend fun getApplicationIcon(
        packageName: String,
        file: File,
    ): String? = withContext(defaultDispatcher) {
        try {
            imageSerializer.createDrawablePath(
                drawable = packageManager.getApplicationIcon(
                    packageName,
                ),
                file = file,
            )

            file.absolutePath
        } catch (_: PackageManager.NameNotFoundException) {
            null
        }
    }

    override suspend fun getApplicationLabel(packageName: String): String? =
        withContext(defaultDispatcher) {
            try {
                val applicationInfo =
                    packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)

                packageManager.getApplicationLabel(applicationInfo).toString()
            } catch (_: PackageManager.NameNotFoundException) {
                null
            }
        }

    override fun getComponentName(packageName: String): String? {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)

        return launchIntent?.component?.flattenToString()
    }

    override fun isDefaultLauncher(): Boolean {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
        }

        val resolveInfo = packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)

        val defaultLauncherPackage = resolveInfo?.activityInfo?.packageName

        return defaultLauncherPackage == context.packageName
    }

    override suspend fun getIconPackInfos(): List<PackageManagerIconPackInfo> {
        val intents = listOf(
            Intent("app.lawnchair.icons.THEMED_ICON"),
            Intent("org.adw.ActivityStarter.THEMES"),
            Intent("com.novalauncher.THEME"),
            Intent("org.adw.launcher.THEMES"),
        )

        val resolveInfos = mutableSetOf<ResolveInfo>()

        return withContext(defaultDispatcher) {
            intents.forEach { intent ->
                resolveInfos.addAll(
                    packageManager.queryIntentActivities(
                        intent,
                        PackageManager.GET_META_DATA,
                    ),
                )
            }

            resolveInfos.map { resolveInfo ->
                PackageManagerIconPackInfo(
                    packageName = resolveInfo.activityInfo.applicationInfo.packageName,
                    icon = resolveInfo.activityInfo.applicationInfo.loadIcon(packageManager)
                        .let { drawable ->
                            imageSerializer.createByteArray(
                                drawable = drawable,
                            )
                        },
                    label = resolveInfo.activityInfo.applicationInfo.loadLabel(packageManager)
                        .toString(),
                )
            }.distinct()
        }
    }

    override fun getLastUpdateTime(packageName: String): Long = try {
        packageManager.getPackageInfo(packageName, 0).lastUpdateTime
    } catch (_: PackageManager.NameNotFoundException) {
        0L
    }

    override fun isSystem(flags: Int): Boolean =
        (flags and ApplicationInfo.FLAG_SYSTEM) != 0 || (flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0

    override fun getUserBadgedLabel(label: CharSequence, userHandle: UserHandle): CharSequence =
        packageManager.getUserBadgedLabel(label, userHandle)
}
