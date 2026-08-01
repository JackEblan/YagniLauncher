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
import com.eblan.launcher.domain.repository.GridRepository
import com.eblan.launcher.domain.repository.UserDataRepository
import com.eblan.launcher.domain.usecase.grid.asGridItem
import com.eblan.launcher.domain.usecase.grid.isTopLevel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

class GetHomeDataUseCase @Inject constructor(
    private val userDataRepository: UserDataRepository,
    private val launcherAppsWrapper: LauncherAppsWrapper,
    private val packageManagerWrapper: PackageManagerWrapper,
    private val fileManager: FileManager,
    private val iconKeyGenerator: IconKeyGenerator,
    private val gridRepository: GridRepository,
    @param:Dispatcher(EblanDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
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
    }.flowOn(ioDispatcher)

    private fun getGridItemsFlow(): Flow<List<GridItem>> = combine(
        userDataRepository.userDataFlow,
        gridRepository.gridItemsFlow,
    ) { userData, gridItems ->
        val currentApplicationInfoGridItems = gridItems.applicationInfoGridItems.map {
            it.asGridItem(
                fileManager = fileManager,
                iconKeyGenerator = iconKeyGenerator,
                iconPackInfoPackageName = userData.generalSettings.iconPackInfoPackageName,
            )
        }

        val currentWidgetGridItems = gridItems.widgetGridItems.map {
            it.asGridItem()
        }

        val currentShortcutInfoGridItems = gridItems.shortcutInfoGridItems.map {
            it.asGridItem()
        }

        val currentShortcutConfigGridItems = gridItems.shortcutConfigGridItems.map {
            it.asGridItem()
        }

        val currentFolderGridItems = gridItems.folderGridItemWrappers.map {
            it.asGridItem(
                fileManager = fileManager,
                iconKeyGenerator = iconKeyGenerator,
                iconPackInfoPackageName = userData.generalSettings.iconPackInfoPackageName,
            )
        }

        buildList {
            addAll(currentApplicationInfoGridItems)
            addAll(currentWidgetGridItems)
            addAll(currentShortcutInfoGridItems)
            addAll(currentShortcutConfigGridItems)
            addAll(currentFolderGridItems)
        }.filter { it.isTopLevel() }
    }
}
