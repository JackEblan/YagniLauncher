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
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.isImeVisible
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithContent
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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.addLastModifiedToFileCacheKey
import com.eblan.launcher.domain.model.AppDrawerSettings
import com.eblan.launcher.domain.model.AppDrawerType
import com.eblan.launcher.domain.model.Associate
import com.eblan.launcher.domain.model.EblanAction
import com.eblan.launcher.domain.model.EblanActionType
import com.eblan.launcher.domain.model.EblanApplicationInfo
import com.eblan.launcher.domain.model.GridItem
import com.eblan.launcher.domain.model.GridItemData
import com.eblan.launcher.feature.home.model.Drag
import com.eblan.launcher.feature.home.model.GridItemSource
import com.eblan.launcher.feature.home.model.SharedElementKey
import com.eblan.launcher.feature.home.util.getHorizontalAlignment
import com.eblan.launcher.feature.home.util.getSystemTextColor
import com.eblan.launcher.feature.home.util.getVerticalArrangement
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
internal fun SharedTransitionScope.EblanApplicationInfoItem(
    modifier: Modifier = Modifier,
    appDrawerSettings: AppDrawerSettings,
    currentPage: Int,
    drag: Drag,
    eblanApplicationInfo: EblanApplicationInfo,
    iconPackFilePaths: Map<String, String>,
    paddingValues: PaddingValues,
    isVisibleOverlay: Boolean,
    appDrawerType: AppDrawerType,
    onDismiss: () -> Unit,
    onDraggingGridItem: () -> Unit,
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
    onScrollToItem: suspend (Int) -> Unit,
) {
    var intOffset by remember { mutableStateOf(IntOffset.Zero) }

    var intSize by remember { mutableStateOf(IntSize.Zero) }

    val graphicsLayer = rememberGraphicsLayer()

    val scope = rememberCoroutineScope()

    val density = LocalDensity.current

    val launcherApps = LocalLauncherApps.current

    val keyboardController = LocalSoftwareKeyboardController.current

    val textColor = getSystemTextColor(
        systemCustomTextColor = appDrawerSettings.gridItemSettings.customTextColor,
        systemTextColor = appDrawerSettings.gridItemSettings.textColor,
    )

    val appDrawerRowsHeight = appDrawerSettings.appDrawerRowsHeight.dp

    val maxLines = if (appDrawerSettings.gridItemSettings.singleLineLabel) 1 else Int.MAX_VALUE

    val icon = iconPackFilePaths[eblanApplicationInfo.componentName] ?: eblanApplicationInfo.icon

    val horizontalAlignment =
        getHorizontalAlignment(horizontalAlignment = appDrawerSettings.gridItemSettings.horizontalAlignment)

    val verticalArrangement =
        getVerticalArrangement(verticalArrangement = appDrawerSettings.gridItemSettings.verticalArrangement)

    val leftPadding = with(density) {
        paddingValues.calculateStartPadding(LayoutDirection.Ltr).roundToPx()
    }

    val topPadding = with(density) {
        paddingValues.calculateTopPadding().roundToPx()
    }

    var isLongPress by remember { mutableStateOf(false) }

    var isTap by remember { mutableStateOf(false) }

    val applicationScreenId = remember { Uuid.random().toHexString() }

    val alpha = if (isLongPress) 0f else 1f

    val isImeVisible by rememberUpdatedState(WindowInsets.isImeVisible)

    fun startMainActivity() {
        val left = intOffset.x + leftPadding

        val top = intOffset.y + topPadding

        launcherApps.startMainActivity(
            serialNumber = eblanApplicationInfo.serialNumber,
            componentName = eblanApplicationInfo.componentName,
            sourceBounds = Rect(
                left,
                top,
                left + intSize.width,
                top + intSize.height,
            ),
        )
    }

    LaunchedEffect(key1 = drag) {
        handleApplicationDrag(
            appDrawerSettings = appDrawerSettings,
            currentPage = currentPage,
            drag = drag,
            eblanApplicationInfo = eblanApplicationInfo,
            isLongPress = isLongPress,
            isVisibleOverlay = isVisibleOverlay,
            onDismiss = onDismiss,
            onDraggingGridItem = onDraggingGridItem,
            onUpdateGridItemSource = onUpdateGridItemSource,
            onUpdateIsDragging = onUpdateIsDragging,
            onUpdateIsLongPress = { newIsLongPress ->
                isLongPress = newIsLongPress
            },
            onUpdateIsVisibleOverlay = onUpdateIsVisibleOverlay,
            onUpdatePopupMenu = onUpdatePopupMenu,
        )
    }

    LaunchedEffect(key1 = isTap, key2 = isImeVisible) {
        handleApplicationTap(
            isTap = isTap,
            isImeVisible = isImeVisible,
            appDrawerSettings = appDrawerSettings,
            keyboardController = keyboardController,
            onDismiss = onDismiss,
            onScrollToItem = onScrollToItem,
            onStartMainActivity = ::startMainActivity,
            onUpdateIsTap = { newIsTap ->
                isTap = newIsTap
            },
        )
    }

    Column(
        modifier = modifier
            .pointerInput(key1 = drag) {
                detectTapGestures(
                    onTap = {
                        isTap = true
                    },
                    onLongPress = {
                        scope.launch {
                            handleApplicationLongPress(
                                applicationScreenId = applicationScreenId,
                                eblanApplicationInfo = eblanApplicationInfo,
                                graphicsLayer = graphicsLayer,
                                intOffset = intOffset,
                                intSize = intSize,
                                keyboardController = keyboardController,
                                onUpdateEblanApplicationInfo = onUpdateEblanApplicationInfo,
                                onUpdateImageBitmap = onUpdateImageBitmap,
                                onUpdateIsLongPress = { isLongPress = it },
                                onUpdateIsVisibleOverlay = onUpdateIsVisibleOverlay,
                                onUpdateOverlayBounds = onUpdateOverlayBounds,
                                onUpdatePopupMenu = onUpdatePopupMenu,
                                onUpdateSharedElementKey = onUpdateSharedElementKey,
                            )
                        }
                    },
                )
            }
            .run {
                when (appDrawerType) {
                    AppDrawerType.Vertical -> height(appDrawerRowsHeight)
                    AppDrawerType.Horizontal -> fillMaxSize()
                    else -> this
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
            model = ImageRequest.Builder(LocalContext.current)
                .data(eblanApplicationInfo.customIcon ?: icon).addLastModifiedToFileCacheKey(true)
                .build(),
            contentDescription = null,
            modifier = Modifier.size(appDrawerSettings.gridItemSettings.iconSize.dp).alpha(alpha)
                .drawWithContent {
                    graphicsLayer.record {
                        this@drawWithContent.drawContent()
                    }

                    drawLayer(graphicsLayer)
                }.onGloballyPositioned { layoutCoordinates ->
                    intOffset = layoutCoordinates.positionInRoot().round()

                    intSize = layoutCoordinates.size
                }.run {
                    if (!isLongPress) {
                        sharedElementWithCallerManagedVisibility(
                            rememberSharedContentState(
                                key = SharedElementKey(
                                    id = applicationScreenId,
                                    parent = SharedElementKey.Parent.SwipeY,
                                ),
                            ),
                            visible = !isVisibleOverlay,
                        )
                    } else {
                        this
                    }
                },
        )

        if (appDrawerSettings.gridItemSettings.showLabel) {
            Spacer(modifier = Modifier.height(10.dp))

            Text(
                modifier = Modifier.alpha(alpha),
                text = eblanApplicationInfo.customLabel ?: eblanApplicationInfo.label,
                color = textColor,
                textAlign = TextAlign.Center,
                maxLines = maxLines,
                fontSize = appDrawerSettings.gridItemSettings.textSize.sp,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@OptIn(ExperimentalUuidApi::class)
internal fun handleApplicationDrag(
    appDrawerSettings: AppDrawerSettings,
    currentPage: Int,
    drag: Drag,
    eblanApplicationInfo: EblanApplicationInfo,
    isLongPress: Boolean,
    isVisibleOverlay: Boolean,
    onDismiss: () -> Unit,
    onDraggingGridItem: () -> Unit,
    onUpdateGridItemSource: (GridItemSource) -> Unit,
    onUpdateIsDragging: (Boolean) -> Unit,
    onUpdateIsLongPress: (Boolean) -> Unit,
    onUpdateIsVisibleOverlay: (Boolean) -> Unit,
    onUpdatePopupMenu: (Boolean) -> Unit,
) {
    when (drag) {
        Drag.Dragging if isLongPress -> {
            onUpdatePopupMenu(false)

            onDismiss()

            val pagerScreenId = Uuid.random().toHexString()

            val data = GridItemData.ApplicationInfo(
                serialNumber = eblanApplicationInfo.serialNumber,
                componentName = eblanApplicationInfo.componentName,
                packageName = eblanApplicationInfo.packageName,
                icon = eblanApplicationInfo.icon,
                label = eblanApplicationInfo.label,
                customIcon = eblanApplicationInfo.customIcon,
                customLabel = eblanApplicationInfo.customLabel,
                index = -1,
                folderId = null,
            )

            val eblanAction = EblanAction(
                eblanActionType = EblanActionType.None,
                serialNumber = 0L,
                componentName = "",
            )

            val gridItem = GridItem(
                id = pagerScreenId,
                page = currentPage,
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

            onUpdateGridItemSource(GridItemSource.New(gridItem = gridItem))

            onUpdateIsDragging(true)

            onDraggingGridItem()
        }

        Drag.Cancel, Drag.End -> {
            if (isLongPress && isVisibleOverlay) {
                onUpdateIsVisibleOverlay(false)

                onUpdateIsLongPress(false)
            }
        }

        else -> Unit
    }
}

@OptIn(ExperimentalUuidApi::class)
internal suspend fun handleApplicationLongPress(
    applicationScreenId: String,
    eblanApplicationInfo: EblanApplicationInfo,
    graphicsLayer: GraphicsLayer,
    intOffset: IntOffset,
    intSize: IntSize,
    keyboardController: SoftwareKeyboardController?,
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

    onUpdateSharedElementKey(
        SharedElementKey(
            id = applicationScreenId,
            parent = SharedElementKey.Parent.SwipeY,
        ),
    )

    onUpdateEblanApplicationInfo(eblanApplicationInfo)

    onUpdateIsVisibleOverlay(true)

    onUpdatePopupMenu(true)

    onUpdateIsLongPress(true)

    keyboardController?.hide()
}

internal suspend fun handleApplicationTap(
    isTap: Boolean,
    isImeVisible: Boolean,
    appDrawerSettings: AppDrawerSettings,
    keyboardController: SoftwareKeyboardController?,
    onDismiss: () -> Unit,
    onScrollToItem: suspend (Int) -> Unit,
    onStartMainActivity: () -> Unit,
    onUpdateIsTap: (Boolean) -> Unit,
) {
    if (isTap && isImeVisible) {
        keyboardController?.hide()
    } else if (isTap) {
        if (appDrawerSettings.resetState) {
            onDismiss()

            onScrollToItem(0)
        }

        onStartMainActivity()

        onUpdateIsTap(false)
    }
}
