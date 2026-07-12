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

import com.eblan.launcher.domain.model.Anchor
import com.eblan.launcher.domain.model.GridItem
import com.eblan.launcher.domain.model.SideAnchor

fun resizeGridItemWithPixels(
    gridItem: GridItem,
    width: Int,
    height: Int,
    rows: Int,
    columns: Int,
    gridWidth: Int,
    gridHeight: Int,
    anchor: Anchor,
): GridItem {
    val cellWidth = gridWidth / columns

    val cellHeight = gridHeight / rows

    val (newColumnSpan, newRowSpan) = pixelDimensionsToGridSpan(
        width = width,
        height = height,
        gridCellWidth = cellWidth,
        gridCellHeight = cellHeight,
    )

    val (newStartColumn, newStartRow) = resizeGridItemByAnchor(
        gridItem = gridItem,
        columnSpan = newColumnSpan,
        rowSpan = newRowSpan,
        anchor = anchor,
    )

    return gridItem.copy(
        startColumn = newStartColumn,
        startRow = newStartRow,
        columnSpan = newColumnSpan,
        rowSpan = newRowSpan,
    )
}

fun resizeWidgetGridItemWithPixels(
    gridItem: GridItem,
    width: Int,
    height: Int,
    rows: Int,
    columns: Int,
    gridWidth: Int,
    gridHeight: Int,
    anchor: SideAnchor,
): GridItem {
    val cellWidth = gridWidth / columns

    val cellHeight = gridHeight / rows

    val (newColumnSpan, newRowSpan) = pixelDimensionsToGridSpan(
        width = width,
        height = height,
        gridCellWidth = cellWidth,
        gridCellHeight = cellHeight,
    )

    val (newStartColumn, newStartRow) = resizeGridItemBySideAnchor(
        gridItem = gridItem,
        columnSpan = newColumnSpan,
        rowSpan = newRowSpan,
        anchor = anchor,
    )

    return gridItem.copy(
        startColumn = newStartColumn,
        startRow = newStartRow,
        columnSpan = newColumnSpan,
        rowSpan = newRowSpan,
    )
}

private fun pixelDimensionsToGridSpan(
    width: Int,
    height: Int,
    gridCellWidth: Int,
    gridCellHeight: Int,
): Pair<Int, Int> {
    val columnSpan = ((width + gridCellWidth - 1) / gridCellWidth).coerceAtLeast(1)

    val rowSpan = ((height + gridCellHeight - 1) / gridCellHeight).coerceAtLeast(1)

    return columnSpan to rowSpan
}

private fun resizeGridItemByAnchor(
    gridItem: GridItem,
    columnSpan: Int,
    rowSpan: Int,
    anchor: Anchor,
): Pair<Int, Int> {
    val newStartColumn: Int

    val newStartRow: Int

    when (anchor) {
        Anchor.TOP_START -> {
            newStartColumn = gridItem.startColumn

            newStartRow = gridItem.startRow
        }

        Anchor.TOP_END -> {
            newStartColumn = gridItem.startColumn + gridItem.columnSpan - columnSpan

            newStartRow = gridItem.startRow
        }

        Anchor.BOTTOM_START -> {
            newStartColumn = gridItem.startColumn

            newStartRow = gridItem.startRow + gridItem.rowSpan - rowSpan
        }

        Anchor.BOTTOM_END -> {
            newStartColumn = gridItem.startColumn + gridItem.columnSpan - columnSpan

            newStartRow = gridItem.startRow + gridItem.rowSpan - rowSpan
        }
    }

    return newStartColumn to newStartRow
}

private fun resizeGridItemBySideAnchor(
    gridItem: GridItem,
    columnSpan: Int,
    rowSpan: Int,
    anchor: SideAnchor,
): Pair<Int, Int> {
    val newStartColumn: Int

    val newStartRow: Int

    when (anchor) {
        SideAnchor.TOP -> {
            newStartColumn = gridItem.startColumn

            newStartRow = gridItem.startRow
        }

        SideAnchor.BOTTOM -> {
            newStartColumn = gridItem.startColumn

            newStartRow = gridItem.startRow + gridItem.rowSpan - rowSpan
        }

        SideAnchor.LEFT -> {
            newStartColumn = gridItem.startColumn

            newStartRow = gridItem.startRow
        }

        SideAnchor.RIGHT -> {
            newStartColumn = gridItem.startColumn + gridItem.columnSpan - columnSpan

            newStartRow = gridItem.startRow
        }
    }

    return newStartColumn to newStartRow
}
