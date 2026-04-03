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
package com.eblan.launcher.feature.editapplicationinfo

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eblan.launcher.designsystem.icon.EblanLauncherIcons
import com.eblan.launcher.domain.model.EblanApplicationInfo
import com.eblan.launcher.domain.model.EblanApplicationInfoTag
import com.eblan.launcher.domain.model.EblanApplicationInfoTagUi
import com.eblan.launcher.domain.model.IconPackInfoComponent
import com.eblan.launcher.domain.model.PackageManagerIconPackInfo
import com.eblan.launcher.feature.editapplicationinfo.dialog.UpdateTagDialog
import com.eblan.launcher.feature.editapplicationinfo.model.EditApplicationInfoUiState
import com.eblan.launcher.ui.dialog.IconPackInfoFilesDialog
import com.eblan.launcher.ui.dialog.SingleTextFieldDialog
import com.eblan.launcher.ui.edit.CustomIcon
import com.eblan.launcher.ui.edit.CustomLabelDialog
import com.eblan.launcher.ui.settings.SettingsColumn
import com.eblan.launcher.ui.settings.SettingsSwitch

@Composable
internal fun EditApplicationInfoRoute(
    modifier: Modifier = Modifier,
    viewModel: EditApplicationInfoViewModel = hiltViewModel(),
    onNavigateUp: () -> Unit,
) {
    val editApplicationInfoUiState by viewModel.editApplicationInfoUiState.collectAsStateWithLifecycle()

    val packageManagerIconPackInfos by viewModel.packageManagerIconPackInfos.collectAsStateWithLifecycle()

    val iconPackInfoComponents by viewModel.iconPackInfoComponents.collectAsStateWithLifecycle()

    val eblanApplicationInfoTagsUi by viewModel.eblanApplicationInfoTagsUi.collectAsStateWithLifecycle()

    EditApplicationInfoScreen(
        modifier = modifier,
        eblanApplicationInfoTagsUi = eblanApplicationInfoTagsUi,
        editApplicationInfoUiState = editApplicationInfoUiState,
        iconPackInfoComponents = iconPackInfoComponents,
        packageManagerIconPackInfos = packageManagerIconPackInfos,
        onAddEblanApplicationInfoCrossRef = viewModel::addEblanApplicationInfoTagCrossRef,
        onAddEblanApplicationInfoTag = viewModel::addEblanApplicationInfoTag,
        onDeleteEblanApplicationInfoCrossRef = viewModel::deleteEblanApplicationInfoTagCrossRef,
        onDeleteEblanApplicationInfoTag = viewModel::deleteEblanApplicationInfoTag,
        onNavigateUp = onNavigateUp,
        onResetIconPackInfoPackageName = viewModel::resetIconPackInfoPackageName,
        onResetEblanApplicationInfoCustomIcon = viewModel::resetEblanApplicationInfoCustomIcon,
        onSearchIconPackInfoComponent = viewModel::searchIconPackInfoComponent,
        onUpdateEblanApplicationInfo = viewModel::updateEblanApplicationInfo,
        onUpdateEblanApplicationInfoTag = viewModel::updateEblanApplicationInfoTag,
        onUpdateGridItemCustomIcon = viewModel::updateEblanApplicationInfoCustomIcon,
        onUpdateIconPackInfoPackageName = viewModel::updateIconPackInfoPackageName,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EditApplicationInfoScreen(
    modifier: Modifier = Modifier,
    eblanApplicationInfoTagsUi: List<EblanApplicationInfoTagUi>,
    editApplicationInfoUiState: EditApplicationInfoUiState,
    iconPackInfoComponents: List<IconPackInfoComponent>,
    packageManagerIconPackInfos: List<PackageManagerIconPackInfo>,
    onAddEblanApplicationInfoCrossRef: (Long) -> Unit,
    onAddEblanApplicationInfoTag: (EblanApplicationInfoTag) -> Unit,
    onDeleteEblanApplicationInfoCrossRef: (Long) -> Unit,
    onDeleteEblanApplicationInfoTag: (EblanApplicationInfoTag) -> Unit,
    onNavigateUp: () -> Unit,
    onResetIconPackInfoPackageName: () -> Unit,
    onSearchIconPackInfoComponent: (String) -> Unit,
    onUpdateEblanApplicationInfo: (EblanApplicationInfo) -> Unit,
    onUpdateEblanApplicationInfoTag: (EblanApplicationInfoTag) -> Unit,
    onUpdateGridItemCustomIcon: (
        customIcon: String?,
        eblanApplicationInfo: EblanApplicationInfo,
    ) -> Unit,
    onUpdateIconPackInfoPackageName: (String) -> Unit,
    onResetEblanApplicationInfoCustomIcon: (EblanApplicationInfo) -> Unit,
) {
    if (editApplicationInfoUiState is EditApplicationInfoUiState.Success && editApplicationInfoUiState.eblanApplicationInfo != null) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(text = "Edit ${editApplicationInfoUiState.eblanApplicationInfo.label}")
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
                Success(
                    eblanApplicationInfo = editApplicationInfoUiState.eblanApplicationInfo,
                    eblanApplicationInfoTagsUi = eblanApplicationInfoTagsUi,
                    iconPackInfoComponents = iconPackInfoComponents,
                    packageManagerIconPackInfos = packageManagerIconPackInfos,
                    onAddEblanApplicationInfoCrossRef = onAddEblanApplicationInfoCrossRef,
                    onAddEblanApplicationInfoTag = onAddEblanApplicationInfoTag,
                    onDeleteEblanApplicationInfoCrossRef = onDeleteEblanApplicationInfoCrossRef,
                    onDeleteEblanApplicationInfoTag = onDeleteEblanApplicationInfoTag,
                    onResetIconPackInfoPackageName = onResetIconPackInfoPackageName,
                    onSearchIconPackInfoComponent = onSearchIconPackInfoComponent,
                    onUpdateEblanApplicationInfo = onUpdateEblanApplicationInfo,
                    onUpdateEblanApplicationInfoTag = onUpdateEblanApplicationInfoTag,
                    onUpdateGridItemCustomIcon = onUpdateGridItemCustomIcon,
                    onUpdateIconPackInfoPackageName = onUpdateIconPackInfoPackageName,
                    onResetEblanApplicationInfoCustomIcon = onResetEblanApplicationInfoCustomIcon,
                )
            }
        }
    }
}

