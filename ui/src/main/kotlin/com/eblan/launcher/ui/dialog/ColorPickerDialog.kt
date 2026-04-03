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
package com.eblan.launcher.ui.dialog

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.eblan.launcher.designsystem.component.EblanDialogContainer

@Composable
fun ColorPickerDialog(
    modifier: Modifier = Modifier,
    title: String,
    customColor: Int,
    onDismissRequest: () -> Unit,
    onSelectColor: (Int) -> Unit,
) {
    val hsv = FloatArray(3).apply {
        android.graphics.Color.RGBToHSV(
            android.graphics.Color.red(customColor),
            android.graphics.Color.green(customColor),
            android.graphics.Color.blue(customColor),
            this,
        )
    }

    var hue by remember { mutableFloatStateOf(hsv[0]) }

    var saturation by remember { mutableFloatStateOf(hsv[1]) }

    var value by remember { mutableFloatStateOf(hsv[2]) }

    var alpha by remember { mutableFloatStateOf(Color(customColor).alpha) }

    EblanDialogContainer(
        content = {
            Column(
                modifier = modifier
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth(),
            ) {
                Text(
                    modifier = Modifier.padding(10.dp),
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                )

                Spacer(modifier = Modifier.height(10.dp))

                ColorPicker(
                    modifier = Modifier.padding(10.dp),
                    hue = hue,
                    saturation = saturation,
                    value = value,
                    alpha = alpha,
                    onSaturationSelected = { newSaturation ->
                        saturation = newSaturation
                    },
                    onValueSelected = { newValue ->
                        value = newValue
                    },
                    onHueSelected = { newHue ->
                        hue = newHue
                    },
                    onAlphaSelected = { newAlpha ->
                        alpha = newAlpha
                    },
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            end = 10.dp,
                            bottom = 10.dp,
                        ),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(
                        onClick = onDismissRequest,
                    ) {
                        Text("Cancel")
                    }

                    Spacer(modifier = Modifier.width(5.dp))

                    TextButton(
                        onClick = {
                            onSelectColor(
                                Color.hsv(hue, saturation, value).copy(alpha = alpha).toArgb(),
                            )
                        },
                    ) {
                        Text("Save")
                    }
                }
            }
        },
        onDismissRequest = onDismissRequest,
    )
}

@Composable
private fun ColorPicker(
    modifier: Modifier = Modifier,
    hue: Float,
    saturation: Float,
    value: Float,
    alpha: Float,
    onSaturationSelected: (Float) -> Unit,
    onValueSelected: (Float) -> Unit,
    onHueSelected: (Float) -> Unit,
    onAlphaSelected: (Float) -> Unit,
) {
    Column(modifier = modifier) {
        SaturationValueCanvas(
            hue = hue,
            saturation = saturation,
            value = value,
            onSaturationSelected = onSaturationSelected,
            onValueSelected = onValueSelected,
        )

        Spacer(modifier = Modifier.height(24.dp))

        HueCanvas(
            hue = hue,
            onHueSelected = onHueSelected,
        )

        Spacer(modifier = Modifier.height(24.dp))

        AlphaCanvas(
            alpha = alpha,
            onAlphaSelected = onAlphaSelected,
        )
    }
}

@Composable
private fun SaturationValueCanvas(
    modifier: Modifier = Modifier,
    hue: Float,
    saturation: Float,
    value: Float,
    onSaturationSelected: (Float) -> Unit,
    onValueSelected: (Float) -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(300.dp),
    ) {
        Canvas(
            modifier = Modifier
                .pointerInput(key1 = Unit) {
                    detectTapGestures(
                        onTap = { offset ->
                            onSaturationSelected((offset.x / size.width).coerceIn(0f, 1f))

                            onValueSelected(1f - (offset.y / size.height).coerceIn(0f, 1f))
                        },
                    )
                }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDrag = { change, _ ->
                            onSaturationSelected((change.position.x / size.width).coerceIn(0f, 1f))

                            onValueSelected(1f - (change.position.y / size.height).coerceIn(0f, 1f))
                        },
                    )
                }
                .matchParentSize()
                .clip(RoundedCornerShape(5.dp)),
        ) {
            drawRect(color = Color.hsv(hue, 1f, 1f))

            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color.White, Color.Transparent),
                ),
            )

            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color.Black),
                ),
            )
        }

        Canvas(modifier = Modifier.matchParentSize()) {
            val indicatorX = saturation * size.width

            val indicatorY = (1f - value) * size.height

            drawCircle(
                color = Color.Black,
                radius = 5.dp.toPx(),
                center = Offset(indicatorX, indicatorY),
                style = Stroke(width = 3.dp.toPx()),
            )

            drawCircle(
                color = Color.White,
                radius = 5.dp.toPx(),
                center = Offset(indicatorX, indicatorY),
                style = Stroke(width = 3.dp.toPx()),
            )
        }
    }
}

