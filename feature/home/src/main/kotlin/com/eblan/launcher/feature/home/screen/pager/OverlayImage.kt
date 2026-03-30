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
package com.eblan.launcher.feature.home.screen.pager

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.eblan.launcher.feature.home.model.SharedElementKey

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun SharedTransitionScope.OverlayImage(
    modifier: Modifier = Modifier,
    overlayImageBitmap: ImageBitmap?,
    overlayIntOffset: IntOffset?,
    overlayIntSize: IntSize?,
    sharedElementKey: SharedElementKey?,
    isVisibleOverlay: Boolean,
    onResetOverlay: () -> Unit,
) {
    if (overlayImageBitmap == null ||
        sharedElementKey == null ||
        overlayIntOffset == null ||
        overlayIntSize == null
    ) {
        return
    }

    val density = LocalDensity.current

    val size = with(density) {
        DpSize(width = overlayIntSize.width.toDp(), height = overlayIntSize.height.toDp())
    }

    LaunchedEffect(key1 = isVisibleOverlay) {
        if (!isVisibleOverlay) {
            onResetOverlay()
        }
    }

    Image(
        modifier = modifier
            .offset {
                overlayIntOffset
            }
            .size(size)
            .sharedElementWithCallerManagedVisibility(
                rememberSharedContentState(key = sharedElementKey),
                visible = isVisibleOverlay,
            ),
        bitmap = overlayImageBitmap,
        contentDescription = null,
    )
}
