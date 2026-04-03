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
package com.eblan.launcher.feature.editgriditem

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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eblan.launcher.designsystem.icon.EblanLauncherIcons
import com.eblan.launcher.domain.model.EblanApplicationInfo
import com.eblan.launcher.domain.model.EblanUser
import com.eblan.launcher.domain.model.GridItem
import com.eblan.launcher.domain.model.GridItemData
import com.eblan.launcher.domain.model.IconPackInfoComponent
import com.eblan.launcher.domain.model.PackageManagerIconPackInfo
import com.eblan.launcher.feature.editgriditem.model.EditGridItemUiState
import com.eblan.launcher.ui.dialog.IconPackInfoFilesDialog
import com.eblan.launcher.ui.dialog.SingleTextFieldDialog
import com.eblan.launcher.ui.edit.CustomIcon
import com.eblan.launcher.ui.edit.CustomLabelDialog
import com.eblan.launcher.ui.settings.EblanActionSettings
import com.eblan.launcher.ui.settings.GridItemSettings
import com.eblan.launcher.ui.settings.SettingsColumn
import com.eblan.launcher.ui.settings.SettingsSwitch

@Composable
internal fun EditGridItemRoute(
    modifier: Modifier = Modifier,
    viewModel: EditGridItemViewModel = hiltViewModel(),
    onNavigateUp: () -> Unit,
) {
    val editUiState by viewModel.editGridItemUiState.collectAsStateWithLifecycle()

    val packageManagerIconPackInfos by viewModel.packageManagerIconPackInfos.collectAsStateWithLifecycle()

    val iconPackInfoComponents by viewModel.iconPackInfoComponents.collectAsStateWithLifecycle()

    val eblanApplicationInfos by viewModel.eblanApplicationInfos.collectAsStateWithLifecycle()

    EditGridItemScreen(
        modifier = modifier,
        eblanApplicationInfos = eblanApplicationInfos,
        editGridItemUiState = editUiState,
        iconPackInfoComponents = iconPackInfoComponents,
        packageManagerIconPackInfos = packageManagerIconPackInfos,
        onNavigateUp = onNavigateUp,
        onResetIconPackInfoPackageName = viewModel::resetIconPackInfoPackageName,
        onResetGridItemCustomIcon = viewModel::resetGridItemCustomIcon,
        onSearchIconPackInfoComponent = viewModel::searchIconPackInfoComponent,
        onUpdateGridItem = viewModel::updateGridItem,
        onUpdateIconPackInfoPackageName = viewModel::updateIconPackInfoPackageName,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EditGridItemScreen(
    modifier: Modifier = Modifier,
    eblanApplicationInfos: Map<EblanUser, List<EblanApplicationInfo>>,
    editGridItemUiState: EditGridItemUiState,
    iconPackInfoComponents: List<IconPackInfoComponent>,
    packageManagerIconPackInfos: List<PackageManagerIconPackInfo>,
    onNavigateUp: () -> Unit,
    onResetIconPackInfoPackageName: () -> Unit,
    onResetGridItemCustomIcon: (GridItem) -> Unit,
    onSearchIconPackInfoComponent: (String) -> Unit,
    onUpdateGridItem: (GridItem) -> Unit,
    onUpdateIconPackInfoPackageName: (String) -> Unit,
) {
    if (editGridItemUiState is EditGridItemUiState.Success &&
        editGridItemUiState.gridItem != null
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        val label = when (val data = editGridItemUiState.gridItem.data) {
                            is GridItemData.ApplicationInfo -> data.label
                            is GridItemData.ShortcutConfig -> data.activityLabel.toString()
                            is GridItemData.ShortcutInfo -> data.shortLabel
                            is GridItemData.Folder -> data.label
                            else -> "Grid Item"
                        }

                        Text(text = "Edit $label")
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
                    eblanApplicationInfos = eblanApplicationInfos,
                    gridItem = editGridItemUiState.gridItem,
                    iconPackInfoComponents = iconPackInfoComponents,
                    packageManagerIconPackInfos = packageManagerIconPackInfos,
                    onResetIconPackInfoPackageName = onResetIconPackInfoPackageName,
                    onSearchIconPackInfoComponent = onSearchIconPackInfoComponent,
                    onUpdateGridItem = onUpdateGridItem,
                    onUpdateIconPackInfoPackageName = onUpdateIconPackInfoPackageName,
                    onResetGridItemCustomIcon = onResetGridItemCustomIcon,
                )
            }
        }
    }
}

