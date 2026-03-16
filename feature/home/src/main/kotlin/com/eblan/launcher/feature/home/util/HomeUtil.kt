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
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.eblan.launcher.domain.model.Associate
import com.eblan.launcher.domain.model.EblanAction
import com.eblan.launcher.domain.model.EblanActionType
import com.eblan.launcher.domain.model.GlobalAction
import com.eblan.launcher.feature.home.model.Drag
import com.eblan.launcher.feature.home.model.GridItemSource
import com.eblan.launcher.feature.home.model.SharedElementKey
import com.eblan.launcher.framework.launcherapps.AndroidLauncherAppsWrapper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal fun handleActionMainIntent(
    intent: Intent,
    onActionMainIntent: () -> Unit,
) {
    if (intent.action != Intent.ACTION_MAIN && !intent.hasCategory(Intent.CATEGORY_HOME)) {
        return
    }

    if ((intent.flags and Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT) != 0) {
        return
    }

    onActionMainIntent()
}

internal suspend fun handleEblanAction(
    context: Context,
    eblanAction: EblanAction,
    launcherApps: AndroidLauncherAppsWrapper,
    onOpenAppDrawer: suspend () -> Unit,
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

internal suspend fun handleApplyFling(
    offsetY: Animatable<Float, AnimationVector1D>,
    remaining: Float,
    screenHeight: Int,
    onDismiss: () -> Unit = {},
) {
    if (offsetY.value <= 0f && remaining > 10000f) {
        offsetY.animateTo(
            targetValue = screenHeight.toFloat(),
            initialVelocity = remaining,
            animationSpec = tween(
                easing = FastOutSlowInEasing,
            ),
        )

        onDismiss()
    } else if (offsetY.value > 200f) {
        offsetY.animateTo(
            targetValue = screenHeight.toFloat(),
            animationSpec = tween(
                easing = FastOutSlowInEasing,
            ),
        )

        onDismiss()
    } else {
        offsetY.animateTo(
            targetValue = 0f,
            initialVelocity = remaining,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessLow,
            ),
        )
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

internal fun onLongPress(
    scope: CoroutineScope,
    graphicsLayer: GraphicsLayer,
    intOffset: IntOffset,
    intSize: IntSize,
    gridItemSource: GridItemSource,
    sharedElementKey: SharedElementKey,
    onUpdateGridItemSource: (GridItemSource) -> Unit,
    onUpdateImageBitmap: (ImageBitmap) -> Unit,
    onUpdateIsLongPress: (Boolean) -> Unit,
    onUpdateOverlayBounds: (
        intOffset: IntOffset,
        intSize: IntSize,
    ) -> Unit,
    onUpdateSharedElementKey: (SharedElementKey?) -> Unit,
    onShowGridItemPopup: (
        intOffset: IntOffset,
        intSize: IntSize,
    ) -> Unit,
    onUpdateAssociate: (Associate) -> Unit,
) {
    scope.launch {
        onUpdateGridItemSource(gridItemSource)

        onUpdateAssociate(gridItemSource.gridItem.associate)

        onUpdateImageBitmap(graphicsLayer.toImageBitmap())

        onUpdateOverlayBounds(
            intOffset,
            intSize,
        )

        onUpdateSharedElementKey(sharedElementKey)

        onUpdateIsLongPress(true)

        onShowGridItemPopup(
            intOffset,
            intSize,
        )
    }
}

internal fun handleDrag(
    drag: Drag,
    isSelected: Boolean,
    isLongPress: Boolean,
    onUpdateIsDragging: (Boolean) -> Unit,
    onDismissGridItemPopup: () -> Unit,
    onDraggingGridItem: () -> Unit,
) {
    if (drag == Drag.Dragging && isSelected && isLongPress) {
        onUpdateIsDragging(true)

        onDismissGridItemPopup()

        onDraggingGridItem()
    }
}

internal val PAGE_INDICATOR_HEIGHT = 30.dp
internal val DRAG_HANDLE_SIZE = 30.dp
internal val FOLDER_GRID_PADDING = 10.dp
