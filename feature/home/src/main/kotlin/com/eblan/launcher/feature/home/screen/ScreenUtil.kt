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

import android.appwidget.AppWidgetManager
import android.content.Context
import android.os.Bundle
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.eblan.launcher.domain.model.EblanAction
import com.eblan.launcher.domain.model.EblanActionType
import com.eblan.launcher.domain.model.GridItem
import com.eblan.launcher.domain.model.GridItemData
import com.eblan.launcher.domain.model.HorizontalAlignment
import com.eblan.launcher.domain.model.VerticalArrangement
import com.eblan.launcher.feature.home.util.handleEblanAction
import com.eblan.launcher.framework.launcherapps.AndroidLauncherAppsWrapper
import com.eblan.launcher.framework.widgetmanager.AndroidAppWidgetManagerWrapper
import kotlin.math.roundToInt

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

internal fun updateAppWidgetOptions(
    allowedHeight: Int,
    allowedWidth: Int,
    androidAppWidgetManagerWrapper: AndroidAppWidgetManagerWrapper,
    columns: Int,
    data: GridItemData.Widget,
    density: Density,
    gridHeight: Int,
    gridItem: GridItem,
    gridWidth: Int,
    rows: Int,
) {
    val cellWidthPx = gridWidth.toFloat() / columns
    val cellHeightPx = gridHeight.toFloat() / rows

    val maxSpanColumns = (columns - gridItem.startColumn).coerceAtLeast(1)
    val maxSpanRows = (rows - gridItem.startRow).coerceAtLeast(1)

    val maxWidthByGridPx = (maxSpanColumns * cellWidthPx).roundToInt()
    val maxHeightByGridPx = (maxSpanRows * cellHeightPx).roundToInt()

    val maxWidthPx = if (data.maxResizeWidth > 0) {
        minOf(
            data.maxResizeWidth,
            maxWidthByGridPx,
        )
    } else {
        maxWidthByGridPx
    }
    val maxHeightPx = if (data.maxResizeHeight > 0) {
        minOf(
            data.maxResizeHeight,
            maxHeightByGridPx,
        )
    } else {
        maxHeightByGridPx
    }

    val minWidthPx = if (data.minResizeWidth > 0) data.minResizeWidth else allowedWidth
    val minHeightPx = if (data.minResizeHeight > 0) data.minResizeHeight else allowedHeight

    val minWidthDp = with(density) { minWidthPx.toDp().value.roundToInt().coerceAtLeast(1) }
    val minHeightDp =
        with(density) { minHeightPx.toDp().value.roundToInt().coerceAtLeast(1) }
    val maxWidthDp =
        with(density) { maxWidthPx.toDp().value.roundToInt().coerceAtLeast(minWidthDp) }
    val maxHeightDp =
        with(density) { maxHeightPx.toDp().value.roundToInt().coerceAtLeast(minHeightDp) }

    val options = Bundle().apply {
        putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, minWidthDp)
        putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, minHeightDp)
        putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, maxWidthDp)
        putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, maxHeightDp)
    }

    androidAppWidgetManagerWrapper.updateAppWidgetOptions(
        appWidgetId = data.appWidgetId,
        options = options,
    )
}

internal val PAGE_INDICATOR_HEIGHT = 30.dp
internal val DRAG_HANDLE_SIZE = 30.dp