@Composable
private fun Success(
    modifier: Modifier = Modifier,
    eblanApplicationInfo: EblanApplicationInfo,
    eblanApplicationInfoTagsUi: List<EblanApplicationInfoTagUi>,
    iconPackInfoComponents: List<IconPackInfoComponent>,
    packageManagerIconPackInfos: List<PackageManagerIconPackInfo>,
    onAddEblanApplicationInfoCrossRef: (Long) -> Unit,
    onAddEblanApplicationInfoTag: (EblanApplicationInfoTag) -> Unit,
    onDeleteEblanApplicationInfoCrossRef: (Long) -> Unit,
    onDeleteEblanApplicationInfoTag: (EblanApplicationInfoTag) -> Unit,
    onResetIconPackInfoPackageName: () -> Unit,
    onSearchIconPackInfoComponent: (String) -> Unit,
    onUpdateEblanApplicationInfo: (EblanApplicationInfo) -> Unit,
    onUpdateEblanApplicationInfoTag: (EblanApplicationInfoTag) -> Unit,
    onUpdateGridItemCustomIcon: (
        customIcon: String?,
        eblanApplicationInfo: EblanApplicationInfo,
    ) -> Unit,
    onUpdateIconPackInfoPackageName: (String) -> Unit,
    onResetEblanApplicationInfoCustomIcon: (EblanApplicationInfo) -> Unit,
) {
    var showCustomIconDialog by remember { mutableStateOf(false) }

    var showCustomLabelDialog by remember { mutableStateOf(false) }

    var iconPackInfoPackageName by remember { mutableStateOf<String?>(null) }

    var iconPackInfoLabel by remember { mutableStateOf<String?>(null) }

    ElevatedCard(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .fillMaxWidth()
            .padding(horizontal = 15.dp),
    ) {
        Tags(
            eblanApplicationInfoTagsUi = eblanApplicationInfoTagsUi,
            onAddEblanApplicationInfoCrossRef = onAddEblanApplicationInfoCrossRef,
            onAddEblanApplicationInfoTag = onAddEblanApplicationInfoTag,
            onDeleteEblanApplicationInfoCrossRef = onDeleteEblanApplicationInfoCrossRef,
            onDeleteEblanApplicationInfoTag = onDeleteEblanApplicationInfoTag,
            onUpdateEblanApplicationInfoTag = onUpdateEblanApplicationInfoTag,
        )

        CustomIcon(
            customIcon = eblanApplicationInfo.customIcon,
            packageManagerIconPackInfos = packageManagerIconPackInfos,
            onUpdateIconPackInfoPackageName = { packageName, label ->
                iconPackInfoPackageName = packageName

                iconPackInfoLabel = label

                showCustomIconDialog = true

                onUpdateIconPackInfoPackageName(packageName)
            },
            onUpdateUri = { uri ->
                onUpdateEblanApplicationInfo(eblanApplicationInfo.copy(customIcon = uri))
            },
            onResetCustomIcon = {
                onResetEblanApplicationInfoCustomIcon(eblanApplicationInfo)
            },
        )

        HorizontalDivider(modifier = Modifier.fillMaxWidth())

        SettingsColumn(
            title = "Custom Label",
            subtitle = eblanApplicationInfo.customLabel ?: "None",
            onClick = {
                showCustomLabelDialog = true
            },
        )

        HorizontalDivider(modifier = Modifier.fillMaxWidth())

        SettingsSwitch(
            checked = eblanApplicationInfo.isHidden,
            title = "Hide From Drawer",
            subtitle = "Hide from drawer",
            onCheckedChange = { isHidden ->
                onUpdateEblanApplicationInfo(eblanApplicationInfo.copy(isHidden = isHidden))
            },
        )

        if (showCustomIconDialog) {
            IconPackInfoFilesDialog(
                iconPackInfoComponents = iconPackInfoComponents,
                iconPackInfoPackageName = iconPackInfoPackageName,
                iconPackInfoLabel = iconPackInfoLabel,
                iconName = eblanApplicationInfo.packageName,
                onDismissRequest = {
                    onResetIconPackInfoPackageName()

                    showCustomIconDialog = false
                },
                onUpdateIcon = { icon ->
                    onUpdateGridItemCustomIcon(
                        icon,
                        eblanApplicationInfo,
                    )
                },
                onSearchIconPackInfoComponent = onSearchIconPackInfoComponent,
            )
        }

        if (showCustomLabelDialog) {
            var value by remember { mutableStateOf(eblanApplicationInfo.customLabel ?: "") }

            var isError by remember { mutableStateOf(false) }

            CustomLabelDialog(
                title = "Custom Label",
                textFieldTitle = "Custom Label",
                value = value,
                isError = isError,
                keyboardType = KeyboardType.Text,
                onValueChange = {
                    value = it
                },
                onDismissRequest = {
                    showCustomLabelDialog = false
                },
                onUpdateClick = {
                    if (value.isNotBlank()) {
                        onUpdateEblanApplicationInfo(eblanApplicationInfo.copy(customLabel = value))

                        showCustomLabelDialog = false
                    } else {
                        isError = true
                    }
                },
                onResetClick = {
                    onUpdateEblanApplicationInfo(eblanApplicationInfo.copy(customLabel = null))

                    showCustomLabelDialog = false
                },
            )
        }
    }
}

