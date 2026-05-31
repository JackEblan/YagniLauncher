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

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.eblan.launcher.domain.model.FolderPopupEntry
import com.eblan.launcher.domain.model.GridItem
import com.eblan.launcher.domain.model.GridItemData
import com.eblan.launcher.domain.model.MoveGridItemResult
import com.eblan.launcher.feature.home.model.Drag
import com.eblan.launcher.feature.home.model.PageDirection
import com.eblan.launcher.feature.home.model.SharedElementKey
import com.eblan.launcher.feature.home.util.PAGE_INDICATOR_HEIGHT
import kotlinx.coroutines.delay

internal suspend fun onLongPressFolderGridItem(
    graphicsLayer: GraphicsLayer,
    intOffset: IntOffset,
    intSize: IntSize,
    sharedElementKey: SharedElementKey,
    gridItem: GridItem,
    scale: Animatable<Float, AnimationVector1D>,
    onUpdateImageBitmap: (ImageBitmap) -> Unit,
    onUpdateOverlayBounds: (
        intOffset: IntOffset,
        intSize: IntSize,
    ) -> Unit,
    onUpdateSharedElementKey: (SharedElementKey?) -> Unit,
    onShowGridItemPopup: (
        intOffset: IntOffset,
        intSize: IntSize,
    ) -> Unit,
    onUpdateIsVisibleOverlay: (Boolean) -> Unit,
    onUpdateMoveGridItemResult: (MoveGridItemResult) -> Unit,
) {
    scale.animateTo(0.5f)

    scale.animateTo(1f)

    onUpdateMoveGridItemResult(
        MoveGridItemResult(
            isSuccess = true,
            movingGridItem = gridItem,
            conflictingGridItem = null,
        ),
    )

    onUpdateImageBitmap(graphicsLayer.toImageBitmap())

    onUpdateOverlayBounds(
        intOffset,
        intSize,
    )

    onUpdateSharedElementKey(sharedElementKey)

    onUpdateIsVisibleOverlay(true)

    onShowGridItemPopup(
        intOffset,
        intSize,
    )
}

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
    folderPopupEntry: FolderPopupEntry,
    isLastFolderGridItem: Boolean,
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
    onUpsertFolderPopupEntry: (FolderPopupEntry) -> Unit,
) {
    if (drag != Drag.Dragging ||
        isScrollInProgress ||
        !isVisibleOverlay ||
        !isDragging ||
        lockMovement ||
        moveGridItemResult == null ||
        folderGridItem == null ||
        !isLastFolderGridItem
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

    val data = folderGridItem.data as GridItemData.Folder

    val minCellWidthPx = with(receiver = density) {
        folderCellWidth.dp.roundToPx()
    }

    val minCellHeightPx = with(receiver = density) {
        folderCellHeight.dp.roundToPx()
    }

    val availableWidth = (safeDrawingWidth - leftPadding * 2).coerceAtLeast(0)
    val availableHeight = (safeDrawingHeight - topPadding * 2).coerceAtLeast(0)

    val folderTitleHeightPx = with(receiver = density) {
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
        ).coerceAtLeast(minimumValue = leftPadding)

    val maximumY = (
        safeDrawingHeight -
            endHeight +
            topPadding
        ).coerceAtLeast(minimumValue = topPadding)

    val endIntOffset = IntOffset(
        x = folderPopupIntOffset.x.coerceIn(
            minimumValue = leftPadding,
            maximumValue = maximumX,
        ),
        y = folderPopupIntOffset.y.coerceIn(
            minimumValue = topPadding,
            maximumValue = maximumY,
        ),
    )

    val movingGridItem = moveGridItemResult.movingGridItem

    val dragX = localDragX - endIntOffset.x
    val dragY = localDragY - endIntOffset.y

    if (dragX in 0 until folderGridWidthPx &&
        dragY in 0 until folderGridHeightPx
    ) {
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
            dragX,
            dragY,
            data.columns,
            data.rows,
            folderGridWidthPx,
            folderGridHeightPx,
            currentPage,
        )
    } else {
        onUpsertFolderPopupEntry(folderPopupEntry.copy(isCloseFolder = true))
    }
}
