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
package com.eblan.launcher.feature.home.screen.shortcutconfig

import androidx.activity.compose.BackHandler
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberOverscrollEffect
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
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
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
import com.eblan.launcher.domain.model.Associate
import com.eblan.launcher.domain.model.EblanAction
import com.eblan.launcher.domain.model.EblanActionType
import com.eblan.launcher.domain.model.EblanApplicationInfoGroup
import com.eblan.launcher.domain.model.EblanShortcutConfig
import com.eblan.launcher.domain.model.EblanUser
import com.eblan.launcher.domain.model.GridItem
import com.eblan.launcher.domain.model.GridItemData
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
internal fun ShortcutConfigScreen(
    modifier: Modifier = Modifier,
    currentPage: Int,
    drag: Drag,
    eblanShortcutConfigs: Map<EblanUser, Map<EblanApplicationInfoGroup, List<EblanShortcutConfig>>>,
    gridItemSettings: GridItemSettings,
    isPressHome: Boolean,
    paddingValues: PaddingValues,
    screenHeight: Int,
    offsetY: Float,
    alpha: Float,
    cornerSize: Dp,
    onDismiss: () -> Unit,
    onDraggingGridItem: () -> Unit,
    onGetEblanShortcutConfigsByLabel: (String) -> Unit,
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
    onUpdateAssociate: (Associate) -> Unit,
) {
    BackHandler(enabled = offsetY < screenHeight.toFloat()) {
        onDismiss()
    }

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
            modifier = modifier,
            currentPage = currentPage,
            drag = drag,
            eblanShortcutConfigs = eblanShortcutConfigs,
            gridItemSettings = gridItemSettings,
            isPressHome = isPressHome,
            paddingValues = paddingValues,
            onDismiss = onDismiss,
            onDragEnd = onDragEnd,
            onDraggingGridItem = onDraggingGridItem,
            onGetEblanShortcutConfigsByLabel = onGetEblanShortcutConfigsByLabel,
            onUpdateOverlayBounds = onUpdateOverlayBounds,
            onVerticalDrag = onVerticalDrag,
            onUpdateImageBitmap = onUpdateImageBitmap,
            onUpdateGridItemSource = onUpdateGridItemSource,
            onUpdateSharedElementKey = onUpdateSharedElementKey,
            onUpdateIsLongPressAndIsDragging = onUpdateIsLongPressAndIsDragging,
            onUpdateAssociate = onUpdateAssociate,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class, FlowPreview::class)
