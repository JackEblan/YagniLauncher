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

import android.graphics.RectF
import androidx.activity.compose.BackHandler
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.eblan.launcher.domain.model.FolderPopup
import com.eblan.launcher.domain.model.FolderPopupEntry
import com.eblan.launcher.domain.model.GridItem
import com.eblan.launcher.domain.model.GridItemData
import com.eblan.launcher.domain.model.GridItemSettings
import com.eblan.launcher.domain.model.HomeSettings
import com.eblan.launcher.domain.model.MoveGridItemResult
import com.eblan.launcher.feature.home.component.FolderGridLayout
import com.eblan.launcher.feature.home.component.HomeHandler
import com.eblan.launcher.feature.home.component.PageIndicator
import com.eblan.launcher.feature.home.model.Drag
import com.eblan.launcher.feature.home.model.PageDirection
import com.eblan.launcher.feature.home.model.SharedElementKey
import com.eblan.launcher.feature.home.util.FOLDER_PREVIEW_COLUMNS
import com.eblan.launcher.feature.home.util.FOLDER_PREVIEW_ROWS
import com.eblan.launcher.feature.home.util.PAGE_INDICATOR_HEIGHT
import kotlin.math.roundToInt

@Composable
internal fun FolderScreen(
    modifier: Modifier = Modifier,
    sharedTransitionScope: SharedTransitionScope,
    drag: Drag,
    folderPopup: FolderPopup,
    gridItemSettings: GridItemSettings,
    paddingValues: PaddingValues,
    safeDrawingHeight: Int,
    safeDrawingWidth: Int,
    statusBarNotifications: Map<String, Int>,
    isVisibleOverlay: Boolean,
    hasShortcutHostPermission: Boolean,
    moveGridItemResult: MoveGridItemResult?,
    homeSettings: HomeSettings,
    isDragging: Boolean,
    dragIntOffset: IntOffset,
    lockMovement: Boolean,
    folderCellWidth: Int,
    folderCellHeight: Int,
    screenHeight: Int,
    screenWidth: Int,
    lastFolderPopup: FolderPopup?,
    showFolderGridItemPopup: Boolean,
    previewFolderGridItems: Map<String, List<GridItem>>,
    onDeleteFolderPopupEntry: (FolderPopupEntry) -> Unit,
    onMoveFolderGridItemOutsideFolder: (GridItem) -> Unit,
    onOpenAppDrawer: () -> Unit,
    onUpdateImageBitmap: (ImageBitmap) -> Unit,
    onUpdateIsDragging: (Boolean) -> Unit,
    onUpdateOverlayBounds: (
        intOffset: IntOffset,
        intSize: IntSize,
    ) -> Unit,
    onUpdateSharedElementKey: (SharedElementKey?) -> Unit,
    onShowGridItemPopup: (
        intOffset: IntOffset,
        intSize: IntSize,
    ) -> Unit,
    onUpdateIsCloseFolderGridItemPopup: (Boolean) -> Unit,
    onUpdateIsVisibleOverlay: (Boolean) -> Unit,
    onUpdateMoveGridItemResult: (MoveGridItemResult) -> Unit,
    onMoveFolderGridItem: (
        folderPopup: FolderPopup,
        movingFolderGridItem: GridItem,
        dragX: Int,
        dragY: Int,
        gridWidth: Int,
        gridHeight: Int,
        currentPage: Int,
    ) -> Unit,
    onDismissFolderGridItemPopup: () -> Unit,
    onResetGrid: () -> Unit,
    onDragEndAfterMoveFolder: () -> Unit,
    onUpsertFolderPopupEntry: (FolderPopupEntry) -> Unit,
) {
    val folderPopupIntOffset = IntOffset(
        x = folderPopup.folderPopupEntry.x,
        y = folderPopup.folderPopupEntry.y,
    )

    val folderPopupIntSize = IntSize(
        width = folderPopup.folderPopupEntry.width,
        height = folderPopup.folderPopupEntry.height,
    )

    val density = LocalDensity.current

    val layoutDirection = LocalLayoutDirection.current

    val leftPadding = with(density) {
        paddingValues.calculateLeftPadding(layoutDirection).roundToPx()
    }

    val topPadding = with(density) {
        paddingValues.calculateTopPadding().roundToPx()
    }

    val minCellWidthDp = homeSettings.folderCellWidth.dp
    val minCellHeightDp = homeSettings.folderCellHeight.dp

    val minCellWidthPx = with(density) { minCellWidthDp.roundToPx() }
    val minCellHeightPx = with(density) { minCellHeightDp.roundToPx() }

    val availableWidth = (safeDrawingWidth - leftPadding * 2).coerceAtLeast(0)
    val availableHeight = (safeDrawingHeight - topPadding * 2).coerceAtLeast(0)

    val folderTitleHeightPx = with(density) {
        PAGE_INDICATOR_HEIGHT.roundToPx()
    }

    val folderGridWidthPx = (minCellWidthPx * folderPopup.columns).coerceAtMost(availableWidth)

    val folderGridHeightPx = (minCellHeightPx * folderPopup.rows).coerceAtMost(
        (availableHeight - folderTitleHeightPx).coerceAtLeast(0),
    )

    val endHeight = folderGridHeightPx + folderTitleHeightPx

    val maximumX = (safeDrawingWidth - folderGridWidthPx + leftPadding).coerceAtLeast(leftPadding)

    val maximumY = (safeDrawingHeight - endHeight + topPadding).coerceAtLeast(topPadding)

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

    val startWidth = folderPopupIntSize.width.toFloat()
    val startHeight = folderPopupIntSize.height.toFloat()

    val startCenterX = folderPopupIntOffset.x + startWidth / 2f
    val startCenterY = folderPopupIntOffset.y + startHeight / 2f

    val endCenterX = endIntOffset.x + folderGridWidthPx.toFloat() / 2f
    val endCenterY = endIntOffset.y + endHeight.toFloat() / 2f

    val progress = remember { Animatable(0f) }

    val animatedRect by remember(
        startWidth,
        folderGridWidthPx.toFloat(),
        startCenterX,
        endCenterY,
    ) {
        derivedStateOf {
            val currentWidth = lerp(
                startWidth,
                folderGridWidthPx.toFloat(),
                progress.value,
            )

            val currentHeight = lerp(
                startHeight,
                endHeight.toFloat(),
                progress.value,
            )

            val currentX = lerp(
                startCenterX,
                endCenterX,
                progress.value,
            ) - currentWidth / 2f

            val currentY = lerp(
                startCenterY,
                endCenterY,
                progress.value,
            ) - currentHeight / 2f

            RectF(
                currentX,
                currentY,
                currentX + currentWidth,
                currentY + currentHeight,
            )
        }
    }

    val folderGridHorizontalPagerState = rememberPagerState(
        pageCount = {
            folderPopup.gridItemsByPage.size
        },
    )

    var pageDirection by remember { mutableStateOf<PageDirection?>(null) }

    val isLastFolderGridItem = lastFolderPopup?.gridItem == folderPopup.gridItem

    val currentDrag = rememberUpdatedState(drag)
    val currentIsDragging = rememberUpdatedState(isDragging)
    val currentIsVisibleOverlay = rememberUpdatedState(isVisibleOverlay)
    val currentMoveGridItemResult = rememberUpdatedState(moveGridItemResult)
    val currentLockMovement = rememberUpdatedState(lockMovement)

    LaunchedEffect(key1 = Unit) {
        progress.animateTo(targetValue = 1f)
    }

    BackHandler(enabled = !folderPopup.folderPopupEntry.isCloseFolder && isLastFolderGridItem) {
        onUpsertFolderPopupEntry(folderPopup.folderPopupEntry.copy(isCloseFolder = true))
    }

    HomeHandler(enabled = !folderPopup.folderPopupEntry.isCloseFolder && isLastFolderGridItem) {
        onUpsertFolderPopupEntry(folderPopup.folderPopupEntry.copy(isCloseFolder = true))
    }

    LaunchedEffect(key1 = folderPopup) {
        handleFolderPopup(
            drag = currentDrag,
            isDragging = currentIsDragging,
            isVisibleOverlay = currentIsVisibleOverlay,
            moveGridItemResult = currentMoveGridItemResult,
            folderPopup = folderPopup,
            progress = progress,
            onAnimateToScrollToPage = folderGridHorizontalPagerState::animateScrollToPage,
            onDeleteFolderPopupEntry = onDeleteFolderPopupEntry,
            onMoveFolderGridItemOutsideFolder = onMoveFolderGridItemOutsideFolder,
            onUpdateSharedElementKey = onUpdateSharedElementKey,
        )
    }

    LaunchedEffect(
        drag,
        dragIntOffset,
        folderPopup,
        moveGridItemResult,
        isLastFolderGridItem,
    ) {
        handleDragFolderGridItem(
            density = density,
            drag = drag,
            dragIntOffset = dragIntOffset,
            currentPage = folderGridHorizontalPagerState.currentPage,
            folderPopup = folderPopup,
            folderPopupIntOffset = folderPopupIntOffset,
            isDragging = currentIsDragging,
            isVisibleOverlay = currentIsVisibleOverlay,
            isScrollInProgress = folderGridHorizontalPagerState.isScrollInProgress,
            lockMovement = currentLockMovement,
            paddingValues = paddingValues,
            screenHeight = screenHeight,
            screenWidth = screenWidth,
            moveGridItemResult = moveGridItemResult,
            layoutDirection = layoutDirection,
            folderCellWidth = folderCellWidth,
            folderCellHeight = folderCellHeight,
            isLastFolderGridItem = isLastFolderGridItem,
            onMoveFolderGridItem = onMoveFolderGridItem,
            onUpdateSharedElementKey = onUpdateSharedElementKey,
            onUpsertFolderPopupEntry = onUpsertFolderPopupEntry,
        )
    }

    LaunchedEffect(
        key1 = drag,
        key2 = isLastFolderGridItem,
    ) {
        handleDropFolderGridItem(
            drag = drag,
            isDragging = currentIsDragging,
            lockMovement = currentLockMovement,
            isVisibleOverlay = currentIsVisibleOverlay,
            isLast = isLastFolderGridItem,
            onResetGrid = onResetGrid,
            onDragEndAfterMoveFolder = onDragEndAfterMoveFolder,
            onUpdateIsDragging = onUpdateIsDragging,
            onUpdateIsVisibleOverlay = onUpdateIsVisibleOverlay,
        )
    }

    LaunchedEffect(key1 = pageDirection) {
        handlePageDirection(
            pageDirection = pageDirection,
            currentPage = folderGridHorizontalPagerState.currentPage,
            onAnimateScrollToPage = folderGridHorizontalPagerState::animateScrollToPage,
        )
    }

    LaunchedEffect(key1 = folderGridHorizontalPagerState.isScrollInProgress) {
        if (folderGridHorizontalPagerState.isScrollInProgress) {
            onDismissFolderGridItemPopup()
        }
    }

    LaunchedEffect(
        drag,
        dragIntOffset,
        moveGridItemResult,
        folderPopup,
        isLastFolderGridItem,
    ) {
        handleAnimateScrollToPage(
            density = density,
            drag = drag,
            isVisibleOverlay = currentIsVisibleOverlay,
            lockMovement = currentLockMovement,
            moveGridItemResult = moveGridItemResult,
            dragIntOffset = dragIntOffset,
            folderPopup = folderPopup,
            folderPopupIntOffset = folderPopupIntOffset,
            isDragging = currentIsDragging,
            paddingValues = paddingValues,
            screenWidth = screenWidth,
            layoutDirection = layoutDirection,
            folderCellWidth = folderCellWidth,
            isLast = isLastFolderGridItem,
            onUpdateFolderPageDirection = {
                pageDirection = it
            },
        )
    }

    Box(
        modifier = modifier
            .pointerInput(key1 = isLastFolderGridItem) {
                if (isLastFolderGridItem) {
                    detectTapGestures(
                        onPress = {
                            awaitRelease()

                            onUpsertFolderPopupEntry(
                                folderPopup.folderPopupEntry.copy(
                                    isCloseFolder = true,
                                ),
                            )
                        },
                    )
                }
            }
            .fillMaxSize(),
    ) {
        Surface(
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = when (layoutDirection) {
                            LayoutDirection.Ltr -> animatedRect.left.roundToInt()

                            LayoutDirection.Rtl -> screenWidth - animatedRect.width()
                                .roundToInt() - animatedRect.left.roundToInt()
                        },
                        y = animatedRect.top.roundToInt(),
                    )
                }
                .size(
                    width = with(density) { animatedRect.width().toDp() },
                    height = with(density) { animatedRect.height().toDp() },
                ),
            shape = RoundedCornerShape(5.dp),
            shadowElevation = 2.dp,
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                HorizontalPager(
                    modifier = Modifier.weight(1f),
                    state = folderGridHorizontalPagerState,
                    userScrollEnabled = !isVisibleOverlay,
                ) { index ->
                    FolderGridLayout(
                        modifier = Modifier.fillMaxSize(),
                        columns = folderPopup.columns,
                        gridItems = folderPopup.gridItemsByPage[index],
                        rows = folderPopup.rows,
                        layoutWidth = folderGridWidthPx,
                        layoutHeight = folderGridHeightPx,
                        previewEnabled = true,
                        previewColumns = FOLDER_PREVIEW_COLUMNS,
                        previewRows = FOLDER_PREVIEW_ROWS,
                        progress = progress.value,
                        content = {
                            InteractiveFolderGridItem(
                                sharedTransitionScope = sharedTransitionScope,
                                drag = drag,
                                gridItem = it,
                                gridItemSettings = gridItemSettings,
                                hasShortcutHostPermission = hasShortcutHostPermission,
                                isScrollInProgress = folderGridHorizontalPagerState.isScrollInProgress,
                                statusBarNotifications = statusBarNotifications,
                                isVisibleOverlay = isVisibleOverlay,
                                moveGridItemResult = moveGridItemResult,
                                progress = progress.value,
                                showFolderGridItemPopup = showFolderGridItemPopup,
                                previewFolderGridItems = previewFolderGridItems,
                                minCellWidthPx = minCellWidthPx,
                                minCellHeightPx = minCellHeightPx,
                                onOpenAppDrawer = onOpenAppDrawer,
                                paddingValues = paddingValues,
                                sharedElementKey = SharedElementKey(
                                    id = it.id,
                                    parent = SharedElementKey.Parent.Folder,
                                ),
                                onUpdateImageBitmap = onUpdateImageBitmap,
                                onUpdateIsDragging = onUpdateIsDragging,
                                onUpdateOverlayBounds = onUpdateOverlayBounds,
                                onUpdateSharedElementKey = onUpdateSharedElementKey,
                                onShowGridItemPopup = onShowGridItemPopup,
                                onUpdateIsCloseFolderGridItemPopup = onUpdateIsCloseFolderGridItemPopup,
                                onUpdateIsVisibleOverlay = onUpdateIsVisibleOverlay,
                                onUpdateMoveGridItemResult = onUpdateMoveGridItemResult,
                                onUpsertFolderPopupEntry = onUpsertFolderPopupEntry,
                            )
                        },
                    )
                }

                if (progress.value > 0.5f) {
                    FolderTitle(
                        label = folderPopup.label,
                        gridItemsByPage = folderPopup.gridItemsByPage,
                        folderGridHorizontalPagerState = folderGridHorizontalPagerState,
                    )
                }
            }
        }
    }
}

