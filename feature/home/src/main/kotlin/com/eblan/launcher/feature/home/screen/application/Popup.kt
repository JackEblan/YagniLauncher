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
package com.eblan.launcher.feature.home.screen.application

import android.graphics.Rect
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import com.eblan.launcher.domain.model.EblanApplicationInfo
import com.eblan.launcher.domain.model.EblanApplicationInfoGroup
import com.eblan.launcher.domain.model.EblanShortcutInfo
import com.eblan.launcher.domain.model.EblanShortcutInfoByGroup
import com.eblan.launcher.domain.model.GridItemSettings
import com.eblan.launcher.domain.model.MoveGridItemResult
import com.eblan.launcher.feature.home.component.PrivateShortcutInfoMenu
import com.eblan.launcher.feature.home.component.ShortcutInfoMenu
import com.eblan.launcher.feature.home.model.Drag
import com.eblan.launcher.feature.home.model.GridItemSource
import com.eblan.launcher.feature.home.model.SharedElementKey
import com.eblan.launcher.ui.local.LocalLauncherApps

@Composable
internal fun ApplicationInfoPopup(
    modifier: Modifier = Modifier,
    currentPage: Int,
    drag: Drag,
    eblanAppWidgetProviderInfos: Map<String, List<EblanAppWidgetProviderInfo>>,
    eblanShortcutInfosGroup: Map<EblanShortcutInfoByGroup, List<EblanShortcutInfo>>,
    eblanApplicationInfo: EblanApplicationInfo?,
    gridItemSettings: GridItemSettings,
    hasShortcutHostPermission: Boolean,
    paddingValues: PaddingValues,
    popupIntOffset: IntOffset,
    popupIntSize: IntSize,
    onDismissRequest: () -> Unit,
    onDraggingShortcutInfoGridItem: () -> Unit,
    onEditApplicationInfo: (
        serialNumber: Long,
        componentName: String,
    ) -> Unit,
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
    onUpdateMoveGridItemResult: (MoveGridItemResult) -> Unit,
) {
    requireNotNull(eblanApplicationInfo)

    val density = LocalDensity.current

    val launcherApps = LocalLauncherApps.current

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
            ApplicationInfoMenu(
                currentPage = currentPage,
                drag = drag,
                eblanAppWidgetProviderInfosByPackageName = eblanAppWidgetProviderInfos[eblanApplicationInfo.packageName],
                eblanShortcutInfosGroup = eblanShortcutInfosGroup[
                    EblanShortcutInfoByGroup(
                        serialNumber = eblanApplicationInfo.serialNumber,
                        packageName = eblanApplicationInfo.packageName,
                    ),
                ],
                gridItemSettings = gridItemSettings,
                hasShortcutHostPermission = hasShortcutHostPermission,
                icon = eblanApplicationInfo.icon,
                onApplicationInfo = {
                    launcherApps.startAppDetailsActivity(
                        serialNumber = eblanApplicationInfo.serialNumber,
                        componentName = eblanApplicationInfo.componentName,
                        sourceBounds = Rect(
                            x,
                            y,
                            x + popupIntSize.width,
                            y + popupIntSize.height,
                        ),
                    )

                    onDismissRequest()
                },
                onDraggingShortcutInfoGridItem = onDraggingShortcutInfoGridItem,
                onEdit = {
                    onDismissRequest()

                    onEditApplicationInfo(
                        eblanApplicationInfo.serialNumber,
                        eblanApplicationInfo.componentName,
                    )
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
                            serialNumber = eblanApplicationInfo.serialNumber,
                            packageName = eblanApplicationInfo.packageName,
                            icon = eblanApplicationInfo.icon,
                            label = eblanApplicationInfo.label,
                        ),
                    )

                    onDismissRequest()
                },
                onUpdateIsVisibleOverlay = onUpdateIsVisibleOverlay,
                onUpdateMoveGridItemResult = onUpdateMoveGridItemResult,
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
internal fun PrivateApplicationInfoPopup(
    modifier: Modifier = Modifier,
    drag: Drag,
    eblanShortcutInfosGroup: Map<EblanShortcutInfoByGroup, List<EblanShortcutInfo>>,
    eblanApplicationInfo: EblanApplicationInfo?,
    hasShortcutHostPermission: Boolean,
    paddingValues: PaddingValues,
    popupIntOffset: IntOffset,
    popupIntSize: IntSize,
    onDismissRequest: () -> Unit,
    onEditApplicationInfo: (
        serialNumber: Long,
        componentName: String,
    ) -> Unit,
    onTapShortcutInfo: (
        serialNumber: Long,
        packageName: String,
        shortcutId: String,
    ) -> Unit,
) {
    requireNotNull(eblanApplicationInfo)

    val density = LocalDensity.current

    val launcherApps = LocalLauncherApps.current

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
            PrivateApplicationInfoMenu(
                drag = drag,
                eblanShortcutInfosGroup = eblanShortcutInfosGroup[
                    EblanShortcutInfoByGroup(
                        serialNumber = eblanApplicationInfo.serialNumber,
                        packageName = eblanApplicationInfo.packageName,
                    ),
                ],
                hasShortcutHostPermission = hasShortcutHostPermission,
                onApplicationInfo = {
                    launcherApps.startAppDetailsActivity(
                        serialNumber = eblanApplicationInfo.serialNumber,
                        componentName = eblanApplicationInfo.componentName,
                        sourceBounds = Rect(
                            x,
                            y,
                            x + popupIntSize.width,
                            y + popupIntSize.height,
                        ),
                    )

                    onDismissRequest()
                },
                onEdit = {
                    onDismissRequest()

                    onEditApplicationInfo(
                        eblanApplicationInfo.serialNumber,
                        eblanApplicationInfo.componentName,
                    )
                },
                onTapShortcutInfo = { serialNumber, packageName, shortcutId ->
                    onTapShortcutInfo(
                        serialNumber,
                        packageName,
                        shortcutId,
                    )

                    onDismissRequest()
                },
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
private fun ApplicationInfoMenu(
    modifier: Modifier = Modifier,
    currentPage: Int,
    drag: Drag,
    eblanAppWidgetProviderInfosByPackageName: List<EblanAppWidgetProviderInfo>?,
    eblanShortcutInfosGroup: List<EblanShortcutInfo>?,
    gridItemSettings: GridItemSettings,
    hasShortcutHostPermission: Boolean,
    icon: String?,
    onApplicationInfo: () -> Unit,
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
    onUpdateMoveGridItemResult: (MoveGridItemResult) -> Unit,
) {
    Surface(
        modifier = modifier.padding(5.dp),
        shape = RoundedCornerShape(30.dp),
        shadowElevation = 2.dp,
        content = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (hasShortcutHostPermission &&
                    !eblanShortcutInfosGroup.isNullOrEmpty()
                ) {
                    ShortcutInfoMenu(
                        modifier = modifier,
                        currentPage = currentPage,
                        drag = drag,
                        eblanShortcutInfosGroup = eblanShortcutInfosGroup,
                        gridItemSettings = gridItemSettings,
                        icon = icon,
                        onDraggingShortcutInfoGridItem = onDraggingShortcutInfoGridItem,
                        onTapShortcutInfo = onTapShortcutInfo,
                        onUpdateGridItemSource = onUpdateGridItemSource,
                        onUpdateImageBitmap = onUpdateImageBitmap,
                        onUpdateOverlayBounds = onUpdateOverlayBounds,
                        onUpdateSharedElementKey = onUpdateSharedElementKey,
                        onUpdateIsVisibleOverlay = onUpdateIsVisibleOverlay,
                        onUpdateMoveGridItemResult = onUpdateMoveGridItemResult,
                    )

                    Spacer(modifier = Modifier.height(5.dp))
                }

                Row {
                    IconButton(
                        onClick = onApplicationInfo,
                    ) {
                        Icon(
                            imageVector = EblanLauncherIcons.Info,
                            contentDescription = null,
                        )
                    }

                    IconButton(
                        onClick = onEdit,
                    ) {
                        Icon(
                            imageVector = EblanLauncherIcons.Edit,
                            contentDescription = null,
                        )
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
private fun PrivateApplicationInfoMenu(
    modifier: Modifier = Modifier,
    drag: Drag,
    eblanShortcutInfosGroup: List<EblanShortcutInfo>?,
    hasShortcutHostPermission: Boolean,
    onApplicationInfo: () -> Unit,
    onEdit: () -> Unit,
    onTapShortcutInfo: (
        serialNumber: Long,
        packageName: String,
        shortcutId: String,
    ) -> Unit,
) {
    Surface(
        modifier = modifier.padding(5.dp),
        shape = RoundedCornerShape(30.dp),
        shadowElevation = 2.dp,
        content = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (hasShortcutHostPermission &&
                    !eblanShortcutInfosGroup.isNullOrEmpty()
                ) {
                    PrivateShortcutInfoMenu(
                        modifier = modifier,
                        drag = drag,
                        eblanShortcutInfosGroup = eblanShortcutInfosGroup,
                        onTapShortcutInfo = onTapShortcutInfo,
                    )

                    Spacer(modifier = Modifier.height(5.dp))
                }

                Row {
                    IconButton(
                        onClick = onApplicationInfo,
                    ) {
                        Icon(
                            imageVector = EblanLauncherIcons.Info,
                            contentDescription = null,
                        )
                    }

                    IconButton(
                        onClick = onEdit,
                    ) {
                        Icon(
                            imageVector = EblanLauncherIcons.Edit,
                            contentDescription = null,
                        )
                    }
                }
            }
        },
    )
}
