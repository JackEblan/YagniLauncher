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
package com.eblan.launcher.feature.home.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import com.eblan.launcher.designsystem.icon.EblanLauncherIcons
import com.eblan.launcher.feature.home.util.calculatePage
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.onEach
import kotlin.math.abs

@OptIn(FlowPreview::class)
@Composable
internal fun GridPagerIndicator(
    modifier: Modifier = Modifier,
    color: Color,
    gridHorizontalPagerState: PagerState,
    infiniteScroll: Boolean,
    pageCount: Int,
) {
    var isScrollInProgress by remember { mutableStateOf(false) }

    LaunchedEffect(key1 = gridHorizontalPagerState) {
        snapshotFlow { gridHorizontalPagerState.isScrollInProgress }
            .debounce(100L)
            .onEach { newIsScrollInProgress ->
                isScrollInProgress = newIsScrollInProgress
            }.collect()
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        if (isScrollInProgress) {
            PageIndicator(
                color = color,
                gridHorizontalPagerState = gridHorizontalPagerState,
                infiniteScroll = infiniteScroll,
                pageCount = pageCount,
            )
        } else {
            Image(
                imageVector = EblanLauncherIcons.KeyboardArrowUp,
                contentDescription = null,
                colorFilter = ColorFilter.tint(color = color),
            )
        }
    }
}

@Composable
internal fun PageIndicator(
    modifier: Modifier = Modifier,
    color: Color,
    gridHorizontalPagerState: PagerState,
    infiniteScroll: Boolean,
    pageCount: Int,
) {
    val baseWidth = 8.dp
    val baseHeight = 8.dp
    val activeWidth = 16.dp

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (pageCount > 1) {
            repeat(pageCount) { index ->
                val distance by remember(key1 = index) {
                    derivedStateOf {
                        val currentPage = calculatePage(
                            index = gridHorizontalPagerState.currentPage,
                            infiniteScroll = infiniteScroll,
                            pageCount = pageCount,
                        )

                        val relative =
                            (currentPage + gridHorizontalPagerState.currentPageOffsetFraction) - index

                        relative.coerceIn(-1f, 1f)
                    }
                }

                val width by remember {
                    derivedStateOf {
                        when (distance) {
                            0f -> activeWidth
                            in -1f..0f -> baseWidth + (activeWidth - baseWidth) * (1f + distance)
                            in 0f..1f -> baseWidth + (activeWidth - baseWidth) * (1f - distance)
                            else -> baseWidth
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .padding(3.dp)
                        .width(width)
                        .height(baseHeight)
                        .clip(CircleShape)
                        .background(
                            color = lerp(
                                start = color.copy(alpha = 0.5f),
                                stop = color,
                                fraction = 1f - abs(distance).coerceIn(0f, 1f),
                            ),
                        ),
                )
            }
        }
    }
}
