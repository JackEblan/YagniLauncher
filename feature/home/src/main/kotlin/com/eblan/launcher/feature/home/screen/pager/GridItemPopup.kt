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
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.eblan.launcher.designsystem.icon.EblanLauncherIcons
import com.eblan.launcher.domain.model.EblanAppWidgetProviderInfo
import com.eblan.launcher.domain.model.EblanApplicationInfoGroup
import com.eblan.launcher.domain.model.EblanShortcutInfo
import com.eblan.launcher.domain.model.EblanShortcutInfoByGroup
import com.eblan.launcher.domain.model.GridItem
import com.eblan.launcher.domain.model.GridItemData
import com.eblan.launcher.domain.model.GridItemSettings
import com.eblan.launcher.domain.model.MoveGridItemResult
import com.eblan.launcher.feature.home.component.popup
import com.eblan.launcher.feature.home.model.GridItemSource
import com.eblan.launcher.feature.home.model.SharedElementKey
import com.eblan.launcher.feature.home.screen.shortcutinfo.ShortcutInfoScreen

@Composable
internal fun GridItemPopup(
    modifier: Modifier = Modifier,
    eblanAppWidgetProviderInfosGroup: Map<String, List<EblanAppWidgetProviderInfo>>,
    eblanShortcutInfosGroup: Map<EblanShortcutInfoByGroup, List<EblanShortcutInfo>>,
    gridItem: GridItem,
    gridItemSettings: GridItemSettings,
    hasShortcutHostPermission: Boolean,
    popupIntOffset: IntOffset?,
    popupIntSize: IntSize?,
    isVisibleOverlay: Boolean,
    paddingValues: PaddingValues,
    isCloseGridItemPopup: Boolean,
    onDeleteGridItem: (GridItem) -> Unit,
    onDismissRequest: () -> Unit,
    onUpdateIsDragging: (Boolean) -> Unit,
    onEdit: (String) -> Unit,
    onInfo: (Long, String) -> Unit,
    onResize: (GridItem) -> Unit,
    onTapShortcutInfo: (Long, String, String) -> Unit,
    onUpdateGridItemSource: (GridItemSource) -> Unit,
    onUpdateImageBitmap: (ImageBitmap) -> Unit,
    onUpdateOverlayBounds: (intOffset: IntOffset, intSize: IntSize) -> Unit,
    onUpdateSharedElementKey: (SharedElementKey?) -> Unit,
    onWidgets: (EblanApplicationInfoGroup) -> Unit,
    onUpdateIsVisibleOverlay: (Boolean) -> Unit,
    onUpdateMoveGridItemResult: (MoveGridItemResult) -> Unit,
) {
    requireNotNull(popupIntOffset)

    requireNotNull(popupIntSize)

    val density = LocalDensity.current

    val layoutDirection = LocalLayoutDirection.current

    val transitionState = remember {
        MutableTransitionState(false).apply { targetState = true }
    }

    val leftPadding =
        with(density) {
            paddingValues.calculateLeftPadding(layoutDirection).roundToPx()
        }

    val topPadding = with(density) {
        paddingValues.calculateTopPadding().roundToPx()
    }

    val x = popupIntOffset.x - leftPadding

    val y = popupIntOffset.y - topPadding

    LaunchedEffect(
        key1 = transitionState.targetState,
        key2 = transitionState.isIdle,
    ) {
        if (!transitionState.targetState && transitionState.isIdle) {
            onDismissRequest()
        }
    }

    LaunchedEffect(key1 = isCloseGridItemPopup) {
        if (isCloseGridItemPopup) {
            transitionState.targetState = false
        }
    }

    BackHandler(enabled = transitionState.targetState) {
        transitionState.targetState = false
    }

    Box(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        awaitRelease()

                        transitionState.targetState = false
                    },
                )
            }
            .fillMaxSize()
            .padding(paddingValues),
    ) {
        AnimatedVisibility(
            modifier = Modifier.popup(
                width = popupIntSize.width,
                height = popupIntSize.height,
                x = x,
                y = y,
            ),
            visibleState = transitionState,
            enter = fadeIn(tween()) + scaleIn(initialScale = 0.8f, animationSpec = tween()),
            exit = fadeOut(tween()) + scaleOut(targetScale = 0.8f, animationSpec = tween()),
        ) {
            GridItemPopupContent(
                eblanAppWidgetProviderInfosGroup = eblanAppWidgetProviderInfosGroup,
                eblanShortcutInfosGroup = eblanShortcutInfosGroup,
                gridItem = gridItem,
                gridItemSettings = gridItemSettings,
                hasShortcutHostPermission = hasShortcutHostPermission,
                isVisibleOverlay = isVisibleOverlay,
                onDeleteGridItem = onDeleteGridItem,
                onUpdateTransitionState = {
                    transitionState.targetState = it
                },
                onUpdateIsDragging = onUpdateIsDragging,
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
                onUpdateMoveGridItemResult = onUpdateMoveGridItemResult,
            )
        }
    }
}

