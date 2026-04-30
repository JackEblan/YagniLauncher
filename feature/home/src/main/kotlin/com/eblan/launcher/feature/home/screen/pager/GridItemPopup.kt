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

import android.appwidget.AppWidgetProviderInfo
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.eblan.launcher.designsystem.icon.EblanLauncherIcons
import com.eblan.launcher.domain.model.EblanAppWidgetProviderInfo
import com.eblan.launcher.domain.model.EblanApplicationInfoGroup
import com.eblan.launcher.domain.model.EblanShortcutInfo
import com.eblan.launcher.domain.model.EblanShortcutInfoByGroup
import com.eblan.launcher.domain.model.GridItem
import com.eblan.launcher.domain.model.GridItemData
import com.eblan.launcher.domain.model.GridItemSettings
import com.eblan.launcher.feature.home.component.ShortcutInfoMenu
import com.eblan.launcher.feature.home.model.Drag
import com.eblan.launcher.feature.home.model.GridItemSource
import com.eblan.launcher.feature.home.model.SharedElementKey

@Composable
internal fun GridItemPopup(
    modifier: Modifier = Modifier,
    currentPage: Int,
    drag: Drag,
    eblanAppWidgetProviderInfosGroup: Map<String, List<EblanAppWidgetProviderInfo>>,
    eblanShortcutInfosGroup: Map<EblanShortcutInfoByGroup, List<EblanShortcutInfo>>,
    gridItem: GridItem?,
    gridItemSettings: GridItemSettings,
    hasShortcutHostPermission: Boolean,
    paddingValues: PaddingValues,
    popupIntOffset: IntOffset?,
    popupIntSize: IntSize?,
    onDeleteGridItem: (GridItem) -> Unit,
    onDismissRequest: () -> Unit,
    onDraggingShortcutInfoGridItem: () -> Unit,
    onEdit: (String) -> Unit,
    onInfo: (Long, String) -> Unit,
    onResize: () -> Unit,
    onTapShortcutInfo: (Long, String, String) -> Unit,
    onUpdateGridItemSource: (GridItemSource) -> Unit,
    onUpdateImageBitmap: (ImageBitmap) -> Unit,
    onUpdateOverlayBounds: (
        intOffset: IntOffset,
        intSize: IntSize,
    ) -> Unit,
    onUpdateSharedElementKey: (SharedElementKey?) -> Unit,
    onWidgets: (EblanApplicationInfoGroup) -> Unit,
    onUpdateIsVisibleOverlay: (Boolean) -> Unit,
) {
    if (gridItem == null || popupIntOffset == null || popupIntSize == null) return

    val density = LocalDensity.current

    val leftPadding = with(density) {
        paddingValues.calculateStartPadding(LayoutDirection.Ltr).roundToPx()
    }

    val topPadding = with(density) {
        paddingValues.calculateTopPadding().roundToPx()
    }
    val x = popupIntOffset.x - leftPadding

    val y = popupIntOffset.y - topPadding

    Layout(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        awaitRelease()

                        onDismissRequest()
                    },
                )
            }
            .fillMaxSize()
            .padding(paddingValues),
        content = {
            GridItemPopupContent(
                currentPage = currentPage,
                drag = drag,
                eblanAppWidgetProviderInfosGroup = eblanAppWidgetProviderInfosGroup,
                eblanShortcutInfosGroup = eblanShortcutInfosGroup,
                gridItem = gridItem,
                gridItemSettings = gridItemSettings,
                hasShortcutHostPermission = hasShortcutHostPermission,
                onDeleteGridItem = onDeleteGridItem,
                onDismissRequest = onDismissRequest,
                onDraggingShortcutInfoGridItem = onDraggingShortcutInfoGridItem,
                onEdit = onEdit,
                onInfo = onInfo,
                onResize = onResize,
                onTapShortcutInfo = onTapShortcutInfo,
                onUpdateGridItemSource = onUpdateGridItemSource,
                onUpdateImageBitmap = onUpdateImageBitmap,
                onUpdateOverlayBounds = onUpdateOverlayBounds,
                onUpdateSharedElementKey = onUpdateSharedElementKey,
                onWidgets = onWidgets,
                onUpdateIsVisibleOverlay = onUpdateIsVisibleOverlay,
            )
        },
    ) { measurables, constraints ->
        val placeable = measurables.first().measure(
            constraints.copy(
                minWidth = 0,
                minHeight = 0,
            ),
        )

        val parentCenterX = x + popupIntSize.width / 2

        val topY = y - placeable.height
        val bottomY = y + popupIntSize.height

        val childX = parentCenterX - placeable.width / 2
        val childY = if (topY < 0) bottomY else topY

        layout(constraints.maxWidth, constraints.maxHeight) {
            placeable.place(
                x = childX.coerceIn(0, constraints.maxWidth - placeable.width),
                y = childY.coerceIn(0, constraints.maxHeight - placeable.height),
            )
        }
    }
}

