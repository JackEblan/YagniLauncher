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
import com.eblan.launcher.domain.model.ApplicationInfoGridItem
import com.eblan.launcher.domain.model.Associate
import com.eblan.launcher.domain.model.DeleteEblanAppWidgetProviderInfo
import com.eblan.launcher.domain.model.DeleteEblanApplicationInfo
import com.eblan.launcher.domain.model.DeleteEblanShortcutConfig
import com.eblan.launcher.domain.model.DeleteEblanShortcutInfo
import com.eblan.launcher.domain.model.EblanAction
import com.eblan.launcher.domain.model.EblanActionType
import com.eblan.launcher.domain.model.EblanAppWidgetProviderInfo
import com.eblan.launcher.domain.model.EblanApplicationInfo
import com.eblan.launcher.domain.model.EblanShortcutConfig
import com.eblan.launcher.domain.model.EblanShortcutInfo
import com.eblan.launcher.domain.model.ExperimentalSettings
import com.eblan.launcher.domain.model.FastAppWidgetManagerAppWidgetProviderInfo
import com.eblan.launcher.domain.model.FastLauncherAppsActivityInfo
import com.eblan.launcher.domain.model.FastLauncherAppsShortcutInfo
import com.eblan.launcher.domain.model.HomeSettings
import com.eblan.launcher.domain.model.SyncEblanApplicationInfo
import com.eblan.launcher.domain.model.UserData
import com.eblan.launcher.domain.repository.ApplicationInfoGridItemRepository
import com.eblan.launcher.domain.repository.EblanAppWidgetProviderInfoRepository
import com.eblan.launcher.domain.repository.EblanApplicationInfoRepository
import com.eblan.launcher.domain.repository.EblanShortcutConfigRepository
import com.eblan.launcher.domain.repository.EblanShortcutInfoRepository
import com.eblan.launcher.domain.repository.ShortcutConfigGridItemRepository
import com.eblan.launcher.domain.repository.ShortcutInfoGridItemRepository
import com.eblan.launcher.domain.repository.UserDataRepository
import com.eblan.launcher.domain.repository.WidgetGridItemRepository
import com.eblan.launcher.domain.usecase.iconpack.updateIconPackInfos
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class SyncDataUseCase @Inject constructor(
    private val userDataRepository: UserDataRepository,
    private val eblanApplicationInfoRepository: EblanApplicationInfoRepository,
    private val launcherAppsWrapper: LauncherAppsWrapper,
    private val fileManager: FileManager,
    private val eblanAppWidgetProviderInfoRepository: EblanAppWidgetProviderInfoRepository,
    private val appWidgetManagerWrapper: AppWidgetManagerWrapper,
    private val packageManagerWrapper: PackageManagerWrapper,
    private val eblanShortcutInfoRepository: EblanShortcutInfoRepository,
    private val applicationInfoGridItemRepository: ApplicationInfoGridItemRepository,
    private val widgetGridItemRepository: WidgetGridItemRepository,
    private val shortcutInfoGridItemRepository: ShortcutInfoGridItemRepository,
    private val eblanShortcutConfigRepository: EblanShortcutConfigRepository,
    private val iconPackManager: IconPackManager,
    private val shortcutConfigGridItemRepository: ShortcutConfigGridItemRepository,
    @param:Dispatcher(EblanDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
) {
    suspend operator fun invoke() {
        withContext(ioDispatcher) {
            val userData = userDataRepository.userData.first()

            launch {
                updateEblanApplicationInfos(userData = userData)
            }

            launch {
                updateAppWidgetProviderInfos()
            }

            launch {
                updateEblanLauncherShortcutInfos()
            }

            launch {
                updateIconPackInfos(
                    iconPackInfoPackageName = userData.generalSettings.iconPackInfoPackageName,
                    fileManager = fileManager,
                    iconPackManager = iconPackManager,
                    fastLauncherAppsActivityInfos = launcherAppsWrapper.getFastActivityList(),
                )
            }
        }
    }

    private suspend fun updateEblanApplicationInfos(userData: UserData) {
        val oldFastEblanLauncherAppsActivityInfo =
            eblanApplicationInfoRepository.getEblanApplicationInfos().map { eblanApplicationInfo ->
                FastLauncherAppsActivityInfo(
                    serialNumber = eblanApplicationInfo.serialNumber,
                    componentName = eblanApplicationInfo.componentName,
                    packageName = eblanApplicationInfo.packageName,
                    lastUpdateTime = eblanApplicationInfo.lastUpdateTime,
                )
            }

        val newFastLauncherAppsActivityInfos = launcherAppsWrapper.getFastActivityList()

        if (oldFastEblanLauncherAppsActivityInfo.toSet() == newFastLauncherAppsActivityInfos.toSet()) return

        val newEblanShortcutConfigs = mutableSetOf<EblanShortcutConfig>()

        val oldSyncEblanApplicationInfos =
            eblanApplicationInfoRepository.getEblanApplicationInfos().map { eblanApplicationInfo ->
                SyncEblanApplicationInfo(
                    serialNumber = eblanApplicationInfo.serialNumber,
                    componentName = eblanApplicationInfo.componentName,
                    packageName = eblanApplicationInfo.packageName,
                    icon = eblanApplicationInfo.icon,
                    label = eblanApplicationInfo.label,
                    lastUpdateTime = eblanApplicationInfo.lastUpdateTime,
                )
            }

        val newSyncEblanApplicationInfos = buildList {
            launcherAppsWrapper.getActivityList().forEach { launcherAppsActivityInfo ->
                currentCoroutineContext().ensureActive()

                newEblanShortcutConfigs.addAll(
                    launcherAppsWrapper.getShortcutConfigActivityList(
                        serialNumber = launcherAppsActivityInfo.serialNumber,
                        packageName = launcherAppsActivityInfo.packageName,
                    ).map { shortcutConfigActivityInfo ->
                        currentCoroutineContext().ensureActive()

                        EblanShortcutConfig(
                            componentName = shortcutConfigActivityInfo.componentName,
                            packageName = shortcutConfigActivityInfo.packageName,
                            serialNumber = shortcutConfigActivityInfo.serialNumber,
                            activityIcon = shortcutConfigActivityInfo.activityIcon,
                            activityLabel = shortcutConfigActivityInfo.activityLabel,
                            applicationIcon = launcherAppsActivityInfo.activityIcon,
                            applicationLabel = launcherAppsActivityInfo.activityLabel,
                        )
                    },
                )

                add(
                    SyncEblanApplicationInfo(
                        serialNumber = launcherAppsActivityInfo.serialNumber,
                        componentName = launcherAppsActivityInfo.componentName,
                        packageName = launcherAppsActivityInfo.packageName,
                        icon = launcherAppsActivityInfo.activityIcon,
                        label = launcherAppsActivityInfo.activityLabel,
                        lastUpdateTime = launcherAppsActivityInfo.lastUpdateTime,
                    ),
                )
            }
        }

        val newDeleteEblanApplicationInfos =
            newSyncEblanApplicationInfos.map { syncEblanApplicationInfo ->
                DeleteEblanApplicationInfo(
                    serialNumber = syncEblanApplicationInfo.serialNumber,
                    componentName = syncEblanApplicationInfo.componentName,
                    packageName = syncEblanApplicationInfo.packageName,
                    icon = syncEblanApplicationInfo.icon,
                )
            }.toSet()

        val oldDeleteEblanApplicationInfos =
            oldSyncEblanApplicationInfos.map { syncEblanApplicationInfo ->
                DeleteEblanApplicationInfo(
                    serialNumber = syncEblanApplicationInfo.serialNumber,
                    componentName = syncEblanApplicationInfo.componentName,
                    packageName = syncEblanApplicationInfo.packageName,
                    icon = syncEblanApplicationInfo.icon,
                )
            }
                .filter { deleteEblanApplicationInfo -> deleteEblanApplicationInfo !in newDeleteEblanApplicationInfos }

        eblanApplicationInfoRepository.upsertSyncEblanApplicationInfos(
            syncEblanApplicationInfos = newSyncEblanApplicationInfos,
        )

        eblanApplicationInfoRepository.deleteSyncEblanApplicationInfos(
            deleteEblanApplicationInfos = oldDeleteEblanApplicationInfos,
        )

        oldDeleteEblanApplicationInfos.forEach { oldDeleteEblanApplicationInfo ->
            currentCoroutineContext().ensureActive()

            val icon = oldDeleteEblanApplicationInfo.icon

            val hasNoIconReference =
                icon != null && eblanApplicationInfoRepository.getEblanApplicationInfos()
                    .none { eblanApplicationInfo ->
                        currentCoroutineContext().ensureActive()

                        eblanApplicationInfo.icon == icon
                    } &&
                    eblanAppWidgetProviderInfoRepository.getEblanAppWidgetProviderInfos()
                        .none { eblanAppWidgetProviderInfo ->
                            currentCoroutineContext().ensureActive()
                            eblanAppWidgetProviderInfo.applicationIcon == icon
                        }

            if (hasNoIconReference) {
                val iconFile = File(icon)

                if (iconFile.exists()) {
                    iconFile.delete()
                }
            }
        }

        updateEblanShortcutConfigs(newEblanShortcutConfigs = newEblanShortcutConfigs)

        updateApplicationInfoGridItems(
            eblanApplicationInfos = eblanApplicationInfoRepository.getEblanApplicationInfos(),
            applicationInfoGridItemRepository = applicationInfoGridItemRepository,
        )

        insertApplicationInfoGridItems(
            eblanApplicationInfos = eblanApplicationInfoRepository.getEblanApplicationInfos(),
            experimentalSettings = userData.experimentalSettings,
            homeSettings = userData.homeSettings,
        )
    }

    private suspend fun updateAppWidgetProviderInfos() {
        if (!packageManagerWrapper.hasSystemFeatureAppWidgets) return

        val oldFastAppWidgetManagerAppWidgetProviderInfos =
            eblanAppWidgetProviderInfoRepository.getEblanAppWidgetProviderInfos()
                .map { eblanAppWidgetProviderInfo ->
                    FastAppWidgetManagerAppWidgetProviderInfo(
                        componentName = eblanAppWidgetProviderInfo.componentName,
                        serialNumber = eblanAppWidgetProviderInfo.serialNumber,
                        configure = eblanAppWidgetProviderInfo.configure,
                        packageName = eblanAppWidgetProviderInfo.packageName,
                        targetCellWidth = eblanAppWidgetProviderInfo.targetCellWidth,
                        targetCellHeight = eblanAppWidgetProviderInfo.targetCellHeight,
                        minWidth = eblanAppWidgetProviderInfo.minWidth,
                        minHeight = eblanAppWidgetProviderInfo.minHeight,
                        resizeMode = eblanAppWidgetProviderInfo.resizeMode,
                        minResizeWidth = eblanAppWidgetProviderInfo.minResizeWidth,
                        minResizeHeight = eblanAppWidgetProviderInfo.minResizeHeight,
                        maxResizeWidth = eblanAppWidgetProviderInfo.maxResizeWidth,
                        maxResizeHeight = eblanAppWidgetProviderInfo.maxResizeHeight,
                        lastUpdateTime = eblanAppWidgetProviderInfo.lastUpdateTime,
                        label = eblanAppWidgetProviderInfo.label,
                        description = eblanAppWidgetProviderInfo.description,
                    )
                }

        val newFastAppWidgetManagerAppWidgetProviderInfos =
            appWidgetManagerWrapper.getFastInstalledProviders()

        if (oldFastAppWidgetManagerAppWidgetProviderInfos.toSet() == newFastAppWidgetManagerAppWidgetProviderInfos.toSet()) return

        val appWidgetManagerAppWidgetProviderInfos = appWidgetManagerWrapper.getInstalledProviders()

        val oldEblanAppWidgetProviderInfos =
            eblanAppWidgetProviderInfoRepository.getEblanAppWidgetProviderInfos()

        val newEblanAppWidgetProviderInfos =
            appWidgetManagerAppWidgetProviderInfos.map { appWidgetManagerAppWidgetProviderInfo ->
                currentCoroutineContext().ensureActive()

                val directory = fileManager.getFilesDirectory(FileManager.ICONS_DIR)

                val componentName =
                    packageManagerWrapper.getComponentName(packageName = appWidgetManagerAppWidgetProviderInfo.packageName)

                val icon = if (componentName != null) {
                    val file = File(
                        directory,
                        fileManager.getHashedFileName(name = "${appWidgetManagerAppWidgetProviderInfo.serialNumber}:$componentName"),
                    )

                    file.absolutePath
                } else {
                    val file = File(
                        directory,
                        fileManager.getHashedFileName(name = "${appWidgetManagerAppWidgetProviderInfo.serialNumber}:${appWidgetManagerAppWidgetProviderInfo.packageName}"),
                    )

                    packageManagerWrapper.getApplicationIcon(
                        packageName = appWidgetManagerAppWidgetProviderInfo.packageName,
                        file = file,
                    )
                }

                EblanAppWidgetProviderInfo(
                    componentName = appWidgetManagerAppWidgetProviderInfo.componentName,
                    serialNumber = appWidgetManagerAppWidgetProviderInfo.serialNumber,
                    configure = appWidgetManagerAppWidgetProviderInfo.configure,
                    packageName = appWidgetManagerAppWidgetProviderInfo.packageName,
                    targetCellWidth = appWidgetManagerAppWidgetProviderInfo.targetCellWidth,
                    targetCellHeight = appWidgetManagerAppWidgetProviderInfo.targetCellHeight,
                    minWidth = appWidgetManagerAppWidgetProviderInfo.minWidth,
                    minHeight = appWidgetManagerAppWidgetProviderInfo.minHeight,
                    resizeMode = appWidgetManagerAppWidgetProviderInfo.resizeMode,
                    minResizeWidth = appWidgetManagerAppWidgetProviderInfo.minResizeWidth,
                    minResizeHeight = appWidgetManagerAppWidgetProviderInfo.minResizeHeight,
                    maxResizeWidth = appWidgetManagerAppWidgetProviderInfo.maxResizeWidth,
                    maxResizeHeight = appWidgetManagerAppWidgetProviderInfo.maxResizeHeight,
                    preview = appWidgetManagerAppWidgetProviderInfo.preview,
                    applicationIcon = icon,
                    applicationLabel = packageManagerWrapper.getApplicationLabel(
                        packageName = appWidgetManagerAppWidgetProviderInfo.packageName,
                    ).toString(),
                    lastUpdateTime = appWidgetManagerAppWidgetProviderInfo.lastUpdateTime,
                    label = appWidgetManagerAppWidgetProviderInfo.label,
                    description = appWidgetManagerAppWidgetProviderInfo.description,
                )
            }

        val newDeleteEblanAppWidgetProviderInfos =
            newEblanAppWidgetProviderInfos.map { eblanAppWidgetProviderInfo ->
                DeleteEblanAppWidgetProviderInfo(
                    componentName = eblanAppWidgetProviderInfo.componentName,
                    serialNumber = eblanAppWidgetProviderInfo.serialNumber,
                    packageName = eblanAppWidgetProviderInfo.packageName,
                    preview = eblanAppWidgetProviderInfo.preview,
                    applicationIcon = eblanAppWidgetProviderInfo.applicationIcon,
                )
            }.toSet()

        val oldDeleteEblanAppWidgetProviderInfos =
            oldEblanAppWidgetProviderInfos.map { eblanAppWidgetProviderInfo ->
                DeleteEblanAppWidgetProviderInfo(
                    componentName = eblanAppWidgetProviderInfo.componentName,
                    serialNumber = eblanAppWidgetProviderInfo.serialNumber,
                    packageName = eblanAppWidgetProviderInfo.packageName,
                    preview = eblanAppWidgetProviderInfo.preview,
                    applicationIcon = eblanAppWidgetProviderInfo.applicationIcon,
                )
            }.filter { deleteEblanAppWidgetProviderInfo ->
                deleteEblanAppWidgetProviderInfo !in newDeleteEblanAppWidgetProviderInfos
            }

        eblanAppWidgetProviderInfoRepository.upsertEblanAppWidgetProviderInfos(
            eblanAppWidgetProviderInfos = newEblanAppWidgetProviderInfos,
        )

        eblanAppWidgetProviderInfoRepository.deleteEblanAppWidgetProviderInfos(
            deleteEblanAppWidgetProviderInfos = oldDeleteEblanAppWidgetProviderInfos,
        )

        oldDeleteEblanAppWidgetProviderInfos.forEach { deleteEblanAppWidgetProviderInfo ->
            currentCoroutineContext().ensureActive()

            val applicationIcon = deleteEblanAppWidgetProviderInfo.applicationIcon

            val hasNoIconReference = applicationIcon != null &&
                eblanAppWidgetProviderInfoRepository.getEblanAppWidgetProviderInfos()
                    .none { eblanAppWidgetProviderInfo ->
                        currentCoroutineContext().ensureActive()
                        eblanAppWidgetProviderInfo.applicationIcon == applicationIcon
                    } &&
                eblanApplicationInfoRepository.getEblanApplicationInfos()
                    .none { eblanApplicationInfo ->
                        currentCoroutineContext().ensureActive()

                        eblanApplicationInfo.icon == applicationIcon
                    }

            if (hasNoIconReference) {
                val iconFile = File(applicationIcon)

                if (iconFile.exists()) {
                    iconFile.delete()
                }
            }

            deleteEblanAppWidgetProviderInfo.preview?.let { preview ->
                val previewFile = File(preview)

                if (previewFile.exists()) {
                    previewFile.delete()
                }
            }
        }

        updateWidgetGridItems(
            eblanAppWidgetProviderInfos = eblanAppWidgetProviderInfoRepository.getEblanAppWidgetProviderInfos(),
            fileManager = fileManager,
            packageManagerWrapper = packageManagerWrapper,
            widgetGridItemRepository = widgetGridItemRepository,
        )
    }

    private suspend fun updateEblanLauncherShortcutInfos() {
        if (!launcherAppsWrapper.hasShortcutHostPermission) return

        val oldFastLauncherAppsShortcutInfos =
            eblanShortcutInfoRepository.getEblanShortcutInfos().map { eblanShortcutInfo ->
                FastLauncherAppsShortcutInfo(
                    packageName = eblanShortcutInfo.packageName,
                    serialNumber = eblanShortcutInfo.serialNumber,
                    lastChangedTimestamp = eblanShortcutInfo.lastChangedTimestamp,
                )
            }

        val newFastLauncherAppsShortcutInfos = launcherAppsWrapper.getFastShortcuts()

        if (oldFastLauncherAppsShortcutInfos.toSet() == newFastLauncherAppsShortcutInfos?.toSet()) return

        val launcherAppsShortcutInfos = launcherAppsWrapper.getShortcuts() ?: return

        val oldEblanShortcutInfos = eblanShortcutInfoRepository.getEblanShortcutInfos()

        val newEblanShortcutInfos = launcherAppsShortcutInfos.map { launcherAppsShortcutInfo ->
            currentCoroutineContext().ensureActive()

            EblanShortcutInfo(
                shortcutId = launcherAppsShortcutInfo.shortcutId,
                serialNumber = launcherAppsShortcutInfo.serialNumber,
                packageName = launcherAppsShortcutInfo.packageName,
                shortLabel = launcherAppsShortcutInfo.shortLabel,
                longLabel = launcherAppsShortcutInfo.longLabel,
                icon = launcherAppsShortcutInfo.icon,
                shortcutQueryFlag = launcherAppsShortcutInfo.shortcutQueryFlag,
                isEnabled = launcherAppsShortcutInfo.isEnabled,
                lastChangedTimestamp = launcherAppsShortcutInfo.lastChangedTimestamp,
            )
        }

        val newDeleteEblanShortcutInfos = newEblanShortcutInfos.map { eblanShortcutInfo ->
            DeleteEblanShortcutInfo(
                serialNumber = eblanShortcutInfo.serialNumber,
                shortcutId = eblanShortcutInfo.shortcutId,
                packageName = eblanShortcutInfo.packageName,
                icon = eblanShortcutInfo.icon,
            )
        }.toSet()

        val oldDeleteEblanShortcutInfos = oldEblanShortcutInfos.map { eblanShortcutInfo ->
            DeleteEblanShortcutInfo(
                serialNumber = eblanShortcutInfo.serialNumber,
                shortcutId = eblanShortcutInfo.shortcutId,
                packageName = eblanShortcutInfo.packageName,
                icon = eblanShortcutInfo.icon,
            )
        }.filter { deleteEblanShortcutInfo ->
            deleteEblanShortcutInfo !in newDeleteEblanShortcutInfos
        }

        eblanShortcutInfoRepository.upsertEblanShortcutInfos(
            eblanShortcutInfos = newEblanShortcutInfos,
        )

        eblanShortcutInfoRepository.deleteEblanShortcutInfos(
            deleteEblanShortcutInfos = oldDeleteEblanShortcutInfos,
        )

        oldDeleteEblanShortcutInfos.forEach { deleteEblanShortcutInfo ->
            currentCoroutineContext().ensureActive()

            val icon = deleteEblanShortcutInfo.icon

            if (icon != null) {
                val iconFile = File(icon)

                if (iconFile.exists()) iconFile.delete()
            }
        }

        updateShortcutInfoGridItems(
            eblanShortcutInfos = eblanShortcutInfoRepository.getEblanShortcutInfos(),
            shortcutInfoGridItemRepository = shortcutInfoGridItemRepository,
            fileManager = fileManager,
            packageManagerWrapper = packageManagerWrapper,
        )
    }

    private suspend fun updateEblanShortcutConfigs(
        newEblanShortcutConfigs: Set<EblanShortcutConfig>,
    ) {
        val oldEblanShortcutConfigs = eblanShortcutConfigRepository.getEblanShortcutConfigs()

        if (oldEblanShortcutConfigs.toSet() != newEblanShortcutConfigs) {
            val newDeleteEblanShortcutConfigs = newEblanShortcutConfigs.map { eblanShortcutConfig ->
                DeleteEblanShortcutConfig(
                    serialNumber = eblanShortcutConfig.serialNumber,
                    componentName = eblanShortcutConfig.componentName,
                    packageName = eblanShortcutConfig.packageName,
                    activityIcon = eblanShortcutConfig.activityIcon,
                )
            }.toSet()

            val oldDeleteEblanShortcutConfigs = oldEblanShortcutConfigs.map { eblanShortcutConfig ->
                DeleteEblanShortcutConfig(
                    serialNumber = eblanShortcutConfig.serialNumber,
                    componentName = eblanShortcutConfig.componentName,
                    packageName = eblanShortcutConfig.packageName,
                    activityIcon = eblanShortcutConfig.activityIcon,
                )
            }.filter { deleteEblanShortcutConfig ->
                deleteEblanShortcutConfig !in newDeleteEblanShortcutConfigs
            }

            eblanShortcutConfigRepository.upsertEblanShortcutConfigs(
                eblanShortcutConfigs = newEblanShortcutConfigs.toList(),
            )

            eblanShortcutConfigRepository.deleteEblanShortcutConfigs(
                deleteEblanShortcutConfigs = oldDeleteEblanShortcutConfigs,
            )

            oldDeleteEblanShortcutConfigs.forEach { deleteEblanShortcutConfig ->
                currentCoroutineContext().ensureActive()

                val activityIcon = deleteEblanShortcutConfig.activityIcon

                if (activityIcon != null) {
                    val activityIconFile = File(activityIcon)

                    if (activityIconFile.exists()) {
                        activityIconFile.delete()
                    }
                }
            }

            updateShortcutConfigGridItems(
                eblanShortcutConfigs = eblanShortcutConfigRepository.getEblanShortcutConfigs(),
                shortcutConfigGridItemRepository = shortcutConfigGridItemRepository,
                fileManager = fileManager,
                packageManagerWrapper = packageManagerWrapper,
            )
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    private suspend fun insertApplicationInfoGridItems(
        eblanApplicationInfos: List<EblanApplicationInfo>,
        experimentalSettings: ExperimentalSettings,
        homeSettings: HomeSettings,
    ) {
        if (!experimentalSettings.firstLaunch) return

        @OptIn(ExperimentalUuidApi::class)
        suspend fun insertApplicationInfoGridItem(
            index: Int,
            eblanApplicationInfo: EblanApplicationInfo,
            columns: Int,
            associate: Associate,
        ) {
            val startColumn = index % columns

            val startRow = index / columns

            applicationInfoGridItemRepository.insertApplicationInfoGridItem(
                applicationInfoGridItem = ApplicationInfoGridItem(
                    id = Uuid.random().toHexString(),
                    page = 0,
                    startColumn = startColumn,
                    startRow = startRow,
                    columnSpan = 1,
                    rowSpan = 1,
                    associate = associate,
                    componentName = eblanApplicationInfo.componentName,
                    packageName = eblanApplicationInfo.packageName,
                    icon = eblanApplicationInfo.icon,
                    label = eblanApplicationInfo.label,
                    override = false,
                    serialNumber = eblanApplicationInfo.serialNumber,
                    customIcon = null,
                    customLabel = null,
                    gridItemSettings = homeSettings.gridItemSettings,
                    doubleTap = EblanAction(
                        eblanActionType = EblanActionType.None,
                        serialNumber = 0L,
                        componentName = "",
                    ),
                    swipeUp = EblanAction(
                        eblanActionType = EblanActionType.None,
                        serialNumber = 0L,
                        componentName = "",
                    ),
                    swipeDown = EblanAction(
                        eblanActionType = EblanActionType.None,
                        serialNumber = 0L,
                        componentName = "",
                    ),
                    index = -1,
                    folderId = null,
                ),
            )
        }

        eblanApplicationInfos.take(homeSettings.columns * homeSettings.rows)
            .forEachIndexed { index, launcherAppsActivityInfo ->
                insertApplicationInfoGridItem(
                    index = index,
                    eblanApplicationInfo = launcherAppsActivityInfo,
                    columns = homeSettings.columns,
                    associate = Associate.Grid,
                )
            }

        eblanApplicationInfos.drop(homeSettings.columns * homeSettings.rows)
            .take(homeSettings.dockColumns * homeSettings.dockRows)
            .forEachIndexed { index, launcherAppsActivityInfo ->
                insertApplicationInfoGridItem(
                    index = index,
                    eblanApplicationInfo = launcherAppsActivityInfo,
                    columns = homeSettings.dockColumns,
                    associate = Associate.Dock,
                )
            }

        userDataRepository.updateExperimentalSettings(
            experimentalSettings = experimentalSettings.copy(
                firstLaunch = false,
            ),
        )
    }
}
