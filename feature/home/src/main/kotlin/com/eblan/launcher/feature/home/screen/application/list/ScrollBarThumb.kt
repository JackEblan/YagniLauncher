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
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
internal fun ScrollBarThumb(
    modifier: Modifier = Modifier,
    lazyListState: LazyListState,
    paddingValues: PaddingValues,
    onScrollToItem: suspend (Int) -> Unit,
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
            val totalItems = lazyListState.layoutInfo.totalItemsCount

            val visibleItems = lazyListState.layoutInfo.visibleItemsInfo.size

            val scrollableItems = (totalItems - visibleItems).coerceAtLeast(0)

            val firstVisibleItemIndex = lazyListState.firstVisibleItemIndex
            val firstVisibleItemScrollOffset = lazyListState.firstVisibleItemScrollOffset

            val size =
                lazyListState.layoutInfo.visibleItemsInfo.firstOrNull()?.size ?: 1

            val totalScrollY = (firstVisibleItemIndex * size) + firstVisibleItemScrollOffset

            val availableScroll = scrollableItems * size

            val thumbHeightPx = with(density) { thumbHeight.toPx() }

            val availableHeight =
                (lazyListState.layoutInfo.viewportSize.height - thumbHeightPx - bottomPadding)
                    .coerceAtLeast(0f)

            if (availableScroll <= 0) {
                0f
            } else {
                (totalScrollY.toFloat() / availableScroll.toFloat() * availableHeight)
                    .coerceIn(0f, availableHeight)
            }
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
                    detectVerticalDragGestures(
                        onDragStart = {
                            thumbY = viewPortThumbY

                            isDraggingThumb = true
                        },
                        onVerticalDrag = { _, deltaY ->
                            val totalItems = lazyListState.layoutInfo.totalItemsCount
                            val visibleItems = lazyListState.layoutInfo.visibleItemsInfo.size

                            val avgItemSize =
                                lazyListState.layoutInfo.visibleItemsInfo.firstOrNull()?.size ?: 1

                            val scrollableItems =
                                (totalItems - visibleItems).coerceAtLeast(0)

                            val availableScroll = scrollableItems * avgItemSize

                            val thumbHeightPx = with(density) { thumbHeight.toPx() }

                            val availableHeight =
                                lazyListState.layoutInfo.viewportSize.height -
                                    thumbHeightPx - bottomPadding

                            thumbY = (thumbY + deltaY).coerceIn(0f, availableHeight)

                            val progress = thumbY / availableHeight
                            val targetScrollY = progress * availableScroll

                            val targetIndex =
                                (targetScrollY / avgItemSize).toInt()
                                    .coerceIn(0, totalItems)

                            scope.launch {
                                onScrollToItem(targetIndex)
                            }
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