@Composable
internal fun FolderGridItemPopup(
    modifier: Modifier = Modifier,
    gridItemSource: GridItemSource?,
    paddingValues: PaddingValues,
    popupIntOffset: IntOffset?,
    popupIntSize: IntSize?,
    currentPage: Int,
    drag: Drag,
    eblanAppWidgetProviderInfosGroup: Map<String, List<EblanAppWidgetProviderInfo>>,
    eblanShortcutInfosGroup: Map<EblanShortcutInfoByGroup, List<EblanShortcutInfo>>,
    gridItemSettings: GridItemSettings,
    hasShortcutHostPermission: Boolean,
    onDraggingShortcutInfoGridItem: () -> Unit,
    onTapShortcutInfo: (
        serialNumber: Long,
        packageName: String,
        shortcutId: String,
    ) -> Unit,
    onUpdateGridItemSource: (GridItemSource) -> Unit,
    onUpdateImageBitmap: (ImageBitmap) -> Unit,
    onUpdateOverlayBounds: (
        intOffset: IntOffset,
        intSize: IntSize,
    ) -> Unit,
    onUpdateSharedElementKey: (SharedElementKey?) -> Unit,
    onWidgets: (EblanApplicationInfoGroup) -> Unit,
    onDeleteGridItem: (GridItem) -> Unit,
    onDismissRequest: () -> Unit,
    onEdit: (String) -> Unit,
    onDismissFolder: () -> Unit,
    onUpdateIsVisibleOverlay: (Boolean) -> Unit,
    onInfo: (Long, String) -> Unit,
) {
    if (popupIntOffset == null || popupIntSize == null) return

    val gridItemSourceFolder = gridItemSource as? GridItemSource.Folder ?: return

    val density = LocalDensity.current

    val leftPadding = with(density) {
        paddingValues.calculateStartPadding(LayoutDirection.Ltr).roundToPx()
    }

    val topPadding = with(density) {
        paddingValues.calculateTopPadding().roundToPx()
    }
    val x = popupIntOffset.x - leftPadding

    val y = popupIntOffset.y - topPadding

    Layout(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        awaitRelease()

                        onDismissRequest()
                    },
                )
            }
            .fillMaxSize()
            .padding(paddingValues),
        content = {
            when (val data = gridItemSourceFolder.folderGridItem.data) {
                is GridItemData.ApplicationInfo -> {
                    ApplicationInfoFolderGridItemPopupContent(
                        modifier = modifier,
                        currentPage = currentPage,
                        drag = drag,
                        eblanAppWidgetProviderInfosByPackageName = eblanAppWidgetProviderInfosGroup[data.packageName],
                        eblanShortcutInfosByPackageName = eblanShortcutInfosGroup[
                            EblanShortcutInfoByGroup(
                                serialNumber = data.serialNumber,
                                packageName = data.packageName,
                            ),
                        ],
                        gridItemSettings = gridItemSettings,
                        hasShortcutHostPermission = hasShortcutHostPermission,
                        icon = data.icon,
                        onDraggingShortcutInfoGridItem = {
                            onDraggingShortcutInfoGridItem()

                            onDismissRequest()

                            onDismissFolder()
                        },
                        onDelete = {
                            onDeleteGridItem(gridItemSource.folderGridItem)

                            onDismissRequest()
                        },
                        onEdit = {
                            onEdit(gridItemSourceFolder.folderGridItem.id)

                            onDismissRequest()
                        },
                        onTapShortcutInfo = { serialNumber, packageName, shortcutId ->
                            onTapShortcutInfo(
                                serialNumber,
                                packageName,
                                shortcutId,
                            )

                            onDismissRequest()
                        },
                        onUpdateGridItemSource = onUpdateGridItemSource,
                        onUpdateImageBitmap = onUpdateImageBitmap,
                        onUpdateOverlayBounds = onUpdateOverlayBounds,
                        onUpdateSharedElementKey = onUpdateSharedElementKey,
                        onWidgets = {
                            onWidgets(
                                EblanApplicationInfoGroup(
                                    serialNumber = data.serialNumber,
                                    packageName = data.packageName,
                                    icon = data.icon,
                                    label = data.label,
                                ),
                            )

                            onDismissRequest()

                            onDismissFolder()
                        },
                        onUpdateIsVisibleOverlay = onUpdateIsVisibleOverlay,
                        onInfo = {
                            onInfo(
                                data.serialNumber,
                                data.componentName,
                            )

                            onDismissRequest()
                        },
                    )
                }

                is GridItemData.ShortcutConfig -> TODO()

                is GridItemData.ShortcutInfo -> TODO()

                else -> Unit
            }
        },
    ) { measurables, constraints ->
        val placeable = measurables.first().measure(
            constraints.copy(
                minWidth = 0,
                minHeight = 0,
            ),
        )

        val parentCenterX = x + popupIntSize.width / 2

        val topY = y - placeable.height
        val bottomY = y + popupIntSize.height

        val childX = parentCenterX - placeable.width / 2
        val childY = if (topY < 0) bottomY else topY

        layout(constraints.maxWidth, constraints.maxHeight) {
            placeable.place(
                x = childX.coerceIn(0, constraints.maxWidth - placeable.width),
                y = childY.coerceIn(0, constraints.maxHeight - placeable.height),
            )
        }
    }
}

