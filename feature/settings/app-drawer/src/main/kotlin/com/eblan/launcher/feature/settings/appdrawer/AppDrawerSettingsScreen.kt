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
package com.eblan.launcher.feature.settings.appdrawer

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
import com.eblan.launcher.domain.model.AppDrawerSettings
import com.eblan.launcher.domain.model.AppDrawerType
import com.eblan.launcher.domain.model.EblanApplicationInfo
import com.eblan.launcher.feature.settings.appdrawer.dialog.EditHorizontalGridDialog
import com.eblan.launcher.feature.settings.appdrawer.dialog.EditVerticalGridDialog
import com.eblan.launcher.feature.settings.appdrawer.dialog.HiddenEblanApplicationInfosDialog
import com.eblan.launcher.feature.settings.appdrawer.model.AppDrawerSettingsUiState
import com.eblan.launcher.ui.dialog.RadioOptionsDialog
import com.eblan.launcher.ui.dialog.TextColorDialog
import com.eblan.launcher.ui.settings.GridItemSettings
import com.eblan.launcher.ui.settings.SettingsColumn
import com.eblan.launcher.ui.settings.SettingsSwitch
import com.eblan.launcher.ui.settings.TextColorSettingsRow

@Composable
internal fun AppDrawerSettingsRoute(
    modifier: Modifier = Modifier,
    viewModel: AppDrawerSettingsViewModel = hiltViewModel(),
    onNavigateUp: () -> Unit,
) {
    val appDrawerSettingsUiState by viewModel.appDrawerSettingsUiState.collectAsStateWithLifecycle()

    AppDrawerSettingsScreen(
        modifier = modifier,
        appDrawerSettingsUiState = appDrawerSettingsUiState,
        onNavigateUp = onNavigateUp,
        onUpdateAppDrawerSettings = viewModel::updateAppDrawerSettings,
        onUpdateEblanApplicationInfo = viewModel::updateEblanApplicationInfo,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AppDrawerSettingsScreen(
    modifier: Modifier = Modifier,
    appDrawerSettingsUiState: AppDrawerSettingsUiState,
    onNavigateUp: () -> Unit,
    onUpdateAppDrawerSettings: (AppDrawerSettings) -> Unit,
    onUpdateEblanApplicationInfo: (EblanApplicationInfo) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(text = stringResource(R.string.app_drawer))
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
            when (appDrawerSettingsUiState) {
                AppDrawerSettingsUiState.Loading -> {
                }

                is AppDrawerSettingsUiState.Success -> {
                    Success(
                        appDrawerSettings = appDrawerSettingsUiState.appDrawerSettings,
                        eblanApplicationInfos = appDrawerSettingsUiState.eblanApplicationInfos,
                        onUpdateAppDrawerSettings = onUpdateAppDrawerSettings,
                        onUpdateEblanApplicationInfo = onUpdateEblanApplicationInfo,
                    )
                }
            }
        }
    }
}

