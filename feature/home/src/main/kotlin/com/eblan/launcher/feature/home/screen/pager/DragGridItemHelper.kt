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
package com.eblan.launcher.feature.home.screen.pager

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.eblan.launcher.domain.grid.getWidgetGridItemSize
import com.eblan.launcher.domain.grid.getWidgetGridItemSpan
import com.eblan.launcher.domain.grid.isGridItemSpanWithinBounds
import com.eblan.launcher.domain.model.Associate
import com.eblan.launcher.domain.model.FolderGridItemId
import com.eblan.launcher.domain.model.GridItem
import com.eblan.launcher.domain.model.GridItemData
import com.eblan.launcher.domain.model.MoveGridItemResult
import com.eblan.launcher.feature.home.model.Drag
import com.eblan.launcher.feature.home.model.GridItemSource
import com.eblan.launcher.feature.home.model.PageDirection
import com.eblan.launcher.feature.home.model.SharedElementKey
import com.eblan.launcher.feature.home.util.PAGE_INDICATOR_HEIGHT
import kotlinx.coroutines.delay

internal fun handleAnimateScrollToPage(
    associate: Associate?,
    density: Density,
    dragIntOffset: IntOffset,
    gridItemSource: GridItemSource?,
    isDragging: Boolean,
    paddingValues: PaddingValues,
    screenWidth: Int,
    layoutDirection: LayoutDirection,
    onUpdateDockPageDirection: (PageDirection?) -> Unit,
    onUpdateGridPageDirection: (PageDirection?) -> Unit,
) {
    if (gridItemSource == null || !isDragging) return

    val leftPadding = with(density) {
        paddingValues.calculateLeftPadding(layoutDirection).roundToPx()
    }

    val rightPadding = with(density) {
        paddingValues.calculateRightPadding(layoutDirection).roundToPx()
    }

    val horizontalPadding = leftPadding + rightPadding

    val safeDrawingWidth = screenWidth - horizontalPadding

    val edgeDistance = with(density) {
        20.dp.roundToPx()
    }

    val dragX = dragIntOffset.x - leftPadding

    when (gridItemSource) {
        is GridItemSource.Existing, is GridItemSource.New, is GridItemSource.Pin -> {
            animateScrollToPage(
                associate = associate,
                dragX = dragX,
                edgeDistance = edgeDistance,
                safeDrawingWidth = safeDrawingWidth,
                onUpdateDockPageDirection = onUpdateDockPageDirection,
                onUpdateGridPageDirection = onUpdateGridPageDirection,
            )
        }

        else -> Unit
    }
}