@Composable
private fun GridItemPopupContent(
    modifier: Modifier = Modifier,
    currentPage: Int,
    drag: Drag,
    eblanAppWidgetProviderInfosGroup: Map<String, List<EblanAppWidgetProviderInfo>>,
    eblanShortcutInfosGroup: Map<EblanShortcutInfoByGroup, List<EblanShortcutInfo>>,
    gridItem: GridItem,
    gridItemSettings: GridItemSettings,
    hasShortcutHostPermission: Boolean,
    onDeleteGridItem: (GridItem) -> Unit,
    onDismissRequest: () -> Unit,
    onDraggingShortcutInfoGridItem: () -> Unit,
    onEdit: (String) -> Unit,
    onInfo: (
        serialNumber: Long,
        componentName: String,
    ) -> Unit,
    onResize: () -> Unit,
    onTapShortcutInfo: (
        serialNumber: Long,
        packageName: String,
        shortcutId: String,
    ) -> Unit,
    onUpdateGridItemSource: (GridItemSource) -> Unit,
    onUpdateImageBitmap: (ImageBitmap) -> Unit,
    onUpdateOverlayBounds: (
        intOffset: IntOffset,
        intSize: IntSize,
    ) -> Unit,
    onUpdateSharedElementKey: (SharedElementKey?) -> Unit,
    onWidgets: (EblanApplicationInfoGroup) -> Unit,
    onUpdateIsVisibleOverlay: (Boolean) -> Unit,
) {
    Surface(
        modifier = modifier.padding(5.dp),
        shape = RoundedCornerShape(30.dp),
        shadowElevation = 2.dp,
        content = {
            when (val data = gridItem.data) {
                is GridItemData.ApplicationInfo -> {
                    ApplicationInfoGridItemMenu(
                        currentPage = currentPage,
                        drag = drag,
                        eblanAppWidgetProviderInfosByPackageName = eblanAppWidgetProviderInfosGroup[data.packageName],
                        eblanShortcutInfosByPackageName = eblanShortcutInfosGroup[
                            EblanShortcutInfoByGroup(
                                serialNumber = data.serialNumber,
                                packageName = data.packageName,
                            ),
                        ],
                        gridItemSettings = gridItemSettings,
                        hasShortcutHostPermission = hasShortcutHostPermission,
                        icon = data.icon,
                        onDelete = {
                            onDeleteGridItem(gridItem)

                            onDismissRequest()
                        },
                        onDraggingShortcutInfoGridItem = {
                            onDraggingShortcutInfoGridItem()

                            onDismissRequest()
                        },
                        onEdit = {
                            onDismissRequest()

                            onEdit(gridItem.id)
                        },
                        onInfo = {
                            onInfo(
                                data.serialNumber,
                                data.componentName,
                            )

                            onDismissRequest()
                        },
                        onResize = {
                            onResize()

                            onDismissRequest()
                        },
                        onTapShortcutInfo = { serialNumber, packageName, shortcutId ->
                            onTapShortcutInfo(
                                serialNumber,
                                packageName,
                                shortcutId,
                            )

                            onDismissRequest()
                        },
                        onUpdateGridItemSource = onUpdateGridItemSource,
                        onUpdateImageBitmap = onUpdateImageBitmap,
                        onUpdateOverlayBounds = onUpdateOverlayBounds,
                        onUpdateSharedElementKey = onUpdateSharedElementKey,
                        onWidgets = {
                            onWidgets(
                                EblanApplicationInfoGroup(
                                    serialNumber = data.serialNumber,
                                    packageName = data.packageName,
                                    icon = data.icon,
                                    label = data.label,
                                ),
                            )

                            onDismissRequest()
                        },
                        onUpdateIsVisibleOverlay = onUpdateIsVisibleOverlay,
                    )
                }

                is GridItemData.Folder, is GridItemData.ShortcutInfo, is GridItemData.ShortcutConfig -> {
                    GridItemMenu(
                        onDelete = {
                            onDeleteGridItem(gridItem)

                            onDismissRequest()
                        },
                        onEdit = {
                            onEdit(gridItem.id)

                            onDismissRequest()
                        },
                        onResize = {
                            onResize()

                            onDismissRequest()
                        },
                    )
                }

                is GridItemData.Widget -> {
                    val showResize = data.resizeMode != AppWidgetProviderInfo.RESIZE_NONE

                    WidgetGridItemMenu(
                        showResize = showResize,
                        onDelete = {
                            onDeleteGridItem(gridItem)

                            onDismissRequest()
                        },
                        onResize = {
                            onResize()

                            onDismissRequest()
                        },
                    )
                }
            }
        },
    )
}

