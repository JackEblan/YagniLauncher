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
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.util.Consumer
import com.eblan.launcher.designsystem.icon.EblanLauncherIcons
import com.eblan.launcher.domain.model.Associate
import com.eblan.launcher.domain.model.EditPageData
import com.eblan.launcher.domain.model.HomeSettings
import com.eblan.launcher.domain.model.PageItem
import com.eblan.launcher.domain.model.TextColor
import com.eblan.launcher.feature.home.R
import com.eblan.launcher.feature.home.component.GridLayout
import com.eblan.launcher.feature.home.model.Screen
import kotlinx.coroutines.launch

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun EditPageScreen(
    modifier: Modifier = Modifier,
    editPageData: EditPageData?,
    hasShortcutHostPermission: Boolean,
    homeSettings: HomeSettings,
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

    val layoutDirection = LocalLayoutDirection.current

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

    var expanded by remember { mutableStateOf(false) }

    DisposableEffect(key1 = activity) {
        val listener = Consumer<Intent> { intent ->
            scope.launch {
                handleActionMainIntent(
                    intent = intent,
                    onUpdateScreen = onUpdateScreen,
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

        ExpandableFloatingActionButton(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(
                    end = paddingValues.calculateEndPadding(layoutDirection),
                    bottom = paddingValues.calculateBottomPadding(),
                ),
            expanded = expanded,
            onExpandedChange = { expanded = it },
            onAdd = {
                currentPageItems = currentPageItems.toMutableList().apply {
                    add(PageItem(id = maxOf { it.id } + 1, gridItems = emptyList()))
                }
            },
            onSave = {
                onSaveEditPage(
                    selectedId,
                    currentPageItems,
                    pageItemsToDelete,
                    editPageData.associate,
                )

                expanded = false
            },
            onCancel = {
                onUpdateScreen(Screen.Pager)
            },
        )
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
private fun ExpandableFloatingActionButton(
    modifier: Modifier = Modifier,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onAdd: () -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
) {
    Column(modifier = modifier.padding(15.dp)) {
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + slideInHorizontally { it },
            exit = fadeOut() + slideOutHorizontally { it },
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                ElevatedButton(onClick = onAdd) {
                    Text(
                        text = stringResource(R.string.add),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }

                ElevatedButton(
                    onClick = onSave,
                ) {
                    Text(
                        text = stringResource(R.string.save),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }

                ElevatedButton(
                    onClick = onCancel,
                ) {
                    Text(
                        text = stringResource(R.string.cancel),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(15.dp))

        FloatingActionButton(
            modifier = Modifier.align(Alignment.End),
            onClick = {
                onExpandedChange(!expanded)
            },
        ) {
            Icon(
                imageVector = if (expanded) {
                    EblanLauncherIcons.Close
                } else {
                    EblanLauncherIcons.Add
                },
                contentDescription = null,
            )
        }
    }
}

private fun handleActionMainIntent(
    intent: Intent,
    onUpdateScreen: (Screen) -> Unit,
) {
    if (intent.action != Intent.ACTION_MAIN && !intent.hasCategory(Intent.CATEGORY_HOME)) {
        return
    }

    if ((intent.flags and Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT) != 0) {
        return
    }

    onUpdateScreen(Screen.Pager)
}
