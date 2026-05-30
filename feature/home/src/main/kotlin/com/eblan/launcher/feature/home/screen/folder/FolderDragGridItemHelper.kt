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
package com.eblan.launcher.feature.home.screen.folder

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.eblan.launcher.domain.model.GridItem
import com.eblan.launcher.domain.model.GridItemData
import com.eblan.launcher.domain.model.MoveGridItemResult
import com.eblan.launcher.feature.home.model.Drag
import com.eblan.launcher.feature.home.model.PageDirection
import com.eblan.launcher.feature.home.model.SharedElementKey
import com.eblan.launcher.feature.home.util.PAGE_INDICATOR_HEIGHT
import kotlinx.coroutines.delay

internal fun handleAnimateScrollToPage(
    density: Density,
    drag: Drag,
    isVisibleOverlay: Boolean,
    lockMovement: Boolean,
    moveGridItemResult: MoveGridItemResult?,
    dragIntOffset: IntOffset,
    folderGridItem: GridItem,
    folderPopupIntOffset: IntOffset,
    isDragging: Boolean,
    paddingValues: PaddingValues,
    screenWidth: Int,
    layoutDirection: LayoutDirection,
    folderCellWidth: Int,
    isLast: Boolean,
    onUpdateFolderPageDirection: (PageDirection?) -> Unit,
) {
    if (drag != Drag.Dragging ||
        !isVisibleOverlay ||
        !isDragging ||
        lockMovement ||
        moveGridItemResult == null ||
        !isLast
    ) {
        return
    }

    val data = folderGridItem.data as GridItemData.Folder

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

    val cellWidthDp = folderCellWidth.dp

    val cellWidthPx = with(receiver = density) { cellWidthDp.roundToPx() }

    val folderGridWidthPx = cellWidthPx * data.columns

    val x = folderPopupIntOffset.x - leftPadding
    val popupX = x.coerceIn(0, safeDrawingWidth - folderGridWidthPx) + leftPadding
    val folderDragX = dragX - popupX

    val isOnLeftGrid = folderDragX < edgeDistance
    val isOnRightGrid = folderDragX > folderGridWidthPx - edgeDistance

    if (isOnLeftGrid) {
        onUpdateFolderPageDirection(PageDirection.Left)
    } else if (isOnRightGrid) {
        onUpdateFolderPageDirection(PageDirection.Right)
    } else {
        onUpdateFolderPageDirection(null)
    }
}