@Composable
private fun ApplicationInfoFolderGridItemPopupContent(
    modifier: Modifier = Modifier,
    currentPage: Int,
    drag: Drag,
    eblanAppWidgetProviderInfosByPackageName: List<EblanAppWidgetProviderInfo>?,
    eblanShortcutInfosByPackageName: List<EblanShortcutInfo>?,
    gridItemSettings: GridItemSettings,
    hasShortcutHostPermission: Boolean,
    icon: String?,
    onDelete: () -> Unit,
    onDraggingShortcutInfoGridItem: () -> Unit,
    onEdit: () -> Unit,
    onTapShortcutInfo: (
        serialNumber: Long,
        packageName: String,
        shortcutId: String,
    ) -> Unit,
    onUpdateGridItemSource: (GridItemSource) -> Unit,
    onUpdateImageBitmap: (ImageBitmap) -> Unit,
    onUpdateOverlayBounds: (
        intOffset: IntOffset,
        intSize: IntSize,
    ) -> Unit,
    onUpdateSharedElementKey: (SharedElementKey?) -> Unit,
    onWidgets: () -> Unit,
    onUpdateIsVisibleOverlay: (Boolean) -> Unit,
    onInfo: () -> Unit,
) {
    Surface(
        modifier = modifier.width(IntrinsicSize.Max),
        shape = RoundedCornerShape(30.dp),
        shadowElevation = 2.dp,
        content = {
            Column(
                modifier = modifier,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (hasShortcutHostPermission &&
                    !eblanShortcutInfosByPackageName.isNullOrEmpty()
                ) {
                    ShortcutInfoMenu(
                        modifier = modifier,
                        currentPage = currentPage,
                        drag = drag,
                        eblanShortcutInfosGroup = eblanShortcutInfosByPackageName,
                        gridItemSettings = gridItemSettings,
                        icon = icon,
                        onDraggingShortcutInfoGridItem = onDraggingShortcutInfoGridItem,
                        onTapShortcutInfo = onTapShortcutInfo,
                        onUpdateGridItemSource = onUpdateGridItemSource,
                        onUpdateImageBitmap = onUpdateImageBitmap,
                        onUpdateOverlayBounds = onUpdateOverlayBounds,
                        onUpdateSharedElementKey = onUpdateSharedElementKey,
                        onUpdateIsVisibleOverlay = onUpdateIsVisibleOverlay,
                    )

                    Spacer(modifier = Modifier.height(5.dp))
                }

                Row(modifier = modifier) {
                    IconButton(
                        onClick = onEdit,
                    ) {
                        Icon(imageVector = EblanLauncherIcons.Edit, contentDescription = null)
                    }

                    IconButton(
                        onClick = onInfo,
                    ) {
                        Icon(imageVector = EblanLauncherIcons.Info, contentDescription = null)
                    }

                    IconButton(
                        onClick = onDelete,
                    ) {
                        Icon(imageVector = EblanLauncherIcons.Delete, contentDescription = null)
                    }

                    if (!eblanAppWidgetProviderInfosByPackageName.isNullOrEmpty()) {
                        IconButton(
                            onClick = onWidgets,
                        ) {
                            Icon(
                                imageVector = EblanLauncherIcons.Widgets,
                                contentDescription = null,
                            )
                        }
                    }
                }
            }
        },
    )
}