@Composable
private fun Tags(
    modifier: Modifier = Modifier,
    eblanApplicationInfoTagsUi: List<EblanApplicationInfoTagUi>,
    onAddEblanApplicationInfoCrossRef: (Long) -> Unit,
    onAddEblanApplicationInfoTag: (EblanApplicationInfoTag) -> Unit,
    onDeleteEblanApplicationInfoCrossRef: (Long) -> Unit,
    onDeleteEblanApplicationInfoTag: (EblanApplicationInfoTag) -> Unit,
    onUpdateEblanApplicationInfoTag: (EblanApplicationInfoTag) -> Unit,
) {
    var showAddTagDialog by remember { mutableStateOf(false) }

    var showUpdateTagDialog by remember { mutableStateOf(false) }

    var isManagedTags by remember { mutableStateOf(false) }

    var selectedEblanApplicationInfoTagUi by remember {
        mutableStateOf<EblanApplicationInfoTagUi?>(null)
    }

    FlowRow(modifier = modifier.fillMaxWidth()) {
        eblanApplicationInfoTagsUi.forEach { eblanApplicationInfoTagUi ->
            EblanApplicationInfoTagUiFilterChip(
                modifier = Modifier.padding(5.dp),
                eblanApplicationInfoTagUi = eblanApplicationInfoTagUi,
                isManagedTags = isManagedTags,
                onAddEblanApplicationInfoCrossRef = onAddEblanApplicationInfoCrossRef,
                onDeleteEblanApplicationInfoCrossRef = onDeleteEblanApplicationInfoCrossRef,
                onShowUpdateTagDialog = { newEblanApplicationInfoTagUi ->
                    showUpdateTagDialog = true

                    selectedEblanApplicationInfoTagUi = newEblanApplicationInfoTagUi
                },
            )
        }

        AddTagAssistChip(
            modifier = Modifier.padding(5.dp),
            onClick = {
                showAddTagDialog = true
            },
        )

        ManageTagsFilterChip(
            modifier = Modifier.padding(5.dp),
            isManagedTags = isManagedTags,
            onClick = {
                isManagedTags = !isManagedTags
            },
        )
    }

    if (showAddTagDialog) {
        var value by remember { mutableStateOf("") }

        var isError by remember { mutableStateOf(false) }

        SingleTextFieldDialog(
            title = "Add Tag",
            textFieldTitle = "Add Tag",
            value = value,
            isError = isError,
            keyboardType = KeyboardType.Text,
            onValueChange = {
                value = it
            },
            onDismissRequest = {
                showAddTagDialog = false
            },
            onUpdateClick = {
                if (value.isNotBlank()) {
                    onAddEblanApplicationInfoTag(EblanApplicationInfoTag(name = value))

                    showAddTagDialog = false
                } else {
                    isError = true
                }
            },
        )
    }

    if (showUpdateTagDialog) {
        UpdateTagDialog(
            eblanApplicationInfoTagUi = selectedEblanApplicationInfoTagUi,
            onDeleteEblanApplicationInfoTag = onDeleteEblanApplicationInfoTag,
            onDismissRequest = {
                showUpdateTagDialog = false
            },
            onUpdateEblanApplicationInfoTag = onUpdateEblanApplicationInfoTag,
        )
    }
}