@Composable
private fun Success(
    modifier: Modifier = Modifier,
    eblanApplicationInfos: Map<EblanUser, List<EblanApplicationInfo>>,
    gridItem: GridItem,
    iconPackInfoComponents: List<IconPackInfoComponent>,
    packageManagerIconPackInfos: List<PackageManagerIconPackInfo>,
    onResetIconPackInfoPackageName: () -> Unit,
    onSearchIconPackInfoComponent: (String) -> Unit,
    onUpdateGridItem: (GridItem) -> Unit,
    onUpdateIconPackInfoPackageName: (String) -> Unit,
    onResetGridItemCustomIcon: (GridItem) -> Unit,
) {
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
            when (val data = gridItem.data) {
                is GridItemData.ApplicationInfo -> {
                    EditApplicationInfo(
                        data = data,
                        gridItem = gridItem,
                        iconPackInfoComponents = iconPackInfoComponents,
                        packageManagerIconPackInfos = packageManagerIconPackInfos,
                        onResetIconPackInfoPackageName = onResetIconPackInfoPackageName,
                        onSearchIconPackInfoComponent = onSearchIconPackInfoComponent,
                        onUpdateGridItem = onUpdateGridItem,
                        onUpdateIconPackInfoPackageName = onUpdateIconPackInfoPackageName,
                        onResetGridItemCustomIcon = onResetGridItemCustomIcon,
                    )
                }

                is GridItemData.Folder -> {
                    EditFolder(
                        data = data,
                        gridItem = gridItem,
                        iconPackInfoComponents = iconPackInfoComponents,
                        packageManagerIconPackInfos = packageManagerIconPackInfos,
                        onResetIconPackInfoPackageName = onResetIconPackInfoPackageName,
                        onSearchIconPackInfoComponent = onSearchIconPackInfoComponent,
                        onUpdateGridItem = onUpdateGridItem,
                        onUpdateIconPackInfoPackageName = onUpdateIconPackInfoPackageName,
                        onResetGridItemCustomIcon = onResetGridItemCustomIcon,
                    )
                }

                is GridItemData.ShortcutInfo -> {
                    EditShortcutInfo(
                        data = data,
                        gridItem = gridItem,
                        iconPackInfoComponents = iconPackInfoComponents,
                        packageManagerIconPackInfos = packageManagerIconPackInfos,
                        onResetIconPackInfoPackageName = onResetIconPackInfoPackageName,
                        onSearchIconPackInfoComponent = onSearchIconPackInfoComponent,
                        onUpdateGridItem = onUpdateGridItem,
                        onUpdateIconPackInfoPackageName = onUpdateIconPackInfoPackageName,
                        onResetGridItemCustomIcon = onResetGridItemCustomIcon,
                    )
                }

                is GridItemData.ShortcutConfig -> {
                    EditShortcutConfig(
                        data = data,
                        gridItem = gridItem,
                        iconPackInfoComponents = iconPackInfoComponents,
                        packageManagerIconPackInfos = packageManagerIconPackInfos,
                        onResetIconPackInfoPackageName = onResetIconPackInfoPackageName,
                        onSearchIconPackInfoComponent = onSearchIconPackInfoComponent,
                        onUpdateGridItem = onUpdateGridItem,
                        onUpdateIconPackInfoPackageName = onUpdateIconPackInfoPackageName,
                        onResetGridItemCustomIcon = onResetGridItemCustomIcon,
                    )
                }

                else -> Unit
            }

            HorizontalDivider(modifier = Modifier.fillMaxWidth())

            SettingsSwitch(
                checked = gridItem.override,
                title = "Override",
                subtitle = "Override the Grid Item Settings",
                onCheckedChange = {
                    onUpdateGridItem(gridItem.copy(override = it))
                },
            )
        }

        if (gridItem.override) {
            GridItemSettings(
                gridItemSettings = gridItem.gridItemSettings,
                onUpdateGridItemSettings = { gridItemSettings ->
                    onUpdateGridItem(gridItem.copy(gridItemSettings = gridItemSettings))
                },
            )
        }

        Text(
            modifier = Modifier.padding(15.dp),
            text = "Grid Item Actions",
            style = MaterialTheme.typography.bodySmall,
        )

        EblanActionSettings(
            doubleTap = gridItem.doubleTap,
            swipeUp = gridItem.swipeUp,
            swipeDown = gridItem.swipeDown,
            eblanApplicationInfos = eblanApplicationInfos,
            onUpdateDoubleTap = { doubleTap ->
                onUpdateGridItem(gridItem.copy(doubleTap = doubleTap))
            },
            onUpdateSwipeUp = { swipeUp ->
                onUpdateGridItem(gridItem.copy(swipeUp = swipeUp))
            },
            onUpdateSwipeDown = { swipeDown ->
                onUpdateGridItem(gridItem.copy(swipeDown = swipeDown))
            },
        )
    }
}

