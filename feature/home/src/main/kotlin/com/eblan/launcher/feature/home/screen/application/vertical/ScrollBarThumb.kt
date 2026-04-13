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
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.eblan.launcher.domain.model.AppDrawerSettings
import kotlinx.coroutines.launch
import kotlin.math.ceil
import kotlin.math.roundToInt

@Composable
internal fun ScrollBarThumb(
    modifier: Modifier = Modifier,
    appDrawerSettings: AppDrawerSettings,
    lazyGridState: LazyGridState,
    paddingValues: PaddingValues,
    onScrollToItem: suspend (Int) -> Unit,
) {
    val density = LocalDensity.current

    val scope = rememberCoroutineScope()

    val appDrawerRowsHeightPx = with(density) {
        appDrawerSettings.appDrawerRowsHeight.dp.roundToPx()
    }

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
            val totalRows =
                (lazyGridState.layoutInfo.totalItemsCount + appDrawerSettings.appDrawerColumns - 1) / appDrawerSettings.appDrawerColumns

            val visibleRows =
                ceil(lazyGridState.layoutInfo.viewportSize.height / appDrawerRowsHeightPx.toFloat()).toInt()

            val scrollableRows = (totalRows - visibleRows).coerceAtLeast(0)

            val availableScroll = scrollableRows * appDrawerRowsHeightPx

            val row = lazyGridState.firstVisibleItemIndex / appDrawerSettings.appDrawerColumns

            val totalScrollY =
                (row * appDrawerRowsHeightPx) + lazyGridState.firstVisibleItemScrollOffset

            val thumbHeightPx = with(density) {
                thumbHeight.toPx()
            }

            val availableHeight =
                (lazyGridState.layoutInfo.viewportSize.height - thumbHeightPx - bottomPadding).coerceAtLeast(
                    0f,
                )

            if (availableScroll <= 0) {
                0f
            } else {
                (totalScrollY.toFloat() / availableScroll.toFloat() * availableHeight).coerceIn(
                    0f,
                    availableHeight,
                )
            }
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
                    detectVerticalDragGestures(
                        onDragStart = {
                            thumbY = viewPortThumbY

                            isDraggingThumb = true
                        },
                        onVerticalDrag = { _, deltaY ->
                            val totalRows =
                                (lazyGridState.layoutInfo.totalItemsCount + appDrawerSettings.appDrawerColumns - 1) / appDrawerSettings.appDrawerColumns

                            val visibleRows =
                                ceil(lazyGridState.layoutInfo.viewportSize.height / appDrawerRowsHeightPx.toFloat()).toInt()

                            val scrollableRows = (totalRows - visibleRows).coerceAtLeast(0)

                            val availableScroll = scrollableRows * appDrawerRowsHeightPx

                            val thumbHeightPx = with(density) { thumbHeight.toPx() }

                            val availableHeight =
                                lazyGridState.layoutInfo.viewportSize.height - thumbHeightPx - bottomPadding

                            thumbY = (thumbY + deltaY).coerceIn(0f, availableHeight)

                            val progress = thumbY / availableHeight

                            val targetScrollY = progress * availableScroll

                            val targetRow = targetScrollY / appDrawerRowsHeightPx

                            val targetIndex =
                                (targetRow * appDrawerSettings.appDrawerColumns).roundToInt()
                                    .coerceIn(0, lazyGridState.layoutInfo.totalItemsCount)

                            scope.launch {
                                onScrollToItem(targetIndex)
                            }
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
                .size(width = 8.dp, height = thumbHeight)
                .background(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(10.dp),
                ),
        )
    }
}
