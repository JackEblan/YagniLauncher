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
package com.eblan.launcher.feature.settings.experimental

import androidx.compose.foundation.layout.Box
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
import com.eblan.launcher.domain.model.ExperimentalSettings
import com.eblan.launcher.feature.settings.experimental.dialog.SyncDataDialog
import com.eblan.launcher.feature.settings.experimental.model.ExperimentalSettingsUiState
import com.eblan.launcher.ui.settings.SettingsColumn
import com.eblan.launcher.ui.settings.SettingsSwitch
import com.eblan.launcher.common.R as commonR

@Composable
internal fun ExperimentalSettingsRoute(
    modifier: Modifier = Modifier,
    viewModel: ExperimentalSettingsViewModel = hiltViewModel(),
    onNavigateUp: () -> Unit,
) {
    val experimentalSettingsUiState by viewModel.experimentalSettingsUiState.collectAsStateWithLifecycle()

    ExperimentalSettingsScreen(
        modifier = modifier,
        experimentalSettingsUiState = experimentalSettingsUiState,
        onNavigateUp = onNavigateUp,
        onUpdateExperimentalSettings = viewModel::updateExperimentalSettings,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ExperimentalSettingsScreen(
    modifier: Modifier = Modifier,
    experimentalSettingsUiState: ExperimentalSettingsUiState,
    onNavigateUp: () -> Unit,
    onUpdateExperimentalSettings: (ExperimentalSettings) -> Unit,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(text = stringResource(commonR.string.experimental))
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
        if (experimentalSettingsUiState is ExperimentalSettingsUiState.Success) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
            ) {
                Success(
                    modifier = modifier,
                    experimentalSettings = experimentalSettingsUiState.experimentalSettings,
                    onUpdateExperimentalSettings = onUpdateExperimentalSettings,
                )
            }
        }
    }
}

@Composable
private fun Success(
    modifier: Modifier = Modifier,
    experimentalSettings: ExperimentalSettings,
    onUpdateExperimentalSettings: (ExperimentalSettings) -> Unit,
) {
    var showSyncDataDialog by remember { mutableStateOf(false) }

    Box(
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
                title = stringResource(R.string.sync_data),
                subtitle = stringResource(R.string.enable_or_disable_sync_data),
                onClick = {
                    showSyncDataDialog = true
                },
            )

            HorizontalDivider(modifier = Modifier.fillMaxWidth())

            SettingsSwitch(
                checked = experimentalSettings.lockMovement,
                title = stringResource(R.string.lock_movement),
                subtitle = stringResource(R.string.prevent_other_grid_items_from_moving),
                onCheckedChange = {
                    onUpdateExperimentalSettings(experimentalSettings.copy(lockMovement = it))
                },
            )
        }
    }

    if (showSyncDataDialog) {
        SyncDataDialog(
            syncData = experimentalSettings.syncData,
            onDismissRequest = {
                showSyncDataDialog = false
            },
            onUpdateSyncData = {
                onUpdateExperimentalSettings(experimentalSettings.copy(syncData = it))
            },
        )
    }
}
