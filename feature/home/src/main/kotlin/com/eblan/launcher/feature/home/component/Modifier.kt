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

import android.graphics.Paint
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.eblan.launcher.domain.model.EblanAction
import com.eblan.launcher.domain.model.EblanActionType
import com.eblan.launcher.feature.home.util.handleEblanAction
import com.eblan.launcher.ui.local.LocalLauncherApps
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
internal fun Modifier.swipeGestures(
    swipeDown: EblanAction,
    swipeUp: EblanAction,
    onOpenAppDrawer: () -> Unit,
): Modifier {
    val context = LocalContext.current

    val density = LocalDensity.current

    val scope = rememberCoroutineScope()

    val launcherApps = LocalLauncherApps.current

    return if (swipeUp.eblanActionType != EblanActionType.None ||
        swipeDown.eblanActionType != EblanActionType.None
    ) {
        val swipeY = remember { Animatable(0f) }

        val maxSwipeY = with(density) {
            40.dp.roundToPx()
        }

        pointerInput(key1 = Unit) {
            detectVerticalDragGestures(
                onDragStart = {
                    scope.launch {
                        swipeY.snapTo(0f)
                    }
                },
                onVerticalDrag = { _, dragAmount ->
                    scope.launch {
                        swipeY.snapTo(swipeY.value + dragAmount)
                    }
                },
                onDragCancel = {
                    scope.launch {
                        swipeY.animateTo(0f)
                    }
                },
                onDragEnd = {
                    scope.launch {
                        when {
                            swipeY.value <= -maxSwipeY -> {
                                swipeY.animateTo(0f)

                                handleEblanAction(
                                    context = context,
                                    eblanAction = swipeUp,
                                    launcherApps = launcherApps,
                                    onOpenAppDrawer = onOpenAppDrawer,
                                )
                            }

                            swipeY.value >= maxSwipeY -> {
                                swipeY.animateTo(0f)

                                handleEblanAction(
                                    context = context,
                                    eblanAction = swipeDown,
                                    launcherApps = launcherApps,
                                    onOpenAppDrawer = onOpenAppDrawer,
                                )
                            }
                        }
                    }
                },
            )
        }.offset {
            IntOffset(
                x = 0,
                y = swipeY.value.roundToInt().coerceIn(-maxSwipeY..maxSwipeY),
            )
        }
    } else {
        this
    }
}

internal fun Modifier.whiteBox(
    textColor: Color,
    visible: Boolean,
): Modifier = if (visible) {
    drawWithCache {
        val strokeWidth = 1.5.dp.toPx()

        val cornerRadius = 5.dp.toPx()

        val inset = strokeWidth / 2f

        val paint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            this.strokeWidth = strokeWidth
            color = textColor.copy(alpha = 0.3f).toArgb()
            setShadowLayer(12.dp.toPx(), 0f, 0f, textColor.toArgb())
        }

        onDrawBehind {
            drawContext.canvas.nativeCanvas.drawRoundRect(
                inset,
                inset,
                size.width - inset,
                size.height - inset,
                cornerRadius,
                cornerRadius,
                paint,
            )
        }
    }
} else {
    this
}
