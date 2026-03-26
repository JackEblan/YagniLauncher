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

import com.eblan.launcher.domain.framework.FileManager
import com.eblan.launcher.domain.framework.PackageManagerWrapper
import com.eblan.launcher.domain.model.AppWidgetManagerAppWidgetProviderInfo
import com.eblan.launcher.domain.model.ApplicationInfoGridItem
import com.eblan.launcher.domain.model.DeleteEblanAppWidgetProviderInfo
import com.eblan.launcher.domain.model.DeleteEblanApplicationInfo
import com.eblan.launcher.domain.model.DeleteEblanShortcutConfig
import com.eblan.launcher.domain.model.DeleteEblanShortcutInfo
import com.eblan.launcher.domain.model.EblanAppWidgetProviderInfo
import com.eblan.launcher.domain.model.EblanApplicationInfo
import com.eblan.launcher.domain.model.EblanShortcutConfig
import com.eblan.launcher.domain.model.EblanShortcutInfo
import com.eblan.launcher.domain.model.FastLauncherAppsActivityInfo
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
import com.eblan.launcher.domain.repository.ShortcutConfigGridItemRepository
import com.eblan.launcher.domain.repository.ShortcutInfoGridItemRepository
import com.eblan.launcher.domain.repository.WidgetGridItemRepository
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.first
import java.io.File

