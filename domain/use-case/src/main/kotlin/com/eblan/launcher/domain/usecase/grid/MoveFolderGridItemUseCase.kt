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
        conflictingId: String,
        movingFolderGridItem: GridItem,
        data: GridItemData.Folder,
        dragX: Int,
        dragY: Int,
        columns: Int,
        rows: Int,
        gridWidth: Int,
        gridHeight: Int,
        currentPage: Int,
    ): GridItem {
        return withContext(defaultDispatcher) {
            val gridItemsPerPage = columns * rows

            val cellWidth = gridWidth / columns
            val cellHeight = gridHeight / rows

            val targetColumn = dragX / cellWidth
            val targetRow = dragY / cellHeight

            val targetIndex = currentPage * gridItemsPerPage + targetRow * columns + targetColumn

            val folderGridItems = data.gridItems.toMutableList()

            val movingIndex =
                folderGridItems.indexOfFirst {
                    ensureActive()

                    it.id == movingFolderGridItem.id
                }

            if (movingIndex != -1) {
                folderGridItems.add(
                    targetIndex.coerceIn(
                        0,
                        folderGridItems.size - 1,
                    ),
                    folderGridItems.removeAt(movingIndex),
                )
            }

            val indexedGridItems = folderGridItems.mapIndexed { index, gridItem ->
                ensureActive()

                when (val data = gridItem.data) {
                    is GridItemData.ApplicationInfo -> {
                        gridItem.copy(data = data.copy(index = index))
                    }

                    is GridItemData.ShortcutConfig -> {
                        gridItem.copy(data = data.copy(index = index))
                    }

                    is GridItemData.ShortcutInfo -> {
                        gridItem.copy(data = data.copy(index = index))
                    }

                    else -> error("Unsupported folder item type: ${data::class.simpleName}")
                }
            }

            val gridItemsByPage = indexedGridItems.getGridItemsByPage()

            val firstPageGridItems = gridItemsByPage[0] ?: emptyList()

            val (columns, rows) = getGridDimension(count = firstPageGridItems.size)

            gridCacheRepository.updateGridItemData(
                id = conflictingId,
                data = data.copy(
                    gridItems = indexedGridItems,
                    gridItemsByPage = gridItemsByPage,
                    columns = columns,
                    rows = rows,
                ),
            )

            movingFolderGridItem
        }
    }
}
