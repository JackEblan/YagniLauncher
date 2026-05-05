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
package com.eblan.launcher.feature.home.screen.widget

import androidx.activity.compose.BackHandler
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import coil3.compose.AsyncImage
import com.eblan.launcher.domain.model.EblanAppWidgetProviderInfo
import com.eblan.launcher.domain.model.EblanApplicationInfoGroup
import com.eblan.launcher.domain.model.GridItemSettings
import com.eblan.launcher.feature.home.model.Drag
import com.eblan.launcher.feature.home.model.GridItemSource
import com.eblan.launcher.feature.home.model.SharedElementKey
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun AppWidgetScreen(
    modifier: Modifier = Modifier,
    columns: Int,
    currentPage: Int,
    drag: Drag,
    eblanAppWidgetProviderInfosGroup: Map<String, List<EblanAppWidgetProviderInfo>>,
    eblanApplicationInfoGroup: EblanApplicationInfoGroup?,
    gridItemSettings: GridItemSettings,
    isPressHome: Boolean,
    paddingValues: PaddingValues,
    rows: Int,
    screenHeight: Int,
    screenWidth: Int,
    offsetY: Float,
    onDismiss: () -> Unit,
    onDismissApplicationScreen: () -> Unit,
    onUpdateOverlayBounds: (
        intOffset: IntOffset,
        intSize: IntSize,
    ) -> Unit,
    onUpdateImageBitmap: (ImageBitmap) -> Unit,
    onUpdateGridItemSource: (GridItemSource) -> Unit,
    onUpdateSharedElementKey: (SharedElementKey?) -> Unit,
    onUpdateIsDragging: (Boolean) -> Unit,
    onVerticalDrag: (Float) -> Unit,
    onDragEnd: () -> Unit,
    onUpdateIsVisibleOverlay: (Boolean) -> Unit,
) {
    if (eblanApplicationInfoGroup == null) return

    LaunchedEffect(key1 = isPressHome) {
        if (isPressHome && offsetY < screenHeight.toFloat()) {
            onDismiss()
        }
    }

    BackHandler(enabled = offsetY < screenHeight.toFloat()) {
        onDismiss()
    }

    Box(
        modifier = modifier
            .offset {
                IntOffset(x = 0, y = offsetY.roundToInt())
            }
            .pointerInput(key1 = Unit) {
                detectTapGestures(
                    onTap = {
                        onDismiss()
                    },
                )
            }
            .fillMaxSize(),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp)),
        ) {
            Success(
                modifier = Modifier
                    .pointerInput(key1 = Unit) {
                        detectVerticalDragGestures(
                            onVerticalDrag = { _, dragAmount ->
                                onVerticalDrag(dragAmount)
                            },
                            onDragEnd = {
                                onDragEnd()
                            },
                            onDragCancel = {
                                onDragEnd()
                            },
                        )
                    }
                    .fillMaxWidth()
                    .padding(paddingValues),
                columns = columns,
                currentPage = currentPage,
                drag = drag,
                eblanAppWidgetProviderInfos = eblanAppWidgetProviderInfosGroup[eblanApplicationInfoGroup.packageName].orEmpty(),
                eblanApplicationInfoGroup = eblanApplicationInfoGroup,
                gridItemSettings = gridItemSettings,
                rows = rows,
                screenHeight = screenHeight,
                screenWidth = screenWidth,
                onUpdateOverlayBounds = onUpdateOverlayBounds,
                onUpdateImageBitmap = onUpdateImageBitmap,
                onUpdateGridItemSource = onUpdateGridItemSource,
                onUpdateSharedElementKey = onUpdateSharedElementKey,
                onDismiss = onDismiss,
                onDismissApplicationScreen = onDismissApplicationScreen,
                onUpdateIsDragging = onUpdateIsDragging,
                onUpdateIsVisibleOverlay = onUpdateIsVisibleOverlay,
            )
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun Success(
    modifier: Modifier = Modifier,
    columns: Int,
    currentPage: Int,
    drag: Drag,
    eblanAppWidgetProviderInfos: List<EblanAppWidgetProviderInfo>,
    eblanApplicationInfoGroup: EblanApplicationInfoGroup,
    gridItemSettings: GridItemSettings,
    rows: Int,
    screenHeight: Int,
    screenWidth: Int,
    onUpdateOverlayBounds: (IntOffset, IntSize) -> Unit,
    onUpdateImageBitmap: (ImageBitmap) -> Unit,
    onUpdateGridItemSource: (GridItemSource) -> Unit,
    onUpdateSharedElementKey: (SharedElementKey?) -> Unit,
    onDismiss: () -> Unit,
    onDismissApplicationScreen: () -> Unit,
    onUpdateIsDragging: (Boolean) -> Unit,
    onUpdateIsVisibleOverlay: (Boolean) -> Unit,
) {
    val lazyListState = rememberLazyListState()

    Column(
        modifier = modifier.animateContentSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AsyncImage(
            model = eblanApplicationInfoGroup.icon,
            contentDescription = null,
            modifier = Modifier.size(40.dp),
        )

        Spacer(modifier = Modifier.height(5.dp))

        Text(text = eblanApplicationInfoGroup.label.toString())

        Spacer(modifier = Modifier.height(5.dp))

        LazyRow(
            state = lazyListState,
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
        ) {
            items(eblanAppWidgetProviderInfos) { eblanAppWidgetProviderInfo ->
                EblanAppWidgetProviderInfoItem(
                    columns = columns,
                    currentPage = currentPage,
                    drag = drag,
                    eblanAppWidgetProviderInfo = eblanAppWidgetProviderInfo,
                    gridItemSettings = gridItemSettings,
                    rows = rows,
                    screenHeight = screenHeight,
                    screenWidth = screenWidth,
                    onUpdateOverlayBounds = onUpdateOverlayBounds,
                    onUpdateImageBitmap = onUpdateImageBitmap,
                    onUpdateGridItemSource = onUpdateGridItemSource,
                    onUpdateSharedElementKey = onUpdateSharedElementKey,
                    onDismiss = onDismiss,
                    onDismissApplicationScreen = onDismissApplicationScreen,
                    onUpdateIsDragging = onUpdateIsDragging,
                    onUpdateIsVisibleOverlay = onUpdateIsVisibleOverlay,
                )
            }
        }
    }
}

@OptIn(ExperimentalUuidApi::class, ExperimentalSharedTransitionApi::class)
@Composable
private fun EblanAppWidgetProviderInfoItem(
    modifier: Modifier = Modifier,
    columns: Int,
    currentPage: Int,
    drag: Drag,
    eblanAppWidgetProviderInfo: EblanAppWidgetProviderInfo,
    gridItemSettings: GridItemSettings,
    rows: Int,
    screenHeight: Int,
    screenWidth: Int,
    onUpdateOverlayBounds: (
        intOffset: IntOffset,
        intSize: IntSize,
    ) -> Unit,
    onUpdateImageBitmap: (ImageBitmap) -> Unit,
    onUpdateGridItemSource: (GridItemSource) -> Unit,
    onUpdateSharedElementKey: (SharedElementKey?) -> Unit,
    onDismiss: () -> Unit,
    onDismissApplicationScreen: () -> Unit,
    onUpdateIsDragging: (Boolean) -> Unit,
    onUpdateIsVisibleOverlay: (Boolean) -> Unit,
) {
    val scope = rememberCoroutineScope()

    var intOffset by remember { mutableStateOf(IntOffset.Zero) }

    var intSize by remember { mutableStateOf(IntSize.Zero) }

    val preview = eblanAppWidgetProviderInfo.preview ?: eblanAppWidgetProviderInfo.applicationIcon

    val graphicsLayer = rememberGraphicsLayer()

    val id = remember { Uuid.random().toHexString() }

    Column(
        modifier = modifier
            .pointerInput(key1 = drag) {
                detectTapGestures(
                    onLongPress = {
                        scope.launch {
                            val gridItem = getWidgetGridItem(
                                componentName = eblanAppWidgetProviderInfo.componentName,
                                configure = eblanAppWidgetProviderInfo.configure,
                                gridItemSettings = gridItemSettings,
                                icon = eblanAppWidgetProviderInfo.applicationIcon,
                                id = id,
                                label = eblanAppWidgetProviderInfo.applicationLabel,
                                maxResizeHeight = eblanAppWidgetProviderInfo.maxResizeHeight,
                                maxResizeWidth = eblanAppWidgetProviderInfo.maxResizeWidth,
                                minHeight = eblanAppWidgetProviderInfo.minHeight,
                                minResizeHeight = eblanAppWidgetProviderInfo.minResizeHeight,
                                minResizeWidth = eblanAppWidgetProviderInfo.minResizeWidth,
                                minWidth = eblanAppWidgetProviderInfo.minWidth,
                                packageName = eblanAppWidgetProviderInfo.packageName,
                                page = currentPage,
                                preview = eblanAppWidgetProviderInfo.preview,
                                resizeMode = eblanAppWidgetProviderInfo.resizeMode,
                                serialNumber = eblanAppWidgetProviderInfo.serialNumber,
                                targetCellHeight = eblanAppWidgetProviderInfo.targetCellHeight,
                                targetCellWidth = eblanAppWidgetProviderInfo.targetCellWidth,
                            )

                            onUpdateGridItemSource(GridItemSource.New(gridItem = gridItem))

                            onUpdateImageBitmap(graphicsLayer.toImageBitmap())

                            onUpdateOverlayBounds(
                                intOffset,
                                intSize,
                            )

                            onUpdateSharedElementKey(
                                SharedElementKey(
                                    id = id,
                                    parent = SharedElementKey.Parent.Grid,
                                ),
                            )

                            onUpdateIsVisibleOverlay(true)

                            onDismiss()

                            onDismissApplicationScreen()

                            onUpdateIsDragging(true)
                        }
                    },
                )
            }
            .size(200.dp)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        val text =
            if (eblanAppWidgetProviderInfo.targetCellWidth > 0 && eblanAppWidgetProviderInfo.targetCellHeight > 0) {
                "${eblanAppWidgetProviderInfo.targetCellWidth}x${eblanAppWidgetProviderInfo.targetCellHeight}"
            } else if (eblanAppWidgetProviderInfo.minWidth > 0 && eblanAppWidgetProviderInfo.minHeight > 0) {
                val cellWidth = screenWidth / columns

                val cellHeight = screenHeight / rows

                val spanX = (eblanAppWidgetProviderInfo.minWidth + cellWidth - 1) / cellWidth

                val spanY = (eblanAppWidgetProviderInfo.minHeight + cellHeight - 1) / cellHeight

                "${spanX}x$spanY"
            } else {
                null
            }

        eblanAppWidgetProviderInfo.label?.let { label ->
            Text(
                text = label,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge,
            )

            Spacer(modifier = Modifier.height(10.dp))
        }

        eblanAppWidgetProviderInfo.description?.let { description ->
            Text(
                text = description,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodySmall,
            )

            Spacer(modifier = Modifier.height(10.dp))
        }

        if (text != null) {
            Text(
                text = text,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodySmall,
            )

            Spacer(modifier = Modifier.height(10.dp))
        }

        AsyncImage(
            modifier = Modifier
                .drawWithContent {
                    graphicsLayer.record {
                        this@drawWithContent.drawContent()
                    }

                    drawLayer(graphicsLayer)
                }
                .onGloballyPositioned { layoutCoordinates ->
                    intOffset = layoutCoordinates.positionInRoot().round()

                    intSize = layoutCoordinates.size
                },
            model = preview,
            contentDescription = null,
        )
    }
}
