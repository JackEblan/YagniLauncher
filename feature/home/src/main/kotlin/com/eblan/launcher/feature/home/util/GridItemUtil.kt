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

internal fun getHorizontalAlignment(horizontalAlignment: HorizontalAlignment): Alignment.Horizontal =
    when (horizontalAlignment) {
        HorizontalAlignment.Start -> Alignment.Start
        HorizontalAlignment.CenterHorizontally -> Alignment.CenterHorizontally
        HorizontalAlignment.End -> Alignment.End
    }

internal fun getVerticalArrangement(verticalArrangement: VerticalArrangement): Arrangement.Vertical =
    when (verticalArrangement) {
        VerticalArrangement.Top -> Arrangement.Top
        VerticalArrangement.Center -> Arrangement.Center
        VerticalArrangement.Bottom -> Arrangement.Bottom
    }

internal fun getHorizontalArrangement(horizontalArrangement: HorizontalArrangement): Arrangement.Horizontal =
    when (horizontalArrangement) {
        HorizontalArrangement.Start -> Arrangement.Start
        HorizontalArrangement.Center -> Arrangement.Center
        HorizontalArrangement.End -> Arrangement.End
    }

internal fun getVerticalAlignment(verticalAlignment: VerticalAlignment): Alignment.Vertical =
    when (verticalAlignment) {
        VerticalAlignment.Top -> Alignment.Top
        VerticalAlignment.CenterVertically -> Alignment.CenterVertically
        VerticalAlignment.Bottom -> Alignment.Bottom
    }