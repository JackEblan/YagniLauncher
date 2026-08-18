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
package com.eblan.launcher.feature.home.util

import androidx.compose.ui.graphics.Color
import com.eblan.launcher.domain.model.TextColor
import kotlin.math.abs
import kotlin.math.pow

internal fun getGridItemTextColor(
    gridItemCustomTextColor: Int,
    gridItemTextColor: TextColor,
    systemCustomTextColor: Int,
    systemTextColor: TextColor,
    defaultColor: Color = Color.Unspecified,
): Color = when (gridItemTextColor) {
    TextColor.System -> getTextColor(
        customTextColor = systemCustomTextColor,
        textColor = systemTextColor,
        defaultColor = defaultColor,
    )

    TextColor.Light -> Color.White
    TextColor.Dark -> Color.Black
    TextColor.Custom -> Color(gridItemCustomTextColor)
}

internal fun getTextColor(
    customTextColor: Int,
    textColor: TextColor,
    defaultColor: Color = Color.Unspecified,
): Color = when (textColor) {
    TextColor.System -> defaultColor
    TextColor.Light -> Color.White
    TextColor.Dark -> Color.Black
    TextColor.Custom -> Color(customTextColor)
}

internal fun getEblanApplicationInfoTextColor(
    backgroundColor: TextColor,
    customBackgroundColor: Int,
    textColor: TextColor,
    customTextColor: Int,
    systemCustomTextColor: Int,
    systemTextColor: TextColor,
    defaultColor: Color = Color.Unspecified,
): Color = when (backgroundColor) {
    TextColor.System -> defaultColor
    TextColor.Light -> Color.Black
    TextColor.Dark -> Color.White
    TextColor.Custom -> {
        val homeGridItemTextColor = getTextColor(
            customTextColor = systemCustomTextColor,
            textColor = systemTextColor,
            defaultColor = Color.Red,
        )

        val gridItemTextColor = getTextColorByLuminance(
            customBackgroundColor = Color(customBackgroundColor),
            systemTextColor = homeGridItemTextColor,
        )

        getTextColor(
            customTextColor = customTextColor,
            textColor = textColor,
            defaultColor = gridItemTextColor,
        )
    }
}

fun getTextColorByLuminance(
    customBackgroundColor: Color,
    systemTextColor: Color,
    minTrustedAlpha: Float = 0.85f,
    luminanceCrossover: Double = 0.179,
    marginOfSafety: Double = 0.05,
): Color {
    if (customBackgroundColor.alpha < 0.3f) return systemTextColor

    val luminance = relativeLuminance(color = customBackgroundColor)

    val distanceFromCrossover = abs(luminance - luminanceCrossover)

    if (customBackgroundColor.alpha < minTrustedAlpha &&
        distanceFromCrossover < marginOfSafety
    ) {
        return systemTextColor
    }

    return if (luminance > luminanceCrossover) Color.Black else Color.White
}

private fun relativeLuminance(color: Color): Double {
    fun channel(c: Float): Double {
        val cs = c.toDouble()

        return if (cs <= 0.03928) cs / 12.92 else ((cs + 0.055) / 1.055).pow(2.4)
    }

    val r = channel(color.red)
    val g = channel(color.green)
    val b = channel(color.blue)

    return 0.2126 * r + 0.7152 * g + 0.0722 * b
}