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
package com.eblan.launcher.domain.grid

import com.eblan.launcher.domain.model.GridItem
import com.eblan.launcher.domain.model.ResolveDirection
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

/**
 * Resolves overlapping grid items by moving conflicting items in the specified
 * direction until all conflicts are resolved or an item cannot be moved.
 *
 * Instead of scanning the entire [gridItems] list for every queued item, this
 * implementation maintains a spatial index mapping each occupied grid cell to
 * the IDs of the items occupying that cell. This significantly reduces the
 * cost
 * of finding potential conflicts, especially for large grids.
 *
 * The [gridItems] list is mutated in place. Its size and ordering are preserved,
 * so the item's index remains stable when a [GridItem] is replaced after being
 * moved.
 *
 * Conflict processing preserves the original list ordering by sorting the
 * discovered conflicting IDs by their corresponding index in [gridItems].
 * This is important because changing the order in which conflicts are resolved
 * can potentially change the resulting grid layout.
 *
 * The spatial index is kept synchronized whenever an item is moved:
 *
 * 1. The conflicting item is removed from its old occupied cells.
 * 2. [moveGridItem] calculates its new position.
 * 3. The moved item replaces the original item at the same list index.
 * 4. The moved item is added to its new occupied cells.
 * 5. The moved item is queued so its own conflicts can be resolved.
 *
 * A [MutableSet] is used for each occupied cell rather than storing a single
 * item ID because the initial grid may contain overlapping items.
 *
 * This approach avoids repeatedly performing an O(n) scan of the entire grid
 * and provides substantially better scaling for large numbers of grid items.
 *
 * @return `true` when all conflicts are successfully resolved, or `false` when
 *         [moveGridItem] cannot resolve a conflict.
 */
suspend fun resolveConflicts(
    gridItems: MutableList<GridItem>,
    resolveDirection: ResolveDirection,
    movingGridItem: GridItem,
    columns: Int,
    rows: Int,
): Boolean {
    val queue = ArrayDeque<String>()

    /*
     * The list itself does not change size or order, so an item's index
     * remains stable even when we replace the GridItem at that index.
     */
    val gridItemsByIndex = gridItems
        .mapIndexed { index, gridItem -> gridItem.id to index }
        .toMap()

    /*
     * Maps every occupied cell to the IDs of the items occupying it.
     *
     * A Set is used instead of a single ID because the initial grid may
     * contain overlapping items.
     */
    val gridItemsByCell = HashMap<Long, MutableSet<String>>()

    gridItems.forEach {
        addToOccupancy(
            gridItem = it,
            gridItemsByCell = gridItemsByCell,
        )
    }

    queue.add(movingGridItem.id)

    while (queue.isNotEmpty()) {
        currentCoroutineContext().ensureActive()

        val currentId = queue.removeFirst()

        val currentIndex = gridItemsByIndex[currentId]
            ?: continue

        val currentGridItem = gridItems[currentIndex]

        /*
         * Instead of scanning every GridItem, only inspect items occupying
         * cells covered by the current item.
         */
        val conflictingIds = findConflictingIds(
            gridItem = currentGridItem,
            gridItemsByCell = gridItemsByCell,
        )

        /*
         * Preserve the same ordering as the old implementation:
         *
         * for (i in gridItems.indices)
         *
         * This is important because changing conflict-processing order could
         * potentially change the final layout.
         */
        conflictingIds.sortBy { gridItemsByIndex[it] }

        for (conflictingId in conflictingIds) {
            currentCoroutineContext().ensureActive()

            val conflictingIndex = gridItemsByIndex[conflictingId]
                ?: continue

            val conflictingGridItem = gridItems[conflictingIndex]

            /*
             * Remove the conflicting item from its old cells before moving it.
             */
            removeFromOccupancy(
                gridItem = conflictingGridItem,
                gridItemsByCell = gridItemsByCell,
            )

            val movedGridItem = moveGridItem(
                resolveDirection = resolveDirection,
                moving = currentGridItem,
                conflicting = conflictingGridItem,
                columns = columns,
                rows = rows,
            ) ?: return false

            gridItems[conflictingIndex] = movedGridItem

            /*
             * Add the moved item at its new position.
             */
            addToOccupancy(
                gridItem = movedGridItem,
                gridItemsByCell = gridItemsByCell,
            )

            /*
             * The moved item now needs its own conflicts resolved.
             */
            queue.add(movedGridItem.id)
        }
    }

    return true
}

/**
 * Finds all grid items that occupy at least one cell also occupied by [gridItem].
 *
 * The spatial index allows conflict candidates to be found by looking up only
 * the cells occupied by [gridItem], rather than scanning the entire grid.
 *
 * A [LinkedHashSet] is used to ensure that an item occupying multiple cells is
 * returned only once while preserving the order in which candidates are found.
 *
 * @return a mutable list containing the IDs of all potential conflicting items.
 */
