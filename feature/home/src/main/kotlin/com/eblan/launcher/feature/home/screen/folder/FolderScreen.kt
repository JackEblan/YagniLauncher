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

import android.content.Intent.parseUri
import android.graphics.Rect
import android.graphics.RectF
import android.os.Build
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
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
import com.eblan.launcher.feature.home.component.PageIndicator
import com.eblan.launcher.feature.home.model.Drag
import com.eblan.launcher.feature.home.model.PageDirection
import com.eblan.launcher.feature.home.model.SharedElementKey
import com.eblan.launcher.feature.home.util.FOLDER_PREVIEW_COLUMNS
import com.eblan.launcher.feature.home.util.FOLDER_PREVIEW_ROWS
import com.eblan.launcher.feature.home.util.PAGE_INDICATOR_HEIGHT
import com.eblan.launcher.ui.local.LocalLauncherApps
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
    onDismissFolderGridItemPopup: () -> Unit,
    onDragCancelAfterMoveFolder: () -> Unit,
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

    val folderGridItem = folderPopup.gridItem

    val data = folderGridItem.data as GridItemData.Folder

    val density = LocalDensity.current

    val context = LocalContext.current

    val layoutDirection = LocalLayoutDirection.current

    val androidLauncherAppsWrapper = LocalLauncherApps.current

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
            data.gridItemsByPage.size
        },
    )

    var pageDirection by remember { mutableStateOf<PageDirection?>(null) }

    val isLastFolderGridItem = lastFolderPopup?.gridItem == folderGridItem

    val currentDrag = rememberUpdatedState(drag)
    val currentIsDragging = rememberUpdatedState(isDragging)
    val currentIsVisibleOverlay = rememberUpdatedState(isVisibleOverlay)
    val currentMoveGridItemResult = rememberUpdatedState(moveGridItemResult)
    val currentLockMovement = rememberUpdatedState(lockMovement)
    val currentFolderPopupEntry = rememberUpdatedState(folderPopup.folderPopupEntry)
    val currentFolderPopupIntOffset = rememberUpdatedState(folderPopupIntOffset)

    LaunchedEffect(key1 = Unit) {
        progress.animateTo(targetValue = 1f)
    }

    BackHandler(enabled = !folderPopup.folderPopupEntry.isCloseFolder && isLastFolderGridItem) {
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
        folderGridItem,
        moveGridItemResult,
        isLastFolderGridItem,
    ) {
        handleDragFolderGridItem(
            density = density,
            drag = drag,
            dragIntOffset = dragIntOffset,
            currentPage = folderGridHorizontalPagerState.currentPage,
            folderGridItem = folderGridItem,
            folderPopupIntOffset = currentFolderPopupIntOffset,
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
            folderPopupEntry = currentFolderPopupEntry,
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
            onDragCancelAfterMoveFolder = onDragCancelAfterMoveFolder,
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
        folderGridItem,
        isLastFolderGridItem,
    ) {
        handleAnimateScrollToPage(
            density = density,
            drag = drag,
            isVisibleOverlay = currentIsVisibleOverlay,
            lockMovement = currentLockMovement,
            moveGridItemResult = moveGridItemResult,
            dragIntOffset = dragIntOffset,
            folderGridItem = folderGridItem,
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
                        x = animatedRect.left.roundToInt(),
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
                        columns = data.columns,
                        gridItems = data.gridItemsByPage[index],
                        rows = data.rows,
                        layoutWidth = folderGridWidthPx,
                        layoutHeight = folderGridHeightPx,
                        previewEnabled = true,
                        previewColumns = FOLDER_PREVIEW_COLUMNS,
                        previewRows = FOLDER_PREVIEW_ROWS,
                        progress = progress.value,
                        content = { gridItem ->
                            val x = gridItem.startColumn * minCellWidthPx

                            val y = gridItem.startRow * minCellHeightPx

                            InteractiveFolderGridItem(
                                sharedTransitionScope = sharedTransitionScope,
                                drag = drag,
                                gridItem = gridItem,
                                gridItemSettings = gridItemSettings,
                                hasShortcutHostPermission = hasShortcutHostPermission,
                                isScrollInProgress = folderGridHorizontalPagerState.isScrollInProgress,
                                statusBarNotifications = statusBarNotifications,
                                isVisibleOverlay = isVisibleOverlay,
                                sharedElementKey = SharedElementKey(
                                    id = gridItem.id,
                                    parent = SharedElementKey.Parent.Folder,
                                ),
                                moveGridItemResult = moveGridItemResult,
                                progress = progress.value,
                                onOpenAppDrawer = onOpenAppDrawer,
                                onTapApplicationInfo = { serialNumber, componentName ->
                                    val sourceBoundsX = x + leftPadding

                                    val sourceBoundsY = y + topPadding

                                    androidLauncherAppsWrapper.startMainActivity(
                                        serialNumber = serialNumber,
                                        componentName = componentName,
                                        sourceBounds = Rect(
                                            sourceBoundsX,
                                            sourceBoundsY,
                                            sourceBoundsX + minCellWidthPx,
                                            sourceBoundsY + minCellHeightPx,
                                        ),
                                    )
                                },
                                onTapShortcutConfig = { uri ->
                                    context.startActivity(parseUri(uri, 0))
                                },
                                onTapShortcutInfo = { serialNumber, packageName, shortcutId ->
                                    val sourceBoundsX = x + leftPadding

                                    val sourceBoundsY = y + topPadding

                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
                                        androidLauncherAppsWrapper.startShortcut(
                                            serialNumber = serialNumber,
                                            packageName = packageName,
                                            id = shortcutId,
                                            sourceBounds = Rect(
                                                sourceBoundsX,
                                                sourceBoundsY,
                                                sourceBoundsX + minCellWidthPx,
                                                sourceBoundsY + minCellHeightPx,
                                            ),
                                        )
                                    }
                                },
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
                        data = data,
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
    data: GridItemData.Folder,
    folderGridHorizontalPagerState: PagerState,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(PAGE_INDICATOR_HEIGHT)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (data.gridItemsByPage.size > 1) {
            Arrangement.SpaceBetween
        } else {
            Arrangement.Center
        },
    ) {
        Text(
            text = data.label,
            style = MaterialTheme.typography.bodySmall,
        )

        if (data.gridItemsByPage.size > 1) {
            Box(contentAlignment = Alignment.Center) {
                PageIndicator(
                    color = MaterialTheme.colorScheme.onSurface,
                    gridHorizontalPagerState = folderGridHorizontalPagerState,
                    infiniteScroll = false,
                    pageCount = data.gridItemsByPage.size,
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

        if (drag.value == Drag.Dragging &&
            isDragging.value &&
            isVisibleOverlay.value &&
            gridItem != null
        ) {
            onUpdateSharedElementKey(
                SharedElementKey(
                    id = gridItem.id,
                    parent = SharedElementKey.Parent.Grid,
                ),
            )

            onMoveFolderGridItemOutsideFolder(gridItem)
        }

        onDeleteFolderPopupEntry(folderPopup.folderPopupEntry)
    }
}
