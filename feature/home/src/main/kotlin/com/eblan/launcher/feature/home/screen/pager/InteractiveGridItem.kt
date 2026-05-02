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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.eblan.launcher.domain.model.GridItem
import com.eblan.launcher.domain.model.GridItemData
import com.eblan.launcher.domain.model.GridItemSettings
import com.eblan.launcher.domain.model.TextColor
import com.eblan.launcher.feature.home.component.InteractiveApplicationInfoGridItem
import com.eblan.launcher.feature.home.component.InteractiveFolderGridItem
import com.eblan.launcher.feature.home.component.InteractiveShortcutConfigGridItem
import com.eblan.launcher.feature.home.component.InteractiveShortcutInfoGridItem
import com.eblan.launcher.feature.home.component.InteractiveWidgetGridItem
import com.eblan.launcher.feature.home.model.Drag
import com.eblan.launcher.feature.home.model.GridItemSource
import com.eblan.launcher.feature.home.model.SharedElementKey
import com.eblan.launcher.feature.home.util.getGridItemTextColor
import com.eblan.launcher.feature.home.util.getSystemTextColor

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun SharedTransitionScope.InteractiveGridItemContent(
    modifier: Modifier = Modifier,
    drag: Drag,
    gridItem: GridItem,
    gridItemSettings: GridItemSettings,
    gridItemSource: GridItemSource?,
    hasShortcutHostPermission: Boolean,
    iconPackFilePaths: Map<String, String>,
    isScrollInProgress: Boolean,
    statusBarNotifications: Map<String, Int>,
    textColor: TextColor,
    isVisibleOverlay: Boolean,
    isVisibleFolder: Boolean,
    newGridItemSource: GridItemSource,
    sharedElementKey: SharedElementKey,
    onDraggingGridItem: () -> Unit,
    onOpenAppDrawer: () -> Unit,
    onTapApplicationInfo: (
        serialNumber: Long,
        componentName: String,
    ) -> Unit,
    onTapFolderGridItem: () -> Unit,
    onTapShortcutConfig: (String) -> Unit,
    onTapShortcutInfo: (
        serialNumber: Long,
        packageName: String,
        shortcutId: String,
    ) -> Unit,
    onUpdateGridItemSource: (GridItemSource) -> Unit,
    onUpdateImageBitmap: (ImageBitmap) -> Unit,
    onUpdateIsDragging: (Boolean) -> Unit,
    onUpdateOverlayBounds: (
        intOffset: IntOffset,
        intSize: IntSize,
    ) -> Unit,
    onUpdateSharedElementKey: (SharedElementKey?) -> Unit,
    onShowGridItemPopup: (
        intOffset: IntOffset,
        intSize: IntSize,
    ) -> Unit,
    onDismissGridItemPopup: () -> Unit,
    onUpdateIsVisibleOverlay: (Boolean) -> Unit,
) {
    val isSelected = gridItemSource != null && gridItemSource.gridItem.id == gridItem.id

    val currentGridItemSettings = if (gridItem.override) {
        gridItem.gridItemSettings
    } else {
        gridItemSettings
    }

    val currentTextColor = if (gridItem.override) {
        getGridItemTextColor(
            gridItemCustomTextColor = gridItem.gridItemSettings.customTextColor,
            gridItemTextColor = gridItem.gridItemSettings.textColor,
            systemCustomTextColor = gridItemSettings.customTextColor,
            systemTextColor = textColor,
        )
    } else {
        getSystemTextColor(
            systemCustomTextColor = gridItemSettings.customTextColor,
            systemTextColor = textColor,
        )
    }

    when (val data = gridItem.data) {
        is GridItemData.ApplicationInfo -> {
            InteractiveApplicationInfoGridItem(
                modifier = modifier,
                data = data,
                drag = drag,
                gridItem = gridItem,
                gridItemSettings = currentGridItemSettings,
                iconPackFilePaths = iconPackFilePaths,
                isScrollInProgress = isScrollInProgress,
                isSelected = isSelected,
                isShowWhiteBox = true,
                isVisibleFolder = isVisibleFolder,
                isVisibleOverlay = isVisibleOverlay,
                newGridItemSource = newGridItemSource,
                sharedElementKey = sharedElementKey,
                statusBarNotifications = statusBarNotifications,
                textColor = currentTextColor,
                onDismissGridItemPopup = onDismissGridItemPopup,
                onDraggingGridItem = onDraggingGridItem,
                onOpenAppDrawer = onOpenAppDrawer,
                onShowGridItemPopup = onShowGridItemPopup,
                onTapApplicationInfo = onTapApplicationInfo,
                onUpdateGridItemSource = onUpdateGridItemSource,
                onUpdateImageBitmap = onUpdateImageBitmap,
                onUpdateIsDragging = onUpdateIsDragging,
                onUpdateIsVisibleOverlay = onUpdateIsVisibleOverlay,
                onUpdateOverlayBounds = onUpdateOverlayBounds,
                onUpdateSharedElementKey = onUpdateSharedElementKey,
            )
        }

        is GridItemData.Widget -> {
            InteractiveWidgetGridItem(
                modifier = modifier,
                data = data,
                drag = drag,
                isScrollInProgress = isScrollInProgress,
                isSelected = isSelected,
                isShowWhiteBox = true,
                isVisibleOverlay = isVisibleOverlay,
                newGridItemSource = newGridItemSource,
                sharedElementKey = sharedElementKey,
                textColor = currentTextColor,
                onDismissGridItemPopup = onDismissGridItemPopup,
                onDraggingGridItem = onDraggingGridItem,
                onShowGridItemPopup = onShowGridItemPopup,
                onUpdateGridItemSource = onUpdateGridItemSource,
                onUpdateImageBitmap = onUpdateImageBitmap,
                onUpdateIsDragging = onUpdateIsDragging,
                onUpdateIsVisibleOverlay = onUpdateIsVisibleOverlay,
                onUpdateOverlayBounds = onUpdateOverlayBounds,
                onUpdateSharedElementKey = onUpdateSharedElementKey,
            )
        }

        is GridItemData.ShortcutInfo -> {
            InteractiveShortcutInfoGridItem(
                modifier = modifier,
                data = data,
                drag = drag,
                gridItem = gridItem,
                gridItemSettings = currentGridItemSettings,
                hasShortcutHostPermission = hasShortcutHostPermission,
                isScrollInProgress = isScrollInProgress,
                isSelected = isSelected,
                isShowWhiteBox = true,
                isVisibleFolder = isVisibleFolder,
                isVisibleOverlay = isVisibleOverlay,
                newGridItemSource = newGridItemSource,
                sharedElementKey = sharedElementKey,
                textColor = currentTextColor,
                onDismissGridItemPopup = onDismissGridItemPopup,
                onDraggingGridItem = onDraggingGridItem,
                onOpenAppDrawer = onOpenAppDrawer,
                onShowGridItemPopup = onShowGridItemPopup,
                onTapShortcutInfo = onTapShortcutInfo,
                onUpdateGridItemSource = onUpdateGridItemSource,
                onUpdateImageBitmap = onUpdateImageBitmap,
                onUpdateIsDragging = onUpdateIsDragging,
                onUpdateIsVisibleOverlay = onUpdateIsVisibleOverlay,
                onUpdateOverlayBounds = onUpdateOverlayBounds,
                onUpdateSharedElementKey = onUpdateSharedElementKey,
            )
        }

        is GridItemData.Folder -> {
            InteractiveFolderGridItem(
                modifier = modifier,
                data = data,
                drag = drag,
                gridItem = gridItem,
                gridItemSettings = currentGridItemSettings,
                gridItemSource = gridItemSource,
                iconPackFilePaths = iconPackFilePaths,
                isScrollInProgress = isScrollInProgress,
                isSelected = isSelected,
                isShowWhiteBox = true,
                isVisibleFolder = isVisibleFolder,
                isVisibleOverlay = isVisibleOverlay,
                newGridItemSource = newGridItemSource,
                sharedElementKey = sharedElementKey,
                textColor = currentTextColor,
                onDismissGridItemPopup = onDismissGridItemPopup,
                onDraggingGridItem = onDraggingGridItem,
                onOpenAppDrawer = onOpenAppDrawer,
                onShowGridItemPopup = onShowGridItemPopup,
                onTap = onTapFolderGridItem,
                onUpdateGridItemSource = onUpdateGridItemSource,
                onUpdateImageBitmap = onUpdateImageBitmap,
                onUpdateIsDragging = onUpdateIsDragging,
                onUpdateIsVisibleOverlay = onUpdateIsVisibleOverlay,
                onUpdateOverlayBounds = onUpdateOverlayBounds,
                onUpdateSharedElementKey = onUpdateSharedElementKey,
            )
        }

        is GridItemData.ShortcutConfig -> {
            InteractiveShortcutConfigGridItem(
                modifier = modifier,
                data = data,
                drag = drag,
                gridItem = gridItem,
                gridItemSettings = currentGridItemSettings,
                isScrollInProgress = isScrollInProgress,
                isSelected = isSelected,
                isShowWhiteBox = true,
                isVisibleFolder = isVisibleFolder,
                isVisibleOverlay = isVisibleOverlay,
                newGridItemSource = newGridItemSource,
                sharedElementKey = sharedElementKey,
                textColor = currentTextColor,
                onDismissGridItemPopup = onDismissGridItemPopup,
                onDraggingGridItem = onDraggingGridItem,
                onOpenAppDrawer = onOpenAppDrawer,
                onShowGridItemPopup = onShowGridItemPopup,
                onTapShortcutConfig = onTapShortcutConfig,
                onUpdateGridItemSource = onUpdateGridItemSource,
                onUpdateImageBitmap = onUpdateImageBitmap,
                onUpdateIsDragging = onUpdateIsDragging,
                onUpdateIsVisibleOverlay = onUpdateIsVisibleOverlay,
                onUpdateOverlayBounds = onUpdateOverlayBounds,
                onUpdateSharedElementKey = onUpdateSharedElementKey,
            )
        }
    }
}
