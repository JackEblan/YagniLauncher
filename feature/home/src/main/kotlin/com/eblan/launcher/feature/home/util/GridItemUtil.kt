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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.Alignment
import com.eblan.launcher.domain.model.HorizontalAlignment
import com.eblan.launcher.domain.model.HorizontalArrangement
import com.eblan.launcher.domain.model.VerticalAlignment
import com.eblan.launcher.domain.model.VerticalArrangement

internal fun getHorizontalAlignment(horizontalAlignment: HorizontalAlignment): Alignment.Horizontal = when (horizontalAlignment) {
    HorizontalAlignment.START -> Alignment.Start
    HorizontalAlignment.CENTER_HORIZONTALLY -> Alignment.CenterHorizontally
    HorizontalAlignment.END -> Alignment.End
}

internal fun getVerticalArrangement(verticalArrangement: VerticalArrangement): Arrangement.Vertical = when (verticalArrangement) {
    VerticalArrangement.TOP -> Arrangement.Top
    VerticalArrangement.CENTER -> Arrangement.Center
    VerticalArrangement.BOTTOM -> Arrangement.Bottom
}

internal fun getHorizontalArrangement(horizontalArrangement: HorizontalArrangement): Arrangement.Horizontal = when (horizontalArrangement) {
    HorizontalArrangement.START -> Arrangement.Start
    HorizontalArrangement.CENTER -> Arrangement.Center
    HorizontalArrangement.END -> Arrangement.End
}

internal fun getVerticalAlignment(verticalAlignment: VerticalAlignment): Alignment.Vertical = when (verticalAlignment) {
    VerticalAlignment.TOP -> Alignment.Top
    VerticalAlignment.CENTER_VERTICALLY -> Alignment.CenterVertically
    VerticalAlignment.BOTTOM -> Alignment.Bottom
}
