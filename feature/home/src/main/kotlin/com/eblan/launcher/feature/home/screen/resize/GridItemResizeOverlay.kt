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
import com.eblan.launcher.domain.grid.resizeGridItemWithPixels
import com.eblan.launcher.domain.model.Anchor
import com.eblan.launcher.domain.model.GridItem
import com.eblan.launcher.feature.home.util.DRAG_HANDLE_SIZE
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
internal fun GridItemResizeOverlay(
    modifier: Modifier = Modifier,
    cellHeight: Int,
    cellWidth: Int,
    color: Color,
    columns: Int,
    gridHeight: Int,
    gridItem: GridItem,
    gridWidth: Int,
    height: Int,
    lockMovement: Boolean,
    rows: Int,
    width: Int,
    x: Int,
    y: Int,
    onResizeGridItem: (
        gridItem: GridItem,
        columns: Int,
        rows: Int,
    ) -> Unit,
) {
    val density = LocalDensity.current

    val scope = rememberCoroutineScope()

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
            when (dragHandle) {
                Alignment.TopStart -> {
                    if (currentWidth.value >= dragHandleSizePx) {
                        currentX.value.roundToInt()
                    } else {
                        (x + width) - dragHandleSizePx
                    }
                }

                Alignment.TopEnd -> {
                    if (currentWidth.value >= dragHandleSizePx) {
                        currentX.value.roundToInt()
                    } else {
                        x
                    }
                }

                Alignment.BottomStart -> {
                    if (currentWidth.value >= dragHandleSizePx) {
                        currentX.value.roundToInt()
                    } else {
                        (x + width) - dragHandleSizePx
                    }
                }

                else -> {
                    if (currentWidth.value >= dragHandleSizePx) {
                        currentX.value.roundToInt()
                    } else {
                        x
                    }
                }
            }
        }
    }

    val borderY by remember {
        derivedStateOf {
            when (dragHandle) {
                Alignment.TopStart -> {
                    if (currentHeight.value >= dragHandleSizePx) {
                        currentY.value.roundToInt()
                    } else {
                        (y + height) - dragHandleSizePx
                    }
                }

                Alignment.TopEnd -> {
                    if (currentHeight.value >= dragHandleSizePx) {
                        currentY.value.roundToInt()
                    } else {
                        (y + height) - dragHandleSizePx
                    }
                }

                Alignment.BottomStart -> {
                    if (currentHeight.value >= dragHandleSizePx) {
                        currentY.value.roundToInt()
                    } else {
                        y
                    }
                }

                else -> {
                    if (currentHeight.value >= dragHandleSizePx) {
                        currentY.value.roundToInt()
                    } else {
                        y
                    }
                }
            }
        }
    }

    val circleModifier = Modifier
        .size(DRAG_HANDLE_SIZE)
        .background(color = color, shape = CircleShape)

    LaunchedEffect(
        key1 = currentWidth.value,
        key2 = currentHeight.value,
    ) {
        val allowedWidth = currentWidth.value.roundToInt().coerceAtLeast(cellWidth)

        val allowedHeight = currentHeight.value.roundToInt().coerceAtLeast(cellHeight)

        val resizingGridItem = when (dragHandle) {
            Alignment.TopStart -> {
                resizeGridItemWithPixels(
                    gridItem = gridItem,
                    width = allowedWidth,
                    height = allowedHeight,
                    rows = rows,
                    columns = columns,
                    gridWidth = gridWidth,
                    gridHeight = gridHeight,
                    anchor = Anchor.BottomEnd,
                )
            }

            Alignment.TopEnd -> {
                resizeGridItemWithPixels(
                    gridItem = gridItem,
                    width = allowedWidth,
                    height = allowedHeight,
                    rows = rows,
                    columns = columns,
                    gridWidth = gridWidth,
                    gridHeight = gridHeight,
                    anchor = Anchor.BottomStart,
                )
            }

            Alignment.BottomStart -> {
                resizeGridItemWithPixels(
                    gridItem = gridItem,
                    width = allowedWidth,
                    height = allowedHeight,
                    rows = rows,
                    columns = columns,
                    gridWidth = gridWidth,
                    gridHeight = gridHeight,
                    anchor = Anchor.TopEnd,
                )
            }

            Alignment.BottomEnd -> {
                resizeGridItemWithPixels(
                    gridItem = gridItem,
                    width = allowedWidth,
                    height = allowedHeight,
                    rows = rows,
                    columns = columns,
                    gridWidth = gridWidth,
                    gridHeight = gridHeight,
                    anchor = Anchor.TopStart,
                )
            }

            else -> null
        }

        if (isResizing &&
            resizingGridItem != null &&
            isGridItemSpanWithinBounds(
                gridItem = resizingGridItem,
                columns = columns,
                rows = rows,
            ) && !lockMovement
        ) {
            delay(100L)

            onResizeGridItem(
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
            .size(
                width = borderWidth,
                height = borderHeight,
            )
            .border(width = 2.dp, color = color),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset((-15).dp, (-15).dp)
                .then(circleModifier)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = {
                            dragHandle = Alignment.TopStart

                            isResizing = true
                        },
                        onDragEnd = {
                            isResizing = false
                        },
                        onDrag = { _, dragAmount ->
                            scope.launch {
                                currentWidth.snapTo(currentWidth.value - dragAmount.x)

                                currentHeight.snapTo(currentHeight.value - dragAmount.y)

                                currentX.snapTo(currentX.value + dragAmount.x)

                                currentY.snapTo(currentY.value + dragAmount.y)
                            }
                        },
                    )
                },
        )

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(15.dp, (-15).dp)
                .then(circleModifier)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = {
                            dragHandle = Alignment.TopEnd

                            isResizing = true
                        },
                        onDragEnd = {
                            isResizing = false
                        },
                        onDrag = { _, dragAmount ->
                            scope.launch {
                                currentWidth.snapTo(currentWidth.value + dragAmount.x)

                                currentHeight.snapTo(currentHeight.value - dragAmount.y)

                                currentY.snapTo(currentY.value + dragAmount.y)
                            }
                        },
                    )
                },
        )

        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset((-15).dp, 15.dp)
                .then(circleModifier)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = {
                            dragHandle = Alignment.BottomStart

                            isResizing = true
                        },
                        onDragEnd = {
                            isResizing = false
                        },
                        onDrag = { _, dragAmount ->
                            scope.launch {
                                currentWidth.snapTo(currentWidth.value - dragAmount.x)

                                currentHeight.snapTo(currentHeight.value + dragAmount.y)

                                currentX.snapTo(currentX.value + dragAmount.x)
                            }
                        },
                    )
                },
        )

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(15.dp, 15.dp)
                .then(circleModifier)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = {
                            dragHandle = Alignment.BottomEnd

                            isResizing = true
                        },
                        onDragEnd = {
                            isResizing = false
                        },
                        onDrag = { _, dragAmount ->
                            scope.launch {
                                currentWidth.snapTo(currentWidth.value + dragAmount.x)

                                currentHeight.snapTo(currentHeight.value + dragAmount.y)
                            }
                        },
                    )
                },
        )
    }
}
