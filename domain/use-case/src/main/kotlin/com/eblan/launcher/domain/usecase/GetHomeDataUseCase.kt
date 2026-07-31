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
import com.eblan.launcher.domain.grid.isGridItemSpanWithinBounds
import com.eblan.launcher.domain.model.Associate
import com.eblan.launcher.domain.model.GridItem
import com.eblan.launcher.domain.model.HomeData
import com.eblan.launcher.domain.repository.ApplicationInfoGridItemRepository
import com.eblan.launcher.domain.repository.FolderGridItemRepository
import com.eblan.launcher.domain.repository.ShortcutConfigGridItemRepository
import com.eblan.launcher.domain.repository.ShortcutInfoGridItemRepository
import com.eblan.launcher.domain.repository.UserDataRepository
import com.eblan.launcher.domain.repository.WidgetGridItemRepository
import com.eblan.launcher.domain.usecase.grid.asGridItem
import com.eblan.launcher.domain.usecase.grid.asPreviewFolderGridItem
import com.eblan.launcher.domain.usecase.grid.isTopLevel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetHomeDataUseCase @Inject constructor(
    private val userDataRepository: UserDataRepository,
    private val launcherAppsWrapper: LauncherAppsWrapper,
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
    ) { userData, gridItems ->
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

        HomeData(
            userData = userData,
            gridItems = gridItems,
            gridItemsByPage = gridItemsByPage,
            dockGridItemsByPage = dockGridItemsByPage,
            hasShortcutHostPermission = launcherAppsWrapper.hasShortcutHostPermission,
            hasSystemFeatureAppWidgets = packageManagerWrapper.hasSystemFeatureAppWidgets,
        )
    }.flowOn(defaultDispatcher)

    private fun getGridItemsFlow(): Flow<List<GridItem>> {
        val gridItemsFlow = combine(
            userDataRepository.userDataFlow,
            folderGridItemRepository.folderGridItemWrappersFlow,
        ) { userData, folderGridItemWrappers ->
            val currentApplicationInfoGridItems =
                applicationInfoGridItemRepository.getApplicationInfoGridItems().map {
                    it.asGridItem(
                        fileManager = fileManager,
                        iconKeyGenerator = iconKeyGenerator,
                        iconPackInfoPackageName = userData.generalSettings.iconPackInfoPackageName,
                    )
                }

            val currentShortcutInfoGridItems =
                shortcutInfoGridItemRepository.getShortcutInfoGridItems().map {
                    it.asGridItem()
                }

            val currentShortcutConfigGridItems =
                shortcutConfigGridItemRepository.getShortcutConfigGridItems().map {
                    it.asGridItem()
                }

            val currentFolderGridItems = folderGridItemWrappers.map {
                it.asPreviewFolderGridItem(
                    fileManager = fileManager,
                    iconKeyGenerator = iconKeyGenerator,
                    iconPackInfoPackageName = userData.generalSettings.iconPackInfoPackageName,
                )
            }

            buildList {
                addAll(currentApplicationInfoGridItems)
                addAll(currentShortcutInfoGridItems)
                addAll(currentShortcutConfigGridItems)
                addAll(currentFolderGridItems)
            }
        }

        val widgetGridItems = widgetGridItemRepository.widgetGridItemsFlow.map { widgetGridItems ->
            widgetGridItems.map {
                it.asGridItem()
            }
        }

        return combine(
            gridItemsFlow,
            widgetGridItems,
        ) { gridItems, widgetGridItems ->
            (gridItems + widgetGridItems).filter { it.isTopLevel() }
        }
    }
}
