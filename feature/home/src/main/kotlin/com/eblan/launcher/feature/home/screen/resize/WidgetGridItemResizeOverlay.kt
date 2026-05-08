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
package com.eblan.launcher.feature.home.screen.resize

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.os.Bundle
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.eblan.launcher.domain.grid.isGridItemSpanWithinBounds
import com.eblan.launcher.domain.grid.resizeWidgetGridItemWithPixels
import com.eblan.launcher.domain.model.GridItem
import com.eblan.launcher.domain.model.GridItemData
import com.eblan.launcher.domain.model.SideAnchor
import com.eblan.launcher.feature.home.util.DRAG_HANDLE_SIZE
import com.eblan.launcher.ui.local.LocalAppWidgetManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
internal fun WidgetGridItemResizeOverlay(
    modifier: Modifier = Modifier,
    color: Color,
    columns: Int,
    data: GridItemData.Widget,
    gridHeight: Int,
    gridItem: GridItem,
    gridWidth: Int,
    height: Int,
    lockMovement: Boolean,
    rows: Int,
    width: Int,
    x: Int,
    y: Int,
    onResizeWidgetGridItem: (
        gridItem: GridItem,
        columns: Int,
        rows: Int,
    ) -> Unit,
) {
    val density = LocalDensity.current

    val scope = rememberCoroutineScope()

    val appWidgetManager = LocalAppWidgetManager.current

    val currentX = remember { Animatable(x.toFloat()) }

    val currentY = remember { Animatable(y.toFloat()) }

    val currentWidth = remember { Animatable(width.toFloat()) }

    val currentHeight = remember { Animatable(height.toFloat()) }

    var isResizing by remember {
        mutableStateOf(true)
    }

    var dragHandle by remember { mutableStateOf(Alignment.Center) }

    val dragHandleSizePx = with(density) {
        DRAG_HANDLE_SIZE.roundToPx()
    }

    val borderWidth by remember {
        derivedStateOf {
            with(density) {
                currentWidth.value.roundToInt().coerceAtLeast(dragHandleSizePx).toDp()
            }
        }
    }

    val borderHeight by remember {
        derivedStateOf {
            with(density) {
                currentHeight.value.roundToInt().coerceAtLeast(dragHandleSizePx).toDp()
            }
        }
    }

    val borderX by remember {
        derivedStateOf {
            if (dragHandle == Alignment.CenterStart) {
                if (currentWidth.value >= dragHandleSizePx) {
                    currentX.value.roundToInt()
                } else {
                    (x + width) - dragHandleSizePx
                }
            } else {
                currentX.value.roundToInt()
            }
        }
    }

    val borderY by remember {
        derivedStateOf {
            if (dragHandle == Alignment.TopCenter) {
                if (currentHeight.value >= dragHandleSizePx) {
                    currentY.value.roundToInt()
                } else {
                    (y + height) - dragHandleSizePx
                }
            } else {
                currentY.value.roundToInt()
            }
        }
    }

    val circleModifier =
        Modifier
            .size(DRAG_HANDLE_SIZE)
            .background(color = color, shape = CircleShape)

    LaunchedEffect(
        key1 = currentWidth.value,
        key2 = currentHeight.value,
    ) {
        val allowedWidth =
            if (data.minResizeWidth > 0 && currentWidth.value.roundToInt() <= data.minResizeWidth) {
                data.minResizeWidth
            } else if (data.maxResizeWidth in 1..<currentWidth.value.roundToInt()) {
                data.maxResizeWidth
            } else {
                currentWidth.value.roundToInt()
            }

        val allowedHeight =
            if (data.minResizeHeight > 0 && currentHeight.value.roundToInt() <= data.minResizeHeight) {
                data.minResizeHeight
            } else if (data.maxResizeHeight in 1..<currentHeight.value.roundToInt()) {
                data.maxResizeHeight
            } else {
                currentHeight.value.roundToInt()
            }

        val resizingGridItem = when (dragHandle) {
            Alignment.TopCenter -> {
                resizeWidgetGridItemWithPixels(
                    gridItem = gridItem,
                    width = width,
                    height = allowedHeight,
                    rows = rows,
                    columns = columns,
                    gridWidth = gridWidth,
                    gridHeight = gridHeight,
                    anchor = SideAnchor.Bottom,
                )
            }

            Alignment.CenterEnd -> {
                resizeWidgetGridItemWithPixels(
                    gridItem = gridItem,
                    width = allowedWidth,
                    height = height,
                    rows = rows,
                    columns = columns,
                    gridWidth = gridWidth,
                    gridHeight = gridHeight,
                    anchor = SideAnchor.Left,
                )
            }

            Alignment.BottomCenter -> {
                resizeWidgetGridItemWithPixels(
                    gridItem = gridItem,
                    width = width,
                    height = allowedHeight,
                    rows = rows,
                    columns = columns,
                    gridWidth = gridWidth,
                    gridHeight = gridHeight,
                    anchor = SideAnchor.Top,
                )
            }

            Alignment.CenterStart -> {
                resizeWidgetGridItemWithPixels(
                    gridItem = gridItem,
                    width = allowedWidth,
                    height = height,
                    rows = rows,
                    columns = columns,
                    gridWidth = gridWidth,
                    gridHeight = gridHeight,
                    anchor = SideAnchor.Right,
                )
            }

            else -> null
        }

        if (isResizing && resizingGridItem != null && isGridItemSpanWithinBounds(
                gridItem = resizingGridItem,
                columns = columns,
                rows = rows,
            ) && !lockMovement
        ) {
            delay(100L)

            val options = Bundle().apply {
                putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, data.minWidth)
                putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, data.minHeight)
                putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, data.minWidth)
                putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, data.minHeight)
            }

            appWidgetManager.updateAppWidgetOptions(
                appWidgetId = data.appWidgetId,
                options = options,
            )

            onResizeWidgetGridItem(
                resizingGridItem,
                columns,
                rows,
            )
        }
    }

    LaunchedEffect(key1 = isResizing) {
        if (!isResizing) {
            launch { currentX.animateTo(x.toFloat()) }
            launch { currentY.animateTo(y.toFloat()) }
            launch { currentWidth.animateTo(width.toFloat()) }
            launch { currentHeight.animateTo(height.toFloat()) }
        }
    }

    Box(
        modifier = modifier
            .offset {
                IntOffset(
                    x = borderX,
                    y = borderY,
                )
            }
            .size(width = borderWidth, height = borderHeight)
            .border(width = 2.dp, color = color),
    ) {
        Box(
            modifier = Modifier.run {
                if (data.resizeMode == AppWidgetProviderInfo.RESIZE_VERTICAL || data.resizeMode == AppWidgetProviderInfo.RESIZE_BOTH) {
                    align(Alignment.TopCenter)
                        .offset(y = (-15).dp)
                        .then(circleModifier)
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = {
                                    dragHandle = Alignment.TopCenter

                                    isResizing = true
                                },
                                onDragEnd = {
                                    isResizing = false
                                },
                                onDrag = { _, dragAmount ->
                                    scope.launch {
                                        currentHeight.snapTo(currentHeight.value - dragAmount.y)
                                        currentY.snapTo(currentY.value + dragAmount.y)
                                    }
                                },
                            )
                        }
                } else {
                    this
                }
            },
        )

        Box(
            modifier = Modifier.run {
                if (data.resizeMode == AppWidgetProviderInfo.RESIZE_HORIZONTAL || data.resizeMode == AppWidgetProviderInfo.RESIZE_BOTH) {
                    align(Alignment.CenterEnd)
                        .offset(15.dp)
                        .then(circleModifier)
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = {
                                    dragHandle = Alignment.CenterEnd

                                    isResizing = true
                                },
                                onDragEnd = {
                                    isResizing = false
                                },
                                onDrag = { _, dragAmount ->
                                    scope.launch {
                                        currentWidth.snapTo(currentWidth.value + dragAmount.x)
                                    }
                                },
                            )
                        }
                } else {
                    this
                }
            },
        )

        Box(
            modifier = Modifier.run {
                if (data.resizeMode == AppWidgetProviderInfo.RESIZE_VERTICAL || data.resizeMode == AppWidgetProviderInfo.RESIZE_BOTH) {
                    align(Alignment.BottomCenter)
                        .offset(y = 15.dp)
                        .then(circleModifier)
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = {
                                    dragHandle = Alignment.BottomCenter

                                    isResizing = true
                                },
                                onDragEnd = {
                                    isResizing = false
                                },
                                onDrag = { _, dragAmount ->
                                    scope.launch {
                                        currentHeight.snapTo(currentHeight.value + dragAmount.y)
                                    }
                                },
                            )
                        }
                } else {
                    this
                }
            },
        )

        Box(
            modifier = Modifier.run {
                if (data.resizeMode == AppWidgetProviderInfo.RESIZE_HORIZONTAL || data.resizeMode == AppWidgetProviderInfo.RESIZE_BOTH) {
                    align(Alignment.CenterStart)
                        .offset((-15).dp)
                        .then(circleModifier)
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = {
                                    dragHandle = Alignment.CenterStart

                                    isResizing = true
                                },
                                onDragEnd = {
                                    isResizing = false
                                },
                                onDrag = { _, dragAmount ->
                                    scope.launch {
                                        currentWidth.snapTo(currentWidth.value - dragAmount.x)
                                        currentX.snapTo(currentX.value + dragAmount.x)
                                    }
                                },
                            )
                        }
                } else {
                    this
                }
            },
        )
    }
}
