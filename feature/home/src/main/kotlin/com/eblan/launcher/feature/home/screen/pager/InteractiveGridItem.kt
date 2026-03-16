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

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil3.compose.AsyncImage
import coil3.request.ImageRequest.Builder
import coil3.request.addLastModifiedToFileCacheKey
import com.eblan.launcher.designsystem.icon.EblanLauncherIcons
import com.eblan.launcher.domain.model.Associate
import com.eblan.launcher.domain.model.GridItem
import com.eblan.launcher.domain.model.GridItemData
import com.eblan.launcher.domain.model.GridItemSettings
import com.eblan.launcher.domain.model.TextColor
import com.eblan.launcher.feature.home.component.modifier.swipeGestures
import com.eblan.launcher.feature.home.component.modifier.whiteBox
import com.eblan.launcher.feature.home.model.Drag
import com.eblan.launcher.feature.home.model.GridItemSource
import com.eblan.launcher.feature.home.model.SharedElementKey
import com.eblan.launcher.feature.home.util.getGridItemTextColor
import com.eblan.launcher.feature.home.util.getHorizontalAlignment
import com.eblan.launcher.feature.home.util.getSystemTextColor
import com.eblan.launcher.feature.home.util.getVerticalArrangement
import com.eblan.launcher.feature.home.util.handleDrag
import com.eblan.launcher.feature.home.util.onDoubleTap
import com.eblan.launcher.feature.home.util.onLongPress
import com.eblan.launcher.ui.local.LocalAppWidgetHost
import com.eblan.launcher.ui.local.LocalAppWidgetManager
import com.eblan.launcher.ui.local.LocalLauncherApps
import com.eblan.launcher.ui.local.LocalSettings
import kotlinx.coroutines.launch

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun SharedTransitionScope.InteractiveGridItemContent(
    modifier: Modifier = Modifier,
    drag: Drag,
    gridItem: GridItem,
    gridItemSettings: GridItemSettings,
    gridItemSource: GridItemSource?,
    hasShortcutHostPermission: Boolean,
    iconPackFilePaths: Map<String, String>,
    isLongPress: Boolean,
    isScrollInProgress: Boolean,
    statusBarNotifications: Map<String, Int>,
    textColor: TextColor,
    isOpenFolder: Boolean,
    onDraggingGridItem: () -> Unit,
    onOpenAppDrawer: () -> Unit,
    onTapApplicationInfo: (
        serialNumber: Long,
        componentName: String,
    ) -> Unit,
    onTapFolderGridItem: () -> Unit,
    onTapShortcutConfig: (String) -> Unit,
    onTapShortcutInfo: (
        serialNumber: Long,
        packageName: String,
        shortcutId: String,
    ) -> Unit,
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
) {
    val isSelected = gridItemSource != null && gridItem.id == gridItemSource.gridItem.id

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

    when (val data = gridItem.data) {
        is GridItemData.ApplicationInfo -> {
            InteractiveApplicationInfoGridItem(
                modifier = modifier,
                data = data,
                drag = drag,
                gridItem = gridItem,
                gridItemSettings = currentGridItemSettings,
                iconPackFilePaths = iconPackFilePaths,
                isLongPress = isLongPress,
                isScrollInProgress = isScrollInProgress,
                isSelected = isSelected,
                statusBarNotifications = statusBarNotifications,
                textColor = currentTextColor,
                isOpenFolder = isOpenFolder,
                onDraggingGridItem = onDraggingGridItem,
                onOpenAppDrawer = onOpenAppDrawer,
                onTapApplicationInfo = onTapApplicationInfo,
                onUpdateGridItemSource = onUpdateGridItemSource,
                onUpdateImageBitmap = onUpdateImageBitmap,
                onUpdateIsDragging = onUpdateIsDragging,
                onUpdateIsLongPress = onUpdateIsLongPress,
                onUpdateOverlayBounds = onUpdateOverlayBounds,
                onUpdateSharedElementKey = onUpdateSharedElementKey,
                onShowGridItemPopup = onShowGridItemPopup,
                onDismissGridItemPopup = onDismissGridItemPopup,
                onUpdateAssociate = onUpdateAssociate,
            )
        }

        is GridItemData.Widget -> {
            InteractiveWidgetGridItem(
                modifier = modifier,
                data = data,
                drag = drag,
                gridItem = gridItem,
                isLongPress = isLongPress,
                isScrollInProgress = isScrollInProgress,
                isSelected = isSelected,
                textColor = currentTextColor,
                onDraggingGridItem = onDraggingGridItem,
                onUpdateGridItemSource = onUpdateGridItemSource,
                onUpdateImageBitmap = onUpdateImageBitmap,
                onUpdateIsDragging = onUpdateIsDragging,
                onUpdateIsLongPress = onUpdateIsLongPress,
                onUpdateOverlayBounds = onUpdateOverlayBounds,
                onUpdateSharedElementKey = onUpdateSharedElementKey,
                onShowGridItemPopup = onShowGridItemPopup,
                onDismissGridItemPopup = onDismissGridItemPopup,
                onUpdateAssociate = onUpdateAssociate,
            )
        }

        is GridItemData.ShortcutInfo -> {
            InteractiveShortcutInfoGridItem(
                modifier = modifier,
                data = data,
                drag = drag,
                gridItem = gridItem,
                gridItemSettings = currentGridItemSettings,
                hasShortcutHostPermission = hasShortcutHostPermission,
                isLongPress = isLongPress,
                isScrollInProgress = isScrollInProgress,
                isSelected = isSelected,
                textColor = currentTextColor,
                onDraggingGridItem = onDraggingGridItem,
                onOpenAppDrawer = onOpenAppDrawer,
                onTapShortcutInfo = onTapShortcutInfo,
                onUpdateGridItemSource = onUpdateGridItemSource,
                onUpdateImageBitmap = onUpdateImageBitmap,
                onUpdateIsDragging = onUpdateIsDragging,
                onUpdateIsLongPress = onUpdateIsLongPress,
                onUpdateOverlayBounds = onUpdateOverlayBounds,
                onUpdateSharedElementKey = onUpdateSharedElementKey,
                onShowGridItemPopup = onShowGridItemPopup,
                onDismissGridItemPopup = onDismissGridItemPopup,
                onUpdateAssociate = onUpdateAssociate,
            )
        }

        is GridItemData.Folder -> {
            InteractiveFolderGridItem(
                modifier = modifier,
                data = data,
                drag = drag,
                gridItem = gridItem,
                gridItemSettings = currentGridItemSettings,
                iconPackFilePaths = iconPackFilePaths,
                isLongPress = isLongPress,
                isScrollInProgress = isScrollInProgress,
                isSelected = isSelected,
                textColor = currentTextColor,
                onDraggingGridItem = onDraggingGridItem,
                onOpenAppDrawer = onOpenAppDrawer,
                onTap = onTapFolderGridItem,
                onUpdateGridItemSource = onUpdateGridItemSource,
                onUpdateImageBitmap = onUpdateImageBitmap,
                onUpdateIsDragging = onUpdateIsDragging,
                onUpdateIsLongPress = onUpdateIsLongPress,
                onUpdateOverlayBounds = onUpdateOverlayBounds,
                onUpdateSharedElementKey = onUpdateSharedElementKey,
                onShowGridItemPopup = onShowGridItemPopup,
                onDismissGridItemPopup = onDismissGridItemPopup,
                onUpdateAssociate = onUpdateAssociate,
            )
        }

        is GridItemData.ShortcutConfig -> {
            InteractiveShortcutConfigGridItem(
                modifier = modifier,
                data = data,
                drag = drag,
                gridItem = gridItem,
                gridItemSettings = currentGridItemSettings,
                isLongPress = isLongPress,
                isScrollInProgress = isScrollInProgress,
                isSelected = isSelected,
                textColor = currentTextColor,
                onDraggingGridItem = onDraggingGridItem,
                onOpenAppDrawer = onOpenAppDrawer,
                onTapShortcutConfig = onTapShortcutConfig,
                onUpdateGridItemSource = onUpdateGridItemSource,
                onUpdateImageBitmap = onUpdateImageBitmap,
                onUpdateIsDragging = onUpdateIsDragging,
                onUpdateIsLongPress = onUpdateIsLongPress,
                onUpdateOverlayBounds = onUpdateOverlayBounds,
                onUpdateSharedElementKey = onUpdateSharedElementKey,
                onShowGridItemPopup = onShowGridItemPopup,
                onDismissGridItemPopup = onDismissGridItemPopup,
                onUpdateAssociate = onUpdateAssociate,
            )
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun SharedTransitionScope.InteractiveApplicationInfoGridItem(
    modifier: Modifier = Modifier,
    data: GridItemData.ApplicationInfo,
    drag: Drag,
    gridItem: GridItem,
    gridItemSettings: GridItemSettings,
    iconPackFilePaths: Map<String, String>,
    isLongPress: Boolean,
    isScrollInProgress: Boolean,
    isSelected: Boolean,
    statusBarNotifications: Map<String, Int>,
    textColor: Color,
    isOpenFolder: Boolean,
    onDraggingGridItem: () -> Unit,
    onOpenAppDrawer: () -> Unit,
    onTapApplicationInfo: (
        serialNumber: Long,
        componentName: String,
    ) -> Unit,
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
) {
    val launcherApps = LocalLauncherApps.current

    val context = LocalContext.current

    val settings = LocalSettings.current

    var intOffset by remember { mutableStateOf(IntOffset.Zero) }

    var intSize by remember { mutableStateOf(IntSize.Zero) }

    val graphicsLayer = rememberGraphicsLayer()

    val scope = rememberCoroutineScope()

    val horizontalAlignment =
        getHorizontalAlignment(horizontalAlignment = gridItemSettings.horizontalAlignment)

    val verticalArrangement =
        getVerticalArrangement(verticalArrangement = gridItemSettings.verticalArrangement)

    val maxLines = if (gridItemSettings.singleLineLabel) 1 else Int.MAX_VALUE

    val icon = iconPackFilePaths[data.componentName] ?: data.icon

    val hasNotifications =
        statusBarNotifications[data.packageName] != null && (
                statusBarNotifications[data.packageName]
                    ?: 0
                ) > 0

    val hasInteraction = isSelected && isLongPress && (drag == Drag.Start || drag == Drag.Dragging)

    val isVisibleWhiteBox = isSelected && drag == Drag.Dragging

    val isInsideOpenFolder = isSelected && isOpenFolder

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
                                gridItemSource = GridItemSource.Existing(gridItem = gridItem),
                                sharedElementKey = SharedElementKey(
                                    id = gridItem.id,
                                    parent = SharedElementKey.Parent.Grid,
                                ),
                                onUpdateGridItemSource = onUpdateGridItemSource,
                                onUpdateImageBitmap = onUpdateImageBitmap,
                                onUpdateIsLongPress = onUpdateIsLongPress,
                                onUpdateOverlayBounds = onUpdateOverlayBounds,
                                onUpdateSharedElementKey = onUpdateSharedElementKey,
                                onShowGridItemPopup = onShowGridItemPopup,
                                onUpdateAssociate = onUpdateAssociate,
                            )
                        }
                    } else {
                        null
                    },
                    onTap = if (!isLongPress) {
                        {
                            scope.launch {
                                onTapApplicationInfo(
                                    data.serialNumber,
                                    data.componentName,
                                )
                            }
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
            .padding(gridItemSettings.padding.dp)
            .background(
                color = Color(gridItemSettings.customBackgroundColor),
                shape = RoundedCornerShape(size = gridItemSettings.cornerRadius.dp),
            )
            .whiteBox(textColor = textColor, visible = isVisibleWhiteBox),
        horizontalAlignment = horizontalAlignment,
        verticalArrangement = verticalArrangement,
    ) {
        if (!hasInteraction && !isInsideOpenFolder) {
            Box(modifier = Modifier.size(gridItemSettings.iconSize.dp)) {
                AsyncImage(
                    model = Builder(LocalContext.current).data(data.customIcon ?: icon)
                        .addLastModifiedToFileCacheKey(true).build(),
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
                        .sharedElementWithCallerManagedVisibility(
                            rememberSharedContentState(
                                key = SharedElementKey(
                                    id = gridItem.id,
                                    parent = SharedElementKey.Parent.Grid,
                                ),
                            ),
                            visible = !isScrollInProgress,
                        ),
                )

                if (settings.isNotificationAccessGranted() && hasNotifications) {
                    Box(
                        modifier = Modifier
                            .size((gridItemSettings.iconSize * 0.3).dp)
                            .align(Alignment.TopEnd)
                            .background(
                                color = MaterialTheme.colorScheme.primary,
                                shape = CircleShape,
                            ),
                    )
                }

                if (data.serialNumber != 0L) {
                    ElevatedCard(
                        modifier = Modifier
                            .size((gridItemSettings.iconSize * 0.4).dp)
                            .align(Alignment.BottomEnd),
                    ) {
                        Icon(
                            imageVector = EblanLauncherIcons.Work,
                            contentDescription = null,
                            modifier = Modifier.padding(2.dp),
                        )
                    }
                }
            }

            if (gridItemSettings.showLabel) {
                Text(
                    text = data.customLabel ?: data.label,
                    color = textColor,
                    textAlign = TextAlign.Center,
                    maxLines = maxLines,
                    fontSize = gridItemSettings.textSize.sp,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun SharedTransitionScope.InteractiveWidgetGridItem(
    modifier: Modifier = Modifier,
    data: GridItemData.Widget,
    drag: Drag,
    gridItem: GridItem,
    isLongPress: Boolean,
    isScrollInProgress: Boolean,
    isSelected: Boolean,
    textColor: Color,
    onDraggingGridItem: () -> Unit,
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
) {
    var intOffset by remember { mutableStateOf(IntOffset.Zero) }

    var intSize by remember { mutableStateOf(IntSize.Zero) }

    val appWidgetHost = LocalAppWidgetHost.current

    val appWidgetManager = LocalAppWidgetManager.current

    val appWidgetInfo = appWidgetManager.getAppWidgetInfo(appWidgetId = data.appWidgetId)

    val graphicsLayer = rememberGraphicsLayer()

    val scope = rememberCoroutineScope()

    val hasInteraction = isSelected && isLongPress && (drag == Drag.Start || drag == Drag.Dragging)

    val isVisibleWhiteBox = isSelected && drag == Drag.Dragging

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

    Box(
        modifier = modifier
            .fillMaxSize()
            .whiteBox(textColor = textColor, visible = isVisibleWhiteBox),
    ) {
        if (!hasInteraction) {
            val commonModifier = Modifier
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
                .sharedElementWithCallerManagedVisibility(
                    rememberSharedContentState(
                        key = SharedElementKey(
                            id = gridItem.id,
                            parent = SharedElementKey.Parent.Grid,
                        ),
                    ),
                    visible = !isScrollInProgress,
                )

            if (appWidgetInfo != null) {
                AndroidView(
                    factory = {
                        appWidgetHost.createView(
                            appWidgetId = data.appWidgetId,
                            appWidgetProviderInfo = appWidgetInfo,
                        )
                    },
                    modifier = commonModifier,
                    update = { view ->
                        if (!isLongPress) {
                            view.setOnLongClickListener {
                                onLongPress(
                                    scope = scope,
                                    graphicsLayer = graphicsLayer,
                                    intOffset = intOffset,
                                    intSize = intSize,
                                    gridItemSource = GridItemSource.Existing(gridItem = gridItem),
                                    sharedElementKey = SharedElementKey(
                                        id = gridItem.id,
                                        parent = SharedElementKey.Parent.Grid,
                                    ),
                                    onUpdateGridItemSource = onUpdateGridItemSource,
                                    onUpdateImageBitmap = onUpdateImageBitmap,
                                    onUpdateIsLongPress = onUpdateIsLongPress,
                                    onUpdateOverlayBounds = onUpdateOverlayBounds,
                                    onUpdateSharedElementKey = onUpdateSharedElementKey,
                                    onShowGridItemPopup = onShowGridItemPopup,
                                    onUpdateAssociate = onUpdateAssociate,
                                )

                                true
                            }
                        }
                    },
                )
            } else {
                AsyncImage(
                    model = data.preview ?: data.icon,
                    contentDescription = null,
                    modifier = commonModifier.pointerInput(key1 = drag) {
                        detectTapGestures(
                            onLongPress = if (!isLongPress) {
                                {
                                    onLongPress(
                                        scope = scope,
                                        graphicsLayer = graphicsLayer,
                                        intOffset = intOffset,
                                        intSize = intSize,
                                        gridItemSource = GridItemSource.Existing(gridItem = gridItem),
                                        sharedElementKey = SharedElementKey(
                                            id = gridItem.id,
                                            parent = SharedElementKey.Parent.Grid,
                                        ),
                                        onUpdateGridItemSource = onUpdateGridItemSource,
                                        onUpdateImageBitmap = onUpdateImageBitmap,
                                        onUpdateIsLongPress = onUpdateIsLongPress,
                                        onUpdateOverlayBounds = onUpdateOverlayBounds,
                                        onUpdateSharedElementKey = onUpdateSharedElementKey,
                                        onShowGridItemPopup = onShowGridItemPopup,
                                        onUpdateAssociate = onUpdateAssociate,
                                    )
                                }
                            } else {
                                null
                            },
                        )
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun SharedTransitionScope.InteractiveShortcutInfoGridItem(
    modifier: Modifier = Modifier,
    data: GridItemData.ShortcutInfo,
    drag: Drag,
    gridItem: GridItem,
    gridItemSettings: GridItemSettings,
    hasShortcutHostPermission: Boolean,
    isLongPress: Boolean,
    isScrollInProgress: Boolean,
    isSelected: Boolean,
    textColor: Color,
    onDraggingGridItem: () -> Unit,
    onOpenAppDrawer: () -> Unit,
    onTapShortcutInfo: (
        serialNumber: Long,
        packageName: String,
        shortcutId: String,
    ) -> Unit,
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
) {
    val launcherApps = LocalLauncherApps.current

    val context = LocalContext.current

    var intOffset by remember { mutableStateOf(IntOffset.Zero) }

    var intSize by remember { mutableStateOf(IntSize.Zero) }

    val graphicsLayer = rememberGraphicsLayer()

    val scope = rememberCoroutineScope()

    val horizontalAlignment =
        getHorizontalAlignment(horizontalAlignment = gridItemSettings.horizontalAlignment)

    val verticalArrangement =
        getVerticalArrangement(verticalArrangement = gridItemSettings.verticalArrangement)

    val maxLines = if (gridItemSettings.singleLineLabel) 1 else Int.MAX_VALUE

    val customIcon = data.customIcon ?: data.icon

    val customShortLabel = data.customShortLabel ?: data.shortLabel

    val alpha = if (hasShortcutHostPermission && data.isEnabled) 1f else 0.3f

    val hasInteraction = isSelected && isLongPress && (drag == Drag.Start || drag == Drag.Dragging)

    val isVisibleWhiteBox = isSelected && drag == Drag.Dragging

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
                                gridItemSource = GridItemSource.Existing(gridItem = gridItem),
                                sharedElementKey = SharedElementKey(
                                    id = gridItem.id,
                                    parent = SharedElementKey.Parent.Grid,
                                ),
                                onUpdateGridItemSource = onUpdateGridItemSource,
                                onUpdateImageBitmap = onUpdateImageBitmap,
                                onUpdateIsLongPress = onUpdateIsLongPress,
                                onUpdateOverlayBounds = onUpdateOverlayBounds,
                                onUpdateSharedElementKey = onUpdateSharedElementKey,
                                onShowGridItemPopup = onShowGridItemPopup,
                                onUpdateAssociate = onUpdateAssociate,
                            )
                        }
                    } else {
                        null
                    },
                    onTap = if (!isLongPress) {
                        {
                            if (hasShortcutHostPermission && data.isEnabled) {
                                scope.launch {
                                    onTapShortcutInfo(
                                        data.serialNumber,
                                        data.packageName,
                                        data.shortcutId,
                                    )
                                }
                            }
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
            .padding(gridItemSettings.padding.dp)
            .background(
                color = Color(gridItemSettings.customBackgroundColor),
                shape = RoundedCornerShape(size = gridItemSettings.cornerRadius.dp),
            )
            .whiteBox(textColor = textColor, visible = isVisibleWhiteBox),
        horizontalAlignment = horizontalAlignment,
        verticalArrangement = verticalArrangement,
    ) {
        if (!hasInteraction) {
            Box(modifier = Modifier.size(gridItemSettings.iconSize.dp)) {
                AsyncImage(
                    model = customIcon,
                    modifier = Modifier
                        .matchParentSize()
                        .alpha(alpha)
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
                        .sharedElementWithCallerManagedVisibility(
                            rememberSharedContentState(
                                key = SharedElementKey(
                                    id = gridItem.id,
                                    parent = SharedElementKey.Parent.Grid,
                                ),
                            ),
                            visible = !isScrollInProgress,
                        ),
                    contentDescription = null,
                )

                AsyncImage(
                    model = data.eblanApplicationInfoIcon,
                    modifier = Modifier
                        .size((gridItemSettings.iconSize * 0.25).dp)
                        .align(Alignment.BottomEnd)
                        .alpha(alpha),
                    contentDescription = null,
                )
            }

            if (gridItemSettings.showLabel) {
                Text(
                    modifier = Modifier.alpha(alpha),
                    text = customShortLabel,
                    color = textColor,
                    textAlign = TextAlign.Center,
                    maxLines = maxLines,
                    fontSize = gridItemSettings.textSize.sp,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun SharedTransitionScope.InteractiveFolderGridItem(
    modifier: Modifier = Modifier,
    data: GridItemData.Folder,
    drag: Drag,
    gridItem: GridItem,
    gridItemSettings: GridItemSettings,
    iconPackFilePaths: Map<String, String>,
    isLongPress: Boolean,
    isScrollInProgress: Boolean,
    isSelected: Boolean,
    textColor: Color,
    onDraggingGridItem: () -> Unit,
    onOpenAppDrawer: () -> Unit,
    onTap: () -> Unit,
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
) {
    val launcherApps = LocalLauncherApps.current

    val context = LocalContext.current

    var intOffset by remember { mutableStateOf(IntOffset.Zero) }

    var intSize by remember { mutableStateOf(IntSize.Zero) }

    val graphicsLayer = rememberGraphicsLayer()

    val scope = rememberCoroutineScope()

    val horizontalAlignment =
        getHorizontalAlignment(horizontalAlignment = gridItemSettings.horizontalAlignment)

    val verticalArrangement =
        getVerticalArrangement(verticalArrangement = gridItemSettings.verticalArrangement)

    val maxLines = if (gridItemSettings.singleLineLabel) 1 else Int.MAX_VALUE

    val hasInteraction = isSelected && isLongPress && (drag == Drag.Start || drag == Drag.Dragging)

    val isVisibleWhiteBox = isSelected && drag == Drag.Dragging

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
                                gridItemSource = GridItemSource.Existing(gridItem = gridItem),
                                sharedElementKey = SharedElementKey(
                                    id = gridItem.id,
                                    parent = SharedElementKey.Parent.Grid,
                                ),
                                onUpdateGridItemSource = onUpdateGridItemSource,
                                onUpdateImageBitmap = onUpdateImageBitmap,
                                onUpdateIsLongPress = onUpdateIsLongPress,
                                onUpdateOverlayBounds = onUpdateOverlayBounds,
                                onUpdateSharedElementKey = onUpdateSharedElementKey,
                                onShowGridItemPopup = onShowGridItemPopup,
                                onUpdateAssociate = onUpdateAssociate,
                            )
                        }
                    } else {
                        null
                    },
                    onTap = if (!isLongPress) {
                        {
                            onTap()
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
            .padding(gridItemSettings.padding.dp)
            .background(
                color = Color(gridItemSettings.customBackgroundColor),
                shape = RoundedCornerShape(size = gridItemSettings.cornerRadius.dp),
            )
            .whiteBox(textColor = textColor, visible = isVisibleWhiteBox),
        horizontalAlignment = horizontalAlignment,
        verticalArrangement = verticalArrangement,
    ) {
        if (!hasInteraction) {
            val commonModifier = Modifier
                .size(gridItemSettings.iconSize.dp)
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
                .sharedElementWithCallerManagedVisibility(
                    rememberSharedContentState(
                        key = SharedElementKey(
                            id = gridItem.id,
                            parent = SharedElementKey.Parent.Grid,
                        ),
                    ),
                    visible = !isScrollInProgress,
                )

            if (data.icon != null) {
                AsyncImage(
                    model = data.icon,
                    contentDescription = null,
                    modifier = commonModifier,
                )
            } else {
                Box(
                    modifier = commonModifier.background(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(5.dp),
                    ),
                ) {
                    FlowRow(
                        modifier = Modifier.matchParentSize(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalArrangement = Arrangement.SpaceEvenly,
                        maxItemsInEachRow = 3,
                        maxLines = 3,
                    ) {
                        data.previewGridItemsByPage.forEach { applicationInfoFolderGridItem ->
                            key(applicationInfoFolderGridItem.id) {
                                val icon =
                                    iconPackFilePaths[applicationInfoFolderGridItem.componentName]
                                        ?: applicationInfoFolderGridItem.icon

                                AsyncImage(
                                    model = Builder(LocalContext.current)
                                        .data(applicationInfoFolderGridItem.customIcon ?: icon)
                                        .addLastModifiedToFileCacheKey(true).build(),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size((gridItemSettings.iconSize * 0.25).dp)
                                        .sharedElementWithCallerManagedVisibility(
                                            rememberSharedContentState(
                                                key = SharedElementKey(
                                                    id = applicationInfoFolderGridItem.id,
                                                    parent = SharedElementKey.Parent.Grid,
                                                ),
                                            ),
                                            visible = !isScrollInProgress,
                                        ),
                                )
                            }
                        }
                    }
                }
            }

            if (gridItemSettings.showLabel) {
                Text(
                    text = data.label,
                    color = textColor,
                    textAlign = TextAlign.Center,
                    maxLines = maxLines,
                    fontSize = gridItemSettings.textSize.sp,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun SharedTransitionScope.InteractiveShortcutConfigGridItem(
    modifier: Modifier = Modifier,
    data: GridItemData.ShortcutConfig,
    drag: Drag,
    gridItem: GridItem,
    gridItemSettings: GridItemSettings,
    isLongPress: Boolean,
    isScrollInProgress: Boolean,
    isSelected: Boolean,
    textColor: Color,
    onDraggingGridItem: () -> Unit,
    onOpenAppDrawer: () -> Unit,
    onTapShortcutConfig: (String) -> Unit,
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
) {
    val launcherApps = LocalLauncherApps.current

    val context = LocalContext.current

    var intOffset by remember { mutableStateOf(IntOffset.Zero) }

    var intSize by remember { mutableStateOf(IntSize.Zero) }

    val graphicsLayer = rememberGraphicsLayer()

    val scope = rememberCoroutineScope()

    val horizontalAlignment =
        getHorizontalAlignment(horizontalAlignment = gridItemSettings.horizontalAlignment)

    val verticalArrangement =
        getVerticalArrangement(verticalArrangement = gridItemSettings.verticalArrangement)

    val maxLines = if (gridItemSettings.singleLineLabel) 1 else Int.MAX_VALUE

    val icon = when {
        data.customIcon != null -> {
            data.customIcon
        }

        data.shortcutIntentIcon != null -> {
            data.shortcutIntentIcon
        }

        data.activityIcon != null -> {
            data.activityIcon
        }

        else -> {
            data.applicationIcon
        }
    }

    val label = when {
        data.customLabel != null -> {
            data.customLabel
        }

        data.shortcutIntentName != null -> {
            data.shortcutIntentName
        }

        data.activityLabel != null -> {
            data.activityLabel
        }

        else -> {
            data.applicationLabel
        }
    }

    val hasInteraction = isSelected && isLongPress && (drag == Drag.Start || drag == Drag.Dragging)

    val isVisibleWhiteBox = isSelected && drag == Drag.Dragging

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
                                gridItemSource = GridItemSource.Existing(gridItem = gridItem),
                                sharedElementKey = SharedElementKey(
                                    id = gridItem.id,
                                    parent = SharedElementKey.Parent.Grid,
                                ),
                                onUpdateGridItemSource = onUpdateGridItemSource,
                                onUpdateImageBitmap = onUpdateImageBitmap,
                                onUpdateIsLongPress = onUpdateIsLongPress,
                                onUpdateOverlayBounds = onUpdateOverlayBounds,
                                onUpdateSharedElementKey = onUpdateSharedElementKey,
                                onShowGridItemPopup = onShowGridItemPopup,
                                onUpdateAssociate = onUpdateAssociate,
                            )
                        }
                    } else {
                        null
                    },
                    onTap = if (!isLongPress) {
                        {
                            data.shortcutIntentUri?.let(onTapShortcutConfig)
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
            .padding(gridItemSettings.padding.dp)
            .background(
                color = Color(gridItemSettings.customBackgroundColor),
                shape = RoundedCornerShape(size = gridItemSettings.cornerRadius.dp),
            )
            .whiteBox(textColor = textColor, visible = isVisibleWhiteBox),
        horizontalAlignment = horizontalAlignment,
        verticalArrangement = verticalArrangement,
    ) {
        if (!hasInteraction) {
            Box(modifier = Modifier.size(gridItemSettings.iconSize.dp)) {
                AsyncImage(
                    model = Builder(LocalContext.current).data(icon)
                        .addLastModifiedToFileCacheKey(true).build(),
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
                        .sharedElementWithCallerManagedVisibility(
                            rememberSharedContentState(
                                key = SharedElementKey(
                                    id = gridItem.id,
                                    parent = SharedElementKey.Parent.Grid,
                                ),
                            ),
                            visible = !isScrollInProgress,
                        ),
                )

                if (data.serialNumber != 0L) {
                    ElevatedCard(
                        modifier = Modifier
                            .size((gridItemSettings.iconSize * 0.4).dp)
                            .align(Alignment.BottomEnd),
                    ) {
                        Icon(
                            imageVector = EblanLauncherIcons.Work,
                            contentDescription = null,
                            modifier = Modifier.padding(2.dp),
                        )
                    }
                }
            }

            if (gridItemSettings.showLabel) {
                Text(
                    text = label.toString(),
                    color = textColor,
                    textAlign = TextAlign.Center,
                    maxLines = maxLines,
                    fontSize = gridItemSettings.textSize.sp,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
