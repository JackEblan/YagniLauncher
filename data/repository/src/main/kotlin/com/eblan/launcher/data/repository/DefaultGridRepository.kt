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
package com.eblan.launcher.data.repository

import com.eblan.launcher.data.repository.mapper.asApplicationInfoGridItem
import com.eblan.launcher.data.repository.mapper.asFolderGridItem
import com.eblan.launcher.data.repository.mapper.asShortcutConfigGridItem
import com.eblan.launcher.data.repository.mapper.asShortcutInfoGridItem
import com.eblan.launcher.data.repository.mapper.asWidgetGridItem
import com.eblan.launcher.domain.common.Dispatcher
import com.eblan.launcher.domain.common.EblanDispatchers
import com.eblan.launcher.domain.framework.AppWidgetHostWrapper
import com.eblan.launcher.domain.model.ApplicationInfoGridItem
import com.eblan.launcher.domain.model.FolderGridItem
import com.eblan.launcher.domain.model.GridItem
import com.eblan.launcher.domain.model.GridItemData
import com.eblan.launcher.domain.model.ShortcutConfigGridItem
import com.eblan.launcher.domain.model.ShortcutInfoGridItem
import com.eblan.launcher.domain.model.WidgetGridItem
import com.eblan.launcher.domain.repository.ApplicationInfoGridItemRepository
import com.eblan.launcher.domain.repository.EblanApplicationInfoRepository
import com.eblan.launcher.domain.repository.FolderGridItemRepository
import com.eblan.launcher.domain.repository.GridRepository
import com.eblan.launcher.domain.repository.ShortcutConfigGridItemRepository
import com.eblan.launcher.domain.repository.ShortcutInfoGridItemRepository
import com.eblan.launcher.domain.repository.WidgetGridItemRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