@Composable
private fun HueCanvas(
    modifier: Modifier = Modifier,
    hue: Float,
    onHueSelected: (Float) -> Unit,
) {
    Canvas(
        modifier = modifier
            .pointerInput(key1 = Unit) {
                detectTapGestures(
                    onTap = { offset ->
                        onHueSelected((offset.x / size.width).coerceIn(0f, 1f) * 360f)
                    },
                )
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onHorizontalDrag = { change, _ ->
                        onHueSelected((change.position.x / size.width).coerceIn(0f, 1f) * 360f)
                    },
                )
            }
            .fillMaxWidth()
            .height(20.dp)
            .clip(RoundedCornerShape(5.dp)),
    ) {
        val hueColors = listOf(
            Color.Red,
            Color.Yellow,
            Color.Green,
            Color.Cyan,
            Color.Blue,
            Color.Magenta,
            Color.Red,
        )

        drawRect(
            brush = Brush.horizontalGradient(hueColors),
            size = size,
        )

        val indicatorSize = 5.dp.toPx()

        val indicatorX = ((hue / 360f) * size.width).coerceIn(0f..size.width - indicatorSize)

        drawRect(
            color = Color.White,
            topLeft = Offset(indicatorX, 0f),
            size = Size(indicatorSize, size.height),
        )

        drawRect(
            color = Color.Black.copy(alpha = 0.4f),
            topLeft = Offset(indicatorX, 0f),
            size = Size(indicatorSize, size.height),
            style = Stroke(width = Dp.Hairline.toPx()),
        )
    }
}

@Composable
private fun AlphaCanvas(
    modifier: Modifier = Modifier,
    alpha: Float,
    onAlphaSelected: (Float) -> Unit,
) {
    Canvas(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    onAlphaSelected((offset.x / size.width).coerceIn(0f, 1f))
                }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures { change, _ ->
                    onAlphaSelected((change.position.x / size.width).coerceIn(0f, 1f))
                }
            }
            .fillMaxWidth()
            .height(20.dp)
            .clip(RoundedCornerShape(5.dp)),
    ) {
        val squareSize = size.height / 2

        val columns = kotlin.math.ceil(size.width / squareSize).toInt()
        val rows = kotlin.math.ceil(size.height / squareSize).toInt()

        for (column in 0 until columns) {
            val columnAlpha = (1f - (column * squareSize) / size.width).coerceIn(0f, 1f)

            val black = Color.Black.copy(alpha = 0.4f * columnAlpha)
            val white = Color.White.copy(alpha = 0.4f * columnAlpha)

            for (row in 0 until rows) {
                val isBlack = (row + column) % 2 == 0

                drawRect(
                    color = if (isBlack) black else white,
                    topLeft = Offset(
                        x = column * squareSize,
                        y = row * squareSize,
                    ),
                    size = Size(squareSize, squareSize),
                )
            }
        }

        val indicatorSize = 5.dp.toPx()

        val indicatorX = (alpha * size.width).coerceIn(0f..size.width - indicatorSize)

        drawRect(
            color = Color.White,
            topLeft = Offset(indicatorX, 0f),
            size = Size(indicatorSize, size.height),
        )

        drawRect(
            color = Color.Black.copy(alpha = 0.4f),
            topLeft = Offset(indicatorX, 0f),
            size = Size(indicatorSize, size.height),
            style = Stroke(width = Dp.Hairline.toPx()),
        )
    }
}
