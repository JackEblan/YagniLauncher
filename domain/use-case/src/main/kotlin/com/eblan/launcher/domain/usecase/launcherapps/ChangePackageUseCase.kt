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
package com.eblan.launcher.domain.usecase.launcherapps

import com.eblan.launcher.domain.common.dispatcher.Dispatcher
import com.eblan.launcher.domain.common.dispatcher.EblanDispatchers
import com.eblan.launcher.domain.framework.AppWidgetManagerWrapper
import com.eblan.launcher.domain.framework.FileManager
import com.eblan.launcher.domain.framework.IconPackManager
import com.eblan.launcher.domain.framework.LauncherAppsWrapper
import com.eblan.launcher.domain.framework.PackageManagerWrapper
import com.eblan.launcher.domain.model.EblanShortcutConfig
import com.eblan.launcher.domain.model.FastLauncherAppsActivityInfo
import com.eblan.launcher.domain.repository.ApplicationInfoGridItemRepository
import com.eblan.launcher.domain.repository.EblanAppWidgetProviderInfoRepository
import com.eblan.launcher.domain.repository.EblanApplicationInfoRepository
import com.eblan.launcher.domain.repository.EblanShortcutConfigRepository
import com.eblan.launcher.domain.repository.EblanShortcutInfoRepository
import com.eblan.launcher.domain.repository.ShortcutConfigGridItemRepository
import com.eblan.launcher.domain.repository.ShortcutInfoGridItemRepository
import com.eblan.launcher.domain.repository.UserDataRepository
import com.eblan.launcher.domain.repository.WidgetGridItemRepository
import com.eblan.launcher.domain.usecase.iconpack.cacheIconPackFile
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