internal class DefaultGridRepository @Inject constructor(
    private val eblanApplicationInfoRepository: EblanApplicationInfoRepository,
    private val applicationInfoGridItemRepository: ApplicationInfoGridItemRepository,
    private val widgetGridItemRepository: WidgetGridItemRepository,
    private val shortcutInfoGridItemRepository: ShortcutInfoGridItemRepository,
    private val folderGridItemRepository: FolderGridItemRepository,
    private val shortcutConfigGridItemRepository: ShortcutConfigGridItemRepository,
    private val appWidgetHostWrapper: AppWidgetHostWrapper,
    @param:Dispatcher(EblanDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
) : GridRepository {
    override val gridItems: Flow<List<GridItem>> = combine(
        applicationInfoGridItemRepository.gridItems,
        widgetGridItemRepository.gridItems,
        shortcutInfoGridItemRepository.gridItems,
        shortcutConfigGridItemRepository.gridItems,
    ) { applicationInfoGridItems, widgetGridItems, shortcutInfoGridItems, shortcutConfigGridItems ->
        (applicationInfoGridItems + widgetGridItems + shortcutInfoGridItems + shortcutConfigGridItems)
    }

    override val gridItemsWithFolderId: Flow<List<GridItem>> = combine(
        applicationInfoGridItemRepository.gridItemsWithFolderId,
        widgetGridItemRepository.gridItems,
        shortcutInfoGridItemRepository.gridItems,
        shortcutConfigGridItemRepository.gridItems,
    ) { applicationInfoGridItems, widgetGridItems, shortcutInfoGridItems, shortcutConfigGridItems ->
        (applicationInfoGridItems + widgetGridItems + shortcutInfoGridItems + shortcutConfigGridItems)
    }

    override suspend fun updateGridItem(gridItem: GridItem) {
        when (val data = gridItem.data) {
            is GridItemData.ApplicationInfo -> {
                applicationInfoGridItemRepository.updateApplicationInfoGridItem(
                    applicationInfoGridItem = gridItem.asApplicationInfoGridItem(data = data),
                )
            }

            is GridItemData.Folder -> {
                folderGridItemRepository.updateFolderGridItem(
                    folderGridItem = gridItem.asFolderGridItem(data = data),
                )
            }

            is GridItemData.ShortcutInfo -> {
                shortcutInfoGridItemRepository.updateShortcutInfoGridItem(
                    shortcutInfoGridItem = gridItem.asShortcutInfoGridItem(data = data),
                )
            }

            is GridItemData.Widget -> {
                widgetGridItemRepository.updateWidgetGridItem(
                    widgetGridItem = gridItem.asWidgetGridItem(data = data),
                )
            }

            is GridItemData.ShortcutConfig -> {
                shortcutConfigGridItemRepository.updateShortcutConfigGridItem(
                    shortcutConfigGridItem = gridItem.asShortcutConfigGridItem(data = data),
                )
            }
        }
    }

    override suspend fun resetGridItemCustomIcon(gridItem: GridItem) {
        withContext(ioDispatcher) {
            val gridItem = when (val data = gridItem.data) {
                is GridItemData.ApplicationInfo -> {
                    data.customIcon?.let { customIcon ->
                        val customIconFile = File(customIcon)

                        if (customIconFile.exists()) {
                            customIconFile.delete()
                        }
                    }

                    val eblanApplicationInfo =
                        eblanApplicationInfoRepository.getEblanApplicationInfoByComponentName(
                            serialNumber = data.serialNumber,
                            componentName = data.componentName,
                        )

                    if (eblanApplicationInfo != null) {
                        eblanApplicationInfoRepository.updateEblanApplicationInfo(
                            eblanApplicationInfo = eblanApplicationInfo.copy(customIcon = null),
                        )
                    }

                    val newData = data.copy(customIcon = null)

                    gridItem.copy(data = newData)
                }

                is GridItemData.ShortcutConfig -> {
                    data.customIcon?.let { customIcon ->
                        val customIconFile = File(customIcon)

                        if (customIconFile.exists()) {
                            customIconFile.delete()
                        }
                    }

                    val newData = data.copy(customIcon = null)

                    gridItem.copy(data = newData)
                }

                is GridItemData.ShortcutInfo -> {
                    data.customIcon?.let { customIcon ->
                        val customIconFile = File(customIcon)

                        if (customIconFile.exists()) {
                            customIconFile.delete()
                        }
                    }

                    val newData = data.copy(customIcon = null)

                    gridItem.copy(data = newData)
                }

                is GridItemData.Folder -> {
                    data.icon?.let { icon ->
                        val iconFile = File(icon)

                        if (iconFile.exists()) {
                            iconFile.delete()
                        }
                    }

                    val newData = data.copy(icon = null)

                    gridItem.copy(data = newData)
                }

                else -> gridItem
            }

            updateGridItem(gridItem = gridItem)
        }
    }

    override suspend fun updateGridItems(gridItems: List<GridItem>) {
        val applicationInfoGridItems = mutableListOf<ApplicationInfoGridItem>()

        val folderApplicationInfoGridItems = mutableListOf<ApplicationInfoGridItem>()

        val widgetGridItems = mutableListOf<WidgetGridItem>()

        val shortcutInfoGridItems = mutableListOf<ShortcutInfoGridItem>()

        val folderGridItems = mutableListOf<FolderGridItem>()

        val shortcutConfigGridItems = mutableListOf<ShortcutConfigGridItem>()

        gridItems.forEach { gridItem ->
            when (val data = gridItem.data) {
                is GridItemData.ApplicationInfo -> {
                    applicationInfoGridItems.add(
                        gridItem.asApplicationInfoGridItem(data = data),
                    )
                }

                is GridItemData.Folder -> {
                    folderGridItems.add(
                        gridItem.asFolderGridItem(data = data),
                    )

                    folderApplicationInfoGridItems.addAll(data.gridItems)
                }

                is GridItemData.Widget -> {
                    widgetGridItems.add(
                        gridItem.asWidgetGridItem(data = data),
                    )
                }

                is GridItemData.ShortcutInfo -> {
                    shortcutInfoGridItems.add(
                        gridItem.asShortcutInfoGridItem(data = data),
                    )
                }

                is GridItemData.ShortcutConfig -> {
                    shortcutConfigGridItems.add(
                        gridItem.asShortcutConfigGridItem(data = data),
                    )
                }
            }
        }

        applicationInfoGridItemRepository.upsertApplicationInfoGridItems(
            applicationInfoGridItems = applicationInfoGridItems,
        )

        applicationInfoGridItemRepository.upsertApplicationInfoGridItems(
            applicationInfoGridItems = folderApplicationInfoGridItems,
        )

        widgetGridItemRepository.upsertWidgetGridItems(widgetGridItems = widgetGridItems)

        shortcutInfoGridItemRepository.upsertShortcutInfoGridItems(shortcutInfoGridItems = shortcutInfoGridItems)

        folderGridItemRepository.upsertFolderGridItems(folderGridItems = folderGridItems)

        shortcutConfigGridItemRepository.upsertShortcutConfigGridItems(
            shortcutConfigGridItems = shortcutConfigGridItems,
        )
    }

    override suspend fun deleteGridItems(gridItems: List<GridItem>) {
        val applicationInfoGridItems = mutableListOf<ApplicationInfoGridItem>()

        val widgetGridItems = mutableListOf<WidgetGridItem>()

        val shortcutInfoGridItems = mutableListOf<ShortcutInfoGridItem>()

        val folderGridItems = mutableListOf<FolderGridItem>()

        val shortcutConfigGridItems = mutableListOf<ShortcutConfigGridItem>()

        gridItems.forEach { gridItem ->
            when (val data = gridItem.data) {
                is GridItemData.ApplicationInfo -> {
                    applicationInfoGridItems.add(
                        gridItem.asApplicationInfoGridItem(data = data),
                    )
                }

                is GridItemData.Folder -> {
                    folderGridItems.add(
                        gridItem.asFolderGridItem(data = data),
                    )
                }

                is GridItemData.Widget -> {
                    appWidgetHostWrapper.deleteAppWidgetId(appWidgetId = data.appWidgetId)

                    widgetGridItems.add(
                        gridItem.asWidgetGridItem(data = data),
                    )
                }

                is GridItemData.ShortcutInfo -> {
                    shortcutInfoGridItems.add(
                        gridItem.asShortcutInfoGridItem(data = data),
                    )
                }

                is GridItemData.ShortcutConfig -> {
                    shortcutConfigGridItems.add(
                        gridItem.asShortcutConfigGridItem(data = data),
                    )
                }
            }
        }

        applicationInfoGridItemRepository.deleteApplicationInfoGridItems(
            applicationInfoGridItems = applicationInfoGridItems,
        )

        widgetGridItemRepository.deleteWidgetGridItemsByPackageName(widgetGridItems = widgetGridItems)

        shortcutInfoGridItemRepository.deleteShortcutInfoGridItems(shortcutInfoGridItems = shortcutInfoGridItems)

        folderGridItemRepository.deleteFolderGridItems(folderGridItems = folderGridItems)

        shortcutConfigGridItemRepository.deleteShortcutConfigGridItems(
            shortcutConfigGridItems = shortcutConfigGridItems,
        )
    }

    override suspend fun deleteGridItem(gridItem: GridItem) {
        when (val data = gridItem.data) {
            is GridItemData.ApplicationInfo -> {
                applicationInfoGridItemRepository.deleteApplicationInfoGridItem(
                    applicationInfoGridItem = gridItem.asApplicationInfoGridItem(data = data),
                )
            }

            is GridItemData.Folder -> {
                folderGridItemRepository.deleteFolderGridItem(
                    folderGridItem = gridItem.asFolderGridItem(data = data),
                )
            }

            is GridItemData.ShortcutInfo -> {
                shortcutInfoGridItemRepository.deleteShortcutInfoGridItem(
                    shortcutInfoGridItem = gridItem.asShortcutInfoGridItem(data = data),
                )
            }

            is GridItemData.Widget -> {
                appWidgetHostWrapper.deleteAppWidgetId(appWidgetId = data.appWidgetId)

                widgetGridItemRepository.deleteWidgetGridItem(
                    widgetGridItem = gridItem.asWidgetGridItem(data = data),
                )
            }

            is GridItemData.ShortcutConfig -> {
                shortcutConfigGridItemRepository.deleteShortcutConfigGridItem(
                    shortcutConfigGridItem = gridItem.asShortcutConfigGridItem(data = data),
                )
            }
        }
    }
}
