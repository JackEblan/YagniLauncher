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
package com.eblan.launcher.feature.home.screen.application.vertical

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.LazyGridState
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
    appDrawerColumns: Int,
    lazyGridState: LazyGridState,
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

    val thumbHeight by remember(lazyGridState) {
        derivedStateOf {
            with(density) {
                (lazyGridState.layoutInfo.viewportSize.height / 4).toDp()
            }
        }
    }

    val viewPortThumbY by remember(key1 = lazyGridState) {
        derivedStateOf {
            getViewPortThumbY(
                lazyGridState = lazyGridState,
                appDrawerColumns = appDrawerColumns,
                density = density,
                thumbHeight = thumbHeight,
                bottomPadding = bottomPadding,
            )
        }
    }

    var isDraggingThumb by remember { mutableStateOf(false) }

    var thumbY by remember { mutableFloatStateOf(0f) }

    val thumbAlpha by animateFloatAsState(
        targetValue = if (lazyGridState.isScrollInProgress || isDraggingThumb) 1f else 0.2f,
    )

    Row(modifier = modifier) {
        Box(
            modifier = Modifier
                .offset {
                    val y = if (isDraggingThumb) {
                        thumbY
                    } else {
                        viewPortThumbY
                    }

                    IntOffset(0, y.roundToInt())
                }
                .pointerInput(key1 = lazyGridState) {
                    detectDragGestures(
                        onDragStart = {
                            thumbY = viewPortThumbY

                            isDraggingThumb = true
                        },
                        onDrag = { _, dragAmount ->
                            handleVerticalDrag(
                                lazyGridState = lazyGridState,
                                appDrawerColumns = appDrawerColumns,
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
                        onDragEnd = {
                            isDraggingThumb = false
                        },
                        onDragCancel = {
                            isDraggingThumb = false
                        },
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
    lazyGridState: LazyGridState,
    appDrawerColumns: Int,
    density: Density,
    thumbHeight: Dp,
    bottomPadding: Int,
    thumbY: Float,
    deltaY: Float,
    scope: CoroutineScope,
    onScrollToItem: suspend (
        index: Int,
        offset: Int,
    ) -> Unit,
    onUpdateThumbY: (Float) -> Unit,
) {
    if (deltaY == 0f) return

    val layoutInfo = lazyGridState.layoutInfo
    val visibleItems = layoutInfo.visibleItemsInfo

    val avgItemHeight = visibleItems.sumOf { it.size.height } / visibleItems.size

    val totalItems = layoutInfo.totalItemsCount
    val totalRows = (totalItems + appDrawerColumns - 1) / appDrawerColumns

    val viewportHeight = layoutInfo.viewportSize.height

    val thumbHeightPx = with(density) { thumbHeight.toPx() }

    val availableHeight = viewportHeight - thumbHeightPx - bottomPadding

    val newThumbY = (thumbY + deltaY).coerceIn(0f, availableHeight)

    val progress = newThumbY / availableHeight

    val totalContentHeight = totalRows * avgItemHeight
    val scrollableHeight = totalContentHeight - viewportHeight

    val targetScrollY = progress * scrollableHeight

    val targetRow = targetScrollY / avgItemHeight
    val rowInt = targetRow.toInt()

    val offsetInRow = (targetScrollY % avgItemHeight).toInt()

    val targetIndex = rowInt * appDrawerColumns

    onUpdateThumbY(newThumbY)

    scope.launch {
        onScrollToItem(
            targetIndex,
            offsetInRow,
        )
    }
}

private fun getViewPortThumbY(
    lazyGridState: LazyGridState,
    appDrawerColumns: Int,
    density: Density,
    thumbHeight: Dp,
    bottomPadding: Int,
): Float {
    val layoutInfo = lazyGridState.layoutInfo
    val visibleItems = layoutInfo.visibleItemsInfo

    val firstItem = visibleItems.first()

    val avgItemHeight = visibleItems.sumOf { it.size.height } / visibleItems.size

    val totalItems = layoutInfo.totalItemsCount
    val totalRows = (totalItems + appDrawerColumns - 1) / appDrawerColumns

    val viewportHeight = layoutInfo.viewportSize.height.toFloat()

    val totalContentHeight = totalRows * avgItemHeight

    val firstRow = firstItem.index / appDrawerColumns

    val scrollY = (firstRow * avgItemHeight) - firstItem.offset.y

    val scrollableHeight = totalContentHeight - viewportHeight

    val progress = scrollY / scrollableHeight

    val thumbHeightPx = with(density) { thumbHeight.toPx() }

    val availableHeight = viewportHeight - thumbHeightPx - bottomPadding

    return progress * availableHeight
}