@Composable
internal fun FolderTitle(
    modifier: Modifier = Modifier,
    label: String,
    gridItemsByPage: Map<Int, List<GridItem>>,
    folderGridHorizontalPagerState: PagerState,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(PAGE_INDICATOR_HEIGHT)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (gridItemsByPage.size > 1) {
            Arrangement.SpaceBetween
        } else {
            Arrangement.Center
        },
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
        )

        if (gridItemsByPage.size > 1) {
            Box(contentAlignment = Alignment.Center) {
                PageIndicator(
                    color = MaterialTheme.colorScheme.onSurface,
                    gridHorizontalPagerState = folderGridHorizontalPagerState,
                    infiniteScroll = false,
                    pageCount = gridItemsByPage.size,
                )
            }
        }
    }
}

private suspend fun handleFolderPopup(
    drag: State<Drag>,
    isDragging: State<Boolean>,
    isVisibleOverlay: State<Boolean>,
    moveGridItemResult: State<MoveGridItemResult?>,
    folderPopup: FolderPopup,
    progress: Animatable<Float, AnimationVector1D>,
    onAnimateToScrollToPage: suspend (Int) -> Unit,
    onDeleteFolderPopupEntry: (FolderPopupEntry) -> Unit,
    onMoveFolderGridItemOutsideFolder: (GridItem) -> Unit,
    onUpdateSharedElementKey: (SharedElementKey?) -> Unit,
) {
    if (folderPopup.folderPopupEntry.isCloseFolder) {
        onAnimateToScrollToPage(0)

        progress.animateTo(targetValue = 0f)

        val gridItem = moveGridItemResult.value?.movingGridItem

        if (drag.value == Drag.Dragging && isDragging.value && isVisibleOverlay.value && gridItem != null) {
            onUpdateSharedElementKey(
                SharedElementKey(
                    id = gridItem.id,
                    parent = SharedElementKey.Parent.Grid,
                ),
            )

            val newGridItem = when (val data = gridItem.data) {
                is GridItemData.ApplicationInfo -> {
                    gridItem.copy(
                        page = folderPopup.gridItem.page,
                        startColumn = folderPopup.gridItem.startColumn,
                        startRow = folderPopup.gridItem.startRow,
                        data = data.copy(
                            index = -1,
                            folderId = null,
                        ),
                    )
                }

                is GridItemData.Folder -> {
                    gridItem.copy(
                        page = folderPopup.gridItem.page,
                        startColumn = folderPopup.gridItem.startColumn,
                        startRow = folderPopup.gridItem.startRow,
                        data = data.copy(
                            index = -1,
                            folderId = null,
                        ),
                    )
                }

                is GridItemData.ShortcutConfig -> {
                    gridItem.copy(
                        page = folderPopup.gridItem.page,
                        startColumn = folderPopup.gridItem.startColumn,
                        startRow = folderPopup.gridItem.startRow,
                        data = data.copy(
                            index = -1,
                            folderId = null,
                        ),
                    )
                }

                is GridItemData.ShortcutInfo -> {
                    gridItem.copy(
                        page = folderPopup.gridItem.page,
                        startColumn = folderPopup.gridItem.startColumn,
                        startRow = folderPopup.gridItem.startRow,
                        data = data.copy(
                            index = -1,
                            folderId = null,
                        ),
                    )
                }

                is GridItemData.Widget -> error("Unsupported Folder Grid Item")
            }

            onMoveFolderGridItemOutsideFolder(newGridItem)
        }

        onDeleteFolderPopupEntry(folderPopup.folderPopupEntry)
    }
}