@Composable
private fun EblanApplicationInfoTagUiFilterChip(
    modifier: Modifier = Modifier,
    eblanApplicationInfoTagUi: EblanApplicationInfoTagUi,
    isManagedTags: Boolean,
    onAddEblanApplicationInfoCrossRef: (Long) -> Unit,
    onDeleteEblanApplicationInfoCrossRef: (Long) -> Unit,
    onShowUpdateTagDialog: (EblanApplicationInfoTagUi) -> Unit,
) {
    FilterChip(
        modifier = modifier,
        onClick = {
            if (isManagedTags) {
                onShowUpdateTagDialog(eblanApplicationInfoTagUi)
            } else {
                if (eblanApplicationInfoTagUi.selected) {
                    onDeleteEblanApplicationInfoCrossRef(eblanApplicationInfoTagUi.id)
                } else {
                    onAddEblanApplicationInfoCrossRef(eblanApplicationInfoTagUi.id)
                }
            }
        },
        label = {
            Text(text = eblanApplicationInfoTagUi.name)
        },
        selected = eblanApplicationInfoTagUi.selected,
        leadingIcon = if (eblanApplicationInfoTagUi.selected) {
            {
                Icon(
                    imageVector = EblanLauncherIcons.Done,
                    contentDescription = null,
                    modifier = Modifier.size(FilterChipDefaults.IconSize),
                )
            }
        } else {
            null
        },
    )
}

@Composable
private fun AddTagAssistChip(modifier: Modifier = Modifier, onClick: () -> Unit) {
    AssistChip(
        modifier = modifier,
        onClick = onClick,
        label = { Text(text = "Add Tag") },
        leadingIcon = {
            Icon(
                modifier = Modifier.size(AssistChipDefaults.IconSize),
                imageVector = EblanLauncherIcons.Add,
                contentDescription = null,
            )
        },
    )
}

@Composable
private fun ManageTagsFilterChip(
    modifier: Modifier = Modifier,
    isManagedTags: Boolean,
    onClick: () -> Unit,
) {
    FilterChip(
        modifier = modifier,
        onClick = onClick,
        label = { Text(text = "Manage Tags") },
        selected = isManagedTags,
        leadingIcon = if (isManagedTags) {
            {
                Icon(
                    imageVector = EblanLauncherIcons.Done,
                    contentDescription = null,
                    modifier = Modifier.size(FilterChipDefaults.IconSize),
                )
            }
        } else {
            null
        },
    )
}