internal suspend fun deleteEblanApplicationInfoIcons(
    eblanApplicationInfos: List<EblanApplicationInfo>,
    eblanAppWidgetProviderInfos: List<EblanAppWidgetProviderInfo>,
    oldDeleteEblanApplicationInfos: List<DeleteEblanApplicationInfo>,
) {
    oldDeleteEblanApplicationInfos.forEach { oldDeleteEblanApplicationInfo ->
        currentCoroutineContext().ensureActive()

        val icon = oldDeleteEblanApplicationInfo.icon

        val hasNoIconReference =
            icon != null && eblanApplicationInfos
                .none { eblanApplicationInfo ->
                    currentCoroutineContext().ensureActive()

                    eblanApplicationInfo.icon == icon
                } && eblanAppWidgetProviderInfos
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
}

internal suspend fun deleteEblanAppWidgetProviderInfoIcons(
    eblanApplicationInfos: List<EblanApplicationInfo>,
    eblanAppWidgetProviderInfos: List<EblanAppWidgetProviderInfo>,
    oldDeleteEblanAppWidgetProviderInfos: List<DeleteEblanAppWidgetProviderInfo>,
) {
    oldDeleteEblanAppWidgetProviderInfos.forEach { deleteEblanAppWidgetProviderInfo ->
        currentCoroutineContext().ensureActive()

        val applicationIcon = deleteEblanAppWidgetProviderInfo.applicationIcon

        val hasNoIconReference = applicationIcon != null &&
            eblanAppWidgetProviderInfos
                .none { eblanAppWidgetProviderInfo ->
                    currentCoroutineContext().ensureActive()

                    eblanAppWidgetProviderInfo.applicationIcon == applicationIcon
                } &&
            eblanApplicationInfos
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
}

internal suspend fun deleteEblanShortInfoIcons(oldDeleteEblanShortcutInfos: List<DeleteEblanShortcutInfo>) {
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

internal suspend fun deleteEblanShortcutConfigIcons(oldDeleteEblanShortcutConfigs: List<DeleteEblanShortcutConfig>) {
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

internal suspend fun updateApplicationInfoGridItems(
    eblanApplicationInfos: List<EblanApplicationInfo>,
    applicationInfoGridItemRepository: ApplicationInfoGridItemRepository,
) {
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

internal suspend fun updateShortcutInfoGridItems(
    eblanShortcutInfos: List<EblanShortcutInfo>?,
    shortcutInfoGridItemRepository: ShortcutInfoGridItemRepository,
    fileManager: FileManager,
    packageManagerWrapper: PackageManagerWrapper,
) {
    val updateShortcutInfoGridItems = mutableListOf<UpdateShortcutInfoGridItem>()

    val deleteShortcutInfoGridItems = mutableListOf<ShortcutInfoGridItem>()

    val shortcutInfoGridItems = shortcutInfoGridItemRepository.shortcutInfoGridItems.first()

    if (eblanShortcutInfos != null) {
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
                            fileManager = fileManager,
                            packageManagerWrapper = packageManagerWrapper,
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
}

internal suspend fun updateShortcutConfigGridItems(
    eblanShortcutConfigs: List<EblanShortcutConfig>,
    shortcutConfigGridItemRepository: ShortcutConfigGridItemRepository,
    fileManager: FileManager,
    packageManagerWrapper: PackageManagerWrapper,
) {
    val updateShortcutConfigGridItems = mutableListOf<UpdateShortcutConfigGridItem>()

    val deleteShortcutConfigGridItems = mutableListOf<ShortcutConfigGridItem>()

    val shortcutConfigGridItems = shortcutConfigGridItemRepository.shortcutConfigGridItems.first()

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
                        fileManager = fileManager,
                        packageManagerWrapper = packageManagerWrapper,
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

internal suspend fun updateWidgetGridItems(
    eblanAppWidgetProviderInfos: List<EblanAppWidgetProviderInfo>,
    fileManager: FileManager,
    packageManagerWrapper: PackageManagerWrapper,
    widgetGridItemRepository: WidgetGridItemRepository,
) {
    if (!packageManagerWrapper.hasSystemFeatureAppWidgets) return

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
                        fileManager = fileManager,
                        packageManagerWrapper = packageManagerWrapper,
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

internal suspend fun AppWidgetManagerAppWidgetProviderInfo.toEblanAppWidgetProviderInfo(
    fileManager: FileManager,
    packageManagerWrapper: PackageManagerWrapper,
): EblanAppWidgetProviderInfo = EblanAppWidgetProviderInfo(
    componentName = componentName,
    serialNumber = serialNumber,
    configure = configure,
    packageName = packageName,
    targetCellWidth = targetCellWidth,
    targetCellHeight = targetCellHeight,
    minWidth = minWidth,
    minHeight = minHeight,
    resizeMode = resizeMode,
    minResizeWidth = minResizeWidth,
    minResizeHeight = minResizeHeight,
    maxResizeWidth = maxResizeWidth,
    maxResizeHeight = maxResizeHeight,
    preview = preview,
    applicationIcon = resolveApplicationIcon(
        fileManager = fileManager,
        packageManagerWrapper = packageManagerWrapper,
        serialNumber = serialNumber,
        packageName = packageName,
    ),
    applicationLabel = packageManagerWrapper.getApplicationLabel(
        packageName = packageName,
    ).toString(),
    lastUpdateTime = lastUpdateTime,
    label = label,
    description = description,
)

internal fun EblanApplicationInfo.toFastLauncherAppsActivityInfo(): FastLauncherAppsActivityInfo = FastLauncherAppsActivityInfo(
    serialNumber = serialNumber,
    componentName = componentName,
    packageName = packageName,
    lastUpdateTime = lastUpdateTime,
)

internal fun EblanApplicationInfo.toSyncEblanApplicationInfo() = SyncEblanApplicationInfo(
    serialNumber = serialNumber,
    componentName = componentName,
    packageName = packageName,
    icon = icon,
    label = label,
    lastUpdateTime = lastUpdateTime,
)

internal fun LauncherAppsActivityInfo.toSyncEblanApplicationInfo() = SyncEblanApplicationInfo(
    serialNumber = serialNumber,
    componentName = componentName,
    packageName = packageName,
    icon = activityIcon,
    label = activityLabel,
    lastUpdateTime = lastUpdateTime,
)

internal fun SyncEblanApplicationInfo.toDeleteEblanApplicationInfo() = DeleteEblanApplicationInfo(
    serialNumber = serialNumber,
    componentName = componentName,
    packageName = packageName,
    icon = icon,
)

internal suspend fun ShortcutConfigActivityInfo.toEblanShortcutConfig(
    fileManager: FileManager,
    packageManagerWrapper: PackageManagerWrapper,
): EblanShortcutConfig = EblanShortcutConfig(
    componentName = componentName,
    packageName = packageName,
    serialNumber = serialNumber,
    activityIcon = activityIcon,
    activityLabel = activityLabel,
    applicationIcon = resolveApplicationIcon(
        fileManager = fileManager,
        packageManagerWrapper = packageManagerWrapper,
        serialNumber = serialNumber,
        packageName = packageName,
    ),
    applicationLabel = packageManagerWrapper.getApplicationLabel(
        packageName = packageName,
    ),
)

internal fun EblanAppWidgetProviderInfo.toDeleteEblanAppWidgetProviderInfo(): DeleteEblanAppWidgetProviderInfo = DeleteEblanAppWidgetProviderInfo(
    componentName = componentName,
    serialNumber = serialNumber,
    packageName = packageName,
    preview = preview,
    applicationIcon = applicationIcon,
)

internal fun LauncherAppsShortcutInfo.toEblanShortcutInfo(): EblanShortcutInfo = EblanShortcutInfo(
    shortcutId = shortcutId,
    serialNumber = serialNumber,
    packageName = packageName,
    shortLabel = shortLabel,
    longLabel = longLabel,
    icon = icon,
    shortcutQueryFlag = shortcutQueryFlag,
    isEnabled = isEnabled,
    lastChangedTimestamp = lastChangedTimestamp,
)

internal fun EblanShortcutInfo.toDeleteEblanShortcutInfo(): DeleteEblanShortcutInfo = DeleteEblanShortcutInfo(
    serialNumber = serialNumber,
    shortcutId = shortcutId,
    packageName = packageName,
    icon = icon,
)

internal fun EblanShortcutConfig.toDeleteEblanShortcutConfig(): DeleteEblanShortcutConfig = DeleteEblanShortcutConfig(
    componentName = componentName,
    packageName = packageName,
    serialNumber = serialNumber,
    activityIcon = activityIcon,
)

private suspend fun resolveApplicationIcon(
    fileManager: FileManager,
    packageManagerWrapper: PackageManagerWrapper,
    serialNumber: Long,
    packageName: String,
): String? {
    val directory = fileManager.getFilesDirectory(FileManager.ICONS_DIR)

    val componentName = packageManagerWrapper.getComponentName(packageName = packageName)

    return if (componentName != null) {
        File(
            directory,
            fileManager.getHashedFileName(name = "$serialNumber:$componentName"),
        ).absolutePath
    } else {
        val file =
            File(
                directory,
                fileManager.getHashedFileName(name = "$serialNumber:$packageName"),
            )

        packageManagerWrapper.getApplicationIcon(packageName = packageName, file = file)
    }
}
