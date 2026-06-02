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
import com.eblan.launcher.domain.model.HomeData
import com.eblan.launcher.domain.model.TextColor
import com.eblan.launcher.domain.model.Theme
import com.eblan.launcher.domain.repository.FolderGridItemRepository
import com.eblan.launcher.domain.repository.GridRepository
import com.eblan.launcher.domain.repository.UserDataRepository
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
    private val gridRepository: GridRepository,
    private val folderGridItemRepository: FolderGridItemRepository,
    private val fileManager: FileManager,
    private val iconKeyGenerator: IconKeyGenerator,
    @param:Dispatcher(EblanDispatchers.Default) private val defaultDispatcher: CoroutineDispatcher,
) {
    operator fun invoke(): Flow<HomeData> {
        val folderGridItemsFlow =
            combine(
                userDataRepository.userDataFlow,
                folderGridItemRepository.folderGridItemWrappersFlow,
            ) { userData, folderGridItemWrappers ->
                folderGridItemWrappers.map { folderGridItemWrapper ->
                    folderGridItemWrapper.asGridItem(
                        folderGridItemRepository = folderGridItemRepository,
                        maxFolderColumns = userData.homeSettings.maxFolderColumns,
                        maxFolderRows = userData.homeSettings.maxFolderRows,
                        fileManager = fileManager,
                        iconKeyGenerator = iconKeyGenerator,
                        iconPackInfoPackageName = userData.generalSettings.iconPackInfoPackageName,
                    )
                }
            }

        return combine(
            userDataRepository.userDataFlow,
            gridRepository.gridItemsFlow,
            folderGridItemsFlow,
            wallpaperManagerWrapper.getColorsChanged(),
        ) { userData, gridItems, folderGridItems, colorHints ->
            val currentGridItems = gridItems + folderGridItems

            val gridItemsByPage = currentGridItems.filter {
                isGridItemSpanWithinBounds(
                    gridItem = it,
                    columns = userData.homeSettings.columns,
                    rows = userData.homeSettings.rows,
                ) && it.associate == Associate.Grid
            }.groupBy { it.page }

            val dockGridItemsByPage = currentGridItems.filter {
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
                gridItems = currentGridItems,
                gridItemsByPage = gridItemsByPage,
                dockGridItemsByPage = dockGridItemsByPage,
                hasShortcutHostPermission = launcherAppsWrapper.hasShortcutHostPermission,
                hasSystemFeatureAppWidgets = packageManagerWrapper.hasSystemFeatureAppWidgets,
                textColor = textColor,
            )
        }.flowOn(defaultDispatcher)
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