@Composable
private fun EditApplicationInfo(
    data: GridItemData.ApplicationInfo,
    gridItem: GridItem,
    iconPackInfoComponents: List<IconPackInfoComponent>,
    packageManagerIconPackInfos: List<PackageManagerIconPackInfo>,
    onResetIconPackInfoPackageName: () -> Unit,
    onSearchIconPackInfoComponent: (String) -> Unit,
    onUpdateGridItem: (GridItem) -> Unit,
    onUpdateIconPackInfoPackageName: (String) -> Unit,
    onResetGridItemCustomIcon: (GridItem) -> Unit,
) {
    var showCustomIconDialog by remember { mutableStateOf(false) }

    var showCustomLabelDialog by remember { mutableStateOf(false) }

    var iconPackInfoPackageName by remember { mutableStateOf<String?>(null) }

    var iconPackInfoLabel by remember { mutableStateOf<String?>(null) }

    CustomIcon(
        customIcon = data.customIcon,
        packageManagerIconPackInfos = packageManagerIconPackInfos,
        onUpdateIconPackInfoPackageName = { packageName, label ->
            iconPackInfoPackageName = packageName

            iconPackInfoLabel = label

            showCustomIconDialog = true

            onUpdateIconPackInfoPackageName(packageName)
        },
        onUpdateUri = { uri ->
            val newData = data.copy(customIcon = uri)

            onUpdateGridItem(gridItem.copy(data = newData))
        },
        onResetCustomIcon = {
            onResetGridItemCustomIcon(gridItem)
        },
    )

    HorizontalDivider(modifier = Modifier.fillMaxWidth())

    SettingsColumn(
        title = "Custom Label",
        subtitle = data.customLabel ?: "None",
        onClick = {
            showCustomLabelDialog = true
        },
    )

    if (showCustomIconDialog) {
        IconPackInfoFilesDialog(
            iconPackInfoComponents = iconPackInfoComponents,
            iconPackInfoPackageName = iconPackInfoPackageName,
            iconPackInfoLabel = iconPackInfoLabel,
            iconName = gridItem.id,
            onDismissRequest = {
                onResetIconPackInfoPackageName()

                showCustomIconDialog = false
            },
            onUpdateIcon = { icon ->
                onUpdateGridItem(
                    getGridItem(
                        gridItem = gridItem,
                        customIcon = icon,
                    ),
                )
            },
            onSearchIconPackInfoComponent = onSearchIconPackInfoComponent,
        )
    }

    if (showCustomLabelDialog) {
        var value by remember { mutableStateOf(data.customLabel ?: "") }

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
                    val newData = data.copy(customLabel = value)

                    onUpdateGridItem(gridItem.copy(data = newData))

                    showCustomLabelDialog = false
                } else {
                    isError = true
                }
            },
            onResetClick = {
                val newData = data.copy(customLabel = null)

                onUpdateGridItem(gridItem.copy(data = newData))

                showCustomLabelDialog = false
            },
        )
    }
}

