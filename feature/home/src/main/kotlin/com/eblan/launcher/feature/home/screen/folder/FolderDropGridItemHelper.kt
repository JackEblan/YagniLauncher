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
package com.eblan.launcher.feature.home.screen.folder

import androidx.compose.runtime.State
import com.eblan.launcher.feature.home.model.Drag

internal fun handleDropFolderGridItem(
    drag: Drag,
    isDragging: State<Boolean>,
    lockMovement: State<Boolean>,
    isVisibleOverlay: State<Boolean>,
    isLast: Boolean,
    onDragCancelAfterMoveFolder: () -> Unit,
    onDragEndAfterMoveFolder: () -> Unit,
    onUpdateIsDragging: (Boolean) -> Unit,
    onUpdateIsVisibleOverlay: (Boolean) -> Unit,
) {
    if (drag == Drag.None ||
        drag == Drag.Start ||
        drag == Drag.Dragging ||
        !isLast
    ) {
        return
    }

    if (isVisibleOverlay.value && !isDragging.value) {
        onUpdateIsVisibleOverlay(false)

        return
    }

    if (lockMovement.value) {
        onUpdateIsVisibleOverlay(false)

        onUpdateIsDragging(false)

        onDragCancelAfterMoveFolder()

        return
    }

    if (isVisibleOverlay.value) {
        onDragEndAfterMoveFolder()

        onUpdateIsDragging(false)
    }
}
