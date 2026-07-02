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
package com.eblan.launcher.domain.usecase

import com.eblan.launcher.domain.common.Dispatcher
import com.eblan.launcher.domain.common.EblanDispatchers
import com.eblan.launcher.domain.common.IconKeyGenerator
import com.eblan.launcher.domain.framework.FileManager
import com.eblan.launcher.domain.framework.LauncherAppsWrapper
import com.eblan.launcher.domain.framework.PackageManagerWrapper
import com.eblan.launcher.domain.framework.ResourcesWrapper
import com.eblan.launcher.domain.framework.WallpaperManagerWrapper
import com.eblan.launcher.domain.grid.isGridItemSpanWithinBounds
import com.eblan.launcher.domain.model.Associate
import com.eblan.launcher.domain.model.GridItem
import com.eblan.launcher.domain.model.GridItemData
import com.eblan.launcher.domain.model.HomeData
import com.eblan.launcher.domain.model.TextColor
import com.eblan.launcher.domain.model.Theme
import com.eblan.launcher.domain.repository.ApplicationInfoGridItemRepository
import com.eblan.launcher.domain.repository.FolderGridItemRepository
import com.eblan.launcher.domain.repository.ShortcutConfigGridItemRepository
import com.eblan.launcher.domain.repository.ShortcutInfoGridItemRepository
import com.eblan.launcher.domain.repository.UserDataRepository
import com.eblan.launcher.domain.repository.WidgetGridItemRepository
import com.eblan.launcher.domain.usecase.grid.asGridItem
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

class GetHomeDataUseCase @Inject constructor(
    private val userDataRepository: UserDataRepository,
    private val launcherAppsWrapper: LauncherAppsWrapper,
    private val wallpaperManagerWrapper: WallpaperManagerWrapper,
    private val resourcesWrapper: ResourcesWrapper,
    private val packageManagerWrapper: PackageManagerWrapper,
    private val fileManager: FileManager,
    private val iconKeyGenerator: IconKeyGenerator,
    private val applicationInfoGridItemRepository: ApplicationInfoGridItemRepository,
    private val widgetGridItemRepository: WidgetGridItemRepository,
    private val shortcutInfoGridItemRepository: ShortcutInfoGridItemRepository,
    private val folderGridItemRepository: FolderGridItemRepository,
    private val shortcutConfigGridItemRepository: ShortcutConfigGridItemRepository,
    @param:Dispatcher(EblanDispatchers.Default) private val defaultDispatcher: CoroutineDispatcher,
) {
    operator fun invoke(): Flow<HomeData> = combine(
        userDataRepository.userDataFlow,
        getGridItemsFlow(),
        wallpaperManagerWrapper.getColorsChanged(),
    ) { userData, gridItems, colorHints ->
        val gridItemsByPage = gridItems.filter {
            isGridItemSpanWithinBounds(
                gridItem = it,
                columns = userData.homeSettings.columns,
                rows = userData.homeSettings.rows,
            ) && it.associate == Associate.Grid
        }.groupBy { it.page }

        val dockGridItemsByPage = gridItems.filter {
            isGridItemSpanWithinBounds(
                gridItem = it,
                columns = userData.homeSettings.dockColumns,
                rows = userData.homeSettings.dockRows,
            ) && it.associate == Associate.Dock
        }.groupBy { it.page }

        val gridItemSettings = userData.homeSettings.gridItemSettings

        val textColor = when (gridItemSettings.textColor) {
            TextColor.System -> {
                getTextColorFromWallpaperColors(
                    theme = userData.generalSettings.theme,
                    colorHints = colorHints,
                )
            }

            else -> gridItemSettings.textColor
        }

        HomeData(
            userData = userData,
            gridItems = gridItems,
            gridItemsByPage = gridItemsByPage,
            dockGridItemsByPage = dockGridItemsByPage,
            hasShortcutHostPermission = launcherAppsWrapper.hasShortcutHostPermission,
            hasSystemFeatureAppWidgets = packageManagerWrapper.hasSystemFeatureAppWidgets,
            textColor = textColor,
        )
    }.flowOn(defaultDispatcher)

    private fun getGridItemsFlow(): Flow<List<GridItem>> {
        val gridItemsFlow = combine(
            userDataRepository.userDataFlow,
            applicationInfoGridItemRepository.applicationInfoGridItems,
            widgetGridItemRepository.widgetGridItemsFlow,
            shortcutInfoGridItemRepository.shortcutInfoGridItemsFlow,
            shortcutConfigGridItemRepository.shortcutConfigGridItemsFlow,
        ) { userData, applicationInfoGridItems, widgetGridItems, shortcutInfoGridItems, shortcutConfigGridItems ->
            val currentApplicationGridItems = applicationInfoGridItems.map {
                it.asGridItem(
                    fileManager = fileManager,
                    iconKeyGenerator = iconKeyGenerator,
                    iconPackInfoPackageName = userData.generalSettings.iconPackInfoPackageName,
                )
            }

            val currentWidgetGridItems = widgetGridItems.map {
                it.asGridItem()
            }

            val currentShortcutInfoGridItems = shortcutInfoGridItems.map {
                it.asGridItem()
            }

            val currentShortcutConfigGridItems = shortcutConfigGridItems.map {
                it.asGridItem()
            }

            buildList {
                addAll(currentApplicationGridItems)
                addAll(currentWidgetGridItems)
                addAll(currentShortcutInfoGridItems)
                addAll(currentShortcutConfigGridItems)
            }
        }.flowOn(defaultDispatcher)

        val folderGridItemsFlow =
            combine(
                userDataRepository.userDataFlow,
                folderGridItemRepository.folderGridItemWrappersFlow,
            ) { userData, folderGridItemWrappers ->
                folderGridItemWrappers.map {
                    it.asGridItem(
                        folderGridItemRepository = folderGridItemRepository,
                        maxFolderColumns = userData.homeSettings.maxFolderColumns,
                        maxFolderRows = userData.homeSettings.maxFolderRows,
                        fileManager = fileManager,
                        iconKeyGenerator = iconKeyGenerator,
                        iconPackInfoPackageName = userData.generalSettings.iconPackInfoPackageName,
                    )
                }
            }.flowOn(defaultDispatcher)

        return combine(
            gridItemsFlow,
            folderGridItemsFlow,
        ) { gridItems, folderGridItems ->
            (gridItems + folderGridItems).filter {
                when (val data = it.data) {
                    is GridItemData.ApplicationInfo -> data.folderId == null
                    is GridItemData.Folder -> data.folderId == null
                    is GridItemData.ShortcutConfig -> data.folderId == null
                    is GridItemData.ShortcutInfo -> data.folderId == null
                    is GridItemData.Widget -> false
                }
            }
        }
    }

    private fun getTextColorFromWallpaperColors(
        theme: Theme,
        colorHints: Int?,
    ): TextColor = if (colorHints != null) {
        val hintSupportsDarkText = colorHints and wallpaperManagerWrapper.hintSupportsDarkText != 0

        if (hintSupportsDarkText) {
            TextColor.Dark
        } else {
            TextColor.Light
        }
    } else {
        getTextColorFromSystemTheme(theme = theme)
    }

    private fun getTextColorFromSystemTheme(theme: Theme): TextColor = when (theme) {
        Theme.System -> {
            getTextColorFromSystemTheme(theme = resourcesWrapper.getSystemTheme())
        }

        Theme.Light -> {
            TextColor.Light
        }

        Theme.Dark -> {
            TextColor.Dark
        }
    }
}
