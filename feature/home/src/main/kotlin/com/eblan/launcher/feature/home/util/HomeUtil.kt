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

import android.content.Context
import android.content.Intent
import android.graphics.Rect
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.foundation.gestures.PressGestureScope
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.State
import androidx.compose.ui.unit.dp
import com.eblan.launcher.domain.model.EblanAction
import com.eblan.launcher.domain.model.EblanActionType
import com.eblan.launcher.domain.model.GlobalAction
import com.eblan.launcher.feature.home.model.Drag
import com.eblan.launcher.feature.home.model.PageDirection
import com.eblan.launcher.framework.launcherapps.AndroidLauncherAppsWrapper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

internal fun handleEblanAction(
    context: Context,
    eblanAction: EblanAction,
    launcherApps: AndroidLauncherAppsWrapper,
    onOpenAppDrawer: () -> Unit,
) {
    when (eblanAction.eblanActionType) {
        EblanActionType.OpenApp -> {
            launcherApps.startMainActivity(
                serialNumber = eblanAction.serialNumber,
                componentName = eblanAction.componentName,
                sourceBounds = Rect(),
            )
        }

        EblanActionType.OpenNotificationPanel -> {
            val intent = Intent(GlobalAction.NAME).setPackage(context.packageName).putExtra(
                GlobalAction.GLOBAL_ACTION_TYPE,
                GlobalAction.Notifications.name,
            )

            context.sendBroadcast(intent)
        }

        EblanActionType.LockScreen -> {
            val intent = Intent(GlobalAction.NAME).setPackage(context.packageName).putExtra(
                GlobalAction.GLOBAL_ACTION_TYPE,
                GlobalAction.LockScreen.name,
            )

            context.sendBroadcast(intent)
        }

        EblanActionType.OpenQuickSettings -> {
            val intent = Intent(GlobalAction.NAME).setPackage(context.packageName).putExtra(
                GlobalAction.GLOBAL_ACTION_TYPE,
                GlobalAction.QuickSettings.name,
            )

            context.sendBroadcast(intent)
        }

        EblanActionType.OpenRecents -> {
            val intent = Intent(GlobalAction.NAME).setPackage(context.packageName).putExtra(
                GlobalAction.GLOBAL_ACTION_TYPE,
                GlobalAction.Recents.name,
            )

            context.sendBroadcast(intent)
        }

        EblanActionType.OpenAppDrawer -> {
            onOpenAppDrawer()
        }

        EblanActionType.None -> Unit
    }
}

internal fun onDoubleTap(
    context: Context,
    doubleTap: EblanAction,
    launcherApps: AndroidLauncherAppsWrapper,
    scope: CoroutineScope,
    onOpenAppDrawer: () -> Unit,
) {
    if (doubleTap.eblanActionType == EblanActionType.None) return

    scope.launch {
        handleEblanAction(
            context = context,
            eblanAction = doubleTap,
            launcherApps = launcherApps,
            onOpenAppDrawer = onOpenAppDrawer,
        )
    }
}

internal suspend fun PressGestureScope.onPress(
    isVisibleOverlay: Boolean,
    scale: Animatable<Float, AnimationVector1D>,
) {
    if (isVisibleOverlay) return

    awaitRelease()

    if (scale.isRunning) {
        scale.stop()

        scale.animateTo(1f)
    }
}

internal suspend fun handleDrag(
    drag: Drag,
    hasInteraction: Boolean,
    isDragging: State<Boolean>,
    isCloseGridItemPopup: State<Boolean>,
    scale: Animatable<Float, AnimationVector1D>,
    onUpdateIsDragging: (Boolean) -> Unit,
    onUpdateIsCloseGridItemPopup: (Boolean) -> Unit,
) {
    if (drag == Drag.Dragging &&
        hasInteraction &&
        !isDragging.value &&
        !isCloseGridItemPopup.value
    ) {
        onUpdateIsDragging(true)

        onUpdateIsCloseGridItemPopup(true)
    } else if ((drag == Drag.End || drag == Drag.Cancel) && scale.isRunning) {
        scale.stop()

        scale.animateTo(1f)
    }
}

internal suspend fun handlePageDirection(
    pageDirection: PageDirection?,
    currentPage: Int,
    onAnimateScrollToPage: suspend (Int) -> Unit,
) {
    if (pageDirection == null) return

    delay(500L.milliseconds)

    when (pageDirection) {
        PageDirection.Left -> {
            onAnimateScrollToPage(currentPage - 1)
        }

        PageDirection.Right -> {
            onAnimateScrollToPage(currentPage + 1)
        }
    }
}

internal val PAGE_INDICATOR_HEIGHT = 30.dp
internal val DRAG_HANDLE_SIZE = 30.dp
internal const val FOLDER_PREVIEW_COLUMNS = 2
internal const val FOLDER_PREVIEW_ROWS = 2
