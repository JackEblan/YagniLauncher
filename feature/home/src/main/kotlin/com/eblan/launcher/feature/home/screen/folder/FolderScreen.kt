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
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Animatable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.eblan.launcher.domain.model.GridItem
import com.eblan.launcher.domain.model.GridItemData
import com.eblan.launcher.domain.model.GridItemSettings
import com.eblan.launcher.domain.model.MoveGridItemResult
import com.eblan.launcher.domain.usecase.grid.FOLDER_MAX_COLUMNS
import com.eblan.launcher.domain.usecase.grid.FOLDER_MAX_ROWS
import com.eblan.launcher.feature.home.component.FolderGridLayout
import com.eblan.launcher.feature.home.component.PageIndicator
import com.eblan.launcher.feature.home.model.Drag
import com.eblan.launcher.feature.home.model.GridItemSource
import com.eblan.launcher.feature.home.model.SharedElementKey
import com.eblan.launcher.feature.home.util.FOLDER_COLUMNS
import com.eblan.launcher.feature.home.util.FOLDER_ROWS
import com.eblan.launcher.feature.home.util.PAGE_INDICATOR_HEIGHT
import com.eblan.launcher.ui.local.LocalLauncherApps
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
internal fun SharedTransitionScope.FolderScreen(
    modifier: Modifier = Modifier,
    drag: Drag,
    folderGridHorizontalPagerState: PagerState,
    folderGridItem: GridItem,
    folderPopupIntOffset: IntOffset?,
    folderPopupIntSize: IntSize?,
    gridItemSettings: GridItemSettings,
    iconPackFilePaths: Map<String, String>,
    paddingValues: PaddingValues,
    safeDrawingHeight: Int,
    safeDrawingWidth: Int,
    statusBarNotifications: Map<String, Int>,
    isVisibleOverlay: Boolean,
    isCloseFolder: Boolean,
    isMoveFolderGridItemOutsideFolder: Boolean,
    hasShortcutHostPermission: Boolean,
    moveGridItemResult: MoveGridItemResult?,
    isDragging: Boolean,
    lockMovement: Boolean,
    onDismissRequest: () -> Unit,
    onMoveFolderGridItemOutsideFolder: () -> Unit,
    onOpenAppDrawer: () -> Unit,
    onUpdateGridItemSource: (GridItemSource) -> Unit,
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
    onDismissGridItemPopup: () -> Unit,
    onUpdateIsVisibleOverlay: (Boolean) -> Unit,
    onUpdateIsClosingFolder: (Boolean) -> Unit,
    onUpdateMoveGridItemResult: (MoveGridItemResult) -> Unit,
    onShowFolderWhenDragging: (
        conflictingGridItem: GridItem,
        movingGridItem: GridItem,
    ) -> Unit,
    onUpdateFolderPopupBounds: (
        intOffset: IntOffset,
        intSize: IntSize,
    ) -> Unit,
) {
    requireNotNull(folderPopupIntOffset)

    requireNotNull(folderPopupIntSize)

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

    val cellWidth = safeDrawingWidth / FOLDER_MAX_COLUMNS
    val cellHeight = safeDrawingHeight / FOLDER_MAX_ROWS

    val folderGridWidthDp = with(density) {
        (cellWidth * data.columns).toDp()
    }

    val folderGridHeightDp = with(density) {
        (cellHeight * data.rows).toDp()
    }

    val folderGridWidthPx = with(density) { folderGridWidthDp.roundToPx() }
    val folderGridHeightPx = with(density) { folderGridHeightDp.roundToPx() }

    val progress = remember { Animatable(0f) }

    val x = folderPopupIntOffset.x - leftPadding
    val y = folderPopupIntOffset.y - topPadding

    val intOffset = IntOffset(
        x = x.coerceIn(
            0,
            safeDrawingWidth - folderGridWidthPx,
        ) + leftPadding,
        y = y.coerceIn(
            0,
            safeDrawingHeight - folderGridHeightPx,
        ) + topPadding,
    )

    val startWidth = folderPopupIntSize.width.toFloat()
    val startHeight = folderPopupIntSize.height.toFloat()

    val endWidth = folderGridWidthPx.toFloat()
    val endHeight = folderGridHeightPx.toFloat()

    val startCenterX = folderPopupIntOffset.x + startWidth / 2f
    val startCenterY = folderPopupIntOffset.y + startHeight / 2f

    val endCenterX = intOffset.x + endWidth / 2f
    val endCenterY = intOffset.y + endHeight / 2f

    val animatedWidth by remember {
        derivedStateOf {
            lerp(startWidth, endWidth, progress.value)
        }
    }

    val animatedHeight by remember {
        derivedStateOf {
            lerp(startHeight, endHeight, progress.value)
        }
    }

    val animatedCenterX by remember {
        derivedStateOf {
            lerp(startCenterX, endCenterX, progress.value)
        }
    }

    val animatedCenterY by remember {
        derivedStateOf {
            lerp(startCenterY, endCenterY, progress.value)
        }
    }

    val animatedOffset by remember {
        derivedStateOf {
            IntOffset(
                x = (animatedCenterX - animatedWidth / 2f).roundToInt(),
                y = (animatedCenterY - animatedHeight / 2f).roundToInt(),
            )
        }
    }

    val animatedColumns = remember { Animatable(FOLDER_COLUMNS.toFloat()) }

    val animatedRows = remember { Animatable(FOLDER_ROWS.toFloat()) }

    LaunchedEffect(key1 = Unit) {
        launch { progress.animateTo(targetValue = 1f) }

        launch { animatedColumns.animateTo(targetValue = data.columns.toFloat()) }

        launch { animatedRows.animateTo(targetValue = data.rows.toFloat()) }
    }

    BackHandler(enabled = !isCloseFolder) {
        onUpdateIsClosingFolder(true)
    }

    LaunchedEffect(key1 = isCloseFolder) {
        if (isCloseFolder) {
            folderGridHorizontalPagerState.animateScrollToPage(0)

            joinAll(
                launch { progress.animateTo(targetValue = 0f) },
                launch { animatedColumns.animateTo(targetValue = 3f) },
                launch { animatedRows.animateTo(targetValue = 3f) },
            )

            onDismissRequest()
        }
    }

    LaunchedEffect(key1 = isMoveFolderGridItemOutsideFolder) {
        if (isMoveFolderGridItemOutsideFolder) {
            folderGridHorizontalPagerState.animateScrollToPage(0)

            joinAll(
                launch { progress.animateTo(targetValue = 0f) },
                launch { animatedColumns.animateTo(targetValue = 3f) },
                launch { animatedRows.animateTo(targetValue = 3f) },
            )

            onMoveFolderGridItemOutsideFolder()
        }
    }

    Box(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        awaitRelease()

                        onUpdateIsClosingFolder(true)
                    },
                )
            }
            .fillMaxSize(),
    ) {
        Surface(
            modifier = Modifier
                .offset { animatedOffset }
                .size(
                    width = with(density) { animatedWidth.toDp() },
                    height = with(density) { animatedHeight.toDp() },
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
                        columns = animatedColumns.value.roundToInt(),
                        gridItems = data.gridItemsByPage[index],
                        rows = animatedRows.value.roundToInt(),
                        content = { gridItem ->
                            val x = gridItem.startColumn * cellWidth

                            val y = gridItem.startRow * cellHeight

                            InteractiveFolderGridItemContent(
                                drag = drag,
                                gridItem = gridItem,
                                gridItemSettings = gridItemSettings,
                                hasShortcutHostPermission = hasShortcutHostPermission,
                                iconPackFilePaths = iconPackFilePaths,
                                isScrollInProgress = folderGridHorizontalPagerState.isScrollInProgress,
                                statusBarNotifications = statusBarNotifications,
                                isVisibleOverlay = isVisibleOverlay,
                                newGridItemSource = GridItemSource.Folder,
                                sharedElementKey = SharedElementKey(
                                    id = gridItem.id,
                                    parent = SharedElementKey.Parent.Folder,
                                ),
                                moveGridItemResult = moveGridItemResult,
                                isDragging = isDragging,
                                lockMovement = lockMovement,
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
                                            sourceBoundsX + cellWidth,
                                            sourceBoundsY + cellHeight,
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
                                                sourceBoundsX + cellWidth,
                                                sourceBoundsY + cellHeight,
                                            ),
                                        )
                                    }
                                },
                                onUpdateGridItemSource = onUpdateGridItemSource,
                                onUpdateImageBitmap = onUpdateImageBitmap,
                                onUpdateIsDragging = onUpdateIsDragging,
                                onUpdateOverlayBounds = onUpdateOverlayBounds,
                                onUpdateSharedElementKey = onUpdateSharedElementKey,
                                onShowGridItemPopup = onShowGridItemPopup,
                                onDismissGridItemPopup = onDismissGridItemPopup,
                                onUpdateIsVisibleOverlay = onUpdateIsVisibleOverlay,
                                onUpdateMoveGridItemResult = onUpdateMoveGridItemResult,
                                onShowFolderWhenDragging = onShowFolderWhenDragging,
                                onUpdateFolderPopupBounds = onUpdateFolderPopupBounds,
                            )
                        },
                    )
                }

                if (progress.value == 1f) {
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
    if (data.gridItemsByPage.size > 1) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = data.label,
                style = MaterialTheme.typography.bodySmall,
            )

            PageIndicator(
                modifier = Modifier.height(PAGE_INDICATOR_HEIGHT),
                color = MaterialTheme.colorScheme.onSurface,
                gridHorizontalPagerState = folderGridHorizontalPagerState,
                infiniteScroll = false,
                pageCount = data.gridItemsByPage.size,
            )
        }
    } else {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(
                text = data.label,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}
