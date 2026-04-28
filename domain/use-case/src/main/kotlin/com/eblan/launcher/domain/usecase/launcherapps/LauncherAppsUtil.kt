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

import com.eblan.launcher.domain.common.IconKeyGenerator
import com.eblan.launcher.domain.framework.FileManager
import com.eblan.launcher.domain.framework.PackageManagerWrapper
import com.eblan.launcher.domain.grid.findAvailableRegionByPage
import com.eblan.launcher.domain.model.AppWidgetManagerAppWidgetProviderInfo
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
import com.eblan.launcher.domain.model.FastLauncherAppsActivityInfo
import com.eblan.launcher.domain.model.GridItem
import com.eblan.launcher.domain.model.GridItemData
import com.eblan.launcher.domain.model.HomeSettings
import com.eblan.launcher.domain.model.LauncherAppsActivityInfo
import com.eblan.launcher.domain.model.LauncherAppsShortcutInfo
import com.eblan.launcher.domain.model.ShortcutConfigActivityInfo
import com.eblan.launcher.domain.model.ShortcutConfigGridItem
import com.eblan.launcher.domain.model.ShortcutInfoGridItem
import com.eblan.launcher.domain.model.SyncEblanApplicationInfo
import com.eblan.launcher.domain.model.UpdateApplicationInfoGridItem
import com.eblan.launcher.domain.model.UpdateShortcutConfigGridItem
import com.eblan.launcher.domain.model.UpdateShortcutInfoGridItem
import com.eblan.launcher.domain.model.UpdateWidgetGridItem
import com.eblan.launcher.domain.model.WidgetGridItem
import com.eblan.launcher.domain.repository.ApplicationInfoGridItemRepository
import com.eblan.launcher.domain.repository.EblanAppWidgetProviderInfoRepository
import com.eblan.launcher.domain.repository.EblanApplicationInfoRepository
import com.eblan.launcher.domain.repository.EblanShortcutConfigRepository
import com.eblan.launcher.domain.repository.EblanShortcutInfoRepository
import com.eblan.launcher.domain.repository.ShortcutConfigGridItemRepository
import com.eblan.launcher.domain.repository.ShortcutInfoGridItemRepository
import com.eblan.launcher.domain.repository.WidgetGridItemRepository
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.first
import java.io.File
import javax.inject.Inject
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class LauncherAppsUtil @Inject internal constructor(
    private val fileManager: FileManager,
    private val packageManagerWrapper: PackageManagerWrapper,
    private val iconKeyGenerator: IconKeyGenerator,
    private val widgetGridItemRepository: WidgetGridItemRepository,
    private val eblanApplicationInfoRepository: EblanApplicationInfoRepository,
    private val eblanAppWidgetProviderInfoRepository: EblanAppWidgetProviderInfoRepository,
    private val applicationInfoGridItemRepository: ApplicationInfoGridItemRepository,
    private val eblanShortcutInfoRepository: EblanShortcutInfoRepository,
    private val shortcutInfoGridItemRepository: ShortcutInfoGridItemRepository,
    private val eblanShortcutConfigRepository: EblanShortcutConfigRepository,
    private val shortcutConfigGridItemRepository: ShortcutConfigGridItemRepository,
) {
    suspend fun deleteEblanApplicationInfoIcons(oldDeleteEblanApplicationInfos: List<DeleteEblanApplicationInfo>) {
        val eblanApplicationInfos = eblanApplicationInfoRepository.getEblanApplicationInfos()

        val eblanAppWidgetProviderInfos =
            eblanAppWidgetProviderInfoRepository.getEblanAppWidgetProviderInfos()

        oldDeleteEblanApplicationInfos.forEach { oldDeleteEblanApplicationInfo ->
            currentCoroutineContext().ensureActive()

            val icon = oldDeleteEblanApplicationInfo.icon

            val hasNoIconReference =
                icon != null && eblanApplicationInfos.none { eblanApplicationInfo ->
                    currentCoroutineContext().ensureActive()

                    eblanApplicationInfo.icon == icon
                } && eblanAppWidgetProviderInfos.none { eblanAppWidgetProviderInfo ->
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
    }

    suspend fun deleteEblanAppWidgetProviderInfoIcons(
        oldDeleteEblanAppWidgetProviderInfos: List<DeleteEblanAppWidgetProviderInfo>,
    ) {
        val eblanApplicationInfos = eblanApplicationInfoRepository.getEblanApplicationInfos()

        val eblanAppWidgetProviderInfos =
            eblanAppWidgetProviderInfoRepository.getEblanAppWidgetProviderInfos()

        oldDeleteEblanAppWidgetProviderInfos.forEach { deleteEblanAppWidgetProviderInfo ->
            currentCoroutineContext().ensureActive()

            val applicationIcon = deleteEblanAppWidgetProviderInfo.applicationIcon

            val hasNoIconReference =
                applicationIcon != null && eblanAppWidgetProviderInfos.none { eblanAppWidgetProviderInfo ->
                    currentCoroutineContext().ensureActive()

                    eblanAppWidgetProviderInfo.applicationIcon == applicationIcon
                } && eblanApplicationInfos.none { eblanApplicationInfo ->
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
    }

    suspend fun deleteEblanShortInfoIcons(oldDeleteEblanShortcutInfos: List<DeleteEblanShortcutInfo>) {
        oldDeleteEblanShortcutInfos.forEach { deleteEblanShortcutInfo ->
            currentCoroutineContext().ensureActive()

            val icon = deleteEblanShortcutInfo.icon

            if (icon != null) {
                val iconFile = File(icon)

                if (iconFile.exists()) {
                    iconFile.delete()
                }
            }
        }
    }

    suspend fun deleteEblanShortcutConfigIcons(oldDeleteEblanShortcutConfigs: List<DeleteEblanShortcutConfig>) {
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
    }

    internal suspend fun updateApplicationInfoGridItems() {
        val eblanApplicationInfos = eblanApplicationInfoRepository.getEblanApplicationInfos()

        val updateApplicationInfoGridItems = mutableListOf<UpdateApplicationInfoGridItem>()

        val deleteApplicationInfoGridItems = mutableListOf<ApplicationInfoGridItem>()

        val applicationInfoGridItems =
            applicationInfoGridItemRepository.applicationInfoGridItems.first()

        applicationInfoGridItems.filterNot { applicationInfoGridItem ->
            currentCoroutineContext().ensureActive()

            applicationInfoGridItem.override
        }.forEach { applicationInfoGridItem ->
            currentCoroutineContext().ensureActive()

            val eblanApplicationInfo = eblanApplicationInfos.find { eblanApplicationInfo ->
                currentCoroutineContext().ensureActive()

                eblanApplicationInfo.serialNumber == applicationInfoGridItem.serialNumber && eblanApplicationInfo.componentName == applicationInfoGridItem.componentName
            }

            if (eblanApplicationInfo != null) {
                updateApplicationInfoGridItems.add(
                    UpdateApplicationInfoGridItem(
                        id = applicationInfoGridItem.id,
                        componentName = eblanApplicationInfo.componentName,
                        icon = eblanApplicationInfo.icon,
                        label = eblanApplicationInfo.label,
                    ),
                )
            } else {
                deleteApplicationInfoGridItems.add(applicationInfoGridItem)
            }
        }

        applicationInfoGridItemRepository.updateApplicationInfoGridItems(
            updateApplicationInfoGridItems = updateApplicationInfoGridItems,
        )

        applicationInfoGridItemRepository.deleteApplicationInfoGridItems(
            applicationInfoGridItems = deleteApplicationInfoGridItems,
        )
    }

    internal suspend fun updateShortcutInfoGridItems() {
        val eblanShortcutInfos = eblanShortcutInfoRepository.getEblanShortcutInfos()

        val updateShortcutInfoGridItems = mutableListOf<UpdateShortcutInfoGridItem>()

        val deleteShortcutInfoGridItems = mutableListOf<ShortcutInfoGridItem>()

        val shortcutInfoGridItems = shortcutInfoGridItemRepository.shortcutInfoGridItems.first()

        shortcutInfoGridItems.filterNot { shortcutInfoGridItem ->
            shortcutInfoGridItem.override
        }.forEach { shortcutInfoGridItem ->
            currentCoroutineContext().ensureActive()

            val eblanShortcutInfo = eblanShortcutInfos.find { eblanShortcutInfo ->
                currentCoroutineContext().ensureActive()

                eblanShortcutInfo.serialNumber == shortcutInfoGridItem.serialNumber && eblanShortcutInfo.shortcutId == shortcutInfoGridItem.shortcutId
            }

            if (eblanShortcutInfo != null) {
                updateShortcutInfoGridItems.add(
                    UpdateShortcutInfoGridItem(
                        id = shortcutInfoGridItem.id,
                        shortLabel = eblanShortcutInfo.shortLabel,
                        longLabel = eblanShortcutInfo.longLabel,
                        isEnabled = eblanShortcutInfo.isEnabled,
                        icon = eblanShortcutInfo.icon,
                        eblanApplicationInfoIcon = resolveApplicationIcon(
                            serialNumber = eblanShortcutInfo.serialNumber,
                            packageName = eblanShortcutInfo.packageName,
                        ),
                    ),
                )
            } else {
                deleteShortcutInfoGridItems.add(shortcutInfoGridItem)
            }
        }

        shortcutInfoGridItemRepository.updateShortcutInfoGridItems(
            updateShortcutInfoGridItems = updateShortcutInfoGridItems,
        )

        shortcutInfoGridItemRepository.deleteShortcutInfoGridItems(shortcutInfoGridItems = deleteShortcutInfoGridItems)
    }

    internal suspend fun updateShortcutConfigGridItems() {
        val eblanShortcutConfigs = eblanShortcutConfigRepository.getEblanShortcutConfigs()

        val updateShortcutConfigGridItems = mutableListOf<UpdateShortcutConfigGridItem>()

        val deleteShortcutConfigGridItems = mutableListOf<ShortcutConfigGridItem>()

        val shortcutConfigGridItems =
            shortcutConfigGridItemRepository.shortcutConfigGridItems.first()

        shortcutConfigGridItems.filterNot { shortcutConfigGridItem ->
            shortcutConfigGridItem.override
        }.forEach { shortcutConfigGridItem ->
            currentCoroutineContext().ensureActive()

            val eblanShortcutConfig = eblanShortcutConfigs.find { eblanShortcutConfig ->
                currentCoroutineContext().ensureActive()

                eblanShortcutConfig.serialNumber == shortcutConfigGridItem.serialNumber && eblanShortcutConfig.componentName == shortcutConfigGridItem.componentName
            }

            if (eblanShortcutConfig != null) {
                updateShortcutConfigGridItems.add(
                    UpdateShortcutConfigGridItem(
                        id = shortcutConfigGridItem.id,
                        componentName = eblanShortcutConfig.componentName,
                        activityLabel = eblanShortcutConfig.activityLabel,
                        activityIcon = eblanShortcutConfig.activityIcon,
                        applicationLabel = packageManagerWrapper.getApplicationLabel(
                            packageName = eblanShortcutConfig.packageName,
                        ).toString(),
                        applicationIcon = resolveApplicationIcon(
                            serialNumber = eblanShortcutConfig.serialNumber,
                            packageName = eblanShortcutConfig.packageName,
                        ),
                    ),
                )
            } else {
                deleteShortcutConfigGridItems.add(shortcutConfigGridItem)
            }
        }

        shortcutConfigGridItemRepository.updateShortcutConfigGridItems(
            updateShortcutConfigGridItems = updateShortcutConfigGridItems,
        )

        shortcutConfigGridItemRepository.deleteShortcutConfigGridItems(
            shortcutConfigGridItems = deleteShortcutConfigGridItems,
        )
    }

    internal suspend fun updateWidgetGridItems() {
        if (!packageManagerWrapper.hasSystemFeatureAppWidgets) return

        val eblanAppWidgetProviderInfos =
            eblanAppWidgetProviderInfoRepository.getEblanAppWidgetProviderInfos()

        val updateWidgetGridItems = mutableListOf<UpdateWidgetGridItem>()

        val deleteWidgetGridItems = mutableListOf<WidgetGridItem>()

        val widgetGridItems = widgetGridItemRepository.widgetGridItems.first()

        widgetGridItems.filterNot { widgetGridItem ->
            widgetGridItem.override
        }.forEach { widgetGridItem ->
            currentCoroutineContext().ensureActive()

            val eblanAppWidgetProviderInfo =
                eblanAppWidgetProviderInfos.find { eblanAppWidgetProviderInfo ->
                    currentCoroutineContext().ensureActive()

                    eblanAppWidgetProviderInfo.serialNumber == widgetGridItem.serialNumber && eblanAppWidgetProviderInfo.componentName == widgetGridItem.componentName
                }

            if (eblanAppWidgetProviderInfo != null) {
                updateWidgetGridItems.add(
                    UpdateWidgetGridItem(
                        id = widgetGridItem.id,
                        componentName = eblanAppWidgetProviderInfo.componentName,
                        configure = eblanAppWidgetProviderInfo.configure,
                        minWidth = eblanAppWidgetProviderInfo.minWidth,
                        minHeight = eblanAppWidgetProviderInfo.minHeight,
                        resizeMode = eblanAppWidgetProviderInfo.resizeMode,
                        minResizeWidth = eblanAppWidgetProviderInfo.minResizeWidth,
                        minResizeHeight = eblanAppWidgetProviderInfo.minResizeHeight,
                        maxResizeWidth = eblanAppWidgetProviderInfo.maxResizeWidth,
                        maxResizeHeight = eblanAppWidgetProviderInfo.maxResizeHeight,
                        targetCellHeight = eblanAppWidgetProviderInfo.targetCellHeight,
                        targetCellWidth = eblanAppWidgetProviderInfo.targetCellWidth,
                        icon = resolveApplicationIcon(
                            serialNumber = eblanAppWidgetProviderInfo.serialNumber,
                            packageName = eblanAppWidgetProviderInfo.packageName,
                        ),
                        label = packageManagerWrapper.getApplicationLabel(
                            packageName = eblanAppWidgetProviderInfo.packageName,
                        ).toString(),
                    ),
                )
            } else {
                deleteWidgetGridItems.add(widgetGridItem)
            }
        }

        widgetGridItemRepository.updateWidgetGridItems(updateWidgetGridItems = updateWidgetGridItems)

        widgetGridItemRepository.deleteWidgetGridItemsByPackageName(widgetGridItems = deleteWidgetGridItems)
    }

    internal suspend fun toEblanAppWidgetProviderInfo(
        appWidgetManagerAppWidgetProviderInfo: AppWidgetManagerAppWidgetProviderInfo,
    ): EblanAppWidgetProviderInfo = EblanAppWidgetProviderInfo(
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
        applicationIcon = resolveApplicationIcon(
            serialNumber = appWidgetManagerAppWidgetProviderInfo.serialNumber,
            packageName = appWidgetManagerAppWidgetProviderInfo.packageName,
        ),
        applicationLabel = packageManagerWrapper.getApplicationLabel(
            packageName = appWidgetManagerAppWidgetProviderInfo.packageName,
        ).toString(),
        lastUpdateTime = appWidgetManagerAppWidgetProviderInfo.lastUpdateTime,
        label = appWidgetManagerAppWidgetProviderInfo.label,
        description = appWidgetManagerAppWidgetProviderInfo.description,
    )

    internal fun toFastLauncherAppsActivityInfo(eblanApplicationInfo: EblanApplicationInfo): FastLauncherAppsActivityInfo = FastLauncherAppsActivityInfo(
        serialNumber = eblanApplicationInfo.serialNumber,
        componentName = eblanApplicationInfo.componentName,
        packageName = eblanApplicationInfo.packageName,
        lastUpdateTime = eblanApplicationInfo.lastUpdateTime,
    )

    internal fun toSyncEblanApplicationInfo(eblanApplicationInfo: EblanApplicationInfo) = SyncEblanApplicationInfo(
        serialNumber = eblanApplicationInfo.serialNumber,
        componentName = eblanApplicationInfo.componentName,
        packageName = eblanApplicationInfo.packageName,
        icon = eblanApplicationInfo.icon,
        label = eblanApplicationInfo.label,
        lastUpdateTime = eblanApplicationInfo.lastUpdateTime,
    )

    internal fun toSyncEblanApplicationInfo(launcherAppsActivityInfo: LauncherAppsActivityInfo) = SyncEblanApplicationInfo(
        serialNumber = launcherAppsActivityInfo.serialNumber,
        componentName = launcherAppsActivityInfo.componentName,
        packageName = launcherAppsActivityInfo.packageName,
        icon = launcherAppsActivityInfo.activityIcon,
        label = launcherAppsActivityInfo.activityLabel,
        lastUpdateTime = launcherAppsActivityInfo.lastUpdateTime,
    )

    internal fun toDeleteEblanApplicationInfo(syncEblanApplicationInfo: SyncEblanApplicationInfo) = DeleteEblanApplicationInfo(
        serialNumber = syncEblanApplicationInfo.serialNumber,
        componentName = syncEblanApplicationInfo.componentName,
        packageName = syncEblanApplicationInfo.packageName,
        icon = syncEblanApplicationInfo.icon,
    )

    internal suspend fun toEblanShortcutConfig(
        shortcutConfigActivityInfo: ShortcutConfigActivityInfo,
    ): EblanShortcutConfig = EblanShortcutConfig(
        componentName = shortcutConfigActivityInfo.componentName,
        packageName = shortcutConfigActivityInfo.packageName,
        serialNumber = shortcutConfigActivityInfo.serialNumber,
        activityIcon = shortcutConfigActivityInfo.activityIcon,
        activityLabel = shortcutConfigActivityInfo.activityLabel,
        applicationIcon = resolveApplicationIcon(
            serialNumber = shortcutConfigActivityInfo.serialNumber,
            packageName = shortcutConfigActivityInfo.packageName,
        ),
        applicationLabel = packageManagerWrapper.getApplicationLabel(
            packageName = shortcutConfigActivityInfo.packageName,
        ),
    )

    internal fun toDeleteEblanAppWidgetProviderInfo(eblanAppWidgetProviderInfo: EblanAppWidgetProviderInfo): DeleteEblanAppWidgetProviderInfo = DeleteEblanAppWidgetProviderInfo(
        componentName = eblanAppWidgetProviderInfo.componentName,
        serialNumber = eblanAppWidgetProviderInfo.serialNumber,
        packageName = eblanAppWidgetProviderInfo.packageName,
        preview = eblanAppWidgetProviderInfo.preview,
        applicationIcon = eblanAppWidgetProviderInfo.applicationIcon,
    )

    internal fun toEblanShortcutInfo(launcherAppsShortcutInfo: LauncherAppsShortcutInfo): EblanShortcutInfo = EblanShortcutInfo(
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

    internal fun toDeleteEblanShortcutInfo(eblanShortcutInfo: EblanShortcutInfo): DeleteEblanShortcutInfo = DeleteEblanShortcutInfo(
        serialNumber = eblanShortcutInfo.serialNumber,
        shortcutId = eblanShortcutInfo.shortcutId,
        packageName = eblanShortcutInfo.packageName,
        icon = eblanShortcutInfo.icon,
    )

    internal fun toDeleteEblanShortcutConfig(eblanShortcutConfig: EblanShortcutConfig): DeleteEblanShortcutConfig = DeleteEblanShortcutConfig(
        componentName = eblanShortcutConfig.componentName,
        packageName = eblanShortcutConfig.packageName,
        serialNumber = eblanShortcutConfig.serialNumber,
        activityIcon = eblanShortcutConfig.activityIcon,
    )

    @OptIn(ExperimentalUuidApi::class)
    internal suspend fun addNewApplicationToHomeScreen(
        gridItems: MutableList<GridItem>,
        componentName: String,
        packageName: String,
        icon: String?,
        label: String?,
        homeSettings: HomeSettings,
        newApplicationsToHomeScreen: MutableList<ApplicationInfoGridItem>,
    ) {
        val alreadyOnHome = gridItems.any { gridItem ->
            when (val data = gridItem.data) {
                is GridItemData.ApplicationInfo ->
                    data.serialNumber == 0L &&
                        data.componentName == componentName

                is GridItemData.Folder ->
                    data.gridItems.any { gridItem ->
                        gridItem.serialNumber == 0L &&
                            gridItem.componentName == componentName
                    }

                else -> false
            }
        }

        if (alreadyOnHome) return

        val eblanAction = EblanAction(
            eblanActionType = EblanActionType.None,
            serialNumber = 0L,
            componentName = "",
        )

        val data = GridItemData.ApplicationInfo(
            serialNumber = 0L,
            componentName = componentName,
            packageName = packageName,
            icon = icon,
            label = label.toString(),
            customIcon = null,
            customLabel = null,
            index = -1,
            folderId = null,
        )

        val gridItem = GridItem(
            id = Uuid.random().toHexString(),
            page = homeSettings.initialPage,
            startColumn = 0,
            startRow = 0,
            columnSpan = 1,
            rowSpan = 1,
            data = data,
            associate = Associate.Grid,
            override = false,
            gridItemSettings = homeSettings.gridItemSettings,
            doubleTap = eblanAction,
            swipeUp = eblanAction,
            swipeDown = eblanAction,
        )

        val newGridItem = findAvailableRegionByPage(
            gridItems = gridItems,
            gridItem = gridItem,
            pageCount = homeSettings.pageCount,
            columns = homeSettings.columns,
            rows = homeSettings.rows,
        )

        if (newGridItem != null) {
            gridItems.add(newGridItem)

            newApplicationsToHomeScreen.add(
                ApplicationInfoGridItem(
                    id = newGridItem.id,
                    page = newGridItem.page,
                    startColumn = newGridItem.startColumn,
                    startRow = newGridItem.startRow,
                    columnSpan = newGridItem.columnSpan,
                    rowSpan = newGridItem.rowSpan,
                    associate = newGridItem.associate,
                    componentName = data.componentName,
                    packageName = data.packageName,
                    icon = data.icon,
                    label = data.label,
                    override = newGridItem.override,
                    serialNumber = data.serialNumber,
                    customIcon = data.customIcon,
                    customLabel = data.customLabel,
                    gridItemSettings = newGridItem.gridItemSettings,
                    doubleTap = newGridItem.doubleTap,
                    swipeUp = newGridItem.swipeUp,
                    swipeDown = newGridItem.swipeDown,
                    index = data.index,
                    folderId = data.folderId,
                ),
            )
        }
    }

    private suspend fun resolveApplicationIcon(
        serialNumber: Long,
        packageName: String,
    ): String? {
        val directory = fileManager.getFilesDirectory(FileManager.ICONS_DIR)

        val componentName = packageManagerWrapper.getComponentName(packageName = packageName)

        return if (componentName != null) {
            File(
                directory,
                iconKeyGenerator.getActivityIconKey(
                    serialNumber = serialNumber,
                    componentName = componentName,
                ),
            ).absolutePath
        } else {
            val file =
                File(
                    directory,
                    iconKeyGenerator.getActivityIconKey(
                        serialNumber = serialNumber,
                        componentName = packageName,
                    ),
                )

            packageManagerWrapper.getApplicationIcon(packageName = packageName, file = file)
        }
    }
}
