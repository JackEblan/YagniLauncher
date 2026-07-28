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
package com.eblan.launcher.feature.settings.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eblan.launcher.designsystem.icon.EblanLauncherIcons
import com.eblan.launcher.domain.model.HomeSettings
import com.eblan.launcher.feature.settings.home.dialog.EditDockGridDialog
import com.eblan.launcher.feature.settings.home.dialog.EditDockHeightDialog
import com.eblan.launcher.feature.settings.home.dialog.EditFolderCellDimensionDialog
import com.eblan.launcher.feature.settings.home.dialog.EditFolderMaxGridDialog
import com.eblan.launcher.feature.settings.home.dialog.EditGridDialog
import com.eblan.launcher.feature.settings.home.model.HomeSettingsUiState
import com.eblan.launcher.ui.model.SettingsItem
import com.eblan.launcher.ui.settings.GridItemSettings
import com.eblan.launcher.ui.settings.SettingsCategoryText
import com.eblan.launcher.ui.settings.SettingsItemContent
import com.eblan.launcher.common.R as commonR

@Composable
internal fun HomeSettingsRoute(
    modifier: Modifier = Modifier,
    viewModel: HomeSettingsViewModel = hiltViewModel(),
    onNavigateUp: () -> Unit,
) {
    val homeSettingsUiState by viewModel.homeSettingsUiState.collectAsStateWithLifecycle()

    HomeSettingsScreen(
        modifier = modifier,
        homeSettingsUiState = homeSettingsUiState,
        onNavigateUp = onNavigateUp,
        onUpdateHomeSettings = viewModel::updateHomeSettings,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HomeSettingsScreen(
    modifier: Modifier = Modifier,
    homeSettingsUiState: HomeSettingsUiState,
    onNavigateUp: () -> Unit,
    onUpdateHomeSettings: (HomeSettings) -> Unit,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(text = stringResource(commonR.string.home))
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(
                            imageVector = EblanLauncherIcons.ArrowBack,
                            contentDescription = null,
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            if (homeSettingsUiState is HomeSettingsUiState.Success) {
                Success(
                    homeSettings = homeSettingsUiState.homeSettings,
                    onUpdateHomeSettings = onUpdateHomeSettings,
                )
            }
        }
    }
}

@Composable
fun buildHomeSettingsItems(
    homeSettings: HomeSettings,
    onGridClick: () -> Unit,
    onUpdateHomeSettings: (HomeSettings) -> Unit,
): List<SettingsItem> = buildList {
    add(
        SettingsItem.Column(
            title = stringResource(commonR.string.grid),
            subtitle = "${homeSettings.columns}x${homeSettings.rows}",
            onClick = onGridClick,
        ),
    )

    add(
        SettingsItem.Switch(
            checked = homeSettings.infiniteScroll,
            title = stringResource(R.string.infinite_scrolling),
            subtitle = stringResource(R.string.seamless_loop_page_scroll),
            onClick = {
                onUpdateHomeSettings(
                    homeSettings.copy(infiniteScroll = !homeSettings.infiniteScroll),
                )
            },
            onCheckedChange = {
                onUpdateHomeSettings(
                    homeSettings.copy(infiniteScroll = it),
                )
            },
        ),
    )

    add(
        SettingsItem.Switch(
            checked = homeSettings.wallpaperScroll,
            title = stringResource(R.string.wallpaper_scrolling),
            subtitle = stringResource(R.string.scroll_wallpaper_across_pages),
            onClick = {
                onUpdateHomeSettings(
                    homeSettings.copy(wallpaperScroll = !homeSettings.wallpaperScroll),
                )
            },
            onCheckedChange = {
                onUpdateHomeSettings(
                    homeSettings.copy(wallpaperScroll = it),
                )
            },
        ),
    )

    add(
        SettingsItem.Switch(
            checked = homeSettings.lockScreenOrientation,
            title = stringResource(R.string.lock_screen_orientation),
            subtitle = stringResource(R.string.prevent_rotation_when_device_orientation_changes),
            onClick = {
                onUpdateHomeSettings(
                    homeSettings.copy(lockScreenOrientation = !homeSettings.lockScreenOrientation),
                )
            },
            onCheckedChange = {
                onUpdateHomeSettings(
                    homeSettings.copy(lockScreenOrientation = it),
                )
            },
        ),
    )

    add(
        SettingsItem.Switch(
            checked = homeSettings.addNewAppsToHomeScreen,
            title = stringResource(R.string.add_new_apps),
            subtitle = stringResource(R.string.add_new_apps_to_home_screen),
            onClick = {
                onUpdateHomeSettings(
                    homeSettings.copy(addNewAppsToHomeScreen = !homeSettings.addNewAppsToHomeScreen),
                )
            },
            onCheckedChange = {
                onUpdateHomeSettings(
                    homeSettings.copy(addNewAppsToHomeScreen = it),
                )
            },
        ),
    )

    add(
        SettingsItem.Switch(
            checked = homeSettings.showPageIndicator,
            title = stringResource(R.string.show_page_indicator),
            subtitle = stringResource(R.string.show_an_indicator_for_the_current_page),
            onClick = {
                onUpdateHomeSettings(
                    homeSettings.copy(showPageIndicator = !homeSettings.showPageIndicator),
                )
            },
            onCheckedChange = {
                onUpdateHomeSettings(
                    homeSettings.copy(showPageIndicator = it),
                )
            },
        ),
    )
}

@Composable
fun buildDockHomeSettingsItems(
    homeSettings: HomeSettings,
    onDockGridClick: () -> Unit,
    onDockHeightClick: () -> Unit,
    onUpdateHomeSettings: (HomeSettings) -> Unit,
): List<SettingsItem> = buildList {
    add(
        SettingsItem.Column(
            title = stringResource(R.string.dock_grid),
            subtitle = "${homeSettings.dockColumns}x${homeSettings.dockRows}",
            onClick = onDockGridClick,
        ),
    )

    add(
        SettingsItem.Column(
            title = stringResource(R.string.dock_height),
            subtitle = "${homeSettings.dockHeight}",
            onClick = onDockHeightClick,
        ),
    )

    add(
        SettingsItem.Switch(
            checked = homeSettings.dockInfiniteScroll,
            title = stringResource(R.string.dock_infinite_scroll),
            subtitle = stringResource(R.string.seamless_loop_page_scroll),
            onClick = {
                onUpdateHomeSettings(
                    homeSettings.copy(dockInfiniteScroll = !homeSettings.dockInfiniteScroll),
                )
            },
            onCheckedChange = {
                onUpdateHomeSettings(
                    homeSettings.copy(dockInfiniteScroll = it),
                )
            },
        ),
    )
}

@Composable
fun buildFolderHomeSettingsItems(
    homeSettings: HomeSettings,
    onFolderCellDimensionClick: () -> Unit,
    onFolderMaxGridClick: () -> Unit,
): List<SettingsItem> = buildList {
    add(
        SettingsItem.Column(
            title = stringResource(R.string.folder_cell_dimension),
            subtitle = "${homeSettings.folderCellWidth}x${homeSettings.folderCellHeight}",
            onClick = onFolderCellDimensionClick,
        ),
    )

    add(
        SettingsItem.Column(
            title = stringResource(R.string.folder_max_grid),
            subtitle = "${homeSettings.maxFolderColumns}x${homeSettings.maxFolderRows}",
            onClick = onFolderMaxGridClick,
        ),
    )
}

@Composable
private fun Success(
    modifier: Modifier = Modifier,
    homeSettings: HomeSettings,
    onUpdateHomeSettings: (HomeSettings) -> Unit,
) {
    var showGridDialog by remember { mutableStateOf(false) }

    var showDockGridDialog by remember { mutableStateOf(false) }

    var showDockHeightDialog by remember { mutableStateOf(false) }

    var showFolderCellDimensionDialog by remember { mutableStateOf(false) }

    var showFolderMaxGridDialog by remember { mutableStateOf(false) }

    val homeSettingsItems = buildHomeSettingsItems(
        homeSettings = homeSettings,
        onGridClick = {
            showGridDialog = true
        },
        onUpdateHomeSettings = onUpdateHomeSettings,
    )

    val dockHomeSettingsItems = buildDockHomeSettingsItems(
        homeSettings = homeSettings,
        onDockGridClick = {
            showDockGridDialog = true
        },
        onDockHeightClick = {
            showDockHeightDialog = true
        },
        onUpdateHomeSettings = onUpdateHomeSettings,
    )

    val folderHomeSettingsItems = buildFolderHomeSettingsItems(
        homeSettings = homeSettings,
        onFolderCellDimensionClick = {
            showFolderCellDimensionDialog = true
        },
        onFolderMaxGridClick = {
            showFolderMaxGridDialog = true
        },
    )

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .fillMaxSize()
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        homeSettingsItems.forEachIndexed { index, settingsItem ->
            SettingsItemContent(
                settingsItem = settingsItem,
                index = index,
                size = homeSettingsItems.size,
            )
        }

        SettingsCategoryText(text = stringResource(R.string.dock))

        dockHomeSettingsItems.forEachIndexed { index, settingsItem ->
            SettingsItemContent(
                settingsItem = settingsItem,
                index = index,
                size = homeSettingsItems.size,
            )
        }

        SettingsCategoryText(text = stringResource(R.string.folder))

        folderHomeSettingsItems.forEachIndexed { index, settingsItem ->
            SettingsItemContent(
                settingsItem = settingsItem,
                index = index,
                size = homeSettingsItems.size,
            )
        }

        GridItemSettings(
            gridItemSettings = homeSettings.gridItemSettings,
            onUpdateGridItemSettings = {
                onUpdateHomeSettings(
                    homeSettings.copy(gridItemSettings = it),
                )
            },
        )
    }

    if (showGridDialog) {
        EditGridDialog(
            homeSettings = homeSettings,
            onDismissRequest = {
                showGridDialog = false
            },
            onUpdateHomeSettings = onUpdateHomeSettings,
        )
    }

    if (showDockGridDialog) {
        EditDockGridDialog(
            homeSettings = homeSettings,
            onDismissRequest = {
                showDockGridDialog = false
            },
            onUpdateHomeSettings = onUpdateHomeSettings,
        )
    }

    if (showDockHeightDialog) {
        EditDockHeightDialog(
            dockHeight = homeSettings.dockHeight,
            onDismissRequest = {
                showDockHeightDialog = false
            },
            onUpdateDockHeight = {
                onUpdateHomeSettings(
                    homeSettings.copy(
                        dockHeight = it,
                    ),
                )

                showDockHeightDialog = false
            },
        )
    }

    if (showFolderCellDimensionDialog) {
        EditFolderCellDimensionDialog(
            homeSettings = homeSettings,
            onDismissRequest = {
                showFolderCellDimensionDialog = false
            },
            onUpdateHomeSettings = onUpdateHomeSettings,
        )
    }

    if (showFolderMaxGridDialog) {
        EditFolderMaxGridDialog(
            homeSettings = homeSettings,
            onDismissRequest = {
                showFolderMaxGridDialog = false
            },
            onUpdateHomeSettings = onUpdateHomeSettings,
        )
    }
}
