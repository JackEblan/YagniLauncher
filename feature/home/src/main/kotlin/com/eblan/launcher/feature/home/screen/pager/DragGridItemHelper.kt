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
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.pager.PagerState
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.eblan.launcher.domain.grid.getWidgetGridItemSize
import com.eblan.launcher.domain.grid.getWidgetGridItemSpan
import com.eblan.launcher.domain.grid.isGridItemSpanWithinBounds
import com.eblan.launcher.domain.model.ApplicationInfoGridItem
import com.eblan.launcher.domain.model.Associate
import com.eblan.launcher.domain.model.GridItem
import com.eblan.launcher.domain.model.GridItemData
import com.eblan.launcher.domain.model.MoveGridItemResult
import com.eblan.launcher.feature.home.model.Drag
import com.eblan.launcher.feature.home.model.GridItemSource
import com.eblan.launcher.feature.home.model.PageDirection
import com.eblan.launcher.feature.home.model.SharedElementKey
import com.eblan.launcher.feature.home.util.FOLDER_GRID_PADDING
import com.eblan.launcher.feature.home.util.PAGE_INDICATOR_HEIGHT
import kotlinx.coroutines.delay

internal fun handleAnimateScrollToPage(
    associate: Associate?,
    columns: Int,
    density: Density,
    dragIntOffset: IntOffset,
    folderGridItem: GridItem?,
    folderPopupIntOffset: IntOffset?,
    folderPopupIntSize: IntSize?,
    gridItemSource: GridItemSource?,
    isDragging: Boolean,
    paddingValues: PaddingValues,
    screenWidth: Int,
    onUpdateDockPageDirection: (PageDirection?) -> Unit,
    onUpdateFolderPageDirection: (PageDirection?) -> Unit,
    onUpdateGridPageDirection: (PageDirection?) -> Unit,
) {
    if (gridItemSource == null || !isDragging) return

    val leftPadding = with(density) {
        paddingValues.calculateStartPadding(LayoutDirection.Ltr).roundToPx()
    }

    val rightPadding = with(density) {
        paddingValues.calculateEndPadding(LayoutDirection.Ltr).roundToPx()
    }

    val horizontalPadding = leftPadding + rightPadding

    val safeDrawingWidth = screenWidth - horizontalPadding

    val edgeDistance = with(density) {
        20.dp.roundToPx()
    }

    val dragX = dragIntOffset.x - leftPadding

    when (gridItemSource) {
        is GridItemSource.Existing, is GridItemSource.New, is GridItemSource.Pin -> {
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

        is GridItemSource.Folder -> {
            if (folderPopupIntOffset == null || folderPopupIntSize == null) return

            val data = folderGridItem?.data as? GridItemData.Folder ?: return

            val folderCellWidth = safeDrawingWidth / columns

            val folderGridPaddingPx = with(density) {
                FOLDER_GRID_PADDING.roundToPx()
            }

            val folderGridWidthPx = folderCellWidth * data.columns

            val centeredX =
                folderPopupIntOffset.x + (folderPopupIntSize.width / 2) - (folderGridWidthPx / 2)

            val popupX = centeredX.coerceIn(0, safeDrawingWidth - folderGridWidthPx)

            val folderDragX = dragX - popupX - folderGridPaddingPx

            val isOnLeftGrid = folderDragX < edgeDistance

            val isOnRightGrid = folderDragX > folderGridWidthPx - folderGridPaddingPx - edgeDistance

            if (isOnLeftGrid) {
                onUpdateFolderPageDirection(PageDirection.Left)
            } else if (isOnRightGrid) {
                onUpdateFolderPageDirection(PageDirection.Right)
            } else {
                onUpdateFolderPageDirection(null)
            }
        }
    }
}

internal suspend fun handleDragGridItem(
    columns: Int,
    currentPage: Int,
    density: Density,
    dockColumns: Int,
    dockHeight: Dp,
    dockRows: Int,
    drag: Drag,
    dragIntOffset: IntOffset,
    folderCurrentPage: Int,
    folderGridItem: GridItem?,
    folderPopupIntOffset: IntOffset?,
    folderPopupIntSize: IntSize?,
    folderTitleHeightPx: Int,
    gridItemSource: GridItemSource?,
    isDragging: Boolean,
    isLongPress: Boolean,
    isGridScrollInProgress: Boolean,
    isDockScrollInProgress: Boolean,
    lockMovement: Boolean,
    paddingValues: PaddingValues,
    rows: Int,
    screenHeight: Int,
    screenWidth: Int,
    onMoveFolderGridItem: (
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
    ) -> Unit,
    onMoveFolderGridItemOutsideFolder: (
        folderGridItem: GridItem,
        movingApplicationInfoGridItem: ApplicationInfoGridItem,
        applicationInfoGridItems: List<ApplicationInfoGridItem>,
    ) -> Unit,
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
    onUpdateGridItemSource: (GridItemSource) -> Unit,
    onUpdateSharedElementKey: (SharedElementKey?) -> Unit,
) {
    if (drag != Drag.Dragging ||
        isGridScrollInProgress ||
        isDockScrollInProgress ||
        gridItemSource == null ||
        !(isLongPress && isDragging) ||
        lockMovement
    ) {
        return
    }

    delay(100L)

    val leftPadding = with(density) {
        paddingValues.calculateStartPadding(LayoutDirection.Ltr).roundToPx()
    }

    val rightPadding = with(density) {
        paddingValues.calculateEndPadding(LayoutDirection.Ltr).roundToPx()
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

    val dragX = dragIntOffset.x - leftPadding

    val dragY = dragIntOffset.y - topPadding

    val isOnDock = dockHeightPx > 0 && dragY > safeDrawingHeight - dockHeightPx

    when (gridItemSource) {
        is GridItemSource.Existing, is GridItemSource.New, is GridItemSource.Pin -> {
            if (isOnDock) {
                handleDragDockGridItem(
                    currentPage = currentPage,
                    dockColumns = dockColumns,
                    dockHeightPx = dockHeightPx,
                    dockRows = dockRows,
                    dragX = dragX,
                    dragY = dragY,
                    gridItemSource = gridItemSource,
                    safeDrawingHeight = safeDrawingHeight,
                    safeDrawingWidth = safeDrawingWidth,
                    onMoveGridItem = onMoveGridItem,
                    onUpdateAssociate = onUpdateAssociate,
                )
            } else {
                handleDragGridItem(
                    columns = columns,
                    currentPage = currentPage,
                    dockHeightPx = dockHeightPx,
                    dragX = dragX,
                    dragY = dragY,
                    gridItemSource = gridItemSource,
                    pageIndicatorHeightPx = pageIndicatorHeightPx,
                    rows = rows,
                    safeDrawingHeight = safeDrawingHeight,
                    safeDrawingWidth = safeDrawingWidth,
                    onMoveGridItem = onMoveGridItem,
                    onUpdateAssociate = onUpdateAssociate,
                )
            }
        }

        is GridItemSource.Folder -> {
            handleDragFolderGridItem(
                columns = columns,
                density = density,
                dragX = dragX,
                dragY = dragY,
                folderCurrentPage = folderCurrentPage,
                folderGridItem = folderGridItem,
                folderPopupIntOffset = folderPopupIntOffset,
                folderPopupIntSize = folderPopupIntSize,
                folderTitleHeightPx = folderTitleHeightPx,
                gridItemSource = gridItemSource,
                rows = rows,
                safeDrawingHeight = safeDrawingHeight,
                safeDrawingWidth = safeDrawingWidth,
                onMoveFolderGridItem = onMoveFolderGridItem,
                onMoveFolderGridItemOutsideFolder = onMoveFolderGridItemOutsideFolder,
                onUpdateGridItemSource = onUpdateGridItemSource,
                onUpdateSharedElementKey = onUpdateSharedElementKey,
            )
        }
    }
}

private fun handleDragFolderGridItem(
    columns: Int,
    density: Density,
    dragX: Int,
    dragY: Int,
    folderCurrentPage: Int,
    folderGridItem: GridItem?,
    folderPopupIntOffset: IntOffset?,
    folderPopupIntSize: IntSize?,
    folderTitleHeightPx: Int,
    gridItemSource: GridItemSource,
    rows: Int,
    safeDrawingHeight: Int,
    safeDrawingWidth: Int,
    onMoveFolderGridItem: (
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
    ) -> Unit,
    onMoveFolderGridItemOutsideFolder: (
        folderGridItem: GridItem,
        movingApplicationInfoGridItem: ApplicationInfoGridItem,
        applicationInfoGridItems: List<ApplicationInfoGridItem>,
    ) -> Unit,
    onUpdateGridItemSource: (GridItemSource) -> Unit,
    onUpdateSharedElementKey: (SharedElementKey?) -> Unit,
) {
    if (folderPopupIntOffset == null || folderPopupIntSize == null) return

    val data = folderGridItem?.data as? GridItemData.Folder ?: return

    val gridItemSourceFolder = gridItemSource as? GridItemSource.Folder ?: return

    val folderCellWidth = safeDrawingWidth / columns

    val folderCellHeight = safeDrawingHeight / rows

    val folderGridPaddingPx = with(density) {
        FOLDER_GRID_PADDING.roundToPx()
    }

    val folderGridWidthPx = folderCellWidth * data.columns
    val folderGridHeightPx = folderCellHeight * data.rows

    val centeredX =
        folderPopupIntOffset.x + (folderPopupIntSize.width / 2) - (folderGridWidthPx / 2)
    val centeredY =
        folderPopupIntOffset.y + (folderPopupIntSize.height / 2) - (folderGridHeightPx / 2)

    val popupX = centeredX.coerceIn(0, safeDrawingWidth - folderGridWidthPx)
    val popupY = centeredY.coerceIn(0, safeDrawingHeight - folderGridHeightPx)

    val folderDragX = dragX - popupX - folderGridPaddingPx

    val folderDragY = dragY - popupY - folderGridPaddingPx

    val folderGridVisibleWidthPx = folderGridWidthPx - (folderGridPaddingPx * 2)
    val folderGridVisibleHeightPx =
        (folderGridHeightPx - folderTitleHeightPx) - (folderGridPaddingPx * 2)

    val isInsideFolder = folderDragX in 0..folderGridVisibleWidthPx &&
        folderDragY in 0..folderGridVisibleHeightPx

    if (isInsideFolder) {
        onMoveFolderGridItem(
            folderGridItem,
            data.gridItems,
            gridItemSourceFolder.applicationInfoGridItem,
            folderDragX,
            folderDragY,
            data.columns,
            data.rows,
            folderGridWidthPx,
            folderGridHeightPx - folderTitleHeightPx,
            folderCurrentPage,
        )
    } else {
        val gridItem = GridItem(
            id = gridItemSourceFolder.applicationInfoGridItem.id,
            page = gridItemSourceFolder.applicationInfoGridItem.page,
            startColumn = gridItemSourceFolder.applicationInfoGridItem.startColumn,
            startRow = gridItemSourceFolder.applicationInfoGridItem.startRow,
            columnSpan = gridItemSourceFolder.applicationInfoGridItem.columnSpan,
            rowSpan = gridItemSourceFolder.applicationInfoGridItem.rowSpan,
            data = GridItemData.ApplicationInfo(
                serialNumber = gridItemSourceFolder.applicationInfoGridItem.serialNumber,
                componentName = gridItemSourceFolder.applicationInfoGridItem.componentName,
                packageName = gridItemSourceFolder.applicationInfoGridItem.packageName,
                icon = gridItemSourceFolder.applicationInfoGridItem.icon,
                label = gridItemSourceFolder.applicationInfoGridItem.label,
                customIcon = gridItemSourceFolder.applicationInfoGridItem.customIcon,
                customLabel = gridItemSourceFolder.applicationInfoGridItem.customLabel,
                index = -1,
                folderId = null,
            ),
            associate = gridItemSourceFolder.applicationInfoGridItem.associate,
            override = gridItemSourceFolder.applicationInfoGridItem.override,
            gridItemSettings = gridItemSourceFolder.applicationInfoGridItem.gridItemSettings,
            doubleTap = gridItemSourceFolder.applicationInfoGridItem.doubleTap,
            swipeUp = gridItemSourceFolder.applicationInfoGridItem.swipeUp,
            swipeDown = gridItemSourceFolder.applicationInfoGridItem.swipeDown,
        )

        onUpdateGridItemSource(GridItemSource.New(gridItem = gridItem))

        onUpdateSharedElementKey(
            SharedElementKey(
                id = gridItem.id,
                parent = SharedElementKey.Parent.Grid,
            ),
        )

        onMoveFolderGridItemOutsideFolder(
            folderGridItem,
            gridItemSourceFolder.applicationInfoGridItem,
            data.gridItems,
        )
    }
}

private fun handleDragGridItem(
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
) {
    val gridItem = requireNotNull(gridItemSource.gridItem)

    onUpdateAssociate(Associate.Grid)

    val gridHeightWithPadding = safeDrawingHeight - dockHeightPx - pageIndicatorHeightPx

    val cellWidth = safeDrawingWidth / columns

    val cellHeight = gridHeightWithPadding / rows

    val moveGridItem = getMoveGridItem(
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

private fun handleDragDockGridItem(
    currentPage: Int,
    dockColumns: Int,
    dockHeightPx: Int,
    dockRows: Int,
    dragX: Int,
    dragY: Int,
    gridItemSource: GridItemSource,
    safeDrawingHeight: Int,
    safeDrawingWidth: Int,
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
) {
    val gridItem = requireNotNull(gridItemSource.gridItem)

    onUpdateAssociate(Associate.Dock)

    val cellWidth = safeDrawingWidth / dockColumns

    val cellHeight = dockHeightPx / dockRows

    val dockY = dragY - (safeDrawingHeight - dockHeightPx)

    val moveGridItem = getMoveGridItem(
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

    val isGridItemSpanWithinBounds = isGridItemSpanWithinBounds(
        gridItem = moveGridItem,
        columns = dockColumns,
        rows = dockRows,
    )

    if (isGridItemSpanWithinBounds) {
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
    columns: Int,
    dockColumns: Int,
    density: Density,
    dockHeight: Dp,
    drag: Drag,
    gridItemSource: GridItemSource?,
    isDragging: Boolean,
    isLongPress: Boolean,
    moveGridItemResult: MoveGridItemResult?,
    paddingValues: PaddingValues,
    rows: Int,
    dockRows: Int,
    screenHeight: Int,
    screenWidth: Int,
    lockMovement: Boolean,
    onUpdateFolderGridItemId: (String) -> Unit,
    onUpdateFolderPopupBounds: (
        intOffset: IntOffset,
        intSize: IntSize,
    ) -> Unit,
    onUpdateGridItemSource: (GridItemSource) -> Unit,
    onUpdateSharedElementKey: (SharedElementKey?) -> Unit,
) {
    if (drag != Drag.Dragging ||
        gridItemSource == null ||
        gridItemSource is GridItemSource.Folder ||
        moveGridItemResult == null ||
        !moveGridItemResult.isSuccess ||
        !(isLongPress && isDragging) ||
        lockMovement
    ) {
        return
    }

    delay(1000L)

    val conflictingGridItem = moveGridItemResult.conflictingGridItem ?: return

    val conflictingData = conflictingGridItem.data as? GridItemData.Folder ?: return

    val movingData =
        moveGridItemResult.movingGridItem.data as? GridItemData.ApplicationInfo ?: return

    val leftPadding = with(density) {
        paddingValues.calculateStartPadding(LayoutDirection.Ltr).roundToPx()
    }

    val rightPadding = with(density) {
        paddingValues.calculateEndPadding(LayoutDirection.Ltr).roundToPx()
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

    val gridHeight = safeDrawingHeight - pageIndicatorHeightPx - dockHeightPx

    val gridCellWidth = safeDrawingWidth / columns

    val gridCellHeight = gridHeight / rows

    val dockCellWidth = safeDrawingWidth / dockColumns

    val dockCellHeight = dockHeightPx / dockRows

    val dockTopLeft = gridHeight + pageIndicatorHeightPx

    val intOffset = when (conflictingGridItem.associate) {
        Associate.Grid -> {
            IntOffset(
                x = conflictingGridItem.startColumn * gridCellWidth,
                y = conflictingGridItem.startRow * gridCellHeight,
            )
        }

        Associate.Dock -> {
            IntOffset(
                x = conflictingGridItem.startColumn * dockCellWidth,
                y = conflictingGridItem.startRow * dockCellHeight + dockTopLeft,
            )
        }
    }

    val intSize = when (conflictingGridItem.associate) {
        Associate.Grid -> {
            IntSize(
                width = conflictingGridItem.columnSpan * gridCellWidth,
                height = conflictingGridItem.rowSpan * gridCellHeight,
            )
        }

        Associate.Dock -> {
            IntSize(
                width = conflictingGridItem.columnSpan * dockCellWidth,
                height = conflictingGridItem.rowSpan * dockCellHeight,
            )
        }
    }

    onUpdateGridItemSource(
        GridItemSource.Folder(
            gridItem = moveGridItemResult.movingGridItem,
            applicationInfoGridItem = ApplicationInfoGridItem(
                id = moveGridItemResult.movingGridItem.id,
                page = moveGridItemResult.movingGridItem.page,
                startColumn = moveGridItemResult.movingGridItem.startColumn,
                startRow = moveGridItemResult.movingGridItem.startRow,
                columnSpan = moveGridItemResult.movingGridItem.columnSpan,
                rowSpan = moveGridItemResult.movingGridItem.rowSpan,
                associate = moveGridItemResult.movingGridItem.associate,
                componentName = movingData.componentName,
                packageName = movingData.packageName,
                icon = movingData.icon,
                label = movingData.label,
                override = moveGridItemResult.movingGridItem.override,
                serialNumber = movingData.serialNumber,
                customIcon = movingData.customIcon,
                customLabel = movingData.customLabel,
                gridItemSettings = moveGridItemResult.movingGridItem.gridItemSettings,
                doubleTap = moveGridItemResult.movingGridItem.doubleTap,
                swipeUp = moveGridItemResult.movingGridItem.swipeUp,
                swipeDown = moveGridItemResult.movingGridItem.swipeDown,
                index = conflictingData.gridItems.lastIndex + 1,
                folderId = conflictingData.id,
            ),
        ),
    )

    onUpdateSharedElementKey(
        SharedElementKey(
            id = moveGridItemResult.movingGridItem.id,
            parent = SharedElementKey.Parent.Folder,
        ),
    )

    onUpdateFolderPopupBounds(
        intOffset,
        intSize,
    )

    onUpdateFolderGridItemId(conflictingData.id)
}

internal suspend fun handlePageDirection(pageDirection: PageDirection?, pagerState: PagerState) {
    if (pageDirection == null) return

    delay(500L)

    when (pageDirection) {
        PageDirection.Left -> {
            pagerState.animateScrollToPage(page = pagerState.currentPage - 1)
        }

        PageDirection.Right -> {
            pagerState.animateScrollToPage(page = pagerState.currentPage + 1)
        }
    }
}

private fun getMoveGridItem(
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
    is GridItemSource.Existing, is GridItemSource.Folder -> {
        gridItem.copy(
            page = currentPage,
            startColumn = gridX / cellWidth,
            startRow = gridY / cellHeight,
            associate = associate,
        )
    }

    is GridItemSource.New, is GridItemSource.Pin -> {
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

        val newData = data.copy(
            minWidth = checkedMinWidth,
            minHeight = checkedMinHeight,
        )

        gridItem.copy(
            page = currentPage,
            startColumn = gridX / cellWidth,
            startRow = gridY / cellHeight,
            columnSpan = checkedColumnSpan.coerceIn(1, columns),
            rowSpan = checkedRowSpan.coerceIn(1, rows),
            data = newData,
            associate = associate,
        )
    }

    else -> {
        gridItem.copy(
            page = currentPage,
            startColumn = gridX / cellWidth,
            startRow = gridY / cellHeight,
            associate = associate,
        )
    }
}