internal suspend fun handleDragGridItem(
    columns: Int,
    gridCurrentPage: Int,
    dockGridCurrentPage: Int,
    density: Density,
    dockColumns: Int,
    dockHeight: Dp,
    dockRows: Int,
    drag: Drag,
    dragIntOffset: IntOffset,
    gridItemSource: GridItemSource?,
    isDragging: Boolean,
    isVisibleOverlay: Boolean,
    isGridScrollInProgress: Boolean,
    isDockScrollInProgress: Boolean,
    lockMovement: Boolean,
    paddingValues: PaddingValues,
    rows: Int,
    screenHeight: Int,
    screenWidth: Int,
    moveGridItemResult: MoveGridItemResult?,
    layoutDirection: LayoutDirection,
    onMoveGridItem: (
        movingGridItem: GridItem,
        x: Int,
        y: Int,
        columns: Int,
        rows: Int,
        gridWidth: Int,
        gridHeight: Int,
    ) -> Unit,
    onUpdateAssociate: (Associate) -> Unit,
    onUpdateSharedElementKey: (SharedElementKey?) -> Unit,
) {
    if (drag != Drag.Dragging ||
        isGridScrollInProgress ||
        isDockScrollInProgress ||
        gridItemSource == null ||
        !isVisibleOverlay ||
        !isDragging ||
        lockMovement ||
        moveGridItemResult == null
    ) {
        return
    }

    delay(50L)

    val leftPadding = with(density) {
        paddingValues.calculateLeftPadding(layoutDirection).roundToPx()
    }

    val rightPadding = with(density) {
        paddingValues.calculateRightPadding(layoutDirection).roundToPx()
    }

    val topPadding = with(density) {
        paddingValues.calculateTopPadding().roundToPx()
    }

    val bottomPadding = with(density) {
        paddingValues.calculateBottomPadding().roundToPx()
    }

    val dockHeightPx = with(density) {
        dockHeight.roundToPx()
    }

    val pageIndicatorHeightPx = with(density) {
        PAGE_INDICATOR_HEIGHT.roundToPx()
    }

    val horizontalPadding = leftPadding + rightPadding

    val verticalPadding = topPadding + bottomPadding

    val safeDrawingWidth = screenWidth - horizontalPadding

    val safeDrawingHeight = screenHeight - verticalPadding

    val localDragX = dragIntOffset.x - leftPadding

    val localDragY = dragIntOffset.y - topPadding

    val isOnDock = dockHeightPx > 0 && localDragY > safeDrawingHeight - dockHeightPx

    when (gridItemSource) {
        is GridItemSource.Existing,
        is GridItemSource.New,
        is GridItemSource.Pin,
        -> {
            if (isOnDock) {
                dragDockGridItem(
                    currentPage = dockGridCurrentPage,
                    dockColumns = dockColumns,
                    dockHeightPx = dockHeightPx,
                    dockRows = dockRows,
                    dragX = localDragX,
                    dragY = localDragY,
                    gridItemSource = gridItemSource,
                    safeDrawingHeight = safeDrawingHeight,
                    safeDrawingWidth = safeDrawingWidth,
                    moveGridItemResult = moveGridItemResult,
                    onMoveGridItem = onMoveGridItem,
                    onUpdateAssociate = onUpdateAssociate,
                    onUpdateSharedElementKey = onUpdateSharedElementKey,
                )
            } else {
                dragGridItem(
                    columns = columns,
                    currentPage = gridCurrentPage,
                    dockHeightPx = dockHeightPx,
                    dragX = localDragX,
                    dragY = localDragY,
                    gridItemSource = gridItemSource,
                    pageIndicatorHeightPx = pageIndicatorHeightPx,
                    rows = rows,
                    safeDrawingHeight = safeDrawingHeight,
                    safeDrawingWidth = safeDrawingWidth,
                    moveGridItemResult = moveGridItemResult,
                    onMoveGridItem = onMoveGridItem,
                    onUpdateAssociate = onUpdateAssociate,
                    onUpdateSharedElementKey = onUpdateSharedElementKey,
                )
            }
        }

        else -> Unit
    }
}

private fun dragGridItem(
    columns: Int,
    currentPage: Int,
    dockHeightPx: Int,
    dragX: Int,
    dragY: Int,
    gridItemSource: GridItemSource,
    pageIndicatorHeightPx: Int,
    rows: Int,
    safeDrawingHeight: Int,
    safeDrawingWidth: Int,
    moveGridItemResult: MoveGridItemResult,
    onMoveGridItem: (
        movingGridItem: GridItem,
        x: Int,
        y: Int,
        columns: Int,
        rows: Int,
        gridWidth: Int,
        gridHeight: Int,
    ) -> Unit,
    onUpdateAssociate: (Associate) -> Unit,
    onUpdateSharedElementKey: (SharedElementKey?) -> Unit,
) {
    val gridItem = moveGridItemResult.movingGridItem

    onUpdateAssociate(Associate.Grid)

    onUpdateSharedElementKey(
        SharedElementKey(
            id = gridItem.id,
            parent = SharedElementKey.Parent.Grid,
        ),
    )

    val gridHeightWithPadding = safeDrawingHeight - dockHeightPx - pageIndicatorHeightPx

    val cellWidth = safeDrawingWidth / columns

    val cellHeight = gridHeightWithPadding / rows

    val moveGridItem = getMoveGridItemAndResetFolderId(
        associate = Associate.Grid,
        cellHeight = cellHeight,
        cellWidth = cellWidth,
        columns = columns,
        gridHeight = gridHeightWithPadding,
        gridItem = gridItem,
        gridItemSource = gridItemSource,
        gridWidth = safeDrawingWidth,
        gridX = dragX,
        gridY = dragY,
        rows = rows,
        currentPage = currentPage,
    )

    val isGridItemSpanWithinBounds = isGridItemSpanWithinBounds(
        gridItem = moveGridItem,
        columns = columns,
        rows = rows,
    )

    if (isGridItemSpanWithinBounds) {
        onMoveGridItem(
            moveGridItem,
            dragX,
            dragY,
            columns,
            rows,
            safeDrawingWidth,
            gridHeightWithPadding,
        )
    }
}