class ChangePackageUseCase @Inject constructor(
    private val userDataRepository: UserDataRepository,
    private val packageManagerWrapper: PackageManagerWrapper,
    private val eblanApplicationInfoRepository: EblanApplicationInfoRepository,
    private val applicationInfoGridItemRepository: ApplicationInfoGridItemRepository,
    private val launcherAppsWrapper: LauncherAppsWrapper,
    private val eblanAppWidgetProviderInfoRepository: EblanAppWidgetProviderInfoRepository,
    private val appWidgetManagerWrapper: AppWidgetManagerWrapper,
    private val eblanShortcutInfoRepository: EblanShortcutInfoRepository,
    private val shortcutInfoGridItemRepository: ShortcutInfoGridItemRepository,
    private val shortcutConfigGridItemRepository: ShortcutConfigGridItemRepository,
    private val eblanShortcutConfigRepository: EblanShortcutConfigRepository,
    private val fileManager: FileManager,
    private val iconPackManager: IconPackManager,
    private val widgetGridItemRepository: WidgetGridItemRepository,
    @param:Dispatcher(EblanDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
) {
    suspend operator fun invoke(
        serialNumber: Long,
        packageName: String,
    ) {
        withContext(ioDispatcher) {
            val userData = userDataRepository.userData.first()

            if (!userData.experimentalSettings.syncData) return@withContext

            updateEblanApplicationInfo(
                packageName = packageName,
                serialNumber = serialNumber,
            )

            updateEblanAppWidgetProviderInfo(
                serialNumber = serialNumber,
                packageName = packageName,
            )

            updateEblanShortcutInfo(
                serialNumber = serialNumber,
                packageName = packageName,
            )

            updateIconPackInfos(
                serialNumber = serialNumber,
                packageName = packageName,
            )
        }
    }

    private suspend fun updateEblanApplicationInfo(
        packageName: String,
        serialNumber: Long,
    ) {
        val launcherAppsActivityInfosByPackageName = launcherAppsWrapper.getActivityList(
            serialNumber = serialNumber,
            packageName = packageName,
        )

        val newEblanShortcutConfigs = mutableListOf<EblanShortcutConfig>()

        val oldSyncEblanApplicationInfosByPackageName =
            eblanApplicationInfoRepository.getEblanApplicationInfosByPackageName(
                serialNumber = serialNumber,
                packageName = packageName,
            ).map { eblanApplicationInfo ->
                eblanApplicationInfo.toSyncEblanApplicationInfo()
            }

        val newSyncEblanApplicationInfosByPackageName = buildList {
            launcherAppsActivityInfosByPackageName.forEach { launcherAppsActivityInfo ->
                currentCoroutineContext().ensureActive()

                newEblanShortcutConfigs.addAll(
                    launcherAppsWrapper.getShortcutConfigActivityList(
                        serialNumber = launcherAppsActivityInfo.serialNumber,
                        packageName = launcherAppsActivityInfo.packageName,
                    ).map { shortcutConfigActivityInfo ->
                        currentCoroutineContext().ensureActive()

                        shortcutConfigActivityInfo.toEblanShortcutConfig(
                            fileManager = fileManager,
                            packageManagerWrapper = packageManagerWrapper,
                        )
                    },
                )

                add(launcherAppsActivityInfo.toSyncEblanApplicationInfo())
            }
        }

        if (oldSyncEblanApplicationInfosByPackageName.toSet() != newSyncEblanApplicationInfosByPackageName.toSet()) {
            val newDeleteEblanApplicationInfos =
                newSyncEblanApplicationInfosByPackageName.map { syncEblanApplicationInfo ->
                    syncEblanApplicationInfo.toDeleteEblanApplicationInfo()
                }.toSet()

            val oldDeleteEblanApplicationInfos =
                oldSyncEblanApplicationInfosByPackageName.map { syncEblanApplicationInfo ->
                    syncEblanApplicationInfo.toDeleteEblanApplicationInfo()
                }
                    .filter { deleteEblanApplicationInfo -> deleteEblanApplicationInfo !in newDeleteEblanApplicationInfos }

            eblanApplicationInfoRepository.upsertSyncEblanApplicationInfos(
                syncEblanApplicationInfos = newSyncEblanApplicationInfosByPackageName,
            )

            eblanApplicationInfoRepository.deleteSyncEblanApplicationInfos(
                deleteEblanApplicationInfos = oldDeleteEblanApplicationInfos,
            )

            deleteEblanApplicationInfoIcons(
                eblanApplicationInfos = eblanApplicationInfoRepository.getEblanApplicationInfos(),
                eblanAppWidgetProviderInfos = eblanAppWidgetProviderInfoRepository.getEblanAppWidgetProviderInfos(),
                oldDeleteEblanApplicationInfos = oldDeleteEblanApplicationInfos,
            )
        }

        updateApplicationInfoGridItems(
            eblanApplicationInfos = eblanApplicationInfoRepository.getEblanApplicationInfos(),
            applicationInfoGridItemRepository = applicationInfoGridItemRepository,
        )

        updateEblanShortcutConfigs(
            serialNumber = serialNumber,
            packageName = packageName,
            newEblanShortcutConfigs = newEblanShortcutConfigs,
        )
    }

    private suspend fun updateEblanAppWidgetProviderInfo(
        serialNumber: Long,
        packageName: String,
    ) {
        if (!packageManagerWrapper.hasSystemFeatureAppWidgets) return

        val appWidgetManagerAppWidgetProviderInfosByPackageName =
            appWidgetManagerWrapper.getInstalledProviders()
                .filter { appWidgetManagerAppWidgetProviderInfo ->
                    appWidgetManagerAppWidgetProviderInfo.serialNumber == serialNumber && appWidgetManagerAppWidgetProviderInfo.packageName == packageName
                }

        val oldEblanAppWidgetProviderInfosByPackageName =
            eblanAppWidgetProviderInfoRepository.getEblanAppWidgetProviderInfos()
                .filter { appWidgetManagerAppWidgetProviderInfo ->
                    appWidgetManagerAppWidgetProviderInfo.serialNumber == serialNumber && appWidgetManagerAppWidgetProviderInfo.packageName == packageName
                }

        val newEblanAppWidgetProviderInfosByPackageName =
            appWidgetManagerAppWidgetProviderInfosByPackageName.map { appWidgetManagerAppWidgetProviderInfo ->
                currentCoroutineContext().ensureActive()

                appWidgetManagerAppWidgetProviderInfo.toEblanAppWidgetProviderInfo(
                    fileManager = fileManager,
                    packageManagerWrapper = packageManagerWrapper,
                )
            }

        if (oldEblanAppWidgetProviderInfosByPackageName.toSet() != newEblanAppWidgetProviderInfosByPackageName.toSet()) {
            val newDeleteEblanAppWidgetProviderInfos =
                newEblanAppWidgetProviderInfosByPackageName.map { eblanAppWidgetProviderInfo ->
                    eblanAppWidgetProviderInfo.toDeleteEblanAppWidgetProviderInfo()
                }.toSet()

            val oldDeleteEblanAppWidgetProviderInfos =
                oldEblanAppWidgetProviderInfosByPackageName.map { eblanAppWidgetProviderInfo ->
                    eblanAppWidgetProviderInfo.toDeleteEblanAppWidgetProviderInfo()
                }.filter { deleteEblanAppWidgetProviderInfo ->
                    deleteEblanAppWidgetProviderInfo !in newDeleteEblanAppWidgetProviderInfos
                }

            eblanAppWidgetProviderInfoRepository.upsertEblanAppWidgetProviderInfos(
                eblanAppWidgetProviderInfos = newEblanAppWidgetProviderInfosByPackageName,
            )

            eblanAppWidgetProviderInfoRepository.deleteEblanAppWidgetProviderInfos(
                deleteEblanAppWidgetProviderInfos = oldDeleteEblanAppWidgetProviderInfos,
            )

            deleteEblanAppWidgetProviderInfoIcons(
                eblanApplicationInfos = eblanApplicationInfoRepository.getEblanApplicationInfos(),
                eblanAppWidgetProviderInfos = eblanAppWidgetProviderInfoRepository.getEblanAppWidgetProviderInfos(),
                oldDeleteEblanAppWidgetProviderInfos = oldDeleteEblanAppWidgetProviderInfos,
            )

            updateWidgetGridItems(
                eblanAppWidgetProviderInfos = eblanAppWidgetProviderInfoRepository.getEblanAppWidgetProviderInfos(),
                fileManager = fileManager,
                packageManagerWrapper = packageManagerWrapper,
                widgetGridItemRepository = widgetGridItemRepository,
            )
        }
    }

    private suspend fun updateEblanShortcutInfo(
        serialNumber: Long,
        packageName: String,
    ) {
        if (!launcherAppsWrapper.hasShortcutHostPermission) return

        val launcherAppsShortcutInfosByPackageName = launcherAppsWrapper.getShortcutsByPackageName(
            serialNumber = serialNumber,
            packageName = packageName,
        ) ?: return

        val oldEblanShortcutInfosByPackageName =
            eblanShortcutInfoRepository.getEblanShortcutInfos(
                serialNumber = serialNumber,
                packageName = packageName,
            )

        val newEblanShortcutInfosByPackageName =
            launcherAppsShortcutInfosByPackageName.map { launcherAppsShortcutInfo ->
                currentCoroutineContext().ensureActive()

                launcherAppsShortcutInfo.toEblanShortcutInfo()
            }

        if (oldEblanShortcutInfosByPackageName.toSet() != newEblanShortcutInfosByPackageName.toSet()) {
            val newDeleteEblanShortcutInfos =
                newEblanShortcutInfosByPackageName.map { eblanShortcutInfo ->
                    eblanShortcutInfo.toDeleteEblanShortcutInfo()
                }.toSet()

            val oldDeleteEblanShortcutInfos =
                oldEblanShortcutInfosByPackageName.map { eblanShortcutInfo ->
                    eblanShortcutInfo.toDeleteEblanShortcutInfo()
                }.filter { deleteEblanShortcutInfo ->
                    deleteEblanShortcutInfo !in newDeleteEblanShortcutInfos
                }

            eblanShortcutInfoRepository.upsertEblanShortcutInfos(
                eblanShortcutInfos = newEblanShortcutInfosByPackageName,
            )

            eblanShortcutInfoRepository.deleteEblanShortcutInfos(
                deleteEblanShortcutInfos = oldDeleteEblanShortcutInfos,
            )

            deleteEblanShortInfoIcons(oldDeleteEblanShortcutInfos = oldDeleteEblanShortcutInfos)

            updateShortcutInfoGridItems(
                eblanShortcutInfos = eblanShortcutInfoRepository.getEblanShortcutInfos(),
                shortcutInfoGridItemRepository = shortcutInfoGridItemRepository,
                fileManager = fileManager,
                packageManagerWrapper = packageManagerWrapper,
            )
        }
    }

    private suspend fun updateEblanShortcutConfigs(
        serialNumber: Long,
        packageName: String,
        newEblanShortcutConfigs: List<EblanShortcutConfig>,
    ) {
        val oldEblanShortcutConfigsByPackageName =
            eblanShortcutConfigRepository.getEblanShortcutConfigsByPackageName(
                serialNumber = serialNumber,
                packageName = packageName,
            )

        if (oldEblanShortcutConfigsByPackageName.toSet() != newEblanShortcutConfigs.toSet()) {
            val newDeleteEblanShortcutConfigs = newEblanShortcutConfigs.map { eblanShortcutConfig ->
                eblanShortcutConfig.toDeleteEblanShortcutConfig()
            }.toSet()

            val oldDeleteEblanShortcutConfigs =
                oldEblanShortcutConfigsByPackageName.map { eblanShortcutConfig ->
                    eblanShortcutConfig.toDeleteEblanShortcutConfig()
                }.filter { deleteEblanShortcutConfig ->
                    deleteEblanShortcutConfig !in newDeleteEblanShortcutConfigs
                }

            eblanShortcutConfigRepository.upsertEblanShortcutConfigs(
                eblanShortcutConfigs = newEblanShortcutConfigs,
            )

            eblanShortcutConfigRepository.deleteEblanShortcutConfigs(
                deleteEblanShortcutConfigs = oldDeleteEblanShortcutConfigs,
            )

            deleteEblanShortcutConfigIcons(oldDeleteEblanShortcutConfigs = oldDeleteEblanShortcutConfigs)

            updateShortcutConfigGridItems(
                eblanShortcutConfigs = eblanShortcutConfigRepository.getEblanShortcutConfigs(),
                shortcutConfigGridItemRepository = shortcutConfigGridItemRepository,
                fileManager = fileManager,
                packageManagerWrapper = packageManagerWrapper,
            )
        }
    }

    private suspend fun updateIconPackInfos(
        serialNumber: Long,
        packageName: String,
    ) {
        val iconPackInfoPackageName =
            userDataRepository.userData.first().generalSettings.iconPackInfoPackageName

        if (iconPackInfoPackageName.isEmpty()) return

        val oldFastEblanLauncherAppsActivityInfo =
            eblanApplicationInfoRepository.getEblanApplicationInfosByPackageName(
                serialNumber = serialNumber,
                packageName = packageName,
            ).map { eblanApplicationInfo ->
                FastLauncherAppsActivityInfo(
                    serialNumber = eblanApplicationInfo.serialNumber,
                    componentName = eblanApplicationInfo.componentName,
                    packageName = eblanApplicationInfo.packageName,
                    lastUpdateTime = eblanApplicationInfo.lastUpdateTime,
                )
            }

        val fastLauncherAppsActivityInfos = launcherAppsWrapper.getFastActivityList(
            serialNumber = serialNumber,
            packageName = packageName,
        )

        val iconPackInfoDirectory = File(
            fileManager.getFilesDirectory(name = FileManager.ICON_PACKS_DIR),
            iconPackInfoPackageName,
        ).apply { if (!exists()) mkdirs() }

        val appFilter =
            iconPackManager.getIconPackInfoComponents(packageName = iconPackInfoPackageName)

        val installedComponentHashCodes = buildSet {
            fastLauncherAppsActivityInfos.forEach { fastLauncherAppsActivityInfo ->
                currentCoroutineContext().ensureActive()

                val file = File(
                    iconPackInfoDirectory,
                    fileManager.getHashedFileName(name = fastLauncherAppsActivityInfo.componentName),
                )

                cacheIconPackFile(
                    iconPackManager = iconPackManager,
                    appFilter = appFilter,
                    iconPackInfoPackageName = iconPackInfoPackageName,
                    file = file,
                    componentName = fastLauncherAppsActivityInfo.componentName,
                )

                add(fileManager.getHashedFileName(name = fastLauncherAppsActivityInfo.componentName))
            }
        }

        if (oldFastEblanLauncherAppsActivityInfo.toSet() != fastLauncherAppsActivityInfos.toSet()) {
            iconPackInfoDirectory.listFiles()
                ?.filter {
                    currentCoroutineContext().ensureActive()

                    it.isFile && it.name !in installedComponentHashCodes
                }
                ?.forEach {
                    currentCoroutineContext().ensureActive()

                    it.delete()
                }
        }
    }
}