private fun findConflictingIds(
    gridItem: GridItem,
    gridItemsByCell: Map<Long, Set<String>>,
): MutableList<String> {
    val conflictingIds = LinkedHashSet<String>()

    forEachOccupiedCell(gridItem) { column, row ->
        val key = cellKey(
            column = column,
            row = row,
        )

        gridItemsByCell[key]?.forEach { id ->
            if (id != gridItem.id) {
                conflictingIds.add(id)
            }
        }
    }

    return conflictingIds.toMutableList()
}

/**
 * Adds [gridItem] to the spatial occupancy index.
 *
 * Every cell occupied by the item is mapped to the item's ID. Multiple items
 * may occupy the same cell, so each cell maintains a set of item IDs.
 */
private fun addToOccupancy(
    gridItem: GridItem,
    gridItemsByCell: MutableMap<Long, MutableSet<String>>,
) {
    forEachOccupiedCell(gridItem) { column, row ->
        val key = cellKey(
            column = column,
            row = row,
        )

        gridItemsByCell
            .getOrPut(key) { LinkedHashSet() }
            .add(gridItem.id)
    }
}

/**
 * Removes [gridItem] from the spatial occupancy index.
 *
 * Empty cell entries are removed from [gridItemsByCell] after the item has
 * been removed to prevent stale entries from accumulating.
 */
private fun removeFromOccupancy(
    gridItem: GridItem,
    gridItemsByCell: MutableMap<Long, MutableSet<String>>,
) {
    forEachOccupiedCell(gridItem) { column, row ->
        val key = cellKey(
            column = column,
            row = row,
        )

        val items = gridItemsByCell[key]
            ?: return@forEachOccupiedCell

        items.remove(gridItem.id)

        if (items.isEmpty()) {
            gridItemsByCell.remove(key)
        }
    }
}

/**
 * Iterates over every grid cell occupied by [GridItem].
 *
 * The iteration accounts for both the item's starting position and its
 * [GridItem.columnSpan] and [GridItem.rowSpan].
 */
private inline fun forEachOccupiedCell(
    item: GridItem,
    action: (
        column: Int,
        row: Int,
    ) -> Unit,
) {
    for (row in item.startRow until item.startRow + item.rowSpan) {
        for (column in item.startColumn until item.startColumn + item.columnSpan) {
            action(column, row)
        }
    }
}

/**
 * Creates a compact, collision-free key for a grid cell.
 *
 * The column and row are packed into a single [Long] to avoid allocating
 * objects such as [Pair] while accessing the spatial index.
 */
private fun cellKey(
    column: Int,
    row: Int,
): Long = (row.toLong() shl 32) or
    (column.toLong() and 0xFFFF_FFFFL)

/**
 * Moves the conflicting grid item according to the specified [resolveDirection].
 *
 * @return the updated conflicting item, or `null` when it cannot be moved.
 */
private fun moveGridItem(
    resolveDirection: ResolveDirection,
    moving: GridItem,
    conflicting: GridItem,
    columns: Int,
    rows: Int,
): GridItem? = when (resolveDirection) {
    ResolveDirection.Left -> {
        moveGridItemToLeft(
            moving = moving,
            conflicting = conflicting,
            columns = columns,
            rows = rows,
        )
    }

    ResolveDirection.Right -> {
        moveGridItemToRight(
            moving = moving,
            conflicting = conflicting,
            columns = columns,
            rows = rows,
        )
    }

    ResolveDirection.Center -> moving
}

/**
 * Moves the conflicting grid item to the right of the moving item.
 *
 * When there is insufficient horizontal space, the item wraps to the next row.
 *
 * @return the moved item, or `null` when there is insufficient grid space.
 */
private fun moveGridItemToRight(
    moving: GridItem,
    conflicting: GridItem,
    columns: Int,
    rows: Int,
): GridItem? {
    var newStartColumn = moving.startColumn + moving.columnSpan
    var newStartRow = conflicting.startRow

    if (newStartColumn + conflicting.columnSpan > columns) {
        newStartColumn = 0
        newStartRow = moving.startRow + moving.rowSpan
    }

    return if (newStartRow + conflicting.rowSpan <= rows) {
        conflicting.copy(
            startColumn = newStartColumn,
            startRow = newStartRow,
        )
    } else {
        null
    }
}

/**
 * Moves the conflicting grid item to the left of the moving item.
 *
 * When there is insufficient horizontal space, the item wraps to the previous
 * row.
 *
 * @return the moved item, or `null` when there is insufficient grid space.
 */
private fun moveGridItemToLeft(
    moving: GridItem,
    conflicting: GridItem,
    columns: Int,
    rows: Int,
): GridItem? {
    var newStartColumn = moving.startColumn - conflicting.columnSpan
    var newStartRow = conflicting.startRow

    if (newStartColumn < 0) {
        newStartColumn = columns - conflicting.columnSpan
        newStartRow = moving.startRow - 1
    }

    return if (
        newStartRow >= 0 &&
        newStartRow + conflicting.rowSpan <= rows
    ) {
        conflicting.copy(
            startColumn = newStartColumn,
            startRow = newStartRow,
        )
    } else {
        null
    }
}
