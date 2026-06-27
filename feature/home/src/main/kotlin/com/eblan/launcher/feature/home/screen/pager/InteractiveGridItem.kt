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
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
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
import coil3.size.Size
import com.eblan.launcher.designsystem.icon.EblanLauncherIcons
import com.eblan.launcher.domain.model.FolderPopupEntry
import com.eblan.launcher.domain.model.GridItem
import com.eblan.launcher.domain.model.GridItemData
import com.eblan.launcher.domain.model.GridItemSettings
import com.eblan.launcher.domain.model.MoveGridItemResult
import com.eblan.launcher.domain.model.TextColor
import com.eblan.launcher.feature.home.component.PreviewFolderGridLayout
import com.eblan.launcher.feature.home.component.swipeGestures
import com.eblan.launcher.feature.home.component.whiteBox
import com.eblan.launcher.feature.home.model.Drag
import com.eblan.launcher.feature.home.model.GridItemSource
import com.eblan.launcher.feature.home.model.SharedElementKey
import com.eblan.launcher.feature.home.util.FOLDER_PREVIEW_COLUMNS
import com.eblan.launcher.feature.home.util.FOLDER_PREVIEW_ROWS
import com.eblan.launcher.feature.home.util.getGridItemTextColor
import com.eblan.launcher.feature.home.util.getHorizontalAlignment
import com.eblan.launcher.feature.home.util.getSystemTextColor
import com.eblan.launcher.feature.home.util.getVerticalArrangement
import com.eblan.launcher.feature.home.util.onDoubleTap
import com.eblan.launcher.feature.home.util.onPress
import com.eblan.launcher.ui.local.LocalAppWidgetHost
import com.eblan.launcher.ui.local.LocalAppWidgetManager
import com.eblan.launcher.ui.local.LocalLauncherApps
import com.eblan.launcher.ui.local.LocalSettings
import kotlinx.coroutines.launch

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun InteractiveGridItem(
    modifier: Modifier = Modifier,
    sharedTransitionScope: SharedTransitionScope,
    drag: Drag,
    gridItem: GridItem,
    gridItemSettings: GridItemSettings,
    hasShortcutHostPermission: Boolean,
    isScrollInProgress: Boolean,
    statusBarNotifications: Map<String, Int>,
    textColor: TextColor,
    isVisibleOverlay: Boolean,
    isVisibleFolder: Boolean,
    sharedElementKey: SharedElementKey,
    moveGridItemResult: MoveGridItemResult?,
    lockMovement: Boolean,
    isDragging: Boolean,
    showGridItemPopup: Boolean,
    onOpenAppDrawer: () -> Unit,
    onTapApplicationInfo: (
        serialNumber: Long,
        componentName: String,
    ) -> Unit,
    onUpsertFolderPopupEntry: (FolderPopupEntry) -> Unit,
    onTapShortcutConfig: (String) -> Unit,
    onTapShortcutInfo: (
        serialNumber: Long,
        packageName: String,
        shortcutId: String,
    ) -> Unit,
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
    onUpdateIsCloseGridItemPopup: (Boolean) -> Unit,
    onUpdateIsVisibleOverlay: (Boolean) -> Unit,
    onUpdateMoveGridItemResult: (MoveGridItemResult) -> Unit,
    onShowFolderWhenDragging: (
        folderPopupEntry: FolderPopupEntry,
        movingGridItem: GridItem,
    ) -> Unit,
) {
    val isSelected =
        moveGridItemResult != null && moveGridItemResult.movingGridItem.id == gridItem.id

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
                sharedTransitionScope = sharedTransitionScope,
                data = data,
                drag = drag,
                gridItem = gridItem,
                gridItemSettings = currentGridItemSettings,
                isScrollInProgress = isScrollInProgress,
                isSelected = isSelected,
                isVisibleFolder = isVisibleFolder,
                isVisibleOverlay = isVisibleOverlay,
                sharedElementKey = sharedElementKey,
                statusBarNotifications = statusBarNotifications,
                textColor = currentTextColor,
                showGridItemPopup = showGridItemPopup,
                onUpdateIsCloseGridItemPopup = onUpdateIsCloseGridItemPopup,
                onOpenAppDrawer = onOpenAppDrawer,
                onShowGridItemPopup = onShowGridItemPopup,
                onTapApplicationInfo = onTapApplicationInfo,
                onUpdateGridItemSource = onUpdateGridItemSource,
                onUpdateImageBitmap = onUpdateImageBitmap,
                onUpdateIsDragging = onUpdateIsDragging,
                onUpdateIsVisibleOverlay = onUpdateIsVisibleOverlay,
                onUpdateOverlayBounds = onUpdateOverlayBounds,
                onUpdateSharedElementKey = onUpdateSharedElementKey,
                onUpdateMoveGridItemResult = onUpdateMoveGridItemResult,
            )
        }

        is GridItemData.Widget -> {
            InteractiveWidgetGridItem(
                modifier = modifier,
                sharedTransitionScope = sharedTransitionScope,
                data = data,
                drag = drag,
                isScrollInProgress = isScrollInProgress,
                isSelected = isSelected,
                isVisibleOverlay = isVisibleOverlay,
                sharedElementKey = sharedElementKey,
                textColor = currentTextColor,
                gridItem = gridItem,
                showGridItemPopup = showGridItemPopup,
                onUpdateIsCloseGridItemPopup = onUpdateIsCloseGridItemPopup,
                onShowGridItemPopup = onShowGridItemPopup,
                onUpdateGridItemSource = onUpdateGridItemSource,
                onUpdateImageBitmap = onUpdateImageBitmap,
                onUpdateIsDragging = onUpdateIsDragging,
                onUpdateIsVisibleOverlay = onUpdateIsVisibleOverlay,
                onUpdateOverlayBounds = onUpdateOverlayBounds,
                onUpdateSharedElementKey = onUpdateSharedElementKey,
                onUpdateMoveGridItemResult = onUpdateMoveGridItemResult,
            )
        }

        is GridItemData.ShortcutInfo -> {
            InteractiveShortcutInfoGridItem(
                modifier = modifier,
                sharedTransitionScope = sharedTransitionScope,
                data = data,
                drag = drag,
                gridItem = gridItem,
                gridItemSettings = currentGridItemSettings,
                hasShortcutHostPermission = hasShortcutHostPermission,
                isScrollInProgress = isScrollInProgress,
                isSelected = isSelected,
                isVisibleFolder = isVisibleFolder,
                isVisibleOverlay = isVisibleOverlay,
                sharedElementKey = sharedElementKey,
                textColor = currentTextColor,
                showGridItemPopup = showGridItemPopup,
                onUpdateIsCloseGridItemPopup = onUpdateIsCloseGridItemPopup,
                onOpenAppDrawer = onOpenAppDrawer,
                onShowGridItemPopup = onShowGridItemPopup,
                onTapShortcutInfo = onTapShortcutInfo,
                onUpdateGridItemSource = onUpdateGridItemSource,
                onUpdateImageBitmap = onUpdateImageBitmap,
                onUpdateIsDragging = onUpdateIsDragging,
                onUpdateIsVisibleOverlay = onUpdateIsVisibleOverlay,
                onUpdateOverlayBounds = onUpdateOverlayBounds,
                onUpdateSharedElementKey = onUpdateSharedElementKey,
                onUpdateMoveGridItemResult = onUpdateMoveGridItemResult,
            )
        }

        is GridItemData.Folder -> {
            InteractiveFolderGridItem(
                modifier = modifier,
                sharedTransitionScope = sharedTransitionScope,
                data = data,
                drag = drag,
                gridItem = gridItem,
                gridItemSettings = currentGridItemSettings,
                isScrollInProgress = isScrollInProgress,
                isSelected = isSelected,
                isVisibleFolder = isVisibleFolder,
                isVisibleOverlay = isVisibleOverlay,
                sharedElementKey = sharedElementKey,
                textColor = currentTextColor,
                moveGridItemResult = moveGridItemResult,
                lockMovement = lockMovement,
                isDragging = isDragging,
                showGridItemPopup = showGridItemPopup,
                onUpdateIsCloseGridItemPopup = onUpdateIsCloseGridItemPopup,
                onOpenAppDrawer = onOpenAppDrawer,
                onShowGridItemPopup = onShowGridItemPopup,
                onUpsertFolderPopupEntry = onUpsertFolderPopupEntry,
                onUpdateGridItemSource = onUpdateGridItemSource,
                onUpdateImageBitmap = onUpdateImageBitmap,
                onUpdateIsDragging = onUpdateIsDragging,
                onUpdateIsVisibleOverlay = onUpdateIsVisibleOverlay,
                onUpdateOverlayBounds = onUpdateOverlayBounds,
                onUpdateSharedElementKey = onUpdateSharedElementKey,
                onUpdateMoveGridItemResult = onUpdateMoveGridItemResult,
                onShowFolderWhenDragging = onShowFolderWhenDragging,
            )
        }

        is GridItemData.ShortcutConfig -> {
            InteractiveShortcutConfigGridItem(
                modifier = modifier,
                sharedTransitionScope = sharedTransitionScope,
                data = data,
                drag = drag,
                gridItem = gridItem,
                gridItemSettings = currentGridItemSettings,
                isScrollInProgress = isScrollInProgress,
                isSelected = isSelected,
                isVisibleFolder = isVisibleFolder,
                isVisibleOverlay = isVisibleOverlay,
                sharedElementKey = sharedElementKey,
                textColor = currentTextColor,
                showGridItemPopup = showGridItemPopup,
                onUpdateIsCloseGridItemPopup = onUpdateIsCloseGridItemPopup,
                onOpenAppDrawer = onOpenAppDrawer,
                onShowGridItemPopup = onShowGridItemPopup,
                onTapShortcutConfig = onTapShortcutConfig,
                onUpdateGridItemSource = onUpdateGridItemSource,
                onUpdateImageBitmap = onUpdateImageBitmap,
                onUpdateIsDragging = onUpdateIsDragging,
                onUpdateIsVisibleOverlay = onUpdateIsVisibleOverlay,
                onUpdateOverlayBounds = onUpdateOverlayBounds,
                onUpdateSharedElementKey = onUpdateSharedElementKey,
                onUpdateMoveGridItemResult = onUpdateMoveGridItemResult,
            )
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun InteractiveApplicationInfoGridItem(
    modifier: Modifier = Modifier,
    sharedTransitionScope: SharedTransitionScope,
    data: GridItemData.ApplicationInfo,
    drag: Drag,
    gridItem: GridItem,
    gridItemSettings: GridItemSettings,
    isScrollInProgress: Boolean,
    isSelected: Boolean,
    isVisibleFolder: Boolean,
    isVisibleOverlay: Boolean,
    sharedElementKey: SharedElementKey,
    statusBarNotifications: Map<String, Int>,
    textColor: Color,
    showGridItemPopup: Boolean,
    onUpdateIsCloseGridItemPopup: (Boolean) -> Unit,
    onOpenAppDrawer: () -> Unit,
    onShowGridItemPopup: (
        intOffset: IntOffset,
        intSize: IntSize,
    ) -> Unit,
    onTapApplicationInfo: (
        serialNumber: Long,
        componentName: String,
    ) -> Unit,
    onUpdateGridItemSource: (GridItemSource) -> Unit,
    onUpdateImageBitmap: (ImageBitmap) -> Unit,
    onUpdateIsDragging: (Boolean) -> Unit,
    onUpdateIsVisibleOverlay: (Boolean) -> Unit,
    onUpdateOverlayBounds: (
        intOffset: IntOffset,
        intSize: IntSize,
    ) -> Unit,
    onUpdateSharedElementKey: (SharedElementKey?) -> Unit,
    onUpdateMoveGridItemResult: (MoveGridItemResult) -> Unit,
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

    val icon = data.iconPackInfoFilePath ?: data.icon

    val hasNotifications =
        statusBarNotifications[data.packageName] != null && (
            statusBarNotifications[data.packageName]
                ?: 0
            ) > 0

    val hasInteraction = isSelected && isVisibleOverlay

    val isVisibleWhiteBox = hasInteraction && drag == Drag.Dragging

    val alpha = if (hasInteraction) 0f else 1f

    val scale = remember { Animatable(1f) }

    LaunchedEffect(
        key1 = drag,
        key2 = hasInteraction,
        key3 = showGridItemPopup,
    ) {
        if (drag == Drag.Dragging &&
            hasInteraction &&
            showGridItemPopup
        ) {
            onUpdateIsDragging(true)

            onUpdateIsCloseGridItemPopup(true)
        }
    }

    Column(
        modifier = modifier
            .pointerInput(key1 = isVisibleOverlay) {
                detectTapGestures(
                    onDoubleTap = if (!isVisibleOverlay) {
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
                    onLongPress = if (!isVisibleOverlay) {
                        {
                            scope.launch {
                                onLongPress(
                                    graphicsLayer = graphicsLayer,
                                    intOffset = intOffset,
                                    intSize = intSize,
                                    sharedElementKey = sharedElementKey,
                                    gridItem = gridItem,
                                    onUpdateGridItemSource = onUpdateGridItemSource,
                                    onUpdateImageBitmap = onUpdateImageBitmap,
                                    onUpdateOverlayBounds = onUpdateOverlayBounds,
                                    onUpdateSharedElementKey = onUpdateSharedElementKey,
                                    onShowGridItemPopup = onShowGridItemPopup,
                                    onUpdateIsVisibleOverlay = onUpdateIsVisibleOverlay,
                                    onUpdateMoveGridItemResult = onUpdateMoveGridItemResult,
                                )
                            }
                        }
                    } else {
                        null
                    },
                    onTap = if (!isVisibleOverlay) {
                        {
                            scope.launch {
                                scale.animateTo(0.8f)

                                scale.animateTo(1f)

                                onTapApplicationInfo(
                                    data.serialNumber,
                                    data.componentName,
                                )
                            }
                        }
                    } else {
                        null
                    },
                    onPress = {
                        onPress(
                            isVisibleOverlay = isVisibleOverlay,
                            scale = scale,
                        )
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
            .whiteBox(
                textColor = textColor,
                visible = isVisibleWhiteBox && !isVisibleFolder,
            ),
        horizontalAlignment = horizontalAlignment,
        verticalArrangement = verticalArrangement,
    ) {
        Box(
            modifier = Modifier
                .size(gridItemSettings.iconSize.dp)
                .scale(scale.value)
                .alpha(alpha),
        ) {
            AsyncImage(
                model = Builder(context).data(data.customIcon ?: icon)
                    .addLastModifiedToFileCacheKey(true)
                    .size(Size.ORIGINAL)
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
                        if (!isScrollInProgress && !hasInteraction) {
                            with(sharedTransitionScope) {
                                sharedElementWithCallerManagedVisibility(
                                    rememberSharedContentState(
                                        key = sharedElementKey,
                                    ),
                                    visible = true,
                                )
                            }
                        } else {
                            this
                        }
                    },
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
        }

        if (gridItemSettings.showLabel) {
            Text(
                modifier = Modifier.alpha(alpha),
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

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun InteractiveWidgetGridItem(
    modifier: Modifier = Modifier,
    sharedTransitionScope: SharedTransitionScope,
    data: GridItemData.Widget,
    drag: Drag,
    isScrollInProgress: Boolean,
    isSelected: Boolean,
    isVisibleOverlay: Boolean,
    sharedElementKey: SharedElementKey,
    textColor: Color,
    gridItem: GridItem,
    showGridItemPopup: Boolean,
    onUpdateIsCloseGridItemPopup: (Boolean) -> Unit,
    onShowGridItemPopup: (
        intOffset: IntOffset,
        intSize: IntSize,
    ) -> Unit,
    onUpdateGridItemSource: (GridItemSource) -> Unit,
    onUpdateImageBitmap: (ImageBitmap) -> Unit,
    onUpdateIsDragging: (Boolean) -> Unit,
    onUpdateIsVisibleOverlay: (Boolean) -> Unit,
    onUpdateOverlayBounds: (
        intOffset: IntOffset,
        intSize: IntSize,
    ) -> Unit,
    onUpdateSharedElementKey: (SharedElementKey?) -> Unit,
    onUpdateMoveGridItemResult: (MoveGridItemResult) -> Unit,
) {
    val appWidgetHost = LocalAppWidgetHost.current

    val appWidgetManager = LocalAppWidgetManager.current

    val appWidgetInfo = appWidgetManager.getAppWidgetInfo(appWidgetId = data.appWidgetId)

    val graphicsLayer = rememberGraphicsLayer()

    val scope = rememberCoroutineScope()

    val hasInteraction = isSelected && isVisibleOverlay

    val isVisibleWhiteBox = hasInteraction && drag == Drag.Dragging

    val alpha = if (hasInteraction) 0f else 1f

    val scale = remember { Animatable(1f) }

    var intOffset by remember { mutableStateOf(IntOffset.Zero) }

    var intSize by remember { mutableStateOf(IntSize.Zero) }
    LaunchedEffect(
        key1 = drag,
        key2 = hasInteraction,
        key3 = showGridItemPopup,
    ) {
        if (drag == Drag.Dragging &&
            hasInteraction &&
            showGridItemPopup
        ) {
            onUpdateIsDragging(true)

            onUpdateIsCloseGridItemPopup(true)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .whiteBox(textColor = textColor, visible = isVisibleWhiteBox),
    ) {
        val commonModifier = Modifier
            .matchParentSize()
            .scale(scale.value)
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
            .run {
                if (!isScrollInProgress && !hasInteraction) {
                    with(sharedTransitionScope) {
                        sharedElementWithCallerManagedVisibility(
                            rememberSharedContentState(
                                key = sharedElementKey,
                            ),
                            visible = true,
                        )
                    }
                } else {
                    this
                }
            }

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
                    if (!isVisibleOverlay) {
                        view.setOnLongClickListener {
                            scope.launch {
                                onLongPress(
                                    graphicsLayer = graphicsLayer,
                                    intOffset = intOffset,
                                    intSize = intSize,
                                    sharedElementKey = sharedElementKey,
                                    gridItem = gridItem,
                                    onUpdateGridItemSource = onUpdateGridItemSource,
                                    onUpdateImageBitmap = onUpdateImageBitmap,
                                    onUpdateOverlayBounds = onUpdateOverlayBounds,
                                    onUpdateSharedElementKey = onUpdateSharedElementKey,
                                    onShowGridItemPopup = onShowGridItemPopup,
                                    onUpdateIsVisibleOverlay = onUpdateIsVisibleOverlay,
                                    onUpdateMoveGridItemResult = onUpdateMoveGridItemResult,
                                )
                            }

                            true
                        }
                    }
                },
            )
        } else {
            AsyncImage(
                model = data.preview ?: data.icon,
                contentDescription = null,
                modifier = commonModifier.pointerInput(key1 = isVisibleOverlay) {
                    detectTapGestures(
                        onLongPress = if (!isVisibleOverlay) {
                            {
                                scope.launch {
                                    onLongPress(
                                        graphicsLayer = graphicsLayer,
                                        intOffset = intOffset,
                                        intSize = intSize,
                                        sharedElementKey = sharedElementKey,
                                        gridItem = gridItem,
                                        onUpdateGridItemSource = onUpdateGridItemSource,
                                        onUpdateImageBitmap = onUpdateImageBitmap,
                                        onUpdateOverlayBounds = onUpdateOverlayBounds,
                                        onUpdateSharedElementKey = onUpdateSharedElementKey,
                                        onShowGridItemPopup = onShowGridItemPopup,
                                        onUpdateIsVisibleOverlay = onUpdateIsVisibleOverlay,
                                        onUpdateMoveGridItemResult = onUpdateMoveGridItemResult,
                                    )
                                }
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

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun InteractiveShortcutInfoGridItem(
    modifier: Modifier = Modifier,
    sharedTransitionScope: SharedTransitionScope,
    data: GridItemData.ShortcutInfo,
    drag: Drag,
    gridItem: GridItem,
    gridItemSettings: GridItemSettings,
    hasShortcutHostPermission: Boolean,
    isScrollInProgress: Boolean,
    isSelected: Boolean,
    isVisibleFolder: Boolean,
    isVisibleOverlay: Boolean,
    sharedElementKey: SharedElementKey,
    textColor: Color,
    showGridItemPopup: Boolean,
    onUpdateIsCloseGridItemPopup: (Boolean) -> Unit,
    onOpenAppDrawer: () -> Unit,
    onShowGridItemPopup: (
        intOffset: IntOffset,
        intSize: IntSize,
    ) -> Unit,
    onTapShortcutInfo: (
        serialNumber: Long,
        packageName: String,
        shortcutId: String,
    ) -> Unit,
    onUpdateGridItemSource: (GridItemSource) -> Unit,
    onUpdateImageBitmap: (ImageBitmap) -> Unit,
    onUpdateIsDragging: (Boolean) -> Unit,
    onUpdateIsVisibleOverlay: (Boolean) -> Unit,
    onUpdateOverlayBounds: (
        intOffset: IntOffset,
        intSize: IntSize,
    ) -> Unit,
    onUpdateSharedElementKey: (SharedElementKey?) -> Unit,
    onUpdateMoveGridItemResult: (MoveGridItemResult) -> Unit,
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

    val hasInteraction = isSelected && isVisibleOverlay

    val isVisibleWhiteBox = hasInteraction && drag == Drag.Dragging

    val alpha = if (hasInteraction) 0f else 1f

    val scale = remember { Animatable(1f) }

    LaunchedEffect(
        key1 = drag,
        key2 = hasInteraction,
        key3 = showGridItemPopup,
    ) {
        if (drag == Drag.Dragging &&
            hasInteraction &&
            showGridItemPopup
        ) {
            onUpdateIsDragging(true)

            onUpdateIsCloseGridItemPopup(true)
        }
    }

    Column(
        modifier = modifier
            .pointerInput(key1 = isVisibleOverlay) {
                detectTapGestures(
                    onDoubleTap = if (!isVisibleOverlay) {
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
                    onLongPress = if (!isVisibleOverlay) {
                        {
                            scope.launch {
                                onLongPress(
                                    graphicsLayer = graphicsLayer,
                                    intOffset = intOffset,
                                    intSize = intSize,
                                    sharedElementKey = sharedElementKey,
                                    gridItem = gridItem,
                                    onUpdateGridItemSource = onUpdateGridItemSource,
                                    onUpdateImageBitmap = onUpdateImageBitmap,
                                    onUpdateOverlayBounds = onUpdateOverlayBounds,
                                    onUpdateSharedElementKey = onUpdateSharedElementKey,
                                    onShowGridItemPopup = onShowGridItemPopup,
                                    onUpdateIsVisibleOverlay = onUpdateIsVisibleOverlay,
                                    onUpdateMoveGridItemResult = onUpdateMoveGridItemResult,
                                )
                            }
                        }
                    } else {
                        null
                    },
                    onTap = if (!isVisibleOverlay) {
                        {
                            if (hasShortcutHostPermission && data.isEnabled) {
                                scope.launch {
                                    scale.animateTo(0.8f)

                                    scale.animateTo(1f)

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
                    onPress = {
                        onPress(
                            isVisibleOverlay = isVisibleOverlay,
                            scale = scale,
                        )
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
            .whiteBox(
                textColor = textColor,

                visible = isVisibleWhiteBox && !isVisibleFolder,
            ),
        horizontalAlignment = horizontalAlignment,
        verticalArrangement = verticalArrangement,
    ) {
        Box(
            modifier = Modifier
                .size(gridItemSettings.iconSize.dp)
                .scale(scale.value)
                .alpha(alpha),
        ) {
            AsyncImage(
                model = Builder(context).data(customIcon)
                    .addLastModifiedToFileCacheKey(true)
                    .size(Size.ORIGINAL)
                    .build(),
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
                        if (!isScrollInProgress && !hasInteraction) {
                            with(sharedTransitionScope) {
                                sharedElementWithCallerManagedVisibility(
                                    rememberSharedContentState(
                                        key = sharedElementKey,
                                    ),
                                    visible = true,
                                )
                            }
                        } else {
                            this
                        }
                    },
                contentDescription = null,
            )

            AsyncImage(
                model = Builder(context).data(data.eblanApplicationInfoIcon)
                    .size(Size.ORIGINAL)
                    .build(),
                modifier = Modifier
                    .size((gridItemSettings.iconSize * 0.25).dp)
                    .align(Alignment.BottomEnd),
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

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun InteractiveFolderGridItem(
    modifier: Modifier = Modifier,
    sharedTransitionScope: SharedTransitionScope,
    data: GridItemData.Folder,
    drag: Drag,
    gridItem: GridItem,
    gridItemSettings: GridItemSettings,
    isScrollInProgress: Boolean,
    isSelected: Boolean,
    isVisibleFolder: Boolean,
    isVisibleOverlay: Boolean,
    sharedElementKey: SharedElementKey,
    textColor: Color,
    moveGridItemResult: MoveGridItemResult?,
    lockMovement: Boolean,
    isDragging: Boolean,
    showGridItemPopup: Boolean,
    onUpdateIsCloseGridItemPopup: (Boolean) -> Unit,
    onOpenAppDrawer: () -> Unit,
    onShowGridItemPopup: (
        intOffset: IntOffset,
        intSize: IntSize,
    ) -> Unit,
    onUpsertFolderPopupEntry: (FolderPopupEntry) -> Unit,
    onUpdateGridItemSource: (GridItemSource) -> Unit,
    onUpdateImageBitmap: (ImageBitmap) -> Unit,
    onUpdateIsDragging: (Boolean) -> Unit,
    onUpdateIsVisibleOverlay: (Boolean) -> Unit,
    onUpdateOverlayBounds: (
        intOffset: IntOffset,
        intSize: IntSize,
    ) -> Unit,
    onUpdateSharedElementKey: (SharedElementKey?) -> Unit,
    onUpdateMoveGridItemResult: (MoveGridItemResult) -> Unit,
    onShowFolderWhenDragging: (
        folderPopupEntry: FolderPopupEntry,
        movingGridItem: GridItem,
    ) -> Unit,
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

    val hasInteraction = isSelected && isVisibleOverlay

    val isVisibleWhiteBox = hasInteraction && drag == Drag.Dragging

    val alpha = if (hasInteraction) 0f else 1f

    val scale = remember { Animatable(1f) }

    val currentDrag = rememberUpdatedState(drag)
    val currentIsDragging = rememberUpdatedState(isDragging)
    val currentIsVisibleOverlay = rememberUpdatedState(isVisibleOverlay)
    val currentGridItem = rememberUpdatedState(gridItem)
    val currentLockMovement = rememberUpdatedState(lockMovement)

    LaunchedEffect(
        key1 = drag,
        key2 = hasInteraction,
        key3 = showGridItemPopup,
    ) {
        if (drag == Drag.Dragging &&
            hasInteraction &&
            showGridItemPopup
        ) {
            onUpdateIsDragging(true)

            onUpdateIsCloseGridItemPopup(true)
        }
    }

    LaunchedEffect(key1 = moveGridItemResult) {
        handleConflictingGridItem(
            drag = currentDrag,
            isDragging = currentIsDragging,
            isVisibleOverlay = currentIsVisibleOverlay,
            moveGridItemResult = moveGridItemResult,
            lockMovement = currentLockMovement,
            intOffset = intOffset,
            intSize = intSize,
            gridItem = currentGridItem,
            onShowFolderWhenDragging = onShowFolderWhenDragging,
            onUpdateSharedElementKey = onUpdateSharedElementKey,
        )
    }

    Column(
        modifier = modifier
            .pointerInput(key1 = isVisibleOverlay) {
                detectTapGestures(
                    onDoubleTap = if (!isVisibleOverlay) {
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
                    onLongPress = if (!isVisibleOverlay) {
                        {
                            scope.launch {
                                onLongPress(
                                    graphicsLayer = graphicsLayer,
                                    intOffset = intOffset,
                                    intSize = intSize,
                                    sharedElementKey = sharedElementKey,
                                    gridItem = gridItem,
                                    onUpdateGridItemSource = onUpdateGridItemSource,
                                    onUpdateImageBitmap = onUpdateImageBitmap,
                                    onUpdateOverlayBounds = onUpdateOverlayBounds,
                                    onUpdateSharedElementKey = onUpdateSharedElementKey,
                                    onShowGridItemPopup = onShowGridItemPopup,
                                    onUpdateIsVisibleOverlay = onUpdateIsVisibleOverlay,
                                    onUpdateMoveGridItemResult = onUpdateMoveGridItemResult,
                                )
                            }
                        }
                    } else {
                        null
                    },
                    onTap = if (!isVisibleOverlay) {
                        {
                            scope.launch {
                                scale.animateTo(0.8f)

                                scale.animateTo(1f)

                                onUpsertFolderPopupEntry(
                                    FolderPopupEntry(
                                        id = gridItem.id,
                                        x = intOffset.x,
                                        y = intOffset.y,
                                        width = intSize.width,
                                        height = intSize.height,
                                        isCloseFolder = false,
                                    ),
                                )
                            }
                        }
                    } else {
                        null
                    },
                    onPress = {
                        onPress(
                            isVisibleOverlay = isVisibleOverlay,
                            scale = scale,
                        )
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
            .whiteBox(
                textColor = textColor,
                visible = isVisibleWhiteBox && !isVisibleFolder,
            ),
        horizontalAlignment = horizontalAlignment,
        verticalArrangement = verticalArrangement,
    ) {
        val commonModifier = Modifier
            .size(gridItemSettings.iconSize.dp)
            .scale(scale.value)
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
            .run {
                if (!isScrollInProgress && !hasInteraction) {
                    with(sharedTransitionScope) {
                        sharedElementWithCallerManagedVisibility(
                            rememberSharedContentState(
                                key = sharedElementKey,
                            ),
                            visible = true,
                        )
                    }
                } else {
                    this
                }
            }

        if (data.icon != null) {
            AsyncImage(
                model = data.icon,
                contentDescription = null,
                modifier = commonModifier,
            )
        } else {
            Box(
                modifier = commonModifier.background(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                    shape = RoundedCornerShape(5.dp),
                ),
            ) {
                PreviewFolderGridLayout(
                    modifier = Modifier.matchParentSize(),
                    gridItems = data.gridItemsByPage.values.firstOrNull()
                        ?.take(FOLDER_PREVIEW_COLUMNS * FOLDER_PREVIEW_ROWS),
                    content = { gridItem ->
                        PreviewFolderGridItem(
                            sharedTransitionScope = sharedTransitionScope,
                            gridItem = gridItem,
                            isScrollInProgress = isScrollInProgress,
                            isVisibleOverlay = isVisibleOverlay,
                            parent = sharedElementKey.parent,
                            moveGridItemResult = moveGridItemResult,
                            textColor = textColor,
                        )
                    },
                )
            }
        }

        if (gridItemSettings.showLabel) {
            Text(
                modifier = Modifier.alpha(alpha),
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

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun InteractiveShortcutConfigGridItem(
    modifier: Modifier = Modifier,
    sharedTransitionScope: SharedTransitionScope,
    data: GridItemData.ShortcutConfig,
    drag: Drag,
    gridItem: GridItem,
    gridItemSettings: GridItemSettings,
    isScrollInProgress: Boolean,
    isSelected: Boolean,
    isVisibleFolder: Boolean,
    isVisibleOverlay: Boolean,
    sharedElementKey: SharedElementKey,
    textColor: Color,
    showGridItemPopup: Boolean,
    onUpdateIsCloseGridItemPopup: (Boolean) -> Unit,
    onOpenAppDrawer: () -> Unit,
    onShowGridItemPopup: (
        intOffset: IntOffset,
        intSize: IntSize,
    ) -> Unit,
    onTapShortcutConfig: (String) -> Unit,
    onUpdateGridItemSource: (GridItemSource) -> Unit,
    onUpdateImageBitmap: (ImageBitmap) -> Unit,
    onUpdateIsDragging: (Boolean) -> Unit,
    onUpdateIsVisibleOverlay: (Boolean) -> Unit,
    onUpdateOverlayBounds: (
        intOffset: IntOffset,
        intSize: IntSize,
    ) -> Unit,
    onUpdateSharedElementKey: (SharedElementKey?) -> Unit,
    onUpdateMoveGridItemResult: (MoveGridItemResult) -> Unit,
) {
    val launcherApps = LocalLauncherApps.current

    val context = LocalContext.current

    var intOffset = remember { IntOffset.Zero }

    var intSize = remember { IntSize.Zero }

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

    val hasInteraction = isSelected && isVisibleOverlay

    val isVisibleWhiteBox = hasInteraction && drag == Drag.Dragging

    val alpha = if (hasInteraction) 0f else 1f

    val scale = remember { Animatable(1f) }

    LaunchedEffect(
        key1 = drag,
        key2 = hasInteraction,
        key3 = showGridItemPopup,
    ) {
        if (drag == Drag.Dragging &&
            hasInteraction &&
            showGridItemPopup
        ) {
            onUpdateIsDragging(true)

            onUpdateIsCloseGridItemPopup(true)
        }
    }

    Column(
        modifier = modifier
            .pointerInput(key1 = isVisibleOverlay) {
                detectTapGestures(
                    onDoubleTap = if (!isVisibleOverlay) {
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
                    onLongPress = if (!isVisibleOverlay) {
                        {
                            scope.launch {
                                onLongPress(
                                    graphicsLayer = graphicsLayer,
                                    intOffset = intOffset,
                                    intSize = intSize,
                                    sharedElementKey = sharedElementKey,
                                    gridItem = gridItem,
                                    onUpdateGridItemSource = onUpdateGridItemSource,
                                    onUpdateImageBitmap = onUpdateImageBitmap,
                                    onUpdateOverlayBounds = onUpdateOverlayBounds,
                                    onUpdateSharedElementKey = onUpdateSharedElementKey,
                                    onShowGridItemPopup = onShowGridItemPopup,
                                    onUpdateIsVisibleOverlay = onUpdateIsVisibleOverlay,
                                    onUpdateMoveGridItemResult = onUpdateMoveGridItemResult,
                                )
                            }
                        }
                    } else {
                        null
                    },
                    onTap = if (!isVisibleOverlay) {
                        {
                            scope.launch {
                                scale.animateTo(0.8f)

                                scale.animateTo(1f)

                                data.shortcutIntentUri?.let(onTapShortcutConfig)
                            }
                        }
                    } else {
                        null
                    },
                    onPress = {
                        onPress(
                            isVisibleOverlay = isVisibleOverlay,
                            scale = scale,
                        )
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
            .whiteBox(
                textColor = textColor,
                visible = isVisibleWhiteBox && !isVisibleFolder,
            ),
        horizontalAlignment = horizontalAlignment,
        verticalArrangement = verticalArrangement,
    ) {
        AsyncImage(
            model = Builder(context)
                .data(icon)
                .addLastModifiedToFileCacheKey(true)
                .size(Size.ORIGINAL)
                .build(),
            contentDescription = null,
            modifier = Modifier
                .size(gridItemSettings.iconSize.dp)
                .scale(scale.value)
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
                .then(
                    with(sharedTransitionScope) {
                        Modifier.sharedElementWithCallerManagedVisibility(
                            rememberSharedContentState(
                                key = sharedElementKey,
                            ),
                            visible = !isScrollInProgress && !hasInteraction,
                        )
                    },
                ),
        )

        if (gridItemSettings.showLabel) {
            Text(
                modifier = Modifier.alpha(alpha),
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

@Composable
private fun PreviewFolderGridItem(
    modifier: Modifier = Modifier,
    sharedTransitionScope: SharedTransitionScope,
    gridItem: GridItem,
    isScrollInProgress: Boolean,
    isVisibleOverlay: Boolean,
    parent: SharedElementKey.Parent,
    moveGridItemResult: MoveGridItemResult?,
    textColor: Color,
) {
    val context = LocalContext.current

    key(gridItem.id) {
        val isSelected =
            moveGridItemResult != null && moveGridItemResult.movingGridItem.id == gridItem.id

        val hasInteraction = isSelected && isVisibleOverlay

        val alpha = if (hasInteraction) 0f else 1f

        val commonModifier = modifier
            .padding(1.dp)
            .alpha(alpha)
            .run {
                if (!isScrollInProgress && !hasInteraction) {
                    with(sharedTransitionScope) {
                        sharedElementWithCallerManagedVisibility(
                            rememberSharedContentState(
                                key = SharedElementKey(
                                    id = gridItem.id,
                                    parent = parent,
                                ),
                            ),
                            visible = true,
                        )
                    }
                } else {
                    this
                }
            }

        when (val data = gridItem.data) {
            is GridItemData.ApplicationInfo -> {
                val icon = data.iconPackInfoFilePath ?: data.icon

                AsyncImage(
                    model = Builder(context)
                        .data(data.customIcon ?: icon)
                        .addLastModifiedToFileCacheKey(true)
                        .size(Size.ORIGINAL)
                        .build(),
                    contentDescription = null,
                    modifier = commonModifier,
                )
            }

            is GridItemData.ShortcutConfig -> {
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

                AsyncImage(
                    model = Builder(context)
                        .data(icon)
                        .addLastModifiedToFileCacheKey(true)
                        .size(Size.ORIGINAL)
                        .build(),
                    contentDescription = null,
                    modifier = commonModifier,
                )
            }

            is GridItemData.ShortcutInfo -> {
                AsyncImage(
                    model = Builder(context)
                        .data(data.customIcon ?: data.icon)
                        .addLastModifiedToFileCacheKey(true)
                        .size(Size.ORIGINAL)
                        .build(),
                    contentDescription = null,
                    modifier = commonModifier,
                )
            }

            is GridItemData.Folder -> {
                if (data.icon != null) {
                    AsyncImage(
                        model = Builder(context)
                            .data(data.icon)
                            .addLastModifiedToFileCacheKey(true)
                            .size(Size.ORIGINAL)
                            .build(),
                        contentDescription = null,
                        modifier = commonModifier,
                    )
                } else {
                    Icon(
                        modifier = commonModifier,
                        imageVector = EblanLauncherIcons.Folder,
                        contentDescription = null,
                        tint = textColor,
                    )
                }
            }

            else -> Unit
        }
    }
}
