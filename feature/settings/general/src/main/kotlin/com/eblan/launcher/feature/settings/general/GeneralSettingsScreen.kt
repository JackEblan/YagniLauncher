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
package com.eblan.launcher.feature.settings.general

import android.content.Intent
import android.os.Build
import android.provider.Settings
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eblan.launcher.designsystem.icon.EblanLauncherIcons
import com.eblan.launcher.domain.model.EblanIconPackInfo
import com.eblan.launcher.domain.model.GeneralSettings
import com.eblan.launcher.domain.model.PackageManagerIconPackInfo
import com.eblan.launcher.domain.model.Theme
import com.eblan.launcher.feature.settings.general.dialog.ImportIconPackInfoDialog
import com.eblan.launcher.feature.settings.general.dialog.SelectIconPackInfoDialog
import com.eblan.launcher.feature.settings.general.model.GeneralSettingsUiState
import com.eblan.launcher.service.IconPackInfoService
import com.eblan.launcher.ui.dialog.RadioOptionsDialog
import com.eblan.launcher.ui.local.LocalSettings
import com.eblan.launcher.ui.settings.SettingsColumn
import com.eblan.launcher.ui.settings.SettingsSwitch

@Composable
internal fun GeneralSettingsRoute(
    modifier: Modifier = Modifier,
    viewModel: GeneralSettingsViewModel = hiltViewModel(),
    onNavigateUp: () -> Unit,
) {
    val generalSettingsUiState by viewModel.generalSettingsUiState.collectAsStateWithLifecycle()

    val packageManagerIconPackInfos by viewModel.packageManagerIconPackInfos.collectAsStateWithLifecycle()

    val eblanIconPackInfos by viewModel.eblanIconPackInfos.collectAsStateWithLifecycle()

    GeneralSettingsScreen(
        modifier = modifier,
        eblanIconPackInfos = eblanIconPackInfos,
        generalSettingsUiState = generalSettingsUiState,
        packageManagerIconPackInfos = packageManagerIconPackInfos,
        onDeleteEblanIconPackInfo = viewModel::deleteIconPackInfo,
        onNavigateUp = onNavigateUp,
        onUpdateGeneralSettings = viewModel::updateGeneralSettings,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun GeneralSettingsScreen(
    modifier: Modifier = Modifier,
    eblanIconPackInfos: List<EblanIconPackInfo>,
    generalSettingsUiState: GeneralSettingsUiState,
    packageManagerIconPackInfos: List<PackageManagerIconPackInfo>,
    onDeleteEblanIconPackInfo: (String) -> Unit,
    onNavigateUp: () -> Unit,
    onUpdateGeneralSettings: (GeneralSettings) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(text = stringResource(R.string.general))
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
            when (generalSettingsUiState) {
                GeneralSettingsUiState.Loading -> {
                }

                is GeneralSettingsUiState.Success -> {
                    Success(
                        modifier = modifier,
                        eblanIconPackInfos = eblanIconPackInfos,
                        generalSettings = generalSettingsUiState.generalSettings,
                        packageManagerIconPackInfos = packageManagerIconPackInfos,
                        onDeleteEblanIconPackInfo = onDeleteEblanIconPackInfo,
                        onUpdateGeneralSettings = onUpdateGeneralSettings,
                    )
                }
            }
        }
    }
}

@Composable
private fun Success(
    modifier: Modifier = Modifier,
    eblanIconPackInfos: List<EblanIconPackInfo>,
    generalSettings: GeneralSettings,
    packageManagerIconPackInfos: List<PackageManagerIconPackInfo>,
    onDeleteEblanIconPackInfo: (String) -> Unit,
    onUpdateGeneralSettings: (GeneralSettings) -> Unit,
) {
    val context = LocalContext.current

    val settings = LocalSettings.current

    var showDarkThemeConfigDialog by remember { mutableStateOf(false) }

    var showImportIconPackDialog by remember { mutableStateOf(false) }

    var selectIconPackDialog by remember { mutableStateOf(false) }

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
                title = stringResource(R.string.import_icon_pack),
                subtitle = stringResource(R.string.import_icon_pack),
                onClick = {
                    showImportIconPackDialog = true
                },
            )

            HorizontalDivider(modifier = Modifier.fillMaxWidth())

            SettingsColumn(
                title = stringResource(R.string.select_icon_pack),
                subtitle = generalSettings.iconPackInfoPackageName.ifEmpty { stringResource(R.string.default_icon_pack) },
                onClick = {
                    selectIconPackDialog = true
                },
            )

            HorizontalDivider(modifier = Modifier.fillMaxWidth())

            SettingsColumn(
                title = stringResource(R.string.theme),
                subtitle = generalSettings.theme.name,
                onClick = {
                    showDarkThemeConfigDialog = true
                },
            )

            HorizontalDivider(modifier = Modifier.fillMaxWidth())

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                SettingsSwitch(
                    checked = generalSettings.dynamicTheme,
                    title = stringResource(R.string.dynamic_theme),
                    subtitle = stringResource(R.string.dynamic_theme),
                    onCheckedChange = { dynamicTheme ->
                        onUpdateGeneralSettings(generalSettings.copy(dynamicTheme = dynamicTheme))
                    },
                )

                HorizontalDivider(modifier = Modifier.fillMaxWidth())
            }

            if (!settings.isNotificationAccessGranted()) {
                SettingsColumn(
                    title = stringResource(R.string.notification_dots),
                    subtitle = stringResource(R.string.show_notification_dots),
                    onClick = {
                        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)

                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

                        context.startActivity(intent)
                    },
                )
            }
        }
    }

    if (showDarkThemeConfigDialog) {
        RadioOptionsDialog(
            title = "Theme",
            options = Theme.entries,
            selected = generalSettings.theme,
            label = {
                it.name
            },
            onDismissRequest = {
                showDarkThemeConfigDialog = false
            },
            onUpdateClick = { darkThemeConfig ->
                onUpdateGeneralSettings(generalSettings.copy(theme = darkThemeConfig))

                showDarkThemeConfigDialog = false
            },
        )
    }

    if (showImportIconPackDialog) {
        ImportIconPackInfoDialog(
            packageManagerIconPackInfos = packageManagerIconPackInfos,
            onDismissRequest = {
                showImportIconPackDialog = false
            },
            onUpdateIconPackInfo = { packageName, label ->
                val intent = Intent(context, IconPackInfoService::class.java).apply {
                    putExtra(IconPackInfoService.ICON_PACK_INFO_PACKAGE_NAME, packageName)
                    putExtra(IconPackInfoService.ICON_PACK_INFO_LABEL, label)
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }

                showImportIconPackDialog = false
            },
        )
    }

    if (selectIconPackDialog) {
        SelectIconPackInfoDialog(
            eblanIconPackInfos = eblanIconPackInfos,
            iconPackInfoPackageName = generalSettings.iconPackInfoPackageName,
            onDeleteEblanIconPackInfo = onDeleteEblanIconPackInfo,
            onDismissRequest = {
                selectIconPackDialog = false
            },
            onReset = {
                onUpdateGeneralSettings(generalSettings.copy(iconPackInfoPackageName = ""))

                selectIconPackDialog = false
            },
            onUpdateIconPackInfoPackageName = { iconPackInfoPackageName ->
                onUpdateGeneralSettings(generalSettings.copy(iconPackInfoPackageName = iconPackInfoPackageName))

                selectIconPackDialog = false
            },
        )
    }
}
