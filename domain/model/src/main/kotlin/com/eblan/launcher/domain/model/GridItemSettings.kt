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
package com.eblan.launcher.domain.model

data class GridItemSettings(
    val iconSize: Int,
    val textColor: TextColor,
    val textSize: Int,
    val showLabel: Boolean,
    val singleLineLabel: Boolean,
    val horizontalAlignment: HorizontalAlignment,
    val verticalArrangement: VerticalArrangement,
    val customTextColor: Int,
    val customBackgroundColor: Int,
    val padding: Int,
    val cornerRadius: Int,
    val gridItemLayoutType: GridItemLayoutType,
    val horizontalArrangement: HorizontalArrangement,
    val verticalAlignment: VerticalAlignment,
)

enum class HorizontalAlignment {
    START,
    CENTER_HORIZONTALLY,
    END,
}

enum class VerticalArrangement {
    TOP,
    CENTER,
    BOTTOM,
}

enum class HorizontalArrangement {
    START,
    CENTER,
    END,
}

enum class VerticalAlignment {
    TOP,
    CENTER_VERTICALLY,
    BOTTOM,
}

enum class GridItemLayoutType {
    START_ICON_END_LABEL,
    START_LABEL_END_ICON,
    TOP_ICON_BOTTOM_LABEL,
    TOP_LABEL_BOTTOM_ICON,
    ICON_ONLY,
    LABEL_ONLY,
}
