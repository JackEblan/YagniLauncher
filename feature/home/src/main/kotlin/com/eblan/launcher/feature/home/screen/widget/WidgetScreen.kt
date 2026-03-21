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
package com.eblan.launcher.feature.home.screen.widget

import androidx.activity.compose.BackHandler
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SearchBarValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import coil3.compose.AsyncImage
import com.eblan.launcher.designsystem.icon.EblanLauncherIcons
import com.eblan.launcher.domain.model.EblanAppWidgetProviderInfo
import com.eblan.launcher.domain.model.EblanApplicationInfoGroup
import com.eblan.launcher.domain.model.GridItemSettings
import com.eblan.launcher.feature.home.component.scroll.OffsetNestedScrollConnection
import com.eblan.launcher.feature.home.component.scroll.OffsetOverscrollEffect
import com.eblan.launcher.feature.home.model.Drag
import com.eblan.launcher.feature.home.model.GridItemSource
import com.eblan.launcher.feature.home.model.SharedElementKey
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun WidgetScreen(
    modifier: Modifier = Modifier,
    columns: Int,
    currentPage: Int,
    drag: Drag,
    eblanAppWidgetProviderInfos: Map<EblanApplicationInfoGroup, List<EblanAppWidgetProviderInfo>>,
    gridItemSettings: GridItemSettings,
    isPressHome: Boolean,
    paddingValues: PaddingValues,
    rows: Int,
    screenHeight: Int,
    screenWidth: Int,
    offsetY: Float,
    alpha: Float,
    cornerSize: Dp,
    onDismiss: () -> Unit,
    onDraggingGridItem: () -> Unit,
    onGetEblanAppWidgetProviderInfosByLabel: (String) -> Unit,
    onUpdateOverlayBounds: (
        intOffset: IntOffset,
        intSize: IntSize,
    ) -> Unit,
    onUpdateImageBitmap: (ImageBitmap) -> Unit,
    onUpdateGridItemSource: (GridItemSource) -> Unit,
    onUpdateSharedElementKey: (SharedElementKey?) -> Unit,
    onUpdateIsLongPressAndIsDragging: () -> Unit,
    onVerticalDrag: (Float) -> Unit,
    onDragEnd: (Float) -> Unit,
    onUpdateIsVisibleOverlay: (Boolean) -> Unit,
) {
    Surface(
        modifier = modifier
            .offset {
                IntOffset(x = 0, y = offsetY.roundToInt())
            }
            .fillMaxSize()
            .clip(RoundedCornerShape(cornerSize))
            .alpha(alpha),
    ) {
        Success(
            columns = columns,
            currentPage = currentPage,
            drag = drag,
            eblanAppWidgetProviderInfos = eblanAppWidgetProviderInfos,
            gridItemSettings = gridItemSettings,
            isPressHome = isPressHome,
            paddingValues = paddingValues,
            rows = rows,
            screenHeight = screenHeight,
            screenWidth = screenWidth,
            offsetY = offsetY,
            onDismiss = onDismiss,
            onDragEnd = onDragEnd,
            onDraggingGridItem = onDraggingGridItem,
            onGetEblanAppWidgetProviderInfosByLabel = onGetEblanAppWidgetProviderInfosByLabel,
            onUpdateOverlayBounds = onUpdateOverlayBounds,
            onVerticalDrag = onVerticalDrag,
            onUpdateImageBitmap = onUpdateImageBitmap,
            onUpdateGridItemSource = onUpdateGridItemSource,
            onUpdateSharedElementKey = onUpdateSharedElementKey,
            onUpdateIsLongPressAndIsDragging = onUpdateIsLongPressAndIsDragging,
            onUpdateIsVisibleOverlay = onUpdateIsVisibleOverlay,
        )
    }
}

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3Api::class, FlowPreview::class)
@Composable
private fun Success(
    modifier: Modifier = Modifier,
    columns: Int,
    currentPage: Int,
    drag: Drag,
    eblanAppWidgetProviderInfos: Map<EblanApplicationInfoGroup, List<EblanAppWidgetProviderInfo>>,
    gridItemSettings: GridItemSettings,
    isPressHome: Boolean,
    paddingValues: PaddingValues,
    rows: Int,
    screenHeight: Int,
    screenWidth: Int,
    offsetY: Float,
    onDismiss: () -> Unit,
    onDragEnd: (Float) -> Unit,
    onDraggingGridItem: () -> Unit,
    onGetEblanAppWidgetProviderInfosByLabel: (String) -> Unit,
    onUpdateOverlayBounds: (
        intOffset: IntOffset,
        intSize: IntSize,
    ) -> Unit,
    onVerticalDrag: (Float) -> Unit,
    onUpdateImageBitmap: (ImageBitmap) -> Unit,
    onUpdateGridItemSource: (GridItemSource) -> Unit,
    onUpdateSharedElementKey: (SharedElementKey?) -> Unit,
    onUpdateIsLongPressAndIsDragging: () -> Unit,
    onUpdateIsVisibleOverlay: (Boolean) -> Unit,
) {
    val scope = rememberCoroutineScope()

    val lazyListState = rememberLazyListState()

    val overscrollEffect = remember(key1 = scope) {
        OffsetOverscrollEffect(
            scope = scope,
            onVerticalDrag = onVerticalDrag,
            onDragEnd = onDragEnd,
        )
    }

    val canOverscroll by remember(key1 = lazyListState) {
        derivedStateOf {
            val layoutInfo = lazyListState.layoutInfo

            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0

            val total = layoutInfo.totalItemsCount

            lastVisible < total - 1
        }
    }

    val nestedScrollConnection = remember {
        OffsetNestedScrollConnection(
            onVerticalDrag = onVerticalDrag,
            onDragEnd = onDragEnd,
        )
    }

    val searchBarState = rememberSearchBarState()

    val textFieldState = rememberTextFieldState()

    LaunchedEffect(key1 = isPressHome) {
        if (isPressHome && offsetY < screenHeight.toFloat()) {
            onDismiss()
        }

        if (isPressHome && offsetY < screenHeight.toFloat() && searchBarState.currentValue == SearchBarValue.Expanded) {
            searchBarState.animateToCollapsed()
        }
    }

    LaunchedEffect(key1 = drag) {
        if (drag == Drag.Start && searchBarState.currentValue == SearchBarValue.Expanded) {
            searchBarState.animateToCollapsed()
        }
    }

    LaunchedEffect(key1 = textFieldState) {
        snapshotFlow { textFieldState.text }.debounce(500L).onEach { text ->
            onGetEblanAppWidgetProviderInfosByLabel(text.toString())
        }.collect()
    }

    BackHandler(enabled = offsetY < screenHeight.toFloat()) {
        onDismiss()
    }

    Column(
        modifier = modifier
            .run {
                if (!canOverscroll) {
                    nestedScroll(nestedScrollConnection)
                } else {
                    this
                }
            }
            .fillMaxSize()
            .padding(
                top = paddingValues.calculateTopPadding(),
                start = paddingValues.calculateStartPadding(LayoutDirection.Ltr),
                end = paddingValues.calculateEndPadding(LayoutDirection.Ltr),
            ),
    ) {
        SearchBar(
            state = searchBarState,
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            inputField = {
                SearchBarDefaults.InputField(
                    textFieldState = textFieldState,
                    searchBarState = searchBarState,
                    leadingIcon = {
                        Icon(
                            imageVector = EblanLauncherIcons.Search,
                            contentDescription = null,
                        )
                    },
                    onSearch = { scope.launch { searchBarState.animateToCollapsed() } },
                    placeholder = { Text(text = "Search Widgets") },
                )
            },
        )

        LazyColumn(
            state = lazyListState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = paddingValues.calculateBottomPadding()),
            overscrollEffect = overscrollEffect,
        ) {
            items(eblanAppWidgetProviderInfos.keys.toList()) { eblanApplicationInfoGroup ->
                key(eblanApplicationInfoGroup.packageName) {
                    EblanApplicationInfoItem(
                        columns = columns,
                        currentPage = currentPage,
                        drag = drag,
                        eblanAppWidgetProviderInfos = eblanAppWidgetProviderInfos,
                        eblanApplicationInfoGroup = eblanApplicationInfoGroup,
                        gridItemSettings = gridItemSettings,
                        rows = rows,
                        screenHeight = screenHeight,
                        screenWidth = screenWidth,
                        onDraggingGridItem = onDraggingGridItem,
                        onUpdateOverlayBounds = onUpdateOverlayBounds,
                        onUpdateImageBitmap = onUpdateImageBitmap,
                        onUpdateGridItemSource = onUpdateGridItemSource,
                        onUpdateSharedElementKey = onUpdateSharedElementKey,
                        onDismiss = onDismiss,
                        onUpdateIsLongPressAndIsDragging = onUpdateIsLongPressAndIsDragging,
                        onUpdateIsVisibleOverlay = onUpdateIsVisibleOverlay,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun EblanApplicationInfoItem(
    modifier: Modifier = Modifier,
    columns: Int,
    currentPage: Int,
    drag: Drag,
    eblanAppWidgetProviderInfos: Map<EblanApplicationInfoGroup, List<EblanAppWidgetProviderInfo>>,
    eblanApplicationInfoGroup: EblanApplicationInfoGroup,
    gridItemSettings: GridItemSettings,
    rows: Int,
    screenHeight: Int,
    screenWidth: Int,
    onDraggingGridItem: () -> Unit,
    onUpdateOverlayBounds: (
        intOffset: IntOffset,
        intSize: IntSize,
    ) -> Unit,
    onUpdateImageBitmap: (ImageBitmap) -> Unit,
    onUpdateGridItemSource: (GridItemSource) -> Unit,
    onUpdateSharedElementKey: (SharedElementKey?) -> Unit,
    onDismiss: () -> Unit,
    onUpdateIsLongPressAndIsDragging: () -> Unit,
    onUpdateIsVisibleOverlay: (Boolean) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        expanded = !expanded
                    },
                    onLongPress = {
                        expanded = !expanded
                    },
                )
            }
            .fillMaxWidth()
            .animateContentSize(),
    ) {
        ListItem(
            headlineContent = { Text(text = eblanApplicationInfoGroup.label.toString()) },
            leadingContent = {
                AsyncImage(
                    model = eblanApplicationInfoGroup.icon,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                )
            },
            trailingContent = {
                Icon(
                    imageVector = if (expanded) {
                        EblanLauncherIcons.ArrowDropUp
                    } else {
                        EblanLauncherIcons.ArrowDropDown
                    },
                    contentDescription = null,
                )
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            modifier = Modifier.fillMaxWidth(),
        )

        if (expanded) {
            Spacer(modifier = Modifier.height(10.dp))

            eblanAppWidgetProviderInfos[eblanApplicationInfoGroup]?.forEach { eblanAppWidgetProviderInfo ->
                EblanAppWidgetProviderInfoItem(
                    columns = columns,
                    currentPage = currentPage,
                    drag = drag,
                    eblanAppWidgetProviderInfo = eblanAppWidgetProviderInfo,
                    gridItemSettings = gridItemSettings,
                    rows = rows,
                    screenHeight = screenHeight,
                    screenWidth = screenWidth,
                    onDraggingGridItem = onDraggingGridItem,
                    onUpdateOverlayBounds = onUpdateOverlayBounds,
                    onUpdateImageBitmap = onUpdateImageBitmap,
                    onUpdateGridItemSource = onUpdateGridItemSource,
                    onUpdateSharedElementKey = onUpdateSharedElementKey,
                    onDismiss = onDismiss,
                    onUpdateIsLongPressAndIsDragging = onUpdateIsLongPressAndIsDragging,
                    onUpdateIsVisibleOverlay = onUpdateIsVisibleOverlay,
                )
            }
        }
    }
}

@OptIn(ExperimentalUuidApi::class, ExperimentalSharedTransitionApi::class)
@Composable
private fun EblanAppWidgetProviderInfoItem(
    modifier: Modifier = Modifier,
    columns: Int,
    currentPage: Int,
    drag: Drag,
    eblanAppWidgetProviderInfo: EblanAppWidgetProviderInfo,
    gridItemSettings: GridItemSettings,
    rows: Int,
    screenHeight: Int,
    screenWidth: Int,
    onDraggingGridItem: () -> Unit,
    onUpdateOverlayBounds: (
        intOffset: IntOffset,
        intSize: IntSize,
    ) -> Unit,
    onUpdateImageBitmap: (ImageBitmap) -> Unit,
    onUpdateGridItemSource: (GridItemSource) -> Unit,
    onUpdateSharedElementKey: (SharedElementKey?) -> Unit,
    onDismiss: () -> Unit,
    onUpdateIsLongPressAndIsDragging: () -> Unit,
    onUpdateIsVisibleOverlay: (Boolean) -> Unit,
) {
    val scope = rememberCoroutineScope()

    var intOffset by remember { mutableStateOf(IntOffset.Zero) }

    var intSize by remember { mutableStateOf(IntSize.Zero) }

    val preview = eblanAppWidgetProviderInfo.preview ?: eblanAppWidgetProviderInfo.applicationIcon

    val graphicsLayer = rememberGraphicsLayer()

    val id = remember { Uuid.random().toHexString() }

    Column(
        modifier = modifier
            .pointerInput(key1 = drag) {
                detectTapGestures(
                    onLongPress = {
                        scope.launch {
                            onUpdateImageBitmap(graphicsLayer.toImageBitmap())

                            val gridItem = getWidgetGridItem(
                                componentName = eblanAppWidgetProviderInfo.componentName,
                                configure = eblanAppWidgetProviderInfo.configure,
                                gridItemSettings = gridItemSettings,
                                icon = eblanAppWidgetProviderInfo.applicationIcon,
                                id = id,
                                label = eblanAppWidgetProviderInfo.applicationLabel,
                                maxResizeHeight = eblanAppWidgetProviderInfo.maxResizeHeight,
                                maxResizeWidth = eblanAppWidgetProviderInfo.maxResizeWidth,
                                minHeight = eblanAppWidgetProviderInfo.minHeight,
                                minResizeHeight = eblanAppWidgetProviderInfo.minResizeHeight,
                                minResizeWidth = eblanAppWidgetProviderInfo.minResizeWidth,
                                minWidth = eblanAppWidgetProviderInfo.minWidth,
                                packageName = eblanAppWidgetProviderInfo.packageName,
                                page = currentPage,
                                preview = eblanAppWidgetProviderInfo.preview,
                                resizeMode = eblanAppWidgetProviderInfo.resizeMode,
                                serialNumber = eblanAppWidgetProviderInfo.serialNumber,
                                targetCellHeight = eblanAppWidgetProviderInfo.targetCellHeight,
                                targetCellWidth = eblanAppWidgetProviderInfo.targetCellWidth,
                            )

                            onUpdateGridItemSource(GridItemSource.New(gridItem = gridItem))

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

                            onDismiss()

                            onUpdateIsLongPressAndIsDragging()

                            onDraggingGridItem()
                        }
                    },
                )
            }
            .fillMaxWidth()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp),
        ) {
            AsyncImage(
                modifier = Modifier
                    .matchParentSize()
                    .drawWithContent {
                        graphicsLayer.record {
                            this@drawWithContent.drawContent()
                        }

                        drawLayer(graphicsLayer)
                    }
                    .onGloballyPositioned { layoutCoordinates ->
                        intOffset = layoutCoordinates.positionInRoot().round()

                        intSize = layoutCoordinates.size
                    },
                model = preview,
                contentDescription = null,
            )
        }

        val text =
            if (eblanAppWidgetProviderInfo.targetCellWidth > 0 && eblanAppWidgetProviderInfo.targetCellHeight > 0) {
                "${eblanAppWidgetProviderInfo.targetCellWidth}x${eblanAppWidgetProviderInfo.targetCellHeight}"
            } else if (eblanAppWidgetProviderInfo.minWidth > 0 && eblanAppWidgetProviderInfo.minHeight > 0) {
                val cellWidth = screenWidth / columns

                val cellHeight = screenHeight / rows

                val spanX = (eblanAppWidgetProviderInfo.minWidth + cellWidth - 1) / cellWidth

                val spanY = (eblanAppWidgetProviderInfo.minHeight + cellHeight - 1) / cellHeight

                "${spanX}x$spanY"
            } else {
                null
            }

        eblanAppWidgetProviderInfo.label?.let { label ->
            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = label,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge,
            )
        }

        if (text != null) {
            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = text,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        eblanAppWidgetProviderInfo.description?.let { description ->
            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = description,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}