private fun dragDockGridItem(
    currentPage: Int,
    dockColumns: Int,
    dockHeightPx: Int,
    dockRows: Int,
    dragX: Int,
    dragY: Int,
    gridItemSource: GridItemSource,
    safeDrawingHeight: Int,
    safeDrawingWidth: Int,
    moveGridItemResult: MoveGridItemResult,
    onMoveGridItem: (
        movingGridItem: GridItem,
        x: Int,
        y: Int,
        columns: Int,
        rows: Int,
        gridWidth: Int,
        gridHeight: Int,
    ) -> Unit,
    onUpdateAssociate: (Associate) -> Unit,
    onUpdateSharedElementKey: (SharedElementKey?) -> Unit,
) {
    val gridItem = moveGridItemResult.movingGridItem

    onUpdateAssociate(Associate.Dock)

    onUpdateSharedElementKey(
        SharedElementKey(
            id = gridItem.id,
            parent = SharedElementKey.Parent.Dock,
        ),
    )

    val cellWidth = safeDrawingWidth / dockColumns

    val cellHeight = dockHeightPx / dockRows

    val dockY = dragY - (safeDrawingHeight - dockHeightPx)

    val moveGridItem = getMoveGridItemAndResetFolderId(
        associate = Associate.Dock,
        cellHeight = cellHeight,
        cellWidth = cellWidth,
        columns = dockColumns,
        gridHeight = dockHeightPx,
        gridItem = gridItem,
        gridItemSource = gridItemSource,
        gridWidth = safeDrawingWidth,
        gridX = dragX,
        gridY = dockY,
        rows = dockRows,
        currentPage = currentPage,
    )

    if (isGridItemSpanWithinBounds(
            gridItem = moveGridItem,
            columns = dockColumns,
            rows = dockRows,
        )
    ) {
        onMoveGridItem(
            moveGridItem,
            dragX,
            dockY,
            dockColumns,
            dockRows,
            safeDrawingWidth,
            dockHeightPx,
        )
    }
}

internal suspend fun handleConflictingGridItem(
    drag: Drag,
    isDragging: Boolean,
    isVisibleOverlay: Boolean,
    moveGridItemResult: MoveGridItemResult?,
    lockMovement: Boolean,
    intOffset: IntOffset,
    intSize: IntSize,
    gridItem: GridItem,
    onShowFolderWhenDragging: (
        folderGridItemId: FolderGridItemId,
        movingGridItem: GridItem,
    ) -> Unit,
    onUpdateFolderPopupBounds: (
        intOffset: IntOffset,
        intSize: IntSize,
    ) -> Unit,
    onUpdateSharedElementKey: (SharedElementKey?) -> Unit,
) {
    val conflictingGridItem = moveGridItemResult?.conflictingGridItem ?: return

    val conflictingData = conflictingGridItem.data as? GridItemData.Folder ?: return

    if (drag != Drag.Dragging ||
        !moveGridItemResult.isSuccess ||
        !isVisibleOverlay ||
        !isDragging ||
        lockMovement ||
        conflictingGridItem.id != gridItem.id
    ) {
        return
    }

    delay(1000L)

    val movingGridItem = moveGridItemResult.movingGridItem

    val movingData = when (val data = movingGridItem.data) {
        is GridItemData.ApplicationInfo -> data.copy(
            index = conflictingData.maxIndex,
            folderId = conflictingData.id,
        )

        is GridItemData.ShortcutConfig -> data.copy(
            index = conflictingData.maxIndex,
            folderId = conflictingData.id,
        )

        is GridItemData.ShortcutInfo -> data.copy(
            index = conflictingData.maxIndex,
            folderId = conflictingData.id,
        )

        is GridItemData.Folder -> data.copy(
            index = conflictingData.maxIndex,
            folderId = conflictingData.id,
        )

        else -> error("Unsupported Folder GridItem ")
    }

    val movingFolderGridItem = movingGridItem.copy(data = movingData)

    onUpdateFolderPopupBounds(
        intOffset,
        intSize,
    )

    onUpdateSharedElementKey(
        SharedElementKey(
            id = movingFolderGridItem.id,
            parent = SharedElementKey.Parent.Folder,
        ),
    )

    onShowFolderWhenDragging(
        FolderGridItemId(
            id = conflictingGridItem.id,
            x = intOffset.x,
            y = intOffset.y,
            width = intSize.width,
            height = intSize.height,
        ),
        movingFolderGridItem,
    )
}

