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
import androidx.compose.runtime.State
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.eblan.launcher.domain.model.FolderPopup
import com.eblan.launcher.domain.model.FolderPopupEntry
import com.eblan.launcher.domain.model.GridItem
import com.eblan.launcher.domain.model.MoveGridItemResult
import com.eblan.launcher.feature.home.model.Drag
import com.eblan.launcher.feature.home.model.PageDirection
import com.eblan.launcher.feature.home.model.SharedElementKey
import com.eblan.launcher.feature.home.screen.PAGE_INDICATOR_HEIGHT
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

internal suspend fun handlePageDirection(
    pageDirection: PageDirection?,
    currentPage: Int,
    onAnimateScrollToPage: suspend (Int) -> Unit,
) {
    if (pageDirection == null) return

    delay(500L.milliseconds)

    when (pageDirection) {
        PageDirection.Left -> {
            onAnimateScrollToPage(currentPage - 1)
        }

        PageDirection.Right -> {
            onAnimateScrollToPage(currentPage + 1)
        }
    }
}

internal suspend fun onLongPressFolderGridItem(
    graphicsLayer: GraphicsLayer,
    intOffset: IntOffset,
    intSize: IntSize,
    sharedElementKey: SharedElementKey,
    gridItem: GridItem,
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

    onShowGridItemPopup(
        intOffset,
        intSize,
    )

    onUpdateIsVisibleOverlay(true)
}

internal fun handleAnimateScrollToPage(
    density: Density,
    drag: Drag,
    isVisibleOverlay: State<Boolean>,
    lockMovement: State<Boolean>,
    moveGridItemResult: MoveGridItemResult?,
    dragIntOffset: IntOffset,
    folderPopup: FolderPopup,
    folderPopupIntOffset: IntOffset,
    isDragging: State<Boolean>,
    paddingValues: PaddingValues,
    screenWidth: Int,
    layoutDirection: LayoutDirection,
    folderCellWidth: Int,
    isLast: Boolean,
    onUpdateFolderPageDirection: (PageDirection?) -> Unit,
) {
    if (drag != Drag.Dragging ||
        !isVisibleOverlay.value ||
        !isDragging.value ||
        lockMovement.value ||
        moveGridItemResult == null ||
        !isLast
    ) {
        return
    }

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

    val folderGridWidthPx = cellWidthPx * folderPopup.columns

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

internal fun handleDragFolderGridItem(
    density: Density,
    drag: Drag,
    dragIntOffset: IntOffset,
    currentPage: Int,
    folderPopup: FolderPopup,
    folderPopupIntOffset: IntOffset,
    isDragging: State<Boolean>,
    isVisibleOverlay: State<Boolean>,
    isScrollInProgress: Boolean,
    lockMovement: State<Boolean>,
    paddingValues: PaddingValues,
    screenHeight: Int,
    screenWidth: Int,
    moveGridItemResult: MoveGridItemResult?,
    layoutDirection: LayoutDirection,
    folderCellWidth: Int,
    folderCellHeight: Int,
    isLastFolderGridItem: Boolean,
    onMoveFolderGridItem: (
        folderPopup: FolderPopup,
        movingFolderGridItem: GridItem,
        dragX: Int,
        dragY: Int,
        gridWidth: Int,
        gridHeight: Int,
        currentPage: Int,
    ) -> Unit,
    onUpdateSharedElementKey: (SharedElementKey?) -> Unit,
    onUpsertFolderPopupEntry: (FolderPopupEntry) -> Unit,
) {
    if (drag != Drag.Dragging ||
        isScrollInProgress ||
        !isVisibleOverlay.value ||
        !isDragging.value ||
        lockMovement.value ||
        moveGridItemResult == null ||
        !isLastFolderGridItem
    ) {
        return
    }

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

    val folderGridWidthPx = (minCellWidthPx * folderPopup.columns).coerceAtMost(availableWidth)
    val folderGridHeightPx = (minCellHeightPx * folderPopup.rows).coerceAtMost(
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
            folderPopup,
            movingGridItem,
            dragX,
            dragY,
            folderGridWidthPx,
            folderGridHeightPx,
            currentPage,
        )
    } else {
        onUpsertFolderPopupEntry(folderPopup.folderPopupEntry.copy(isCloseFolder = true))
    }
}