internal suspend fun handleDragFolderGridItem(
    density: Density,
    drag: Drag,
    dragIntOffset: IntOffset,
    currentPage: Int,
    folderGridItem: GridItem?,
    folderPopupIntOffset: IntOffset,
    folderPopupIntSize: IntSize,
    isDragging: Boolean,
    isVisibleOverlay: Boolean,
    isScrollInProgress: Boolean,
    lockMovement: Boolean,
    paddingValues: PaddingValues,
    screenHeight: Int,
    screenWidth: Int,
    moveGridItemResult: MoveGridItemResult?,
    layoutDirection: LayoutDirection,
    folderCellWidth: Int,
    folderCellHeight: Int,
    isLast: Boolean,
    onMoveFolderGridItem: (
        conflictingGridItem: GridItem,
        movingFolderGridItem: GridItem,
        data: GridItemData.Folder,
        dragX: Int,
        dragY: Int,
        columns: Int,
        rows: Int,
        gridWidth: Int,
        gridHeight: Int,
        currentPage: Int,
    ) -> Unit,
    onUpdateSharedElementKey: (SharedElementKey?) -> Unit,
    onUpdateIsCloseFolder: (Boolean) -> Unit,
) {
    if (drag != Drag.Dragging ||
        isScrollInProgress ||
        !isVisibleOverlay ||
        !isDragging ||
        lockMovement ||
        moveGridItemResult == null ||
        !isLast
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

    val horizontalPadding = leftPadding + rightPadding

    val verticalPadding = topPadding + bottomPadding

    val safeDrawingWidth = screenWidth - horizontalPadding

    val safeDrawingHeight = screenHeight - verticalPadding

    val localDragX = dragIntOffset.x - leftPadding

    val localDragY = dragIntOffset.y - topPadding

    dragFolderGridItem(
        density = density,
        leftPadding = leftPadding,
        topPadding = topPadding,
        dragX = localDragX,
        dragY = localDragY,
        folderCurrentPage = currentPage,
        folderGridItem = folderGridItem,
        folderPopupIntOffset = folderPopupIntOffset,
        folderPopupIntSize = folderPopupIntSize,
        moveGridItemResult = moveGridItemResult,
        safeDrawingHeight = safeDrawingHeight,
        safeDrawingWidth = safeDrawingWidth,
        minFolderCellWidth = folderCellWidth,
        minFolderCellHeight = folderCellHeight,
        onMoveFolderGridItem = onMoveFolderGridItem,
        onUpdateSharedElementKey = onUpdateSharedElementKey,
        onUpdateIsCloseFolder = onUpdateIsCloseFolder,
    )
}

private fun dragFolderGridItem(
    density: Density,
    leftPadding: Int,
    topPadding: Int,
    dragX: Int,
    dragY: Int,
    folderCurrentPage: Int,
    folderGridItem: GridItem?,
    folderPopupIntOffset: IntOffset?,
    folderPopupIntSize: IntSize?,
    safeDrawingHeight: Int,
    safeDrawingWidth: Int,
    moveGridItemResult: MoveGridItemResult,
    minFolderCellWidth: Int,
    minFolderCellHeight: Int,
    onMoveFolderGridItem: (
        conflictingGridItem: GridItem,
        movingFolderGridItem: GridItem,
        data: GridItemData.Folder,
        dragX: Int,
        dragY: Int,
        columns: Int,
        rows: Int,
        gridWidth: Int,
        gridHeight: Int,
        currentPage: Int,
    ) -> Unit,
    onUpdateSharedElementKey: (SharedElementKey?) -> Unit,
    onUpdateIsCloseFolder: (Boolean) -> Unit,
) {
    if (
        folderGridItem == null ||
        folderPopupIntOffset == null ||
        folderPopupIntSize == null
    ) {
        return
    }

    val data = folderGridItem.data as GridItemData.Folder

    val minCellWidthPx = with(density) {
        minFolderCellWidth.dp.roundToPx()
    }

    val minCellHeightPx = with(density) {
        minFolderCellHeight.dp.roundToPx()
    }

    val availableWidth = (safeDrawingWidth - leftPadding * 2).coerceAtLeast(0)
    val availableHeight = (safeDrawingHeight - topPadding * 2).coerceAtLeast(0)

    val folderTitleHeightPx = with(density) {
        PAGE_INDICATOR_HEIGHT.roundToPx()
    }

    val folderGridWidthPx = (minCellWidthPx * data.columns).coerceAtMost(availableWidth)

    val folderGridHeightPx = (minCellHeightPx * data.rows).coerceAtMost(
        (availableHeight - folderTitleHeightPx).coerceAtLeast(0),
    )

    val endHeight = folderGridHeightPx + folderTitleHeightPx

    val maximumX = (
        safeDrawingWidth -
            folderGridWidthPx +
            leftPadding
        ).coerceAtLeast(leftPadding)

    val maximumY = (
        safeDrawingHeight -
            endHeight +
            topPadding
        ).coerceAtLeast(topPadding)

    val endIntOffset = IntOffset(
        x = folderPopupIntOffset.x.coerceIn(
            leftPadding,
            maximumX,
        ),
        y = folderPopupIntOffset.y.coerceIn(
            topPadding,
            maximumY,
        ),
    )

    val localDragX = dragX - endIntOffset.x

    val localDragY = dragY - endIntOffset.y

    val isInsideFolder =
        localDragX in 0 until folderGridWidthPx &&
            localDragY in 0 until folderGridHeightPx

    val movingGridItem = moveGridItemResult.movingGridItem

    if (isInsideFolder) {
        onUpdateSharedElementKey(
            SharedElementKey(
                id = movingGridItem.id,
                parent = SharedElementKey.Parent.Folder,
            ),
        )

        onMoveFolderGridItem(
            folderGridItem,
            movingGridItem,
            data,
            localDragX,
            localDragY,
            data.columns,
            data.rows,
            folderGridWidthPx,
            folderGridHeightPx,
            folderCurrentPage,
        )
    } else {
        onUpdateIsCloseFolder(true)
    }
}