@Composable
private fun EditFolder(
    data: GridItemData.Folder,
    gridItem: GridItem,
    iconPackInfoComponents: List<IconPackInfoComponent>,
    packageManagerIconPackInfos: List<PackageManagerIconPackInfo>,
    onResetIconPackInfoPackageName: () -> Unit,
    onSearchIconPackInfoComponent: (String) -> Unit,
    onUpdateGridItem: (GridItem) -> Unit,
    onUpdateIconPackInfoPackageName: (String) -> Unit,
    onResetGridItemCustomIcon: (GridItem) -> Unit,
) {
    var showCustomIconDialog by remember { mutableStateOf(false) }

    var showEditLabelDialog by remember { mutableStateOf(false) }

    var iconPackInfoPackageName by remember { mutableStateOf<String?>(null) }

    var iconPackInfoLabel by remember { mutableStateOf<String?>(null) }

    CustomIcon(
        customIcon = data.icon,
        packageManagerIconPackInfos = packageManagerIconPackInfos,
        onUpdateIconPackInfoPackageName = { packageName, label ->
            iconPackInfoPackageName = packageName

            iconPackInfoLabel = label

            showCustomIconDialog = true

            onUpdateIconPackInfoPackageName(packageName)
        },
        onUpdateUri = { uri ->
            val newData = data.copy(icon = uri)

            onUpdateGridItem(gridItem.copy(data = newData))
        },
        onResetCustomIcon = {
            onResetGridItemCustomIcon(gridItem)
        },
    )

    HorizontalDivider(modifier = Modifier.fillMaxWidth())

    SettingsColumn(
        title = "Edit Label",
        subtitle = data.label,
        onClick = {
            showEditLabelDialog = true
        },
    )

    if (showCustomIconDialog) {
        IconPackInfoFilesDialog(
            iconPackInfoComponents = iconPackInfoComponents,
            iconPackInfoPackageName = iconPackInfoPackageName,
            iconPackInfoLabel = iconPackInfoLabel,
            iconName = gridItem.id,
            onDismissRequest = {
                onResetIconPackInfoPackageName()

                showCustomIconDialog = false
            },
            onUpdateIcon = { icon ->
                onUpdateGridItem(
                    getGridItem(
                        gridItem = gridItem,
                        customIcon = icon,
                    ),
                )
            },
            onSearchIconPackInfoComponent = onSearchIconPackInfoComponent,
        )
    }

    if (showEditLabelDialog) {
        var value by remember { mutableStateOf(data.label) }

        var isError by remember { mutableStateOf(false) }

        SingleTextFieldDialog(
            title = "Label",
            textFieldTitle = "Label",
            value = value,
            isError = isError,
            keyboardType = KeyboardType.Text,
            onValueChange = {
                value = it
            },
            onDismissRequest = {
                showEditLabelDialog = false
            },
            onUpdateClick = {
                if (value.isNotBlank()) {
                    val newData = data.copy(label = value)

                    onUpdateGridItem(gridItem.copy(data = newData))

                    showEditLabelDialog = false
                } else {
                    isError = true
                }
            },
        )
    }
}

@Composable
private fun EditShortcutInfo(
    data: GridItemData.ShortcutInfo,
    gridItem: GridItem,
    iconPackInfoComponents: List<IconPackInfoComponent>,
    packageManagerIconPackInfos: List<PackageManagerIconPackInfo>,
    onResetIconPackInfoPackageName: () -> Unit,
    onSearchIconPackInfoComponent: (String) -> Unit,
    onUpdateGridItem: (GridItem) -> Unit,
    onUpdateIconPackInfoPackageName: (String) -> Unit,
    onResetGridItemCustomIcon: (GridItem) -> Unit,
) {
    var showCustomIconDialog by remember { mutableStateOf(false) }

    var showCustomShortLabelDialog by remember { mutableStateOf(false) }

    var iconPackInfoPackageName by remember { mutableStateOf<String?>(null) }

    var iconPackInfoLabel by remember { mutableStateOf<String?>(null) }

    CustomIcon(
        customIcon = data.customIcon,
        packageManagerIconPackInfos = packageManagerIconPackInfos,
        onUpdateIconPackInfoPackageName = { packageName, label ->
            iconPackInfoPackageName = packageName

            iconPackInfoLabel = label

            showCustomIconDialog = true

            onUpdateIconPackInfoPackageName(packageName)
        },
        onUpdateUri = { uri ->
            val newData = data.copy(customIcon = uri)

            onUpdateGridItem(gridItem.copy(data = newData))
        },
        onResetCustomIcon = {
            onResetGridItemCustomIcon(gridItem)
        },
    )

    HorizontalDivider(modifier = Modifier.fillMaxWidth())

    SettingsColumn(
        title = "Custom Short Label",
        subtitle = data.customShortLabel ?: "None",
        onClick = {
            showCustomShortLabelDialog = true
        },
    )

    if (showCustomIconDialog) {
        IconPackInfoFilesDialog(
            iconPackInfoComponents = iconPackInfoComponents,
            iconPackInfoPackageName = iconPackInfoPackageName,
            iconPackInfoLabel = iconPackInfoLabel,
            iconName = gridItem.id,
            onDismissRequest = {
                onResetIconPackInfoPackageName()

                showCustomIconDialog = false
            },
            onUpdateIcon = { icon ->
                onUpdateGridItem(
                    getGridItem(
                        gridItem = gridItem,
                        customIcon = icon,
                    ),
                )
            },
            onSearchIconPackInfoComponent = onSearchIconPackInfoComponent,
        )
    }

    if (showCustomShortLabelDialog) {
        var value by remember { mutableStateOf(data.customShortLabel ?: "") }

        var isError by remember { mutableStateOf(false) }

        CustomLabelDialog(
            title = "Custom Short Label",
            textFieldTitle = "Custom Short Label",
            value = value,
            isError = isError,
            keyboardType = KeyboardType.Text,
            onValueChange = {
                value = it
            },
            onDismissRequest = {
                showCustomShortLabelDialog = false
            },
            onUpdateClick = {
                if (value.isNotBlank()) {
                    val newData = data.copy(customShortLabel = value)

                    onUpdateGridItem(gridItem.copy(data = newData))

                    showCustomShortLabelDialog = false
                } else {
                    isError = true
                }
            },
            onResetClick = {
                val newData = data.copy(customShortLabel = null)

                onUpdateGridItem(gridItem.copy(data = newData))

                showCustomShortLabelDialog = false
            },
        )
    }
}

