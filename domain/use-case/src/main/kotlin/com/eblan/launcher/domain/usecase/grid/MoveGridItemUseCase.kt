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
import com.eblan.launcher.domain.grid.getGridItemByCoordinates
import com.eblan.launcher.domain.grid.getRelativeResolveDirection
import com.eblan.launcher.domain.grid.getResolveDirectionByX
import com.eblan.launcher.domain.grid.isGridItemSpanWithinBounds
import com.eblan.launcher.domain.grid.rectanglesOverlap
import com.eblan.launcher.domain.grid.resolveConflicts
import com.eblan.launcher.domain.model.GridItem
import com.eblan.launcher.domain.model.GridItemData
import com.eblan.launcher.domain.model.MoveGridItemResult
import com.eblan.launcher.domain.model.ResolveDirection
import com.eblan.launcher.domain.repository.GridRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject

class MoveGridItemUseCase @Inject constructor(
    private val gridRepository: GridRepository,
    private val getFolderGridItemsUseCase: GetFolderGridItemsUseCase,
    @param:Dispatcher(EblanDispatchers.Default) private val defaultDispatcher: CoroutineDispatcher,
) {
    suspend operator fun invoke(
        movingGridItem: GridItem,
        x: Int,
        y: Int,
        columns: Int,
        rows: Int,
        gridWidth: Int,
        gridHeight: Int,
    ): MoveGridItemResult {
        return withContext(defaultDispatcher) {
            val gridItems = gridRepository.gridItemsFlow.first()

            val folderGridItems = getFolderGridItemsUseCase().first()

            val currentGridItems = gridItems.plus(folderGridItems).filter { gridItem ->
                ensureActive()

                isGridItemSpanWithinBounds(
                    gridItem = gridItem,
                    columns = columns,
                    rows = rows,
                ) && gridItem.page == movingGridItem.page &&
                    gridItem.associate == movingGridItem.associate
            }.toMutableList()

            val index =
                currentGridItems.indexOfFirst { gridItem ->
                    ensureActive()

                    gridItem.id == movingGridItem.id
                }

            if (index != -1) {
                currentGridItems[index] = movingGridItem
            } else {
                currentGridItems.add(movingGridItem)
            }

            val gridItemByCoordinates = getGridItemByCoordinates(
                id = movingGridItem.id,
                gridItems = currentGridItems,
                columns = columns,
                rows = rows,
                x = x,
                y = y,
                gridWidth = gridWidth,
                gridHeight = gridHeight,
            )

            if (gridItemByCoordinates != null) {
                return@withContext handleConflictsOfGridItemCoordinates(
                    gridItems = currentGridItems,
                    movingGridItem = movingGridItem,
                    conflictingGridItem = gridItemByCoordinates,
                    x = x,
                    columns = columns,
                    rows = rows,
                    gridWidth = gridWidth,
                )
            }

            val gridItemBySpan = currentGridItems.find { gridItem ->
                ensureActive()

                gridItem.id != movingGridItem.id && rectanglesOverlap(
                    moving = movingGridItem,
                    other = gridItem,
                )
            }

            if (gridItemBySpan != null) {
                return@withContext handleConflictsOfGridItemSpan(
                    movingGridItem = movingGridItem,
                    conflictingGridItem = gridItemBySpan,
                    gridItems = currentGridItems,
                    columns = columns,
                    rows = rows,
                )
            }

            gridRepository.upsertGridItems(gridItems = gridItems)

            return@withContext MoveGridItemResult(
                isSuccess = true,
                movingGridItem = movingGridItem,
                conflictingGridItem = null,
            )
        }
    }

    private suspend fun handleConflictsOfGridItemCoordinates(
        gridItems: MutableList<GridItem>,
        movingGridItem: GridItem,
        conflictingGridItem: GridItem,
        x: Int,
        columns: Int,
        rows: Int,
        gridWidth: Int,
    ): MoveGridItemResult {
        val resolveDirection = getResolveDirectionByX(
            gridItem = conflictingGridItem,
            x = x,
            columns = columns,
            gridWidth = gridWidth,
        )

        return when (resolveDirection) {
            ResolveDirection.Left, ResolveDirection.Right -> {
                val resolvedConflicts = resolveConflicts(
                    gridItems = gridItems,
                    resolveDirection = resolveDirection,
                    movingGridItem = movingGridItem,
                    columns = columns,
                    rows = rows,
                )

                if (resolvedConflicts) {
                    gridRepository.upsertGridItems(gridItems = gridItems)
                }

                MoveGridItemResult(
                    isSuccess = resolvedConflicts,
                    movingGridItem = movingGridItem,
                    conflictingGridItem = null,
                )
            }

            ResolveDirection.Center -> {
                if (movingGridItem.data is GridItemData.Widget ||
                    movingGridItem.data is GridItemData.Folder ||
                    conflictingGridItem.data is GridItemData.Widget
                ) {
                    return MoveGridItemResult(
                        isSuccess = false,
                        movingGridItem = movingGridItem,
                        conflictingGridItem = null,
                    )
                }

                gridRepository.upsertGridItems(gridItems = gridItems)

                MoveGridItemResult(
                    isSuccess = true,
                    movingGridItem = movingGridItem,
                    conflictingGridItem = conflictingGridItem,
                )
            }
        }
    }

    private suspend fun handleConflictsOfGridItemSpan(
        movingGridItem: GridItem,
        conflictingGridItem: GridItem,
        gridItems: MutableList<GridItem>,
        columns: Int,
        rows: Int,
    ): MoveGridItemResult {
        val resolveDirection = getRelativeResolveDirection(
            moving = movingGridItem,
            other = conflictingGridItem,
        ) ?: return MoveGridItemResult(
            isSuccess = false,
            movingGridItem = movingGridItem,
            conflictingGridItem = null,
        )

        val resolvedConflicts = resolveConflicts(
            gridItems = gridItems,
            resolveDirection = resolveDirection,
            movingGridItem = movingGridItem,
            columns = columns,
            rows = rows,
        )

        if (resolvedConflicts) {
            gridRepository.upsertGridItems(gridItems = gridItems)
        }

        return MoveGridItemResult(
            isSuccess = resolvedConflicts,
            movingGridItem = movingGridItem,
            conflictingGridItem = null,
        )
    }
}