@Composable
private fun ApplicationInfoGridItemMenu(
    modifier: Modifier = Modifier,
    currentPage: Int,
    drag: Drag,
    eblanAppWidgetProviderInfosByPackageName: List<EblanAppWidgetProviderInfo>?,
    eblanShortcutInfosByPackageName: List<EblanShortcutInfo>?,
    gridItemSettings: GridItemSettings,
    hasShortcutHostPermission: Boolean,
    icon: String?,
    onDelete: () -> Unit,
    onDraggingShortcutInfoGridItem: () -> Unit,
    onEdit: () -> Unit,
    onInfo: () -> Unit,
    onResize: () -> Unit,
    onTapShortcutInfo: (
        serialNumber: Long,
        packageName: String,
        shortcutId: String,
    ) -> Unit,
    onUpdateGridItemSource: (GridItemSource) -> Unit,
    onUpdateImageBitmap: (ImageBitmap) -> Unit,
    onUpdateOverlayBounds: (
        intOffset: IntOffset,
        intSize: IntSize,
    ) -> Unit,
    onUpdateSharedElementKey: (SharedElementKey?) -> Unit,
    onWidgets: () -> Unit,
    onUpdateIsVisibleOverlay: (Boolean) -> Unit,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (hasShortcutHostPermission &&
            !eblanShortcutInfosByPackageName.isNullOrEmpty()
        ) {
            ShortcutInfoMenu(
                modifier = modifier,
                currentPage = currentPage,
                drag = drag,
                eblanShortcutInfosGroup = eblanShortcutInfosByPackageName,
                gridItemSettings = gridItemSettings,
                icon = icon,
                onDraggingShortcutInfoGridItem = onDraggingShortcutInfoGridItem,
                onTapShortcutInfo = onTapShortcutInfo,
                onUpdateGridItemSource = onUpdateGridItemSource,
                onUpdateImageBitmap = onUpdateImageBitmap,
                onUpdateOverlayBounds = onUpdateOverlayBounds,
                onUpdateSharedElementKey = onUpdateSharedElementKey,
                onUpdateIsVisibleOverlay = onUpdateIsVisibleOverlay,
            )

            Spacer(modifier = Modifier.height(5.dp))
        }

        Row {
            IconButton(
                onClick = onEdit,
            ) {
                Icon(imageVector = EblanLauncherIcons.Edit, contentDescription = null)
            }

            IconButton(
                onClick = onResize,
            ) {
                Icon(imageVector = EblanLauncherIcons.Resize, contentDescription = null)
            }

            IconButton(
                onClick = onInfo,
            ) {
                Icon(imageVector = EblanLauncherIcons.Info, contentDescription = null)
            }

            IconButton(
                onClick = onDelete,
            ) {
                Icon(imageVector = EblanLauncherIcons.Delete, contentDescription = null)
            }

            if (!eblanAppWidgetProviderInfosByPackageName.isNullOrEmpty()) {
                IconButton(
                    onClick = onWidgets,
                ) {
                    Icon(
                        imageVector = EblanLauncherIcons.Widgets,
                        contentDescription = null,
                    )
                }
            }
        }
    }
}

@Composable
private fun GridItemMenu(
    modifier: Modifier = Modifier,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    onResize: () -> Unit,
) {
    Row(modifier = modifier) {
        IconButton(
            onClick = onEdit,
        ) {
            Icon(imageVector = EblanLauncherIcons.Edit, contentDescription = null)
        }

        IconButton(
            onClick = onResize,
        ) {
            Icon(imageVector = EblanLauncherIcons.Resize, contentDescription = null)
        }

        IconButton(
            onClick = onDelete,
        ) {
            Icon(imageVector = EblanLauncherIcons.Delete, contentDescription = null)
        }
    }
}

@Composable
private fun WidgetGridItemMenu(
    modifier: Modifier = Modifier,
    showResize: Boolean,
    onDelete: () -> Unit,
    onResize: () -> Unit,
) {
    Row(modifier = modifier) {
        if (showResize) {
            IconButton(
                onClick = onResize,
            ) {
                Icon(
                    imageVector = EblanLauncherIcons.Resize,
                    contentDescription = null,
                )
            }
        }

        IconButton(
            onClick = onDelete,
        ) {
            Icon(
                imageVector = EblanLauncherIcons.Delete,
                contentDescription = null,
            )
        }
    }
}
