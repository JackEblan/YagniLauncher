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

import com.eblan.launcher.domain.common.Dispatcher
import com.eblan.launcher.domain.common.EblanDispatchers
import com.eblan.launcher.domain.model.ApplicationInfoGridItem
import com.eblan.launcher.domain.model.GridItem
import com.eblan.launcher.domain.model.GridItemData
import com.eblan.launcher.domain.repository.GridCacheRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import javax.inject.Inject

class MoveFolderGridItemUseCase @Inject constructor(
    private val gridCacheRepository: GridCacheRepository,
    @param:Dispatcher(EblanDispatchers.Default) private val defaultDispatcher: CoroutineDispatcher,
) {
    suspend operator fun invoke(
        folderGridItem: GridItem,
        applicationInfoGridItems: List<ApplicationInfoGridItem>,
        movingApplicationInfoGridItem: ApplicationInfoGridItem,
        dragX: Int,
        dragY: Int,
        columns: Int,
        rows: Int,
        gridWidth: Int,
        gridHeight: Int,
        currentPage: Int,
    ) {
        withContext(defaultDispatcher) {
            val gridItemsPerPage = columns * rows

            val cellWidth = gridWidth / columns
            val cellHeight = gridHeight / rows

            val targetColumn = dragX / cellWidth
            val targetRow = dragY / cellHeight

            val targetIndex = currentPage * gridItemsPerPage + targetRow * columns + targetColumn

            val currentApplicationInfoGridItems = applicationInfoGridItems.toMutableList()

            val movingIndex =
                currentApplicationInfoGridItems.indexOfFirst {
                    ensureActive()

                    it.id == movingApplicationInfoGridItem.id
                }

            val applicationInfoGridItem = if (movingIndex != -1) {
                currentApplicationInfoGridItems.removeAt(movingIndex)
            } else {
                movingApplicationInfoGridItem
            }

            currentApplicationInfoGridItems.add(
                targetIndex.coerceIn(
                    0,
                    currentApplicationInfoGridItems.size,
                ),
                applicationInfoGridItem,
            )

            val gridItems = currentApplicationInfoGridItems.mapIndexed { index, gridItem ->
                ensureActive()

                gridItem.copy(index = index)
            }

            val folderData = folderGridItem.data as? GridItemData.Folder
                ?: error("Expected GridItemData.Folder")

            val gridItemsByPage = gridItems.getGridItemsByPage()

            val firstPageGridItems = gridItemsByPage[0] ?: emptyList()

            val (columns, rows) = getGridDimension(count = firstPageGridItems.size)

            gridCacheRepository.updateGridItemData(
                id = folderGridItem.id,
                data = folderData.copy(
                    gridItems = gridItems,
                    gridItemsByPage = gridItemsByPage,
                    columns = columns,
                    rows = rows,
                ),
            )
        }
    }
}
