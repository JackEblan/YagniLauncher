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
package com.eblan.launcher.feature.home.screen.application.list

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
internal fun ScrollBarThumb(
    modifier: Modifier = Modifier,
    lazyListState: LazyListState,
    paddingValues: PaddingValues,
    onScrollToItem: suspend (
        index: Int,
        offset: Int,
    ) -> Unit,
) {
    val density = LocalDensity.current

    val scope = rememberCoroutineScope()

    val bottomPadding = with(density) {
        paddingValues.calculateBottomPadding().roundToPx()
    }

    val thumbHeight by remember(lazyListState) {
        derivedStateOf {
            with(density) {
                (lazyListState.layoutInfo.viewportSize.height / 4).toDp()
            }
        }
    }

    val viewPortThumbY by remember(lazyListState) {
        derivedStateOf {
            getViewPortThumbY(
                lazyListState = lazyListState,
                density = density,
                thumbHeight = thumbHeight,
                bottomPadding = bottomPadding,
            )
        }
    }

    var isDraggingThumb by remember { mutableStateOf(false) }

    var thumbY by remember { mutableFloatStateOf(0f) }

    val thumbAlpha by animateFloatAsState(
        targetValue = if (lazyListState.isScrollInProgress || isDraggingThumb) 1f else 0.2f,
    )

    Row(modifier = modifier) {
        Box(
            modifier = Modifier
                .offset {
                    val y = if (isDraggingThumb) thumbY else viewPortThumbY

                    IntOffset(0, y.roundToInt())
                }
                .pointerInput(lazyListState) {
                    detectDragGestures(
                        onDragStart = {
                            thumbY = viewPortThumbY

                            isDraggingThumb = true
                        },
                        onDrag = { _, dragAmount ->
                            handleVerticalDrag(
                                lazyListState = lazyListState,
                                density = density,
                                thumbHeight = thumbHeight,
                                bottomPadding = bottomPadding,
                                thumbY = thumbY,
                                deltaY = dragAmount.y,
                                scope = scope,
                                onScrollToItem = onScrollToItem,
                                onUpdateThumbY = { newThumbY ->
                                    thumbY = newThumbY
                                },
                            )
                        },
                        onDragEnd = { isDraggingThumb = false },
                        onDragCancel = { isDraggingThumb = false },
                    )
                }
                .alpha(thumbAlpha)
                .size(width = 10.dp, height = thumbHeight)
                .background(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(10.dp),
                ),
        )
    }
}

private fun handleVerticalDrag(
    lazyListState: LazyListState,
    density: Density,
    thumbHeight: Dp,
    bottomPadding: Int,
    thumbY: Float,
    deltaY: Float,
    scope: CoroutineScope,
    onScrollToItem: suspend (index: Int, offset: Int) -> Unit,
    onUpdateThumbY: (Float) -> Unit,
) {
    if (deltaY == 0f) return

    val layoutInfo = lazyListState.layoutInfo
    val visibleItems = layoutInfo.visibleItemsInfo

    val avgItemSize = visibleItems.sumOf { it.size } / visibleItems.size

    val totalItems = layoutInfo.totalItemsCount
    val viewportHeight = layoutInfo.viewportSize.height

    val thumbHeightPx = with(density) { thumbHeight.toPx() }

    val availableHeight = (viewportHeight - thumbHeightPx - bottomPadding)
        .coerceAtLeast(0f)

    val newThumbY = (thumbY + deltaY)
        .coerceIn(0f, availableHeight)

    val progress = if (availableHeight > 0f) {
        (newThumbY / availableHeight).coerceIn(0f, 1f)
    } else {
        0f
    }

    val totalContentHeight = totalItems * avgItemSize
    val scrollableHeight = (totalContentHeight - viewportHeight)
        .coerceAtLeast(0)

    val targetScrollY = (progress * scrollableHeight)
        .coerceIn(0f, scrollableHeight.toFloat())

    val targetIndex = (targetScrollY / avgItemSize)
        .toInt()
        .coerceIn(0, totalItems - 1)

    val offset = (targetScrollY % avgItemSize)
        .toInt()
        .coerceAtLeast(0)

    onUpdateThumbY(newThumbY)

    scope.launch {
        onScrollToItem(targetIndex, offset)
    }
}

private fun getViewPortThumbY(
    lazyListState: LazyListState,
    density: Density,
    thumbHeight: Dp,
    bottomPadding: Int,
): Float {
    val layoutInfo = lazyListState.layoutInfo
    val visibleItems = layoutInfo.visibleItemsInfo

    val firstItem = visibleItems.first()

    val visibleHeight = visibleItems.sumOf { it.size }
    val avgItemSize = visibleHeight / visibleItems.size

    val totalItems = layoutInfo.totalItemsCount
    val totalContentHeight = totalItems * avgItemSize

    val scrollY = (firstItem.index * avgItemSize) - firstItem.offset

    val viewportHeight = layoutInfo.viewportSize.height.toFloat()

    val scrollableHeight = (totalContentHeight - viewportHeight)
        .coerceAtLeast(0f)

    val progress = if (scrollableHeight > 0f) {
        (scrollY / scrollableHeight).coerceIn(0f, 1f)
    } else {
        0f
    }

    val thumbHeightPx = with(density) { thumbHeight.toPx() }

    val availableHeight = (viewportHeight - thumbHeightPx - bottomPadding)
        .coerceAtLeast(0f)

    return (progress * availableHeight)
        .coerceIn(0f, availableHeight)
}
