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
package com.eblan.launcher.feature.home.screen.application

import android.graphics.Rect
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.addLastModifiedToFileCacheKey
import coil3.request.crossfade
import com.eblan.launcher.domain.model.AppDrawerSettings
import com.eblan.launcher.domain.model.AppDrawerType
import com.eblan.launcher.domain.model.Associate
import com.eblan.launcher.domain.model.EblanAction
import com.eblan.launcher.domain.model.EblanActionType
import com.eblan.launcher.domain.model.EblanApplicationInfo
import com.eblan.launcher.domain.model.EblanApplicationInfoWithIconPackInfo
import com.eblan.launcher.domain.model.GridItem
import com.eblan.launcher.domain.model.GridItemData
import com.eblan.launcher.domain.model.MoveGridItemResult
import com.eblan.launcher.feature.home.model.Drag
import com.eblan.launcher.feature.home.model.GridItemSource
import com.eblan.launcher.feature.home.model.SharedElementKey
import com.eblan.launcher.feature.home.screen.getHorizontalAlignment
import com.eblan.launcher.feature.home.screen.getVerticalArrangement
import com.eblan.launcher.feature.home.screen.onPress
import com.eblan.launcher.feature.home.util.getSystemTextColor
import com.eblan.launcher.framework.launcherapps.AndroidLauncherAppsWrapper
import com.eblan.launcher.ui.local.LocalLauncherApps
import kotlinx.coroutines.launch
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(
    ExperimentalUuidApi::class,
    ExperimentalSharedTransitionApi::class,
    ExperimentalLayoutApi::class,
)
@Composable
internal fun EblanApplicationInfoGridItem(
    modifier: Modifier = Modifier,
    sharedTransitionScope: SharedTransitionScope,
    appDrawerSettings: AppDrawerSettings,
    drag: Drag,
    eblanApplicationInfoWithIconPackInfo: EblanApplicationInfoWithIconPackInfo,
    paddingValues: PaddingValues,
    isVisibleOverlay: Boolean,
    appDrawerType: AppDrawerType,
    isScrollInProgress: Boolean,
    isSwiping: Boolean,
    onDismiss: () -> Unit,
    onUpdateGridItemSource: (GridItemSource) -> Unit,
    onUpdateImageBitmap: (ImageBitmap) -> Unit,
    onUpdateIsDragging: (Boolean) -> Unit,
    onUpdateOverlayBounds: (
        intOffset: IntOffset,
        intSize: IntSize,
    ) -> Unit,
    onUpdatePopupMenu: (Boolean) -> Unit,
    onUpdateSharedElementKey: (SharedElementKey?) -> Unit,
    onUpdateEblanApplicationInfo: (EblanApplicationInfo) -> Unit,
    onUpdateIsVisibleOverlay: (Boolean) -> Unit,
    onUpdateMoveGridItemResult: (MoveGridItemResult) -> Unit,
) {
    val graphicsLayer = rememberGraphicsLayer()

    val scope = rememberCoroutineScope()

    val context = LocalContext.current

    val density = LocalDensity.current

    val launcherApps = LocalLauncherApps.current

    val layoutDirection = LocalLayoutDirection.current

    val keyboardController = LocalSoftwareKeyboardController.current

    val textColor = getSystemTextColor(
        systemCustomTextColor = appDrawerSettings.gridItemSettings.customTextColor,
        systemTextColor = appDrawerSettings.gridItemSettings.textColor,
    )

    val maxLines = if (appDrawerSettings.gridItemSettings.singleLineLabel) 1 else Int.MAX_VALUE

    val icon = eblanApplicationInfoWithIconPackInfo.iconPackInfoFilePath
        ?: eblanApplicationInfoWithIconPackInfo.eblanApplicationInfo.icon

    val horizontalAlignment =
        getHorizontalAlignment(horizontalAlignment = appDrawerSettings.gridItemSettings.horizontalAlignment)

    val verticalArrangement =
        getVerticalArrangement(verticalArrangement = appDrawerSettings.gridItemSettings.verticalArrangement)

    val leftPadding = with(density) {
        paddingValues.calculateLeftPadding(layoutDirection).roundToPx()
    }

    val topPadding = with(density) {
        paddingValues.calculateTopPadding().roundToPx()
    }

    var isLongPress by remember { mutableStateOf(false) }

    val alpha = if (isLongPress) 0f else 1f

    val scale = remember { Animatable(1f) }

    var intOffset by remember { mutableStateOf(IntOffset.Zero) }

    var intSize by remember { mutableStateOf(IntSize.Zero) }

    val iconSizePx = with(density) {
        appDrawerSettings.gridItemSettings.iconSize.dp.roundToPx()
    }

    val sharedElementKey = SharedElementKey(
        id = "${eblanApplicationInfoWithIconPackInfo.eblanApplicationInfo.serialNumber} ${eblanApplicationInfoWithIconPackInfo.eblanApplicationInfo.packageName} ${eblanApplicationInfoWithIconPackInfo.eblanApplicationInfo.componentName}",
        parent = SharedElementKey.Parent.SwipeY,
    )

    LaunchedEffect(
        key1 = drag,
        key2 = isLongPress,
    ) {
        handleDragEblanApplicationInfoItem(
            appDrawerSettings = appDrawerSettings,
            drag = drag,
            eblanApplicationInfoWithIconPackInfo = eblanApplicationInfoWithIconPackInfo,
            isLongPress = isLongPress,
            isSwiping = isSwiping,
            onDismiss = onDismiss,
            onUpdateGridItemSource = onUpdateGridItemSource,
            onUpdateIsDragging = onUpdateIsDragging,
            onUpdateIsLongPress = {
                isLongPress = it
            },
            onUpdateIsVisibleOverlay = onUpdateIsVisibleOverlay,
            onUpdatePopupMenu = onUpdatePopupMenu,
            onUpdateMoveGridItemResult = onUpdateMoveGridItemResult,
        )
    }

    Column(
        modifier = modifier
            .pointerInput(key1 = isVisibleOverlay) {
                detectTapGestures(
                    onTap = if (!isVisibleOverlay) {
                        {
                            scope.launch {
                                handleOnTapEblanApplicationInfoItem(
                                    eblanApplicationInfoWithIconPackInfo = eblanApplicationInfoWithIconPackInfo,
                                    intOffset = intOffset,
                                    intSize = intSize,
                                    keyboardController = keyboardController,
                                    launcherApps = launcherApps,
                                    leftPadding = leftPadding,
                                    topPadding = topPadding,
                                    scale = scale,
                                )
                            }
                        }
                    } else {
                        null
                    },
                    onLongPress = if (!isVisibleOverlay) {
                        {
                            scope.launch {
                                handleOnLongPressEblanApplicationInfoItem(
                                    eblanApplicationInfo = eblanApplicationInfoWithIconPackInfo.eblanApplicationInfo,
                                    graphicsLayer = graphicsLayer,
                                    intOffset = intOffset,
                                    intSize = intSize,
                                    keyboardController = keyboardController,
                                    sharedElementKey = sharedElementKey,
                                    onUpdateEblanApplicationInfo = onUpdateEblanApplicationInfo,
                                    onUpdateImageBitmap = onUpdateImageBitmap,
                                    onUpdateIsLongPress = { isLongPress = it },
                                    onUpdateIsVisibleOverlay = onUpdateIsVisibleOverlay,
                                    onUpdateOverlayBounds = onUpdateOverlayBounds,
                                    onUpdatePopupMenu = onUpdatePopupMenu,
                                    onUpdateSharedElementKey = onUpdateSharedElementKey,
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
            .run {
                if (appDrawerType == AppDrawerType.Vertical) {
                    height(appDrawerSettings.appDrawerRowsHeight.dp)
                } else {
                    fillMaxSize()
                }
            }
            .padding(appDrawerSettings.gridItemSettings.padding.dp)
            .background(
                color = Color(appDrawerSettings.gridItemSettings.customBackgroundColor),
                shape = RoundedCornerShape(size = appDrawerSettings.gridItemSettings.cornerRadius.dp),
            ),
        horizontalAlignment = horizontalAlignment,
        verticalArrangement = verticalArrangement,
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(eblanApplicationInfoWithIconPackInfo.eblanApplicationInfo.customIcon ?: icon)
                .addLastModifiedToFileCacheKey(true)
                .size(iconSizePx)
                .crossfade(false)
                .build(),
            contentDescription = null,
            modifier = Modifier.size(appDrawerSettings.gridItemSettings.iconSize.dp)
                .scale(scale.value)
                .alpha(alpha)
                .drawWithContent {
                    graphicsLayer.record {
                        this@drawWithContent.drawContent()
                    }

                    drawLayer(graphicsLayer)
                }.onGloballyPositioned { layoutCoordinates ->
                    intOffset = layoutCoordinates.positionInRoot().round()

                    intSize = layoutCoordinates.size
                }
                .run {
                    if (!isSwiping &&
                        !isScrollInProgress &&
                        !isLongPress &&
                        !isVisibleOverlay
                    ) {
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

        if (appDrawerSettings.gridItemSettings.showLabel) {
            Spacer(modifier = Modifier.height(10.dp))

            Text(
                modifier = Modifier.alpha(alpha),
                text = eblanApplicationInfoWithIconPackInfo.eblanApplicationInfo.customLabel
                    ?: eblanApplicationInfoWithIconPackInfo.eblanApplicationInfo.label,
                color = textColor,
                textAlign = TextAlign.Center,
                maxLines = maxLines,
                fontSize = appDrawerSettings.gridItemSettings.textSize.sp,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

internal suspend fun handleOnTapEblanApplicationInfoItem(
    eblanApplicationInfoWithIconPackInfo: EblanApplicationInfoWithIconPackInfo,
    intOffset: IntOffset,
    intSize: IntSize,
    keyboardController: SoftwareKeyboardController?,
    launcherApps: AndroidLauncherAppsWrapper,
    leftPadding: Int,
    topPadding: Int,
    scale: Animatable<Float, AnimationVector1D>,
) {
    val left = intOffset.x + leftPadding

    val top = intOffset.y + topPadding

    scale.animateTo(0.8f)

    scale.animateTo(1f)

    launcherApps.startMainActivity(
        serialNumber = eblanApplicationInfoWithIconPackInfo.eblanApplicationInfo.serialNumber,
        componentName = eblanApplicationInfoWithIconPackInfo.eblanApplicationInfo.componentName,
        sourceBounds = Rect(
            left,
            top,
            left + intSize.width,
            top + intSize.height,
        ),
    )

    keyboardController?.hide()
}

@OptIn(ExperimentalUuidApi::class)
internal fun handleDragEblanApplicationInfoItem(
    appDrawerSettings: AppDrawerSettings,
    drag: Drag,
    eblanApplicationInfoWithIconPackInfo: EblanApplicationInfoWithIconPackInfo,
    isLongPress: Boolean,
    isSwiping: Boolean,
    onDismiss: () -> Unit,
    onUpdateGridItemSource: (GridItemSource) -> Unit,
    onUpdateIsDragging: (Boolean) -> Unit,
    onUpdateIsLongPress: (Boolean) -> Unit,
    onUpdateIsVisibleOverlay: (Boolean) -> Unit,
    onUpdatePopupMenu: (Boolean) -> Unit,
    onUpdateMoveGridItemResult: (MoveGridItemResult) -> Unit,
) {
    if (!isLongPress) return

    when (drag) {
        Drag.Dragging -> {
            onUpdatePopupMenu(false)

            onDismiss()

            val pagerScreenId = Uuid.random().toHexString()

            val data = GridItemData.ApplicationInfo(
                serialNumber = eblanApplicationInfoWithIconPackInfo.eblanApplicationInfo.serialNumber,
                componentName = eblanApplicationInfoWithIconPackInfo.eblanApplicationInfo.componentName,
                packageName = eblanApplicationInfoWithIconPackInfo.eblanApplicationInfo.packageName,
                icon = eblanApplicationInfoWithIconPackInfo.eblanApplicationInfo.icon,
                label = eblanApplicationInfoWithIconPackInfo.eblanApplicationInfo.label,
                customIcon = eblanApplicationInfoWithIconPackInfo.eblanApplicationInfo.customIcon,
                customLabel = eblanApplicationInfoWithIconPackInfo.eblanApplicationInfo.customLabel,
                index = -1,
                folderId = null,
                iconPackInfoFilePath = eblanApplicationInfoWithIconPackInfo.iconPackInfoFilePath,
            )

            val eblanAction = EblanAction(
                eblanActionType = EblanActionType.None,
                serialNumber = 0L,
                componentName = "",
            )

            val gridItem = GridItem(
                id = pagerScreenId,
                page = 0,
                startColumn = -1,
                startRow = -1,
                columnSpan = 1,
                rowSpan = 1,
                data = data,
                associate = Associate.Grid,
                override = false,
                gridItemSettings = appDrawerSettings.gridItemSettings,
                doubleTap = eblanAction,
                swipeUp = eblanAction,
                swipeDown = eblanAction,
            )

            onUpdateGridItemSource(GridItemSource.New)

            onUpdateMoveGridItemResult(
                MoveGridItemResult(
                    isSuccess = false,
                    movingGridItem = gridItem,
                    conflictingGridItem = null,
                ),
            )

            onUpdateIsDragging(true)
        }

        Drag.Cancel, Drag.End -> {
            onUpdateIsLongPress(false)

            if (!isSwiping) {
                onUpdateIsVisibleOverlay(false)
            }
        }

        else -> Unit
    }
}

@OptIn(ExperimentalUuidApi::class)
internal suspend fun handleOnLongPressEblanApplicationInfoItem(
    eblanApplicationInfo: EblanApplicationInfo,
    graphicsLayer: GraphicsLayer,
    intOffset: IntOffset,
    intSize: IntSize,
    keyboardController: SoftwareKeyboardController?,
    sharedElementKey: SharedElementKey,
    onUpdateEblanApplicationInfo: (EblanApplicationInfo) -> Unit,
    onUpdateImageBitmap: (ImageBitmap) -> Unit,
    onUpdateIsLongPress: (Boolean) -> Unit,
    onUpdateIsVisibleOverlay: (Boolean) -> Unit,
    onUpdateOverlayBounds: (IntOffset, IntSize) -> Unit,
    onUpdatePopupMenu: (Boolean) -> Unit,
    onUpdateSharedElementKey: (SharedElementKey?) -> Unit,
) {
    onUpdateImageBitmap(graphicsLayer.toImageBitmap())

    onUpdateOverlayBounds(
        intOffset,
        intSize,
    )

    onUpdateSharedElementKey(sharedElementKey)

    onUpdateEblanApplicationInfo(eblanApplicationInfo)

    onUpdatePopupMenu(true)

    onUpdateIsLongPress(true)

    keyboardController?.hide()

    onUpdateIsVisibleOverlay(true)
}
