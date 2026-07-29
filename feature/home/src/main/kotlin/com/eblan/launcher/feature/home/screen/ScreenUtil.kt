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
package com.eblan.launcher.feature.home.screen

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.eblan.launcher.domain.model.EblanAction
import com.eblan.launcher.domain.model.EblanActionType
import com.eblan.launcher.domain.model.HorizontalAlignment
import com.eblan.launcher.domain.model.VerticalArrangement
import com.eblan.launcher.feature.home.util.handleEblanAction
import com.eblan.launcher.framework.launcherapps.AndroidLauncherAppsWrapper

internal fun getHorizontalAlignment(horizontalAlignment: HorizontalAlignment): Alignment.Horizontal = when (horizontalAlignment) {
    HorizontalAlignment.Start -> Alignment.Start
    HorizontalAlignment.CenterHorizontally -> Alignment.CenterHorizontally
    HorizontalAlignment.End -> Alignment.End
}

internal fun getVerticalArrangement(verticalArrangement: VerticalArrangement): Arrangement.Vertical = when (verticalArrangement) {
    VerticalArrangement.Top -> Arrangement.Top
    VerticalArrangement.Center -> Arrangement.Center
    VerticalArrangement.Bottom -> Arrangement.Bottom
}

internal fun onDoubleTap(
    context: Context,
    doubleTap: EblanAction,
    launcherApps: AndroidLauncherAppsWrapper,
    onOpenAppDrawer: () -> Unit,
) {
    if (doubleTap.eblanActionType == EblanActionType.None) return

    handleEblanAction(
        context = context,
        eblanAction = doubleTap,
        launcherApps = launcherApps,
        onOpenAppDrawer = onOpenAppDrawer,
    )
}

internal val PAGE_INDICATOR_HEIGHT = 30.dp
internal val DRAG_HANDLE_SIZE = 30.dp