@Composable
private fun Success(
    modifier: Modifier = Modifier,
    currentPage: Int,
    drag: Drag,
    eblanShortcutConfigs: Map<EblanUser, Map<EblanApplicationInfoGroup, List<EblanShortcutConfig>>>,
    gridItemSettings: GridItemSettings,
    isPressHome: Boolean,
    paddingValues: PaddingValues,
    onDismiss: () -> Unit,
    onDragEnd: (Float) -> Unit,
    onDraggingGridItem: () -> Unit,
    onGetEblanShortcutConfigsByLabel: (String) -> Unit,
    onUpdateOverlayBounds: (
        intOffset: IntOffset,
        intSize: IntSize,
    ) -> Unit,
    onVerticalDrag: (Float) -> Unit,
    onUpdateImageBitmap: (ImageBitmap) -> Unit,
    onUpdateGridItemSource: (GridItemSource) -> Unit,
    onUpdateSharedElementKey: (SharedElementKey?) -> Unit,
    onUpdateIsLongPressAndIsDragging: () -> Unit,
    onUpdateAssociate: (Associate) -> Unit,
) {
    val horizontalPagerState = rememberPagerState(
        pageCount = {
            eblanShortcutConfigs.keys.size
        },
    )

    val searchBarState = rememberSearchBarState()

    val textFieldState = rememberTextFieldState()

    val scope = rememberCoroutineScope()

    LaunchedEffect(key1 = textFieldState) {
        snapshotFlow { textFieldState.text }.debounce(500L).onEach { text ->
            onGetEblanShortcutConfigsByLabel(text.toString())
        }.collect()
    }

    LaunchedEffect(key1 = isPressHome) {
        if (isPressHome) {
            onDismiss()
        }

        if (isPressHome && searchBarState.currentValue == SearchBarValue.Expanded) {
            searchBarState.animateToCollapsed()
        }
    }

    LaunchedEffect(key1 = drag) {
        if (drag == Drag.Start && searchBarState.currentValue == SearchBarValue.Expanded) {
            searchBarState.animateToCollapsed()
        }
    }

    Column(
        modifier = modifier
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
                    placeholder = { Text(text = "Search Applications") },
                )
            },
        )

        if (eblanShortcutConfigs.keys.size > 1) {
            EblanShortcutConfigTabRow(
                currentPage = horizontalPagerState.currentPage,
                eblanShortcutConfigs = eblanShortcutConfigs,
                onAnimateScrollToPage = horizontalPagerState::animateScrollToPage,
            )

            HorizontalPager(
                modifier = Modifier.fillMaxSize(),
                state = horizontalPagerState,
            ) { index ->
                EblanShortcutConfigsPage(
                    currentPage = currentPage,
                    drag = drag,
                    eblanShortcutConfigs = eblanShortcutConfigs,
                    gridItemSettings = gridItemSettings,
                    index = index,
                    paddingValues = paddingValues,
                    onDragEnd = onDragEnd,
                    onDraggingGridItem = onDraggingGridItem,
                    onUpdateOverlayBounds = onUpdateOverlayBounds,
                    onVerticalDrag = onVerticalDrag,
                    onUpdateImageBitmap = onUpdateImageBitmap,
                    onUpdateGridItemSource = onUpdateGridItemSource,
                    onUpdateSharedElementKey = onUpdateSharedElementKey,
                    onDismiss = onDismiss,
                    onUpdateIsLongPressAndIsDragging = onUpdateIsLongPressAndIsDragging,
                    onUpdateAssociate = onUpdateAssociate,
                )
            }
        } else {
            EblanShortcutConfigsPage(
                currentPage = currentPage,
                drag = drag,
                eblanShortcutConfigs = eblanShortcutConfigs,
                gridItemSettings = gridItemSettings,
                index = 0,
                paddingValues = paddingValues,
                onDragEnd = onDragEnd,
                onDraggingGridItem = onDraggingGridItem,
                onUpdateOverlayBounds = onUpdateOverlayBounds,
                onVerticalDrag = onVerticalDrag,
                onUpdateImageBitmap = onUpdateImageBitmap,
                onUpdateGridItemSource = onUpdateGridItemSource,
                onUpdateSharedElementKey = onUpdateSharedElementKey,
                onDismiss = onDismiss,
                onUpdateIsLongPressAndIsDragging = onUpdateIsLongPressAndIsDragging,
                onUpdateAssociate = onUpdateAssociate,
            )
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun EblanShortcutConfigTabRow(
    currentPage: Int,
    eblanShortcutConfigs: Map<EblanUser, Map<EblanApplicationInfoGroup, List<EblanShortcutConfig>>>,
    onAnimateScrollToPage: suspend (Int) -> Unit,
) {
    val scope = rememberCoroutineScope()

    SecondaryTabRow(selectedTabIndex = currentPage) {
        eblanShortcutConfigs.keys.forEachIndexed { index, eblanUser ->
            Tab(
                selected = currentPage == index,
                onClick = {
                    scope.launch {
                        onAnimateScrollToPage(index)
                    }
                },
                text = {
                    Text(
                        text = eblanUser.eblanUserType.name,
                        maxLines = 1,
                    )
                },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalSharedTransitionApi::class)
@Composable
private fun EblanShortcutConfigsPage(
    modifier: Modifier = Modifier,
    currentPage: Int,
    drag: Drag,
    eblanShortcutConfigs: Map<EblanUser, Map<EblanApplicationInfoGroup, List<EblanShortcutConfig>>>,
    gridItemSettings: GridItemSettings,
    index: Int,
    paddingValues: PaddingValues,
    onDragEnd: (Float) -> Unit,
    onDraggingGridItem: () -> Unit,
    onUpdateOverlayBounds: (IntOffset, IntSize) -> Unit,
    onVerticalDrag: (Float) -> Unit,
    onUpdateImageBitmap: (ImageBitmap) -> Unit,
    onUpdateGridItemSource: (GridItemSource) -> Unit,
    onUpdateSharedElementKey: (SharedElementKey?) -> Unit,
    onDismiss: () -> Unit,
    onUpdateIsLongPressAndIsDragging: () -> Unit,
    onUpdateAssociate: (Associate) -> Unit,
) {
    val scope = rememberCoroutineScope()

    val overscrollEffect = remember {
        OffsetOverscrollEffect(
            scope = scope,
            onVerticalDrag = onVerticalDrag,
            onDragEnd = onDragEnd,
        )
    }

    val lazyListState = rememberLazyListState()

    val serialNumber = eblanShortcutConfigs.keys.toList().getOrElse(
        index = index,
        defaultValue = {
            0
        },
    )

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

    Box(
        modifier = modifier
            .run {
                if (!canOverscroll) {
                    nestedScroll(nestedScrollConnection)
                } else {
                    this
                }
            }
            .fillMaxSize(),
    ) {
        LazyColumn(
            state = lazyListState,
            modifier = Modifier.matchParentSize(),
            contentPadding = PaddingValues(
                bottom = paddingValues.calculateBottomPadding(),
            ),
            overscrollEffect = if (canOverscroll) {
                overscrollEffect
            } else {
                rememberOverscrollEffect()
            },
        ) {
            items(eblanShortcutConfigs[serialNumber].orEmpty().keys.toList()) { eblanApplicationInfoGroup ->
                key(eblanApplicationInfoGroup.serialNumber, eblanApplicationInfoGroup.packageName) {
                    EblanApplicationInfoItem(
                        modifier = modifier,
                        currentPage = currentPage,
                        drag = drag,
                        eblanApplicationInfoGroup = eblanApplicationInfoGroup,
                        eblanShortcutConfigs = eblanShortcutConfigs[serialNumber].orEmpty(),
                        gridItemSettings = gridItemSettings,
                        onDraggingGridItem = onDraggingGridItem,
                        onUpdateOverlayBounds = onUpdateOverlayBounds,
                        onUpdateImageBitmap = onUpdateImageBitmap,
                        onUpdateGridItemSource = onUpdateGridItemSource,
                        onUpdateSharedElementKey = onUpdateSharedElementKey,
                        onDismiss = onDismiss,
                        onUpdateIsLongPressAndIsDragging = onUpdateIsLongPressAndIsDragging,
                        onUpdateAssociate = onUpdateAssociate,
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
    currentPage: Int,
    drag: Drag,
    eblanApplicationInfoGroup: EblanApplicationInfoGroup,
    eblanShortcutConfigs: Map<EblanApplicationInfoGroup, List<EblanShortcutConfig>>,
    gridItemSettings: GridItemSettings,
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
    onUpdateAssociate: (Associate) -> Unit,
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

            eblanShortcutConfigs[eblanApplicationInfoGroup]?.forEach { eblanShortcutConfig ->
                EblanShortcutConfigItem(
                    currentPage = currentPage,
                    drag = drag,
                    eblanShortcutConfig = eblanShortcutConfig,
                    gridItemSettings = gridItemSettings,
                    onDraggingGridItem = onDraggingGridItem,
                    onUpdateOverlayBounds = onUpdateOverlayBounds,
                    onUpdateImageBitmap = onUpdateImageBitmap,
                    onUpdateGridItemSource = onUpdateGridItemSource,
                    onUpdateSharedElementKey = onUpdateSharedElementKey,
                    onDismiss = onDismiss,
                    onUpdateIsLongPressAndIsDragging = onUpdateIsLongPressAndIsDragging,
                    onUpdateAssociate = onUpdateAssociate,
                )
            }
        }
    }
}

@OptIn(ExperimentalUuidApi::class, ExperimentalSharedTransitionApi::class)
@Composable
private fun EblanShortcutConfigItem(
    modifier: Modifier = Modifier,
    currentPage: Int,
    drag: Drag,
    eblanShortcutConfig: EblanShortcutConfig,
    gridItemSettings: GridItemSettings,
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
    onUpdateAssociate: (Associate) -> Unit,
) {
    val scope = rememberCoroutineScope()

    var intOffset by remember { mutableStateOf(IntOffset.Zero) }

    var intSize by remember { mutableStateOf(IntSize.Zero) }

    val graphicsLayer = rememberGraphicsLayer()

    Column(
        modifier = modifier
            .pointerInput(key1 = drag) {
                detectTapGestures(
                    onLongPress = {
                        val id = Uuid.random().toHexString()

                        scope.launch {
                            val data = GridItemData.ShortcutConfig(
                                serialNumber = eblanShortcutConfig.serialNumber,
                                componentName = eblanShortcutConfig.componentName,
                                packageName = eblanShortcutConfig.packageName,
                                activityLabel = eblanShortcutConfig.activityLabel,
                                activityIcon = eblanShortcutConfig.activityIcon,
                                applicationIcon = eblanShortcutConfig.activityIcon,
                                applicationLabel = eblanShortcutConfig.activityLabel,
                                shortcutIntentName = null,
                                shortcutIntentIcon = null,
                                shortcutIntentUri = null,
                                customIcon = null,
                                customLabel = null,
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

                            onUpdateAssociate(gridItem.associate)

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
        AsyncImage(
            model = eblanShortcutConfig.activityIcon,
            contentDescription = null,
            modifier = Modifier
                .size(gridItemSettings.iconSize.dp)
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
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = eblanShortcutConfig.activityLabel.toString(),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}
