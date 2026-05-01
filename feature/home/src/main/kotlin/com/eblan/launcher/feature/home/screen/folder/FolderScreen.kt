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
import androidx.compose.foundation.layout.calculateStartPadding
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.eblan.launcher.domain.model.GridItem
import com.eblan.launcher.domain.model.GridItemData
import com.eblan.launcher.domain.model.GridItemSettings
import com.eblan.launcher.domain.usecase.grid.FOLDER_MAX_COLUMNS
import com.eblan.launcher.domain.usecase.grid.FOLDER_MAX_ROWS
import com.eblan.launcher.feature.home.component.FolderGridLayout
import com.eblan.launcher.feature.home.component.PageIndicator
import com.eblan.launcher.feature.home.model.Drag
import com.eblan.launcher.feature.home.model.GridItemSource
import com.eblan.launcher.feature.home.model.SharedElementKey
import com.eblan.launcher.feature.home.util.FOLDER_GRID_PADDING
import com.eblan.launcher.feature.home.util.PAGE_INDICATOR_HEIGHT
import com.eblan.launcher.ui.local.LocalLauncherApps

@Composable
internal fun SharedTransitionScope.FolderScreen(
    modifier: Modifier = Modifier,
    drag: Drag,
    folderGridHorizontalPagerState: PagerState,
    folderGridItem: GridItem,
    folderPopupIntOffset: IntOffset?,
    folderPopupIntSize: IntSize?,
    gridItemSettings: GridItemSettings,
    gridItemSource: GridItemSource?,
    iconPackFilePaths: Map<String, String>,
    paddingValues: PaddingValues,
    safeDrawingHeight: Int,
    safeDrawingWidth: Int,
    statusBarNotifications: Map<String, Int>,
    isVisibleOverlay: Boolean,
    isClosingFolder: Boolean,
    isMoveFolderGridItemOutsideFolder: Boolean,
    hasShortcutHostPermission: Boolean,
    onDismissRequest: () -> Unit,
    onMoveFolderGridItemOutsideFolder: () -> Unit,
    onDraggingGridItem: () -> Unit,
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
) {
    if (folderPopupIntOffset == null || folderPopupIntSize == null) return

    val data = folderGridItem.data as? GridItemData.Folder ?: return

    val density = LocalDensity.current

    val context = LocalContext.current

    val androidLauncherAppsWrapper = LocalLauncherApps.current

    val leftPadding = with(density) {
        paddingValues.calculateStartPadding(LayoutDirection.Ltr).roundToPx()
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

    val centeredX =
        folderPopupIntOffset.x + (folderPopupIntSize.width / 2) - (folderGridWidthPx / 2)

    val centeredY =
        folderPopupIntOffset.y + (folderPopupIntSize.height / 2) - (folderGridHeightPx / 2)

    val endOffset = IntOffset(
        x = centeredX.coerceIn(0, safeDrawingWidth - folderGridWidthPx),
        y = centeredY.coerceIn(0, safeDrawingHeight - folderGridHeightPx),
    )

    val startCenterX = folderPopupIntOffset.x + folderPopupIntSize.width / 2f
    val startCenterY = folderPopupIntOffset.y + folderPopupIntSize.height / 2f

    val endCenterX = endOffset.x + folderGridWidthPx / 2f
    val endCenterY = endOffset.y + folderGridHeightPx / 2f

    val scaleX by remember {
        derivedStateOf {
            lerp(
                folderPopupIntSize.width.toFloat() / folderGridWidthPx,
                1f,
                progress.value,
            )
        }
    }

    val scaleY by remember {
        derivedStateOf {
            lerp(
                folderPopupIntSize.height.toFloat() / folderGridHeightPx,
                1f,
                progress.value,
            )
        }
    }

    val translationX by remember {
        derivedStateOf {
            lerp(
                startCenterX - endCenterX,
                0f,
                progress.value,
            )
        }
    }

    val translationY by remember {
        derivedStateOf {
            lerp(
                startCenterY - endCenterY,
                0f,
                progress.value,
            )
        }
    }

    LaunchedEffect(key1 = Unit) {
        progress.snapTo(targetValue = 0f)

        progress.animateTo(targetValue = 1f)
    }

    BackHandler(enabled = !isClosingFolder) {
        onUpdateIsClosingFolder(true)
    }

    LaunchedEffect(key1 = isClosingFolder) {
        if (isClosingFolder) {
            progress.animateTo(targetValue = 0f)

            onDismissRequest()
        }
    }

    LaunchedEffect(key1 = isMoveFolderGridItemOutsideFolder) {
        if (isMoveFolderGridItemOutsideFolder) {
            progress.animateTo(targetValue = 0f)

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
            .fillMaxSize()
            .padding(paddingValues),
    ) {
        Surface(
            modifier = Modifier
                .offset { endOffset }
                .graphicsLayer(
                    scaleX = scaleX,
                    scaleY = scaleY,
                    translationX = translationX,
                    translationY = translationY,
                    transformOrigin = TransformOrigin.Center,
                )
                .size(
                    width = folderGridWidthDp,
                    height = folderGridHeightDp,
                )
                .padding(FOLDER_GRID_PADDING)
                .alpha(progress.value),
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
                        content = { gridItem ->
                            val x = gridItem.startColumn * cellWidth

                            val y = gridItem.startRow * cellHeight

                            InteractiveFolderGridItemContent(
                                drag = drag,
                                gridItem = gridItem,
                                gridItemSettings = gridItemSettings,
                                gridItemSource = gridItemSource,
                                hasShortcutHostPermission = hasShortcutHostPermission,
                                iconPackFilePaths = iconPackFilePaths,
                                isScrollInProgress = folderGridHorizontalPagerState.isScrollInProgress,
                                statusBarNotifications = statusBarNotifications,
                                isVisibleOverlay = isVisibleOverlay,
                                newGridItemSource = GridItemSource.Folder(gridItem = gridItem),
                                sharedElementKey = SharedElementKey(
                                    id = gridItem.id,
                                    parent = SharedElementKey.Parent.Folder,
                                ),
                                onDraggingGridItem = onDraggingGridItem,
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
                                onTapFolderGridItem = {
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
                            )
                        },
                    )
                }

                FolderTitle(
                    data = data,
                    folderGridHorizontalPagerState = folderGridHorizontalPagerState,
                )
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
