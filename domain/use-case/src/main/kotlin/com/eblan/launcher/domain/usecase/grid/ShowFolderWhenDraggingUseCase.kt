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
package com.eblan.launcher.domain.usecase.grid

import com.eblan.launcher.domain.common.dispatcher.Dispatcher
import com.eblan.launcher.domain.common.dispatcher.EblanDispatchers
import com.eblan.launcher.domain.model.ApplicationInfoGridItem
import com.eblan.launcher.domain.model.GridItem
import com.eblan.launcher.domain.model.GridItemData
import com.eblan.launcher.domain.repository.GridCacheRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject

class ShowFolderWhenDraggingUseCase @Inject constructor(
    private val gridCacheRepository: GridCacheRepository,
    @param:Dispatcher(EblanDispatchers.Default) private val defaultDispatcher: CoroutineDispatcher,
) {
    suspend operator fun invoke(
        conflictingGridItem: GridItem,
        movingGridItem: GridItem,
    ) {
        withContext(defaultDispatcher) {
            val conflictingData =
                conflictingGridItem.data as? GridItemData.Folder
                    ?: error("Expected GridItemData.Folder")

            val movingData =
                movingGridItem.data as? GridItemData.ApplicationInfo
                    ?: error("Expected GridItemData.Application")

            val currentApplicationInfoGridItems = conflictingData.gridItems.toMutableList()

            val index = currentApplicationInfoGridItems.indexOfFirst { gridItem ->
                gridItem.id == movingGridItem.id
            }

            val applicationInfoGridItem = ApplicationInfoGridItem(
                id = movingGridItem.id,
                page = movingGridItem.page,
                startColumn = movingGridItem.startColumn,
                startRow = movingGridItem.startRow,
                columnSpan = movingGridItem.columnSpan,
                rowSpan = movingGridItem.rowSpan,
                associate = movingGridItem.associate,
                componentName = movingData.componentName,
                packageName = movingData.packageName,
                icon = movingData.icon,
                label = movingData.label,
                override = movingGridItem.override,
                serialNumber = movingData.serialNumber,
                customIcon = movingData.customIcon,
                customLabel = movingData.customLabel,
                gridItemSettings = movingGridItem.gridItemSettings,
                doubleTap = movingGridItem.doubleTap,
                swipeUp = movingGridItem.swipeUp,
                swipeDown = movingGridItem.swipeDown,
                index = conflictingData.gridItems.lastIndex + 1,
                folderId = conflictingData.id,
            )

            if (index != -1) {
                currentApplicationInfoGridItems[index] = applicationInfoGridItem
            } else {
                currentApplicationInfoGridItems.add(applicationInfoGridItem)
            }

            val gridItems = currentApplicationInfoGridItems.mapIndexed { index, gridItem ->
                gridItem.copy(index = index)
            }

            val gridItemsByPage = gridItems.getGridItemsByPage()

            val firstPageGridItems = gridItemsByPage[0] ?: emptyList()

            val (columns, rows) = getGridDimension(count = firstPageGridItems.size)

            val newData = conflictingData.copy(
                gridItems = gridItems,
                gridItemsByPage = gridItemsByPage,
                columns = columns,
                rows = rows,
            )

            gridCacheRepository.updateGridItemData(
                id = conflictingGridItem.id,
                data = newData,
            )
        }
    }
}