private fun getMoveGridItemAndResetFolderId(
    associate: Associate,
    cellHeight: Int,
    cellWidth: Int,
    columns: Int,
    gridHeight: Int,
    gridItem: GridItem,
    gridItemSource: GridItemSource,
    gridWidth: Int,
    gridX: Int,
    gridY: Int,
    rows: Int,
    currentPage: Int,
): GridItem = when (gridItemSource) {
    is GridItemSource.Existing, is GridItemSource.Folder,
    -> {
        val (startColumn, startRow) = getStartPosition(
            x = gridX,
            y = gridY,
            cellWidth = cellWidth,
            cellHeight = cellHeight,
            columns = columns,
            rows = rows,
            columnSpan = gridItem.columnSpan,
            rowSpan = gridItem.rowSpan,
        )

        when (val data = gridItem.data) {
            is GridItemData.ApplicationInfo -> {
                gridItem.copy(
                    page = currentPage,
                    startColumn = startColumn,
                    startRow = startRow,
                    data = data.copy(
                        index = -1,
                        folderId = null,
                    ),
                    associate = associate,
                )
            }

            is GridItemData.ShortcutConfig -> {
                gridItem.copy(
                    page = currentPage,
                    startColumn = startColumn,
                    startRow = startRow,
                    data = data.copy(
                        index = -1,
                        folderId = null,
                    ),
                    associate = associate,
                )
            }

            is GridItemData.ShortcutInfo -> {
                gridItem.copy(
                    page = currentPage,
                    startColumn = startColumn,
                    startRow = startRow,
                    data = data.copy(
                        index = -1,
                        folderId = null,
                    ),
                    associate = associate,
                )
            }

            is GridItemData.Folder -> {
                gridItem.copy(
                    page = currentPage,
                    startColumn = gridX / cellWidth,
                    startRow = gridY / cellHeight,
                    data = data.copy(
                        index = -1,
                        folderId = null,
                    ),
                    associate = associate,
                )
            }

            is GridItemData.Widget -> {
                gridItem.copy(
                    page = currentPage,
                    startColumn = gridX / cellWidth,
                    startRow = gridY / cellHeight,
                    associate = associate,
                )
            }
        }
    }

    is GridItemSource.New, is GridItemSource.Pin,
    -> {
        getMoveNewGridItem(
            associate = associate,
            cellHeight = cellHeight,
            cellWidth = cellWidth,
            columns = columns,
            gridHeight = gridHeight,
            gridItem = gridItem,
            gridWidth = gridWidth,
            gridX = gridX,
            gridY = gridY,
            rows = rows,
            currentPage = currentPage,
        )
    }
}