@Composable
private fun GridItemPopupContent(
    modifier: Modifier = Modifier,
    eblanAppWidgetProviderInfosGroup: Map<String, List<EblanAppWidgetProviderInfo>>,
    eblanShortcutInfosGroup: Map<EblanShortcutInfoByGroup, List<EblanShortcutInfo>>,
    gridItem: GridItem,
    gridItemSettings: GridItemSettings,
    hasShortcutHostPermission: Boolean,
    isVisibleOverlay: Boolean,
    onDeleteGridItem: (GridItem) -> Unit,
    onUpdateTransitionState: (Boolean) -> Unit,
    onUpdateIsDragging: (Boolean) -> Unit,
    onEdit: (String) -> Unit,
    onInfo: (
        serialNumber: Long,
        componentName: String,
    ) -> Unit,
    onResize: (GridItem) -> Unit,
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
    Surface(
        modifier = modifier.padding(5.dp),
        shape = RoundedCornerShape(30.dp),
        shadowElevation = 2.dp,
        content = {
            when (val data = gridItem.data) {
                is GridItemData.ApplicationInfo -> {
                    ApplicationInfoGridItemMenu(
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
                        isVisibleOverlay = isVisibleOverlay,
                        onDelete = {
                            onDeleteGridItem(gridItem)

                            onUpdateTransitionState(false)
                        },
                        onUpdateIsDragging = {
                            onUpdateIsDragging(it)

                            onUpdateTransitionState(false)
                        },
                        onEdit = {
                            onUpdateTransitionState(false)

                            onEdit(gridItem.id)
                        },
                        onInfo = {
                            onInfo(
                                data.serialNumber,
                                data.componentName,
                            )

                            onUpdateTransitionState(false)
                        },
                        onResize = {
                            onResize(gridItem)

                            onUpdateTransitionState(false)
                        },
                        onTapShortcutInfo = { serialNumber, packageName, shortcutId ->
                            onTapShortcutInfo(
                                serialNumber,
                                packageName,
                                shortcutId,
                            )

                            onUpdateTransitionState(false)
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

                            onUpdateTransitionState(false)
                        },
                        onUpdateIsVisibleOverlay = onUpdateIsVisibleOverlay,
                        onUpdateMoveGridItemResult = onUpdateMoveGridItemResult,
                        onUpdateTransitionState = onUpdateTransitionState,
                    )
                }

                is GridItemData.Folder, is GridItemData.ShortcutInfo, is GridItemData.ShortcutConfig -> {
                    GridItemMenu(
                        onDelete = {
                            onDeleteGridItem(gridItem)

                            onUpdateTransitionState(false)
                        },
                        onEdit = {
                            onEdit(gridItem.id)

                            onUpdateTransitionState(false)
                        },
                        onResize = {
                            onResize(gridItem)

                            onUpdateTransitionState(false)
                        },
                    )
                }

                is GridItemData.Widget -> {
                    val showResize = data.resizeMode != AppWidgetProviderInfo.RESIZE_NONE

                    WidgetGridItemMenu(
                        showResize = showResize,
                        onDelete = {
                            onDeleteGridItem(gridItem)

                            onUpdateTransitionState(false)
                        },
                        onResize = {
                            onResize(gridItem)

                            onUpdateTransitionState(false)
                        },
                    )
                }
            }
        },
    )
}

@Composable
private fun ApplicationInfoGridItemMenu(
    modifier: Modifier = Modifier,
    eblanAppWidgetProviderInfosByPackageName: List<EblanAppWidgetProviderInfo>?,
    eblanShortcutInfosByPackageName: List<EblanShortcutInfo>?,
    gridItemSettings: GridItemSettings,
    hasShortcutHostPermission: Boolean,
    icon: String?,
    isVisibleOverlay: Boolean,
    onDelete: () -> Unit,
    onUpdateIsDragging: (Boolean) -> Unit,
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
    onUpdateMoveGridItemResult: (MoveGridItemResult) -> Unit,
    onUpdateTransitionState: (Boolean) -> Unit,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (hasShortcutHostPermission &&
            !eblanShortcutInfosByPackageName.isNullOrEmpty()
        ) {
            ShortcutInfoScreen(
                modifier = modifier,
                eblanShortcutInfosGroup = eblanShortcutInfosByPackageName,
                gridItemSettings = gridItemSettings,
                icon = icon,
                isVisibleOverlay = isVisibleOverlay,
                onUpdateIsDragging = onUpdateIsDragging,
                onTapShortcutInfo = onTapShortcutInfo,
                onUpdateGridItemSource = onUpdateGridItemSource,
                onUpdateImageBitmap = onUpdateImageBitmap,
                onUpdateOverlayBounds = onUpdateOverlayBounds,
                onUpdateSharedElementKey = onUpdateSharedElementKey,
                onUpdateIsVisibleOverlay = onUpdateIsVisibleOverlay,
                onUpdateMoveGridItemResult = onUpdateMoveGridItemResult,
                onUpdateTransitionState = onUpdateTransitionState,
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