@Composable
private fun EditShortcutConfig(
    data: GridItemData.ShortcutConfig,
    gridItem: GridItem,
    iconPackInfoComponents: List<IconPackInfoComponent>,
    packageManagerIconPackInfos: List<PackageManagerIconPackInfo>,
    onResetIconPackInfoPackageName: () -> Unit,
    onSearchIconPackInfoComponent: (String) -> Unit,
    onUpdateGridItem: (GridItem) -> Unit,
    onUpdateIconPackInfoPackageName: (String) -> Unit,
    onResetGridItemCustomIcon: (GridItem) -> Unit,
) {
    var showCustomIconDialog by remember { mutableStateOf(false) }

    var showCustomLabelDialog by remember { mutableStateOf(false) }

    var iconPackInfoPackageName by remember { mutableStateOf<String?>(null) }

    var iconPackInfoLabel by remember { mutableStateOf<String?>(null) }

    CustomIcon(
        customIcon = data.customIcon,
        packageManagerIconPackInfos = packageManagerIconPackInfos,
        onUpdateIconPackInfoPackageName = { packageName, label ->
            iconPackInfoPackageName = packageName

            iconPackInfoLabel = label

            showCustomIconDialog = true

            onUpdateIconPackInfoPackageName(packageName)
        },
        onUpdateUri = { uri ->
            val newData = data.copy(customIcon = uri)

            onUpdateGridItem(gridItem.copy(data = newData))
        },
        onResetCustomIcon = {
            onResetGridItemCustomIcon(gridItem)
        },
    )

    HorizontalDivider(modifier = Modifier.fillMaxWidth())

    SettingsColumn(
        title = "Custom Label",
        subtitle = data.customLabel ?: "None",
        onClick = {
            showCustomLabelDialog = true
        },
    )

    if (showCustomIconDialog) {
        IconPackInfoFilesDialog(
            iconPackInfoComponents = iconPackInfoComponents,
            iconPackInfoPackageName = iconPackInfoPackageName,
            iconPackInfoLabel = iconPackInfoLabel,
            iconName = gridItem.id,
            onDismissRequest = {
                onResetIconPackInfoPackageName()

                showCustomIconDialog = false
            },
            onUpdateIcon = { icon ->
                onUpdateGridItem(
                    getGridItem(
                        gridItem = gridItem,
                        customIcon = icon,
                    ),
                )
            },
            onSearchIconPackInfoComponent = onSearchIconPackInfoComponent,
        )
    }

    if (showCustomLabelDialog) {
        var value by remember { mutableStateOf(data.customLabel ?: "") }

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
                    val newData = data.copy(customLabel = value)

                    onUpdateGridItem(gridItem.copy(data = newData))

                    showCustomLabelDialog = false
                } else {
                    isError = true
                }
            },
            onResetClick = {
                val newData = data.copy(customLabel = null)

                onUpdateGridItem(gridItem.copy(data = newData))

                showCustomLabelDialog = false
            },
        )
    }
}
