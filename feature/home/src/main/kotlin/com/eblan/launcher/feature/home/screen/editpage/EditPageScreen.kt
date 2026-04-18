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
package com.eblan.launcher.feature.home.screen.editpage

import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.core.util.Consumer
import com.eblan.launcher.designsystem.icon.EblanLauncherIcons
import com.eblan.launcher.domain.model.Associate
import com.eblan.launcher.domain.model.EditPageData
import com.eblan.launcher.domain.model.HomeSettings
import com.eblan.launcher.domain.model.PageItem
import com.eblan.launcher.domain.model.TextColor
import com.eblan.launcher.feature.home.component.GridLayout
import com.eblan.launcher.feature.home.model.Screen
import com.eblan.launcher.feature.home.util.handleActionMainIntent
import kotlinx.coroutines.launch

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun EditPageScreen(
    modifier: Modifier = Modifier,
    editPageData: EditPageData?,
    hasShortcutHostPermission: Boolean,
    homeSettings: HomeSettings,
    iconPackFilePaths: Map<String, String>,
    paddingValues: PaddingValues,
    screenHeight: Int,
    textColor: TextColor,
    onSaveEditPage: (
        id: Int,
        pageItems: List<PageItem>,
        pageItemsToDelete: List<PageItem>,
        associate: Associate,
    ) -> Unit,
    onUpdateScreen: (Screen) -> Unit,
) {
    requireNotNull(editPageData)

    val density = LocalDensity.current

    val topPadding = with(density) {
        paddingValues.calculateTopPadding().roundToPx()
    }

    val bottomPadding = with(density) {
        paddingValues.calculateBottomPadding().roundToPx()
    }

    val verticalPadding = topPadding + bottomPadding

    val gridHeight = screenHeight - verticalPadding

    var currentPageItems by remember { mutableStateOf(editPageData.pageItems) }

    val pageItemsToDelete = remember { mutableStateListOf<PageItem>() }

    var selectedId by remember {
        mutableIntStateOf(
            when (editPageData.associate) {
                Associate.Grid -> homeSettings.initialPage
                Associate.Dock -> homeSettings.dockInitialPage
            },
        )
    }

    val lazyListState = rememberLazyListState()

    val lazyColumnDragDropState =
        rememberLazyColumnDragDropState(lazyListState = lazyListState) { from, to ->
            currentPageItems = currentPageItems.toMutableList().apply {
                add(
                    index = to,
                    element = removeAt(from),
                )
            }
        }

    val isAtTop by remember(key1 = lazyListState) {
        derivedStateOf {
            lazyListState.firstVisibleItemIndex == 0 && lazyListState.firstVisibleItemScrollOffset == 0
        }
    }

    val activity = LocalActivity.current as ComponentActivity

    val scope = rememberCoroutineScope()

    val columns = when (editPageData.associate) {
        Associate.Grid -> homeSettings.columns
        Associate.Dock -> homeSettings.dockColumns
    }

    val rows = when (editPageData.associate) {
        Associate.Grid -> homeSettings.rows
        Associate.Dock -> homeSettings.dockRows
    }

    val cardHeight = when (editPageData.associate) {
        Associate.Grid -> with(density) {
            gridHeight.toDp() - homeSettings.dockHeight.dp
        }

        Associate.Dock -> homeSettings.dockHeight.dp
    }

    DisposableEffect(key1 = activity) {
        val listener = Consumer<Intent> { intent ->
            scope.launch {
                handleActionMainIntent(
                    intent = intent,
                    onActionMainIntent = {
                        onUpdateScreen(Screen.Pager)
                    },
                )
            }
        }

        activity.addOnNewIntentListener(listener)

        onDispose {
            activity.removeOnNewIntentListener(listener)
        }
    }

    BackHandler {
        onUpdateScreen(Screen.Pager)
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .dragContainer(lazyColumnDragDropState = lazyColumnDragDropState)
                .matchParentSize(),
            state = lazyListState,
            contentPadding = paddingValues,
        ) {
            itemsIndexed(
                items = currentPageItems,
                key = { _, pageItem -> pageItem.id },
            ) { index, pageItem ->
                DraggableItem(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    lazyColumnDragDropState = lazyColumnDragDropState,
                    index = index,
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                                shape = RoundedCornerShape(8.dp),
                            ),
                    ) {
                        GridLayout(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(cardHeight),
                            columns = columns,
                            gridItems = pageItem.gridItems,
                            rows = rows,
                            content = { gridItem ->
                                GridItemContent(
                                    gridItem = gridItem,
                                    gridItemSettings = homeSettings.gridItemSettings,
                                    hasShortcutHostPermission = hasShortcutHostPermission,
                                    iconPackFilePaths = iconPackFilePaths,
                                    statusBarNotifications = emptyMap(),
                                    textColor = textColor,
                                )
                            },
                        )

                        PageButtons(
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .padding(5.dp),
                            pageItem = pageItem,
                            selectedId = selectedId,
                            onDeleteClick = {
                                currentPageItems = currentPageItems.toMutableList().apply {
                                    removeIf { currentPageItem ->
                                        currentPageItem.id == pageItem.id
                                    }
                                }

                                pageItemsToDelete.add(pageItem)
                            },
                            onHomeClick = {
                                selectedId = pageItem.id
                            },
                        )
                    }
                }
            }
        }

        AnimatedVisibility(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(paddingValues),
            visible = isAtTop,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            ActionButtons(
                onAdd = {
                    currentPageItems = currentPageItems.toMutableList().apply {
                        add(PageItem(id = maxOf { it.id } + 1, gridItems = emptyList()))
                    }
                },
                onCancel = {
                    onUpdateScreen(Screen.Pager)
                },
                onSave = {
                    onSaveEditPage(
                        selectedId,
                        currentPageItems,
                        pageItemsToDelete,
                        editPageData.associate,
                    )
                },
            )
        }
    }
}

@Composable
private fun PageButtons(
    modifier: Modifier = Modifier,
    pageItem: PageItem,
    selectedId: Int,
    onDeleteClick: () -> Unit,
    onHomeClick: () -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(30.dp),
        tonalElevation = 10.dp,
    ) {
        Row(
            modifier = Modifier.padding(5.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            IconButton(
                onClick = onDeleteClick,
                enabled = pageItem.id != selectedId,
            ) {
                Icon(
                    imageVector = EblanLauncherIcons.Delete,
                    contentDescription = null,
                )
            }

            IconButton(
                onClick = onHomeClick,
                enabled = pageItem.id != selectedId,
            ) {
                Icon(
                    imageVector = EblanLauncherIcons.Home,
                    contentDescription = null,
                )
            }
        }
    }
}

@Composable
private fun ActionButtons(
    modifier: Modifier = Modifier,
    onAdd: () -> Unit,
    onCancel: () -> Unit,
    onSave: () -> Unit,
) {
    Surface(
        modifier = modifier.padding(10.dp),
        shape = RoundedCornerShape(30.dp),
        tonalElevation = 10.dp,
    ) {
        Row(
            modifier = Modifier.padding(5.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            IconButton(onClick = onCancel) {
                Icon(
                    imageVector = EblanLauncherIcons.Close,
                    contentDescription = null,
                )
            }

            IconButton(onClick = onSave) {
                Icon(
                    imageVector = EblanLauncherIcons.Save,
                    contentDescription = null,
                )
            }

            IconButton(onClick = onAdd) {
                Icon(
                    imageVector = EblanLauncherIcons.Add,
                    contentDescription = null,
                )
            }
        }
    }
}