private fun getMoveNewGridItem(
    associate: Associate,
    cellHeight: Int,
    cellWidth: Int,
    columns: Int,
    gridHeight: Int,
    gridItem: GridItem,
    gridWidth: Int,
    gridX: Int,
    gridY: Int,
    rows: Int,
    currentPage: Int,
): GridItem = when (val data = gridItem.data) {
    is GridItemData.Widget -> {
        val (checkedColumnSpan, checkedRowSpan) = getWidgetGridItemSpan(
            cellWidth = cellWidth,
            cellHeight = cellHeight,
            minWidth = data.minWidth,
            minHeight = data.minHeight,
            targetCellWidth = data.targetCellWidth,
            targetCellHeight = data.targetCellHeight,
        )

        val (checkedMinWidth, checkedMinHeight) = getWidgetGridItemSize(
            columns = columns,
            rows = rows,
            gridWidth = gridWidth,
            gridHeight = gridHeight,
            minWidth = data.minWidth,
            minHeight = data.minHeight,
            targetCellWidth = data.targetCellWidth,
            targetCellHeight = data.targetCellHeight,
        )

        val (startColumn, startRow) = getStartPosition(
            x = gridX,
            y = gridY,
            cellWidth = cellWidth,
            cellHeight = cellHeight,
            columns = columns,
            rows = rows,
            columnSpan = checkedColumnSpan,
            rowSpan = checkedRowSpan,
        )

        val newData = data.copy(
            minWidth = checkedMinWidth,
            minHeight = checkedMinHeight,
        )

        gridItem.copy(
            page = currentPage,
            startColumn = startColumn,
            startRow = startRow,
            columnSpan = checkedColumnSpan.coerceIn(1, columns),
            rowSpan = checkedRowSpan.coerceIn(1, rows),
            data = newData,
            associate = associate,
        )
    }

    else -> {
        val (startColumn, startRow) = getStartPosition(
            x = gridX,
            y = gridY,
            cellWidth = cellWidth,
            cellHeight = cellHeight,
            columns = columns,
            rows = rows,
            columnSpan = gridItem.columnSpan,
            rowSpan = gridItem.rowSpan,
        )

        gridItem.copy(
            page = currentPage,
            startColumn = startColumn,
            startRow = startRow,
            associate = associate,
        )
    }
}

private fun animateScrollToPage(
    associate: Associate?,
    dragX: Int,
    edgeDistance: Int,
    safeDrawingWidth: Int,
    onUpdateDockPageDirection: (PageDirection?) -> Unit,
    onUpdateGridPageDirection: (PageDirection?) -> Unit,
) {
    val isOnLeftGrid = dragX < edgeDistance

    val isOnRightGrid = dragX > safeDrawingWidth - edgeDistance

    fun animateScrollToPage(onUpdatePageDirection: (PageDirection?) -> Unit) {
        if (isOnLeftGrid) {
            onUpdatePageDirection(PageDirection.Left)
        } else if (isOnRightGrid) {
            onUpdatePageDirection(PageDirection.Right)
        } else {
            onUpdatePageDirection(null)
        }
    }

    when (associate) {
        Associate.Grid -> {
            animateScrollToPage(onUpdatePageDirection = onUpdateGridPageDirection)
        }

        Associate.Dock -> {
            animateScrollToPage(onUpdatePageDirection = onUpdateDockPageDirection)
        }

        null -> Unit
    }
}

private fun getStartPosition(
    x: Int,
    y: Int,
    cellWidth: Int,
    cellHeight: Int,
    columns: Int,
    rows: Int,
    columnSpan: Int,
    rowSpan: Int,
): Pair<Int, Int> {
    val safeColumnSpan = columnSpan.coerceIn(1, columns)
    val safeRowSpan = rowSpan.coerceIn(1, rows)

    val targetColumn = x / cellWidth
    val targetRow = y / cellHeight

    val maxStartColumn = (columns - safeColumnSpan).coerceAtLeast(0)
    val maxStartRow = (rows - safeRowSpan).coerceAtLeast(0)

    val startColumn = targetColumn.coerceIn(0, maxStartColumn)
    val startRow = targetRow.coerceIn(0, maxStartRow)

    return startColumn to startRow
}
