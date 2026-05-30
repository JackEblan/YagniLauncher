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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.round
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest.Builder
import coil3.request.addLastModifiedToFileCacheKey
import coil3.size.Size
import com.eblan.launcher.designsystem.icon.EblanLauncherIcons
import com.eblan.launcher.domain.model.GridItem
import com.eblan.launcher.domain.model.GridItemData
import com.eblan.launcher.domain.model.GridItemSettings
import com.eblan.launcher.domain.model.MoveGridItemResult
import com.eblan.launcher.feature.home.component.PreviewFolderGridLayout
import com.eblan.launcher.feature.home.component.swipeGestures
import com.eblan.launcher.feature.home.component.whiteBox
import com.eblan.launcher.feature.home.model.Drag
import com.eblan.launcher.feature.home.model.GridItemSource
import com.eblan.launcher.feature.home.model.SharedElementKey
import com.eblan.launcher.feature.home.screen.pager.handleConflictingGridItem
import com.eblan.launcher.feature.home.util.FOLDER_PREVIEW_COLUMNS
import com.eblan.launcher.feature.home.util.FOLDER_PREVIEW_ROWS
import com.eblan.launcher.feature.home.util.getHorizontalAlignment
import com.eblan.launcher.feature.home.util.getVerticalArrangement
import com.eblan.launcher.feature.home.util.handleDrag
import com.eblan.launcher.feature.home.util.onDoubleTap
import com.eblan.launcher.feature.home.util.onLongPress
import com.eblan.launcher.feature.home.util.onPress
import com.eblan.launcher.ui.local.LocalLauncherApps
import com.eblan.launcher.ui.local.LocalSettings
import kotlinx.coroutines.launch

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun SharedTransitionScope.InteractiveFolderGridItemContent(
    modifier: Modifier = Modifier,
    drag: Drag,
    gridItem: GridItem,
    gridItemSettings: GridItemSettings,
    hasShortcutHostPermission: Boolean,
    iconPackFilePaths: Map<String, String>,
    isScrollInProgress: Boolean,
    statusBarNotifications: Map<String, Int>,
    isVisibleOverlay: Boolean,
    newGridItemSource: GridItemSource,
    sharedElementKey: SharedElementKey,
    moveGridItemResult: MoveGridItemResult?,
    progress: Float,
    isDragging: Boolean,
    isCloseFolderGridItemPopup: Boolean,
    onOpenAppDrawer: () -> Unit,
    onTapApplicationInfo: (
        serialNumber: Long,
        componentName: String,
    ) -> Unit,
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
    onUpdateIsCloseFolderGridItemPopup: (Boolean) -> Unit,
    onUpdateIsVisibleOverlay: (Boolean) -> Unit,
    onUpdateMoveGridItemResult: (MoveGridItemResult) -> Unit,
    onOpenNestedFolder: (
        intOffset: IntOffset,
        intSize: IntSize,
    ) -> Unit,
) {
    val isSelected =
        moveGridItemResult != null && moveGridItemResult.movingGridItem.id == gridItem.id

    val currentGridItemSettings = if (gridItem.override) {
        gridItem.gridItemSettings
    } else {
        gridItemSettings
    }

    val padding = lerp(1.dp, gridItemSettings.padding.dp, progress)

    when (val data = gridItem.data) {
        is GridItemData.ApplicationInfo -> {
            InteractiveFolderApplicationInfoGridItem(
                modifier = modifier,
                data = data,
                drag = drag,
                gridItem = gridItem,
                gridItemSettings = currentGridItemSettings,
                iconPackFilePaths = iconPackFilePaths,
                isScrollInProgress = isScrollInProgress,
                isSelected = isSelected,
                isVisibleOverlay = isVisibleOverlay,
                newGridItemSource = newGridItemSource,
                sharedElementKey = sharedElementKey,
                statusBarNotifications = statusBarNotifications,
                padding = padding,
                isDragging = isDragging,
                isCloseFolderGridItemPopup = isCloseFolderGridItemPopup,
                onUpdateIsCloseFolderGridItemPopup = onUpdateIsCloseFolderGridItemPopup,
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

        is GridItemData.ShortcutInfo -> {
            InteractiveFolderShortcutInfoGridItem(
                modifier = modifier,
                data = data,
                drag = drag,
                gridItem = gridItem,
                gridItemSettings = currentGridItemSettings,
                hasShortcutHostPermission = hasShortcutHostPermission,
                isScrollInProgress = isScrollInProgress,
                isSelected = isSelected,
                isVisibleOverlay = isVisibleOverlay,
                newGridItemSource = newGridItemSource,
                sharedElementKey = sharedElementKey,
                padding = padding,
                isDragging = isDragging,
                isCloseFolderGridItemPopup = isCloseFolderGridItemPopup,
                onUpdateIsCloseFolderGridItemPopup = onUpdateIsCloseFolderGridItemPopup,
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

        is GridItemData.ShortcutConfig -> {
            InteractiveFolderShortcutConfigGridItem(
                modifier = modifier,
                data = data,
                drag = drag,
                gridItem = gridItem,
                gridItemSettings = currentGridItemSettings,
                isScrollInProgress = isScrollInProgress,
                isSelected = isSelected,
                isVisibleOverlay = isVisibleOverlay,
                newGridItemSource = newGridItemSource,
                sharedElementKey = sharedElementKey,
                padding = padding,
                isDragging = isDragging,
                isCloseFolderGridItemPopup = isCloseFolderGridItemPopup,
                onUpdateIsCloseFolderGridItemPopup = onUpdateIsCloseFolderGridItemPopup,
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

        is GridItemData.Folder -> {
            InteractiveNestedFolderGridItem(
                modifier = modifier,
                data = data,
                drag = drag,
                gridItem = gridItem,
                gridItemSettings = currentGridItemSettings,
                iconPackFilePaths = iconPackFilePaths,
                isScrollInProgress = isScrollInProgress,
                isSelected = isSelected,
                isVisibleOverlay = isVisibleOverlay,
                newGridItemSource = newGridItemSource,
                sharedElementKey = sharedElementKey,
                moveGridItemResult = moveGridItemResult,
                isDragging = isDragging,
                isCloseFolderGridItemPopup = isCloseFolderGridItemPopup,
                onUpdateIsCloseFolderGridItemPopup = onUpdateIsCloseFolderGridItemPopup,
                onOpenAppDrawer = onOpenAppDrawer,
                onShowGridItemPopup = onShowGridItemPopup,
                onTap = onOpenNestedFolder,
                onUpdateGridItemSource = onUpdateGridItemSource,
                onUpdateImageBitmap = onUpdateImageBitmap,
                onUpdateIsDragging = onUpdateIsDragging,
                onUpdateIsVisibleOverlay = onUpdateIsVisibleOverlay,
                onUpdateOverlayBounds = onUpdateOverlayBounds,
                onUpdateSharedElementKey = onUpdateSharedElementKey,
                onUpdateMoveGridItemResult = onUpdateMoveGridItemResult,
            )
        }

        else -> error("Unsupported Folder Grid Item")
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun SharedTransitionScope.InteractiveFolderApplicationInfoGridItem(
    modifier: Modifier = Modifier,
    data: GridItemData.ApplicationInfo,
    drag: Drag,
    gridItem: GridItem,
    gridItemSettings: GridItemSettings,
    iconPackFilePaths: Map<String, String>,
    isScrollInProgress: Boolean,
    isSelected: Boolean,
    isVisibleOverlay: Boolean,
    newGridItemSource: GridItemSource,
    sharedElementKey: SharedElementKey,
    statusBarNotifications: Map<String, Int>,
    padding: Dp,
    isDragging: Boolean,
    isCloseFolderGridItemPopup: Boolean,
    onUpdateIsCloseFolderGridItemPopup: (Boolean) -> Unit,
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

    val icon = iconPackFilePaths[data.componentName] ?: data.icon

    val hasNotifications =
        statusBarNotifications[data.packageName] != null && (
            statusBarNotifications[data.packageName]
                ?: 0
            ) > 0

    val hasInteraction = isSelected && isVisibleOverlay

    val alpha = if (hasInteraction) 0f else 1f

    val scale = remember { Animatable(1f) }

    LaunchedEffect(
        drag,
        hasInteraction,
        isDragging,
        isCloseFolderGridItemPopup,
    ) {
        handleDrag(
            drag = drag,
            hasInteraction = hasInteraction,
            scale = scale,
            isDragging = isDragging,
            isCloseGridItemPopup = isCloseFolderGridItemPopup,
            onUpdateIsDragging = onUpdateIsDragging,
            onUpdateIsCloseGridItemPopup = onUpdateIsCloseFolderGridItemPopup,
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
                                    gridItemSource = newGridItemSource,
                                    sharedElementKey = sharedElementKey,
                                    gridItem = gridItem,
                                    scale = scale,
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
                                scale.animateTo(0.5f)

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
            .padding(padding)
            .background(
                color = Color(gridItemSettings.customBackgroundColor),
                shape = RoundedCornerShape(size = gridItemSettings.cornerRadius.dp),
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
                model = Builder(LocalContext.current).data(data.customIcon ?: icon)
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
                        if (!hasInteraction) {
                            sharedElementWithCallerManagedVisibility(
                                rememberSharedContentState(
                                    key = sharedElementKey,
                                ),
                                visible = !isScrollInProgress,
                            )
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
private fun SharedTransitionScope.InteractiveFolderShortcutInfoGridItem(
    modifier: Modifier = Modifier,
    data: GridItemData.ShortcutInfo,
    drag: Drag,
    gridItem: GridItem,
    gridItemSettings: GridItemSettings,
    hasShortcutHostPermission: Boolean,
    isScrollInProgress: Boolean,
    isSelected: Boolean,
    isVisibleOverlay: Boolean,
    newGridItemSource: GridItemSource,
    sharedElementKey: SharedElementKey,
    padding: Dp,
    isDragging: Boolean,
    isCloseFolderGridItemPopup: Boolean,
    onUpdateIsCloseFolderGridItemPopup: (Boolean) -> Unit,
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

    val alpha = if (hasInteraction) 0f else 1f

    val scale = remember { Animatable(1f) }

    LaunchedEffect(
        drag,
        hasInteraction,
        isDragging,
        isCloseFolderGridItemPopup,
    ) {
        handleDrag(
            drag = drag,
            hasInteraction = hasInteraction,
            scale = scale,
            isDragging = isDragging,
            isCloseGridItemPopup = isCloseFolderGridItemPopup,
            onUpdateIsDragging = onUpdateIsDragging,
            onUpdateIsCloseGridItemPopup = onUpdateIsCloseFolderGridItemPopup,
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
                                    gridItemSource = newGridItemSource,
                                    sharedElementKey = sharedElementKey,
                                    gridItem = gridItem,
                                    scale = scale,
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
                                    scale.animateTo(0.5f)

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
            .padding(padding)
            .background(
                color = Color(gridItemSettings.customBackgroundColor),
                shape = RoundedCornerShape(size = gridItemSettings.cornerRadius.dp),
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
                model = Builder(LocalContext.current).data(customIcon)
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
                        if (!hasInteraction) {
                            sharedElementWithCallerManagedVisibility(
                                rememberSharedContentState(
                                    key = sharedElementKey,
                                ),
                                visible = !isScrollInProgress,
                            )
                        } else {
                            this
                        }
                    },
                contentDescription = null,
            )

            AsyncImage(
                model = Builder(LocalContext.current).data(data.eblanApplicationInfoIcon)
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
private fun SharedTransitionScope.InteractiveFolderShortcutConfigGridItem(
    modifier: Modifier = Modifier,
    data: GridItemData.ShortcutConfig,
    drag: Drag,
    gridItem: GridItem,
    gridItemSettings: GridItemSettings,
    isScrollInProgress: Boolean,
    isSelected: Boolean,
    isVisibleOverlay: Boolean,
    newGridItemSource: GridItemSource,
    sharedElementKey: SharedElementKey,
    padding: Dp,
    isDragging: Boolean,
    isCloseFolderGridItemPopup: Boolean,
    onUpdateIsCloseFolderGridItemPopup: (Boolean) -> Unit,
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

    val hasInteraction = isSelected && isVisibleOverlay

    val alpha = if (hasInteraction) 0f else 1f

    val scale = remember { Animatable(1f) }

    LaunchedEffect(
        drag,
        hasInteraction,
        isDragging,
        isCloseFolderGridItemPopup,
    ) {
        handleDrag(
            drag = drag,
            hasInteraction = hasInteraction,
            scale = scale,
            isDragging = isDragging,
            isCloseGridItemPopup = isCloseFolderGridItemPopup,
            onUpdateIsDragging = onUpdateIsDragging,
            onUpdateIsCloseGridItemPopup = onUpdateIsCloseFolderGridItemPopup,
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
                                    gridItemSource = newGridItemSource,
                                    sharedElementKey = sharedElementKey,
                                    gridItem = gridItem,
                                    scale = scale,
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
                                scale.animateTo(0.5f)

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
            .padding(padding)
            .background(
                color = Color(gridItemSettings.customBackgroundColor),
                shape = RoundedCornerShape(size = gridItemSettings.cornerRadius.dp),
            ),
        horizontalAlignment = horizontalAlignment,
        verticalArrangement = verticalArrangement,
    ) {
        AsyncImage(
            model = Builder(LocalContext.current).data(icon)
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
                .run {
                    if (!hasInteraction) {
                        sharedElementWithCallerManagedVisibility(
                            rememberSharedContentState(
                                key = sharedElementKey,
                            ),
                            visible = !isScrollInProgress,
                        )
                    } else {
                        this
                    }
                },
        )

        if (gridItemSettings.showLabel) {
            Text(
                modifier = Modifier.alpha(alpha),
                text = label.toString(),
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
private fun SharedTransitionScope.InteractiveNestedFolderGridItem(
    modifier: Modifier = Modifier,
    data: GridItemData.Folder,
    drag: Drag,
    gridItem: GridItem,
    gridItemSettings: GridItemSettings,
    iconPackFilePaths: Map<String, String>,
    isScrollInProgress: Boolean,
    isSelected: Boolean,
    isVisibleOverlay: Boolean,
    newGridItemSource: GridItemSource,
    sharedElementKey: SharedElementKey,
    moveGridItemResult: MoveGridItemResult?,
    isDragging: Boolean,
    isCloseFolderGridItemPopup: Boolean,
    onUpdateIsCloseFolderGridItemPopup: (Boolean) -> Unit,
    onOpenAppDrawer: () -> Unit,
    onShowGridItemPopup: (
        intOffset: IntOffset,
        intSize: IntSize,
    ) -> Unit,
    onTap: (
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

    val alpha = if (hasInteraction) 0f else 1f

    val scale = remember { Animatable(1f) }

    LaunchedEffect(
        drag,
        hasInteraction,
        isDragging,
        isCloseFolderGridItemPopup,
    ) {
        handleDrag(
            drag = drag,
            hasInteraction = hasInteraction,
            scale = scale,
            isDragging = isDragging,
            isCloseGridItemPopup = isCloseFolderGridItemPopup,
            onUpdateIsDragging = onUpdateIsDragging,
            onUpdateIsCloseGridItemPopup = onUpdateIsCloseFolderGridItemPopup,
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
                                    gridItemSource = newGridItemSource,
                                    sharedElementKey = sharedElementKey,
                                    gridItem = gridItem,
                                    scale = scale,
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
                                scale.animateTo(0.5f)

                                scale.animateTo(1f)

                                onTap(
                                    intOffset,
                                    intSize,
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
                if (!hasInteraction) {
                    sharedElementWithCallerManagedVisibility(
                        rememberSharedContentState(
                            key = sharedElementKey,
                        ),
                        visible = !isScrollInProgress,
                    )
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
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(5.dp),
                ),
            ) {
                PreviewFolderGridLayout(
                    modifier = Modifier.matchParentSize(),
                    gridItems = data.gridItemsByPage.values.firstOrNull()
                        ?.take(FOLDER_PREVIEW_COLUMNS * FOLDER_PREVIEW_ROWS),
                    content = { gridItem ->
                        PreviewNestedFolderGridItem(
                            alpha = alpha,
                            gridItem = gridItem,
                            iconPackFilePaths = iconPackFilePaths,
                            isScrollInProgress = isScrollInProgress,
                            isVisibleOverlay = isVisibleOverlay,
                            parent = sharedElementKey.parent,
                            moveGridItemResult = moveGridItemResult,
                        )
                    },
                )
            }
        }

        if (gridItemSettings.showLabel) {
            Text(
                modifier = Modifier.alpha(alpha),
                text = data.label,
                textAlign = TextAlign.Center,
                maxLines = maxLines,
                fontSize = gridItemSettings.textSize.sp,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun SharedTransitionScope.PreviewNestedFolderGridItem(
    modifier: Modifier = Modifier,
    alpha: Float,
    gridItem: GridItem,
    iconPackFilePaths: Map<String, String>,
    isScrollInProgress: Boolean,
    isVisibleOverlay: Boolean,
    parent: SharedElementKey.Parent,
    moveGridItemResult: MoveGridItemResult?,
) {
    val context = LocalContext.current

    key(gridItem.id) {
        val isSelected =
            moveGridItemResult != null && moveGridItemResult.movingGridItem.id == gridItem.id

        val hasInteraction = isSelected && isVisibleOverlay

        val commonModifier = modifier
            .padding(1.dp)
            .alpha(alpha)
            .run {
                if (!hasInteraction) {
                    sharedElementWithCallerManagedVisibility(
                        rememberSharedContentState(
                            key = SharedElementKey(
                                id = gridItem.id,
                                parent = parent,
                            ),
                        ),
                        visible = !isScrollInProgress,
                    )
                } else {
                    this
                }
            }

        when (val data = gridItem.data) {
            is GridItemData.ApplicationInfo -> {
                val icon =
                    iconPackFilePaths[data.componentName]
                        ?: data.icon

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
                        imageVector = EblanLauncherIcons.Folder,
                        contentDescription = null,
                        modifier = commonModifier,
                    )
                }
            }

            else -> Unit
        }
    }
}
