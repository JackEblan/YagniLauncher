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
package com.eblan.launcher.feature.home.component.popup

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import coil3.compose.AsyncImage
import com.eblan.launcher.domain.model.Associate
import com.eblan.launcher.domain.model.EblanAction
import com.eblan.launcher.domain.model.EblanActionType
import com.eblan.launcher.domain.model.EblanShortcutInfo
import com.eblan.launcher.domain.model.GridItem
import com.eblan.launcher.domain.model.GridItemData
import com.eblan.launcher.domain.model.GridItemSettings
import com.eblan.launcher.feature.home.model.Drag
import com.eblan.launcher.feature.home.model.GridItemSource
import com.eblan.launcher.feature.home.model.SharedElementKey
import kotlinx.coroutines.launch
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Composable
internal fun ShortcutInfoMenu(
    modifier: Modifier = Modifier,
    currentPage: Int,
    drag: Drag,
    eblanShortcutInfosGroup: List<EblanShortcutInfo>,
    gridItemSettings: GridItemSettings,
    icon: String?,
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
    onUpdateIsVisibleOverlay: (Boolean) -> Unit,
) {
    Column(
        modifier = modifier
            .sizeIn(
                maxWidth = 300.dp,
                maxHeight = 150.dp,
            )
            .verticalScroll(rememberScrollState()),
    ) {
        eblanShortcutInfosGroup.forEach { eblanShortcutInfo ->
            ShortcutInfoMenuItem(
                currentPage = currentPage,
                drag = drag,
                eblanShortcutInfo = eblanShortcutInfo,
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
        }
    }
}

@OptIn(ExperimentalUuidApi::class)
@Composable
private fun ShortcutInfoMenuItem(
    modifier: Modifier = Modifier,
    currentPage: Int,
    drag: Drag,
    eblanShortcutInfo: EblanShortcutInfo,
    gridItemSettings: GridItemSettings,
    icon: String?,
    onDraggingShortcutInfoGridItem: () -> Unit,
    onTapShortcutInfo: (Long, String, String) -> Unit,
    onUpdateGridItemSource: (GridItemSource) -> Unit,
    onUpdateImageBitmap: (ImageBitmap) -> Unit,
    onUpdateOverlayBounds: (
        intOffset: IntOffset,
        intSize: IntSize,
    ) -> Unit,
    onUpdateSharedElementKey: (SharedElementKey?) -> Unit,
    onUpdateIsVisibleOverlay: (Boolean) -> Unit,
) {
    var intOffset by remember { mutableStateOf(IntOffset.Zero) }

    var intSize by remember { mutableStateOf(IntSize.Zero) }

    val graphicsLayer = rememberGraphicsLayer()

    val scope = rememberCoroutineScope()

    var isLongPress by remember { mutableStateOf(false) }

    LaunchedEffect(key1 = drag) {
        if (drag == Drag.End || drag == Drag.Cancel) {
            isLongPress = false
        }
    }

    ListItem(
        modifier = modifier
            .clickable {
                onTapShortcutInfo(
                    eblanShortcutInfo.serialNumber,
                    eblanShortcutInfo.packageName,
                    eblanShortcutInfo.shortcutId,
                )
            },
        headlineContent = {
            Text(text = eblanShortcutInfo.shortLabel)
        },
        leadingContent = {
            Box(
                modifier = Modifier
                    .drawWithContent {
                        graphicsLayer.record {
                            this@drawWithContent.drawContent()
                        }

                        drawLayer(graphicsLayer)
                    }
                    .pointerInput(key1 = drag) {
                        detectTapGestures(
                            onLongPress = {
                                scope.launch {
                                    val id = Uuid.random().toHexString()

                                    val data = GridItemData.ShortcutInfo(
                                        shortcutId = eblanShortcutInfo.shortcutId,
                                        packageName = eblanShortcutInfo.packageName,
                                        serialNumber = eblanShortcutInfo.serialNumber,
                                        shortLabel = eblanShortcutInfo.shortLabel,
                                        longLabel = eblanShortcutInfo.longLabel,
                                        icon = eblanShortcutInfo.icon,
                                        isEnabled = eblanShortcutInfo.isEnabled,
                                        eblanApplicationInfoIcon = icon,
                                        customIcon = null,
                                        customShortLabel = null,
                                    )

                                    val gridItem = GridItem(
                                        id = id,
                                        page = currentPage,
                                        startColumn = -1,
                                        startRow = -1,
                                        columnSpan = 1,
                                        rowSpan = 1,
                                        data = data,
                                        associate = Associate.Grid,
                                        override = false,
                                        gridItemSettings = gridItemSettings,
                                        doubleTap = EblanAction(
                                            eblanActionType = EblanActionType.None,
                                            serialNumber = 0L,
                                            componentName = "",
                                        ),
                                        swipeUp = EblanAction(
                                            eblanActionType = EblanActionType.None,
                                            serialNumber = 0L,
                                            componentName = "",
                                        ),
                                        swipeDown = EblanAction(
                                            eblanActionType = EblanActionType.None,
                                            serialNumber = 0L,
                                            componentName = "",
                                        ),
                                    )

                                    onUpdateGridItemSource(GridItemSource.New(gridItem = gridItem))

                                    onUpdateImageBitmap(graphicsLayer.toImageBitmap())

                                    onUpdateOverlayBounds(
                                        intOffset,
                                        intSize,
                                    )

                                    onUpdateSharedElementKey(
                                        SharedElementKey(
                                            id = id,
                                            parent = SharedElementKey.Parent.Grid,
                                        ),
                                    )

                                    onUpdateIsVisibleOverlay(true)

                                    onDraggingShortcutInfoGridItem()

                                    isLongPress = true
                                }
                            },
                        )
                    }
                    .onGloballyPositioned { layoutCoordinates ->
                        intOffset =
                            layoutCoordinates.positionInRoot().round()

                        intSize = layoutCoordinates.size
                    }
                    .size(30.dp),
            ) {
                if (!isLongPress) {
                    AsyncImage(
                        model = eblanShortcutInfo.icon,
                        contentDescription = null,
                        modifier = Modifier.matchParentSize(),
                    )
                }
            }
        },
    )
}
