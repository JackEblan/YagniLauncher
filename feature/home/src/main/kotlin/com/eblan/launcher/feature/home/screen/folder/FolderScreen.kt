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

import android.graphics.Rect
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest.Builder
import coil3.request.addLastModifiedToFileCacheKey
import com.eblan.launcher.domain.model.ApplicationInfoGridItem
import com.eblan.launcher.domain.model.Associate
import com.eblan.launcher.domain.model.GridItem
import com.eblan.launcher.domain.model.GridItemData
import com.eblan.launcher.domain.model.GridItemSettings
import com.eblan.launcher.domain.model.HomeSettings
import com.eblan.launcher.domain.model.TextColor
import com.eblan.launcher.feature.home.component.grid.FolderGridLayout
import com.eblan.launcher.feature.home.component.indicator.PageIndicator
import com.eblan.launcher.feature.home.component.modifier.swipeGestures
import com.eblan.launcher.feature.home.component.modifier.whiteBox
import com.eblan.launcher.feature.home.model.Drag
import com.eblan.launcher.feature.home.model.GridItemSource
import com.eblan.launcher.feature.home.model.SharedElementKey
import com.eblan.launcher.feature.home.util.FOLDER_GRID_PADDING
import com.eblan.launcher.feature.home.util.PAGE_INDICATOR_HEIGHT
import com.eblan.launcher.feature.home.util.getGridItemTextColor
import com.eblan.launcher.feature.home.util.getHorizontalAlignment
import com.eblan.launcher.feature.home.util.getSystemTextColor
import com.eblan.launcher.feature.home.util.getVerticalArrangement
import com.eblan.launcher.feature.home.util.handleDrag
import com.eblan.launcher.feature.home.util.onDoubleTap
import com.eblan.launcher.feature.home.util.onLongPress
import com.eblan.launcher.ui.local.LocalLauncherApps
import com.eblan.launcher.ui.local.LocalSettings

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
    homeSettings: HomeSettings,
    iconPackFilePaths: Map<String, String>,
    isLongPress: Boolean,
    paddingValues: PaddingValues,
    safeDrawingHeight: Int,
    safeDrawingWidth: Int,
    statusBarNotifications: Map<String, Int>,
    textColor: TextColor,
    isVisibleOverlay: Boolean,
    onDismissRequest: () -> Unit,
    onDraggingGridItem: () -> Unit,
    onOpenAppDrawer: () -> Unit,
    onUpdateGridItemSource: (GridItemSource) -> Unit,
    onUpdateImageBitmap: (ImageBitmap) -> Unit,
    onUpdateIsDragging: (Boolean) -> Unit,
    onUpdateIsLongPress: (Boolean) -> Unit,
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
    onUpdateAssociate: (Associate) -> Unit,
    onUpdateIsVisibleOverlay: (Boolean) -> Unit,
) {
    if (folderPopupIntOffset == null || folderPopupIntSize == null) return

    val data = folderGridItem.data as? GridItemData.Folder ?: return

    val density = LocalDensity.current

    val folderCellWidth = safeDrawingWidth / homeSettings.columns

    val folderCellHeight = safeDrawingHeight / homeSettings.rows

    val folderGridWidthDp = with(density) {
        (folderCellWidth * data.columns).toDp()
    }

    val folderGridHeightDp = with(density) {
        (folderCellHeight * data.rows).toDp()
    }

    val folderGridWidthPx = with(density) {
        folderGridWidthDp.roundToPx()
    }

    val folderGridHeightPx = with(density) {
        folderGridHeightDp.roundToPx()
    }

    Box(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        awaitRelease()

                        onDismissRequest()
                    },
                )
            }
            .fillMaxSize()
            .padding(paddingValues),
    ) {
        Surface(
            modifier = Modifier
                .offset {
                    val centeredX =
                        folderPopupIntOffset.x + (folderPopupIntSize.width / 2) - (folderGridWidthPx / 2)

                    val centeredY =
                        folderPopupIntOffset.y + (folderPopupIntSize.height / 2) - (folderGridHeightPx / 2)

                    val popupX = centeredX.coerceIn(0, safeDrawingWidth - folderGridWidthPx)

                    val popupY = centeredY.coerceIn(0, safeDrawingHeight - folderGridHeightPx)

                    IntOffset(x = popupX, y = popupY)
                }
                .size(
                    width = folderGridWidthDp,
                    height = folderGridHeightDp,
                )
                .padding(FOLDER_GRID_PADDING),
            shape = RoundedCornerShape(5.dp),
            shadowElevation = 2.dp,
            content = {
                Column(modifier = Modifier.fillMaxSize()) {
                    HorizontalPager(
                        modifier = Modifier.weight(1f),
                        state = folderGridHorizontalPagerState,
                    ) { index ->
                        FolderGridLayout(
                            modifier = Modifier.fillMaxSize(),
                            columns = data.columns,
                            gridItems = data.gridItemsByPage[index],
                            rows = data.rows,
                            content = { applicationInfoGridItem ->
                                FolderGridItemContent(
                                    drag = drag,
                                    folderGridItem = folderGridItem,
                                    gridItem = applicationInfoGridItem,
                                    gridItemSettings = gridItemSettings,
                                    gridItemSource = gridItemSource,
                                    iconPackFilePaths = iconPackFilePaths,
                                    isLongPress = isLongPress,
                                    statusBarNotifications = statusBarNotifications,
                                    textColor = textColor,
                                    isVisibleOverlay = isVisibleOverlay,
                                    onDraggingGridItem = onDraggingGridItem,
                                    onOpenAppDrawer = onOpenAppDrawer,
                                    onUpdateGridItemSource = onUpdateGridItemSource,
                                    onUpdateImageBitmap = onUpdateImageBitmap,
                                    onUpdateIsDragging = onUpdateIsDragging,
                                    onUpdateIsLongPress = onUpdateIsLongPress,
                                    onUpdateOverlayBounds = onUpdateOverlayBounds,
                                    onUpdateSharedElementKey = onUpdateSharedElementKey,
                                    onShowGridItemPopup = onShowGridItemPopup,
                                    onDismissGridItemPopup = onDismissGridItemPopup,
                                    onUpdateAssociate = onUpdateAssociate,
                                    onUpdateIsVisibleOverlay = onUpdateIsVisibleOverlay,
                                )
                            },
                        )
                    }

                    FolderTitle(
                        data = data,
                        folderGridHorizontalPagerState = folderGridHorizontalPagerState,
                        homeSettings = homeSettings,
                        textColor = textColor,
                    )
                }
            },
        )
    }
}

