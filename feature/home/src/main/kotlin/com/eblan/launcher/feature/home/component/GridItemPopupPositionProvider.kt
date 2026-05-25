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

import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.PopupPositionProvider

internal class GridItemPopupPositionProvider(
    private val popupIntOffset: IntOffset,
    private val popupIntSize: IntSize,
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val parentCenterX = popupIntOffset.x + (popupIntSize.width / 2)

        val topY = popupIntOffset.y - popupContentSize.height
        val bottomY = popupIntOffset.y + popupIntSize.height

        val childX = parentCenterX - (popupContentSize.width / 2)

        val childY = if (topY < 0) {
            bottomY
        } else {
            topY
        }

        return IntOffset(
            x = childX.coerceIn(
                0,
                windowSize.width - popupContentSize.width,
            ),
            y = childY.coerceIn(
                0,
                windowSize.height - popupContentSize.height,
            ),
        )
    }
}
