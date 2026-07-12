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
package com.eblan.launcher.feature.home.screen.pager.griditem

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest.Builder
import coil3.request.addLastModifiedToFileCacheKey
import coil3.size.Size
import com.eblan.launcher.domain.model.GridItem
import com.eblan.launcher.domain.model.GridItemData
import com.eblan.launcher.domain.model.GridItemSettings
import com.eblan.launcher.domain.model.MoveGridItemResult
import com.eblan.launcher.feature.home.component.swipeGestures
import com.eblan.launcher.feature.home.component.whiteBox
import com.eblan.launcher.feature.home.model.GridItemSource
import com.eblan.launcher.feature.home.model.SharedElementKey
import com.eblan.launcher.feature.home.screen.pager.onLongPress
import com.eblan.launcher.feature.home.util.getHorizontalAlignment
import com.eblan.launcher.feature.home.util.getHorizontalArrangement
import com.eblan.launcher.feature.home.util.getVerticalAlignment
import com.eblan.launcher.feature.home.util.getVerticalArrangement
import com.eblan.launcher.feature.home.util.onDoubleTap
import com.eblan.launcher.feature.home.util.onPress
import com.eblan.launcher.ui.local.LocalLauncherApps
import com.eblan.launcher.ui.local.LocalSettings
import kotlinx.coroutines.launch

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun StartIconEndLabelApplicationInfoGridItem(
    modifier: Modifier = Modifier,
    sharedTransitionScope: SharedTransitionScope,
    data: GridItemData.ApplicationInfo,
    gridItem: GridItem,
    gridItemSettings: GridItemSettings,
    isScrollInProgress: Boolean,
    isVisibleFolder: Boolean,
    isVisibleOverlay: Boolean,
    sharedElementKey: SharedElementKey,
    statusBarNotifications: Map<String, Int>,
    textColor: Color,
    hasInteraction: Boolean,
    isVisibleWhiteBox: Boolean,
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

    val horizontalArrangement =
        getHorizontalArrangement(horizontalArrangement = gridItemSettings.horizontalArrangement)

    val verticalAlignment =
        getVerticalAlignment(verticalAlignment = gridItemSettings.verticalAlignment)

    val maxLines = if (gridItemSettings.singleLineLabel) 1 else Int.MAX_VALUE

    val icon = data.iconPackInfoFilePath ?: data.icon

    val hasNotifications =
        statusBarNotifications[data.packageName] != null && (
            statusBarNotifications[data.packageName]
                ?: 0
            ) > 0

    val alpha = if (hasInteraction) 0f else 1f

    val scale = remember { Animatable(1f) }

    Row(
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
        horizontalArrangement = horizontalArrangement,
        verticalAlignment = verticalAlignment,
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

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun StartLabelEndIconApplicationInfoGridItem(
    modifier: Modifier = Modifier,
    sharedTransitionScope: SharedTransitionScope,
    data: GridItemData.ApplicationInfo,
    gridItem: GridItem,
    gridItemSettings: GridItemSettings,
    isScrollInProgress: Boolean,
    isVisibleFolder: Boolean,
    isVisibleOverlay: Boolean,
    sharedElementKey: SharedElementKey,
    statusBarNotifications: Map<String, Int>,
    textColor: Color,
    hasInteraction: Boolean,
    isVisibleWhiteBox: Boolean,
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

    val horizontalArrangement =
        getHorizontalArrangement(horizontalArrangement = gridItemSettings.horizontalArrangement)

    val verticalAlignment =
        getVerticalAlignment(verticalAlignment = gridItemSettings.verticalAlignment)

    val maxLines = if (gridItemSettings.singleLineLabel) 1 else Int.MAX_VALUE

    val icon = data.iconPackInfoFilePath ?: data.icon

    val hasNotifications =
        statusBarNotifications[data.packageName] != null && (
            statusBarNotifications[data.packageName]
                ?: 0
            ) > 0

    val alpha = if (hasInteraction) 0f else 1f

    val scale = remember { Animatable(1f) }

    Row(
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
        horizontalArrangement = horizontalArrangement,
        verticalAlignment = verticalAlignment,
    ) {
        Text(
            modifier = Modifier.alpha(alpha),
            text = data.customLabel ?: data.label,
            color = textColor,
            textAlign = TextAlign.Center,
            maxLines = maxLines,
            fontSize = gridItemSettings.textSize.sp,
            overflow = TextOverflow.Ellipsis,
        )

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
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun TopIconBottomLabelApplicationInfoGridItem(
    modifier: Modifier = Modifier,
    sharedTransitionScope: SharedTransitionScope,
    data: GridItemData.ApplicationInfo,
    gridItem: GridItem,
    gridItemSettings: GridItemSettings,
    isScrollInProgress: Boolean,
    isVisibleFolder: Boolean,
    isVisibleOverlay: Boolean,
    sharedElementKey: SharedElementKey,
    statusBarNotifications: Map<String, Int>,
    textColor: Color,
    hasInteraction: Boolean,
    isVisibleWhiteBox: Boolean,
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

    val alpha = if (hasInteraction) 0f else 1f

    val scale = remember { Animatable(1f) }

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

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun TopLabelBottomIconApplicationInfoGridItem(
    modifier: Modifier = Modifier,
    sharedTransitionScope: SharedTransitionScope,
    data: GridItemData.ApplicationInfo,
    gridItem: GridItem,
    gridItemSettings: GridItemSettings,
    isScrollInProgress: Boolean,
    isVisibleFolder: Boolean,
    isVisibleOverlay: Boolean,
    sharedElementKey: SharedElementKey,
    statusBarNotifications: Map<String, Int>,
    textColor: Color,
    hasInteraction: Boolean,
    isVisibleWhiteBox: Boolean,
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

    val alpha = if (hasInteraction) 0f else 1f

    val scale = remember { Animatable(1f) }

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

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun IconOnlyApplicationInfoGridItem(
    modifier: Modifier = Modifier,
    sharedTransitionScope: SharedTransitionScope,
    data: GridItemData.ApplicationInfo,
    gridItem: GridItem,
    gridItemSettings: GridItemSettings,
    isScrollInProgress: Boolean,
    isVisibleFolder: Boolean,
    isVisibleOverlay: Boolean,
    sharedElementKey: SharedElementKey,
    statusBarNotifications: Map<String, Int>,
    textColor: Color,
    hasInteraction: Boolean,
    isVisibleWhiteBox: Boolean,
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

    val icon = data.iconPackInfoFilePath ?: data.icon

    val hasNotifications =
        statusBarNotifications[data.packageName] != null && (
            statusBarNotifications[data.packageName]
                ?: 0
            ) > 0

    val alpha = if (hasInteraction) 0f else 1f

    val scale = remember { Animatable(1f) }

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
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun LabelOnlyApplicationInfoGridItem(
    modifier: Modifier = Modifier,
    sharedTransitionScope: SharedTransitionScope,
    data: GridItemData.ApplicationInfo,
    gridItem: GridItem,
    gridItemSettings: GridItemSettings,
    isScrollInProgress: Boolean,
    isVisibleFolder: Boolean,
    isVisibleOverlay: Boolean,
    sharedElementKey: SharedElementKey,
    textColor: Color,
    hasInteraction: Boolean,
    isVisibleWhiteBox: Boolean,
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

    val alpha = if (hasInteraction) 0f else 1f

    val scale = remember { Animatable(1f) }

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
        Text(
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
            text = data.customLabel ?: data.label,
            color = textColor,
            textAlign = TextAlign.Center,
            maxLines = maxLines,
            fontSize = gridItemSettings.textSize.sp,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