@Composable
private fun Success(
    modifier: Modifier = Modifier,
    appDrawerSettings: AppDrawerSettings,
    eblanApplicationInfos: List<EblanApplicationInfo>,
    onUpdateAppDrawerSettings: (AppDrawerSettings) -> Unit,
    onUpdateEblanApplicationInfo: (EblanApplicationInfo) -> Unit,
) {
    var showAppDrawerTypeDialog by remember { mutableStateOf(false) }

    var showVerticalGridDialog by remember { mutableStateOf(false) }

    var showHorizontalGridDialog by remember { mutableStateOf(false) }

    var showHiddenEblanApplicationInfosDialog by remember { mutableStateOf(false) }

    var showTextColorDialog by remember { mutableStateOf(false) }

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
                title = stringResource(R.string.app_drawer_type),
                subtitle = appDrawerSettings.appDrawerType.name,
                onClick = {
                    showAppDrawerTypeDialog = true
                },
            )

            HorizontalDivider(modifier = Modifier.fillMaxWidth())

            when (appDrawerSettings.appDrawerType) {
                AppDrawerType.Vertical -> {
                    SettingsColumn(
                        title = stringResource(R.string.grid),
                        subtitle = "${appDrawerSettings.appDrawerColumns}x${appDrawerSettings.appDrawerRowsHeight}",
                        onClick = {
                            showVerticalGridDialog = true
                        },
                    )
                }

                AppDrawerType.Horizontal -> {
                    SettingsColumn(
                        title = stringResource(R.string.grid),
                        subtitle = "${appDrawerSettings.horizontalAppDrawerColumns}x${appDrawerSettings.horizontalAppDrawerRows}",
                        onClick = {
                            showHorizontalGridDialog = true
                        },
                    )
                }

                AppDrawerType.List -> Unit
            }

            HorizontalDivider(modifier = Modifier.fillMaxWidth())

            TextColorSettingsRow(
                textColorTitle = stringResource(R.string.background_color),
                customColorTitle = stringResource(R.string.custom_background_color),
                textColor = appDrawerSettings.backgroundColor,
                customColor = appDrawerSettings.customBackgroundColor,
                onClick = {
                    showTextColorDialog = true
                },
            )

            HorizontalDivider(modifier = Modifier.fillMaxWidth())

            SettingsColumn(
                title = stringResource(R.string.hidden_applications),
                subtitle = stringResource(R.string.hidden_applications),
                onClick = {
                    showHiddenEblanApplicationInfosDialog = true
                },
            )

            HorizontalDivider(modifier = Modifier.fillMaxWidth())

            SettingsSwitch(
                checked = appDrawerSettings.excludeTaggedApps,
                title = stringResource(R.string.exclude_tagged_apps),
                subtitle = stringResource(R.string.exclude_tagged_apps),
                onCheckedChange = { excludeTaggedApps ->
                    onUpdateAppDrawerSettings(appDrawerSettings.copy(excludeTaggedApps = excludeTaggedApps))
                },
            )
        }

        GridItemSettings(
            gridItemSettings = appDrawerSettings.gridItemSettings,
            onUpdateGridItemSettings = { gridItemSettings ->
                onUpdateAppDrawerSettings(appDrawerSettings.copy(gridItemSettings = gridItemSettings))
            },
        )
    }

    if (showAppDrawerTypeDialog) {
        RadioOptionsDialog(
            title = stringResource(R.string.app_drawer_type),
            options = AppDrawerType.entries,
            selected = appDrawerSettings.appDrawerType,
            label = {
                it.name
            },
            onDismissRequest = {
                showAppDrawerTypeDialog = false
            },
            onUpdateClick = { appDrawerType ->
                onUpdateAppDrawerSettings(appDrawerSettings.copy(appDrawerType = appDrawerType))

                showAppDrawerTypeDialog = false
            },
        )
    }

    if (showVerticalGridDialog) {
        EditVerticalGridDialog(
            appDrawerSettings = appDrawerSettings,
            onDismissRequest = {
                showVerticalGridDialog = false
            },
            onUpdateAppDrawerSettings = onUpdateAppDrawerSettings,
        )
    }

    if (showHorizontalGridDialog) {
        EditHorizontalGridDialog(
            appDrawerSettings = appDrawerSettings,
            onDismissRequest = {
                showHorizontalGridDialog = false
            },
            onUpdateAppDrawerSettings = onUpdateAppDrawerSettings,
        )
    }

    if (showHiddenEblanApplicationInfosDialog) {
        HiddenEblanApplicationInfosDialog(
            eblanApplicationInfos = eblanApplicationInfos,
            onDismissRequest = {
                showHiddenEblanApplicationInfosDialog = false
            },
            onUpdateEblanApplicationInfo = onUpdateEblanApplicationInfo,
        )
    }

    if (showTextColorDialog) {
        TextColorDialog(
            title = stringResource(R.string.background_color),
            textColor = appDrawerSettings.backgroundColor,
            customTextColor = appDrawerSettings.customBackgroundColor,
            onDismissRequest = {
                showTextColorDialog = false
            },
            onUpdateClick = { textColor, customColor ->
                onUpdateAppDrawerSettings(
                    appDrawerSettings.copy(
                        backgroundColor = textColor,
                        customBackgroundColor = customColor,
                    ),
                )

                showTextColorDialog = false
            },
        )
    }
}
