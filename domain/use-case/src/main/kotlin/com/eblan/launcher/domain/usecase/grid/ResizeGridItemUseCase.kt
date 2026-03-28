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
import com.eblan.launcher.domain.grid.getRelativeResolveDirection
import com.eblan.launcher.domain.grid.isGridItemSpanWithinBounds
import com.eblan.launcher.domain.grid.rectanglesOverlap
import com.eblan.launcher.domain.grid.resolveConflicts
import com.eblan.launcher.domain.model.GridItem
import com.eblan.launcher.domain.repository.GridCacheRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject

class ResizeGridItemUseCase @Inject constructor(
    private val gridCacheRepository: GridCacheRepository,
    @param:Dispatcher(EblanDispatchers.Default) private val defaultDispatcher: CoroutineDispatcher,
) {
    suspend operator fun invoke(
        resizingGridItem: GridItem,
        columns: Int,
        rows: Int,
    ): GridItem = withContext(defaultDispatcher) {
        val gridItems = gridCacheRepository.gridItemsCache.first().filter { gridItem ->
            isGridItemSpanWithinBounds(
                gridItem = gridItem,
                columns = columns,
                rows = rows,
            ) && gridItem.page == resizingGridItem.page &&
                gridItem.associate == resizingGridItem.associate
        }.toMutableList()

        val index =
            gridItems.indexOfFirst { gridItem -> gridItem.id == resizingGridItem.id }

        val oldGridItem = gridItems[index]

        gridItems[index] = resizingGridItem

        val gridItemBySpan = gridItems.find { gridItem ->
            gridItem.id != resizingGridItem.id && rectanglesOverlap(
                moving = resizingGridItem,
                other = gridItem,
            )
        }

        if (gridItemBySpan != null) {
            handleConflictsOfGridItemSpan(
                oldGridItem = oldGridItem,
                conflictingGridItem = gridItemBySpan,
                gridItems = gridItems,
                resizingGridItem = resizingGridItem,
                columns = columns,
                rows = rows,
            )
        } else {
            gridCacheRepository.upsertGridItems(gridItems = gridItems)

            resizingGridItem
        }
    }

    private suspend fun handleConflictsOfGridItemSpan(
        oldGridItem: GridItem,
        conflictingGridItem: GridItem,
        gridItems: MutableList<GridItem>,
        resizingGridItem: GridItem,
        columns: Int,
        rows: Int,
    ): GridItem {
        val resolveDirection = getRelativeResolveDirection(
            moving = oldGridItem,
            other = conflictingGridItem,
        ) ?: return oldGridItem

        val resolvedConflicts = resolveConflicts(
            gridItems = gridItems,
            resolveDirection = resolveDirection,
            movingGridItem = resizingGridItem,
            columns = columns,
            rows = rows,
        )

        return if (resolvedConflicts) {
            gridCacheRepository.upsertGridItems(gridItems = gridItems)

            resizingGridItem
        } else {
            oldGridItem
        }
    }
}
