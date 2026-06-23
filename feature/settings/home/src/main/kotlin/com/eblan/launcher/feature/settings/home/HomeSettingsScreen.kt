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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.eblan.launcher.ui.settings.GridItemSettings
import com.eblan.launcher.ui.settings.SettingsColumn
import com.eblan.launcher.ui.settings.SettingsSwitch

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
        topBar = {
            TopAppBar(
                title = {
                    Text(text = stringResource(R.string.home))
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
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            when (homeSettingsUiState) {
                HomeSettingsUiState.Loading -> {
                }

                is HomeSettingsUiState.Success -> {
                    Success(
                        homeSettings = homeSettingsUiState.homeSettings,
                        onUpdateHomeSettings = onUpdateHomeSettings,
                    )
                }
            }
        }
    }
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

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .fillMaxSize(),
    ) {
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 15.dp),
        ) {
            SettingsColumn(
                title = stringResource(R.string.grid),
                subtitle = "${homeSettings.columns}x${homeSettings.rows}",
                onClick = {
                    showGridDialog = true
                },
            )

            HorizontalDivider(modifier = Modifier.fillMaxWidth())

            SettingsSwitch(
                checked = homeSettings.infiniteScroll,
                title = stringResource(R.string.infinite_scrolling),
                subtitle = stringResource(R.string.seamless_loop_page_scroll),
                onCheckedChange = { infiniteScroll ->
                    onUpdateHomeSettings(homeSettings.copy(infiniteScroll = infiniteScroll))
                },
            )

            HorizontalDivider(modifier = Modifier.fillMaxWidth())

            SettingsSwitch(
                checked = homeSettings.wallpaperScroll,
                title = stringResource(R.string.wallpaper_scrolling),
                subtitle = stringResource(R.string.scroll_wallpaper_across_pages),
                onCheckedChange = { wallpaperScroll ->
                    onUpdateHomeSettings(homeSettings.copy(wallpaperScroll = wallpaperScroll))
                },
            )

            HorizontalDivider(modifier = Modifier.fillMaxWidth())

            SettingsSwitch(
                checked = homeSettings.lockScreenOrientation,
                title = stringResource(R.string.lock_screen_orientation),
                subtitle = stringResource(R.string.prevent_rotation_when_device_orientation_changes),
                onCheckedChange = { lockScreenOrientation ->
                    onUpdateHomeSettings(homeSettings.copy(lockScreenOrientation = lockScreenOrientation))
                },
            )

            HorizontalDivider(modifier = Modifier.fillMaxWidth())

            SettingsSwitch(
                checked = homeSettings.addNewAppsToHomeScreen,
                title = stringResource(R.string.add_new_apps),
                subtitle = stringResource(R.string.add_new_apps_to_home_screen),
                onCheckedChange = { addNewAppsToHomeScreen ->
                    onUpdateHomeSettings(homeSettings.copy(addNewAppsToHomeScreen = addNewAppsToHomeScreen))
                },
            )
        }

        Text(
            modifier = Modifier.padding(15.dp),
            text = stringResource(R.string.dock),
            style = MaterialTheme.typography.bodySmall,
        )

        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 15.dp),
        ) {
            SettingsColumn(
                title = stringResource(R.string.dock_grid),
                subtitle = "${homeSettings.dockColumns}x${homeSettings.dockRows}",
                onClick = {
                    showDockGridDialog = true
                },
            )

            HorizontalDivider(modifier = Modifier.fillMaxWidth())

            SettingsColumn(
                title = stringResource(R.string.dock_height),
                subtitle = "${homeSettings.dockHeight}",
                onClick = {
                    showDockHeightDialog = true
                },
            )

            HorizontalDivider(modifier = Modifier.fillMaxWidth())

            SettingsSwitch(
                checked = homeSettings.dockInfiniteScroll,
                title = stringResource(R.string.dock_infinite_scroll),
                subtitle = stringResource(R.string.seamless_loop_page_scroll),
                onCheckedChange = { dockInfiniteScroll ->
                    onUpdateHomeSettings(homeSettings.copy(dockInfiniteScroll = dockInfiniteScroll))
                },
            )
        }

        Text(
            modifier = Modifier.padding(15.dp),
            text = stringResource(R.string.folder),
            style = MaterialTheme.typography.bodySmall,
        )

        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 15.dp),
        ) {
            SettingsColumn(
                title = stringResource(R.string.folder_cell_dimension),
                subtitle = "${homeSettings.folderCellWidth}x${homeSettings.folderCellHeight}",
                onClick = {
                    showFolderCellDimensionDialog = true
                },
            )

            HorizontalDivider(modifier = Modifier.fillMaxWidth())

            SettingsColumn(
                title = stringResource(R.string.folder_max_grid),
                subtitle = "${homeSettings.maxFolderColumns}x${homeSettings.maxFolderRows}",
                onClick = {
                    showFolderMaxGridDialog = true
                },
            )
        }

        GridItemSettings(
            gridItemSettings = homeSettings.gridItemSettings,
            onUpdateGridItemSettings = { gridItemSettings ->
                onUpdateHomeSettings(homeSettings.copy(gridItemSettings = gridItemSettings))
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
            onUpdateDockHeight = { dockHeight ->
                onUpdateHomeSettings(
                    homeSettings.copy(
                        dockHeight = dockHeight,
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