@Composable
internal fun FolderTitle(
    modifier: Modifier = Modifier,
    data: GridItemData.Folder,
    folderGridHorizontalPagerState: PagerState,
    homeSettings: HomeSettings,
    textColor: TextColor,
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
                color = getSystemTextColor(
                    systemCustomTextColor = homeSettings.gridItemSettings.customTextColor,
                    systemTextColor = textColor,
                ),
                style = MaterialTheme.typography.bodySmall,
            )

            PageIndicator(
                modifier = Modifier.height(PAGE_INDICATOR_HEIGHT),
                color = getSystemTextColor(
                    systemCustomTextColor = homeSettings.gridItemSettings.customTextColor,
                    systemTextColor = textColor,
                ),
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
                color = getSystemTextColor(
                    systemCustomTextColor = homeSettings.gridItemSettings.customTextColor,
                    systemTextColor = textColor,
                ),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun SharedTransitionScope.FolderGridItemContent(
    modifier: Modifier = Modifier,
    drag: Drag,
    folderGridItem: GridItem,
    gridItem: ApplicationInfoGridItem,
    gridItemSettings: GridItemSettings,
    gridItemSource: GridItemSource?,
    iconPackFilePaths: Map<String, String>,
    isLongPress: Boolean,
    statusBarNotifications: Map<String, Int>,
    textColor: TextColor,
    isVisibleOverlay: Boolean,
    onDraggingGridItem: () -> Unit,
    onOpenAppDrawer: () -> Unit,
    onUpdateGridItemSource: (GridItemSource) -> Unit,
    onUpdateImageBitmap: (ImageBitmap) -> Unit,
    onUpdateIsDragging: (Boolean) -> Unit,
    onUpdateIsLongPress: (Boolean) -> Unit,
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
    onUpdateAssociate: (Associate) -> Unit,
    onUpdateIsVisibleOverlay: (Boolean) -> Unit,
) {
    val launcherApps = LocalLauncherApps.current

    val context = LocalContext.current

    val settings = LocalSettings.current

    val gridItemSourceFolder = gridItemSource as? GridItemSource.Folder

    val isSelected = gridItemSourceFolder != null &&
        gridItem.id == gridItemSourceFolder.applicationInfoGridItem.id

    val currentGridItemSettings = if (gridItem.override) {
        gridItem.gridItemSettings
    } else {
        gridItemSettings
    }

    val currentTextColor = if (gridItem.override) {
        getGridItemTextColor(
            gridItemCustomTextColor = gridItem.gridItemSettings.customTextColor,
            gridItemTextColor = gridItem.gridItemSettings.textColor,
            systemCustomTextColor = gridItemSettings.customTextColor,
            systemTextColor = textColor,
        )
    } else {
        getSystemTextColor(
            systemCustomTextColor = gridItemSettings.customTextColor,
            systemTextColor = textColor,
        )
    }

    val horizontalAlignment =
        getHorizontalAlignment(horizontalAlignment = currentGridItemSettings.horizontalAlignment)

    val verticalArrangement =
        getVerticalArrangement(verticalArrangement = currentGridItemSettings.verticalArrangement)

    var intOffset by remember { mutableStateOf(IntOffset.Zero) }

    var intSize by remember { mutableStateOf(IntSize.Zero) }

    val graphicsLayer = rememberGraphicsLayer()

    val scope = rememberCoroutineScope()

    val maxLines = if (currentGridItemSettings.singleLineLabel) 1 else Int.MAX_VALUE

    val icon = iconPackFilePaths[gridItem.componentName] ?: gridItem.icon

    val hasNotifications =
        statusBarNotifications[gridItem.packageName] != null && (
            statusBarNotifications[gridItem.packageName]
                ?: 0
            ) > 0

    val hasInteraction = isSelected && isLongPress && (drag == Drag.Start || drag == Drag.Dragging)

    val isVisibleWhiteBox = isSelected && drag == Drag.Dragging

    val alpha = if (hasInteraction) 0f else 1f

    LaunchedEffect(key1 = drag) {
        handleDrag(
            drag = drag,
            isSelected = isSelected,
            isLongPress = isLongPress,
            onUpdateIsDragging = onUpdateIsDragging,
            onDismissGridItemPopup = onDismissGridItemPopup,
            onDraggingGridItem = onDraggingGridItem,
        )
    }

    Column(
        modifier = modifier
            .pointerInput(key1 = drag) {
                detectTapGestures(
                    onDoubleTap = if (!isLongPress) {
                        {
                            onDoubleTap(
                                context = context,
                                doubleTap = gridItem.doubleTap,
                                launcherApps = launcherApps,
                                scope = scope,
                                onOpenAppDrawer = onOpenAppDrawer,
                            )
                        }
                    } else {
                        null
                    },
                    onLongPress = if (!isLongPress) {
                        {
                            onLongPress(
                                scope = scope,
                                graphicsLayer = graphicsLayer,
                                intOffset = intOffset,
                                intSize = intSize,
                                gridItemSource = GridItemSource.Folder(
                                    gridItem = folderGridItem,
                                    applicationInfoGridItem = gridItem,
                                ),
                                sharedElementKey = SharedElementKey(
                                    id = gridItem.id,
                                    parent = SharedElementKey.Parent.Folder,
                                ),
                                onUpdateGridItemSource = onUpdateGridItemSource,
                                onUpdateImageBitmap = onUpdateImageBitmap,
                                onUpdateIsLongPress = onUpdateIsLongPress,
                                onUpdateOverlayBounds = onUpdateOverlayBounds,
                                onUpdateSharedElementKey = onUpdateSharedElementKey,
                                onShowGridItemPopup = onShowGridItemPopup,
                                onUpdateAssociate = onUpdateAssociate,
                                onUpdateIsVisibleOverlay = onUpdateIsVisibleOverlay,
                            )
                        }
                    } else {
                        null
                    },
                    onTap = if (!isLongPress) {
                        {
                            launcherApps.startMainActivity(
                                serialNumber = gridItem.serialNumber,
                                componentName = gridItem.componentName,
                                sourceBounds = Rect(
                                    intOffset.x,
                                    intOffset.y,
                                    intOffset.x + intSize.width,
                                    intOffset.y + intSize.height,
                                ),
                            )
                        }
                    } else {
                        null
                    },
                )
            }
            .swipeGestures(
                swipeDown = gridItem.swipeDown,
                swipeUp = gridItem.swipeUp,
                onOpenAppDrawer = onOpenAppDrawer,
            )
            .fillMaxSize()
            .padding(currentGridItemSettings.padding.dp)
            .background(
                color = Color(currentGridItemSettings.customBackgroundColor),
                shape = RoundedCornerShape(size = currentGridItemSettings.cornerRadius.dp),
            )
            .whiteBox(textColor = currentTextColor, visible = isVisibleWhiteBox),
        horizontalAlignment = horizontalAlignment,
        verticalArrangement = verticalArrangement,
    ) {
        Box(
            modifier = Modifier
                .size(currentGridItemSettings.iconSize.dp)
                .alpha(alpha),
        ) {
            AsyncImage(
                model = Builder(LocalContext.current).data(gridItem.customIcon ?: icon)
                    .addLastModifiedToFileCacheKey(true)
                    .build(),
                contentDescription = null,
                modifier = Modifier
                    .matchParentSize()
                    .drawWithContent {
                        graphicsLayer.record {
                            this@drawWithContent.drawContent()
                        }

                        drawLayer(graphicsLayer)
                    }
                    .onGloballyPositioned { layoutCoordinates ->
                        intOffset = layoutCoordinates.positionInRoot().round()

                        intSize = layoutCoordinates.size
                    }
                    .run {
                        if (!hasInteraction) {
                            sharedElementWithCallerManagedVisibility(
                                rememberSharedContentState(
                                    key = SharedElementKey(
                                        id = gridItem.id,
                                        parent = SharedElementKey.Parent.Folder,
                                    ),
                                ),
                                visible = !isVisibleOverlay,
                            )
                        } else {
                            this
                        }
                    },
            )

            if (settings.isNotificationAccessGranted() && hasNotifications) {
                Box(
                    modifier = Modifier
                        .size((currentGridItemSettings.iconSize * 0.3).dp)
                        .align(Alignment.TopEnd)
                        .background(
                            color = MaterialTheme.colorScheme.primary,
                            shape = CircleShape,
                        ),
                )
            }
        }
        if (currentGridItemSettings.showLabel) {
            Text(
                modifier = Modifier.alpha(alpha),
                text = gridItem.customLabel ?: gridItem.label,
                color = currentTextColor,
                textAlign = TextAlign.Center,
                maxLines = maxLines,
                fontSize = currentGridItemSettings.textSize.sp,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
