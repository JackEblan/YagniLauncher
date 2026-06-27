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

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.OverscrollEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sign

internal class OffsetOverscrollEffect(
    private val scope: CoroutineScope,
    private val onVerticalDrag: (Float) -> Unit,
    private val onDragEnd: () -> Unit,
) : OverscrollEffect {
    private val overscrollOffset = Animatable(0f)
    private val overscrollBounceOffset = Animatable(0f)

    override fun applyToScroll(
        delta: Offset,
        source: NestedScrollSource,
        performScroll: (Offset) -> Offset,
    ): Offset {
        val sameDirection = sign(delta.y) == sign(overscrollOffset.value)

        val consumedByPreScroll = if (abs(overscrollOffset.value) > 0.5 && !sameDirection) {
            val prevOverscrollValue = overscrollOffset.value

            val newOverscrollValue = overscrollOffset.value + delta.y

            if (sign(prevOverscrollValue) != sign(newOverscrollValue)) {
                scope.launch {
                    onVerticalDrag(0f)

                    overscrollOffset.snapTo(0f)
                }

                Offset(x = 0f, y = delta.y + prevOverscrollValue)
            } else {
                scope.launch {
                    onVerticalDrag(delta.y)

                    overscrollOffset.snapTo(newOverscrollValue)
                }

                delta.copy(x = 0f)
            }
        } else {
            Offset.Zero
        }

        val leftForScroll = delta - consumedByPreScroll

        val consumedByScroll = performScroll(leftForScroll)

        val overscrollDelta = leftForScroll - consumedByScroll

        if (abs(overscrollDelta.y) > 0.5 && source == NestedScrollSource.UserInput) {
            scope.launch {
                onVerticalDrag(overscrollDelta.y)

                overscrollOffset.snapTo(overscrollOffset.value + overscrollDelta.y)
            }
        }

        return consumedByPreScroll + consumedByScroll
    }

    override suspend fun applyToFling(
        velocity: Velocity,
        performFling: suspend (Velocity) -> Velocity,
    ) {
        if (overscrollOffset.value == 0f) {
            val remaining = velocity - performFling(velocity)

            overscrollBounceOffset.animateTo(
                targetValue = 0f,
                initialVelocity = remaining.y,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessLow,
                ),
            )
        }

        overscrollOffset.snapTo(0f)

        onDragEnd()
    }

    override val isInProgress: Boolean
        get() = overscrollOffset.value != 0f || overscrollBounceOffset.value != 0f

    override val node: DelegatableNode =
        object : Modifier.Node(), LayoutModifierNode {
            override fun MeasureScope.measure(
                measurable: Measurable,
                constraints: Constraints,
            ): MeasureResult {
                val placeable = measurable.measure(constraints)

                return layout(
                    width = placeable.width,
                    height = placeable.height,
                ) {
                    placeable.placeRelativeWithLayer(
                        x = 0,
                        y = overscrollBounceOffset.value.roundToInt(),
                    )
                }
            }
        }
}
