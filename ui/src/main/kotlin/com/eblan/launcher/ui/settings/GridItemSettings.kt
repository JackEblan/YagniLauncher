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
package com.eblan.launcher.ui.settings

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.eblan.launcher.domain.model.GridItemSettings
import com.eblan.launcher.domain.model.HorizontalAlignment
import com.eblan.launcher.domain.model.TextColor
import com.eblan.launcher.domain.model.VerticalArrangement
import com.eblan.launcher.ui.R
import com.eblan.launcher.ui.dialog.ColorPickerDialog
import com.eblan.launcher.ui.dialog.EditCornerRadiusDialog
import com.eblan.launcher.ui.dialog.EditIconSizeDialog
import com.eblan.launcher.ui.dialog.EditPaddingDialog
import com.eblan.launcher.ui.dialog.EditTextSizeDialog
import com.eblan.launcher.ui.dialog.RadioOptionsDialog
import com.eblan.launcher.ui.dialog.TextColorDialog
import com.eblan.launcher.ui.model.SettingsItem
import com.eblan.launcher.common.R as commonR

@Composable
fun buildGridItemSettingsItems(
    gridItemSettings: GridItemSettings,
    onIconSizeClick: () -> Unit,
    onTextColorClick: () -> Unit,
    onTextSizeClick: () -> Unit,
    onBackgroundColorClick: () -> Unit,
    onPaddingClick: () -> Unit,
    onCornerRadiusClick: () -> Unit,
    onHorizontalAlignmentClick: () -> Unit,
    onVerticalArrangementClick: () -> Unit,
    onUpdateGridItemSettings: (GridItemSettings) -> Unit,
): List<SettingsItem> = buildList {
    add(
        SettingsItem.Category(
            title = stringResource(R.string.grid_item),
        ),
    )

    add(
        SettingsItem.Column(
            title = stringResource(R.string.icon_size),
            subtitle = "${gridItemSettings.iconSize}",
            onClick = onIconSizeClick,
        ),
    )

    add(
        SettingsItem.Column(
            title = stringResource(R.string.text_color),
            subtitle = gridItemSettings.textColor.getTitle(),
            onClick = onTextColorClick,
        ),
    )

    add(
        SettingsItem.Column(
            title = stringResource(R.string.text_size),
            subtitle = "${gridItemSettings.textSize}",
            onClick = onTextSizeClick,
        ),
    )

    add(
        SettingsItem.CustomBackgroundColor(
            title = stringResource(commonR.string.background_color),
            customBackgroundColor = gridItemSettings.customBackgroundColor,
            onClick = onBackgroundColorClick,
        ),
    )

    add(
        SettingsItem.Column(
            title = stringResource(R.string.padding),
            subtitle = "${gridItemSettings.padding}",
            onClick = onPaddingClick,
        ),
    )

    add(
        SettingsItem.Column(
            title = stringResource(R.string.corner_radius),
            subtitle = "${gridItemSettings.cornerRadius}",
            onClick = onCornerRadiusClick,
        ),
    )

    add(
        SettingsItem.Switch(
            checked = gridItemSettings.showLabel,
            title = stringResource(R.string.show_label),
            subtitle = stringResource(R.string.display_app_names_below_icons),
            onClick = {
                onUpdateGridItemSettings(
                    gridItemSettings.copy(showLabel = !gridItemSettings.showLabel),
                )
            },
            onCheckedChange = {
                onUpdateGridItemSettings(
                    gridItemSettings.copy(showLabel = it),
                )
            },
        ),
    )

    add(
        SettingsItem.Switch(
            checked = gridItemSettings.singleLineLabel,
            title = stringResource(R.string.single_line_label),
            subtitle = stringResource(R.string.limit_app_names_to_one_line),
            onClick = {
                onUpdateGridItemSettings(
                    gridItemSettings.copy(singleLineLabel = !gridItemSettings.singleLineLabel),
                )
            },
            onCheckedChange = {
                onUpdateGridItemSettings(
                    gridItemSettings.copy(singleLineLabel = it),
                )
            },
        ),
    )

    add(
        SettingsItem.Column(
            title = stringResource(R.string.horizontal_alignment),
            subtitle = gridItemSettings.horizontalAlignment.name.replace(
                Regex("([a-z])([A-Z])"),
                "$1 $2",
            ),
            onClick = onHorizontalAlignmentClick,
        ),
    )

    add(
        SettingsItem.Column(
            title = stringResource(R.string.vertical_arrangement),
            subtitle = gridItemSettings.verticalArrangement.name,
            onClick = onVerticalArrangementClick,
        ),
    )
}

@Composable
fun GridItemSettings(
    modifier: Modifier = Modifier,
    gridItemSettings: GridItemSettings,
    onUpdateGridItemSettings: (GridItemSettings) -> Unit,
) {
    val context = LocalContext.current

    var showIconSizeDialog by remember { mutableStateOf(false) }

    var showTextColorDialog by remember { mutableStateOf(false) }

    var showTextSizeDialog by remember { mutableStateOf(false) }

    var showBackgroundColorDialog by remember { mutableStateOf(false) }

    var showPaddingDialog by remember { mutableStateOf(false) }

    var showCornerRadiusDialog by remember { mutableStateOf(false) }

    var showHorizontalAlignment by remember { mutableStateOf(false) }

    var showVerticalArrangement by remember { mutableStateOf(false) }

    val items = buildGridItemSettingsItems(
        gridItemSettings = gridItemSettings,
        onIconSizeClick = {
            showIconSizeDialog = true
        },
        onTextColorClick = {
            showTextColorDialog = true
        },
        onTextSizeClick = {
            showTextSizeDialog = true
        },
        onBackgroundColorClick = {
            showBackgroundColorDialog = true
        },
        onPaddingClick = {
            showPaddingDialog = true
        },
        onCornerRadiusClick = {
            showCornerRadiusDialog = true
        },
        onHorizontalAlignmentClick = {
            showHorizontalAlignment = true
        },
        onVerticalArrangementClick = {
            showVerticalArrangement = true
        },
        onUpdateGridItemSettings = onUpdateGridItemSettings,
    )

    Column(modifier = modifier) {
        items.forEachIndexed { index, settingsItem ->
            when (settingsItem) {
                is SettingsItem.Column -> {
                    SettingsColumn(
                        index = index,
                        size = items.size,
                        title = settingsItem.title,
                        subtitle = settingsItem.subtitle,
                        onClick = settingsItem.onClick,
                    )
                }

                is SettingsItem.Switch -> {
                    SettingsSwitch(
                        index = index,
                        size = items.size,
                        checked = settingsItem.checked,
                        title = settingsItem.title,
                        subtitle = settingsItem.subtitle,
                        onClick = settingsItem.onClick,
                        onCheckedChange = settingsItem.onCheckedChange,
                    )
                }

                is SettingsItem.CustomBackgroundColor -> {
                    CustomBackgroundColor(
                        index = index,
                        size = items.size,
                        title = settingsItem.title,
                        customBackgroundColor = settingsItem.customBackgroundColor,
                        onClick = settingsItem.onClick,
                    )
                }

                is SettingsItem.Category -> {
                    Text(
                        modifier = Modifier.padding(15.dp),
                        text = settingsItem.title,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }

                is SettingsItem.Row -> Unit
            }
        }
    }

    if (showIconSizeDialog) {
        EditIconSizeDialog(
            iconSize = gridItemSettings.iconSize,
            onDismissRequest = {
                showIconSizeDialog = false
            },
            onUpdateIconSize = {
                onUpdateGridItemSettings(
                    gridItemSettings.copy(
                        iconSize = it,
                    ),
                )

                showIconSizeDialog = false
            },
        )
    }

    if (showTextColorDialog) {
        TextColorDialog(
            title = stringResource(R.string.text_color),
            textColor = gridItemSettings.textColor,
            customTextColor = gridItemSettings.customTextColor,
            onDismissRequest = {
                showTextColorDialog = false
            },
            onUpdateClick = { textColor, customTextColor ->
                onUpdateGridItemSettings(
                    gridItemSettings.copy(
                        textColor = textColor,
                        customTextColor = customTextColor,
                    ),
                )

                showTextColorDialog = false
            },
        )
    }

    if (showTextSizeDialog) {
        EditTextSizeDialog(
            textSize = gridItemSettings.textSize,
            onDismissRequest = {
                showTextSizeDialog = false
            },
            onUpdateTextSize = {
                onUpdateGridItemSettings(
                    gridItemSettings.copy(
                        textSize = it,
                    ),
                )

                showTextSizeDialog = false
            },
        )
    }

    if (showBackgroundColorDialog) {
        ColorPickerDialog(
            title = stringResource(commonR.string.background_color),
            customColor = gridItemSettings.customBackgroundColor,
            onDismissRequest = {
                showBackgroundColorDialog = false
            },
            onSelectColor = {
                onUpdateGridItemSettings(gridItemSettings.copy(customBackgroundColor = it))

                showBackgroundColorDialog = false
            },
        )
    }

    if (showPaddingDialog) {
        EditPaddingDialog(
            padding = gridItemSettings.padding,
            onDismissRequest = {
                showPaddingDialog = false
            },
            onUpdatePadding = {
                onUpdateGridItemSettings(
                    gridItemSettings.copy(
                        padding = it,
                    ),
                )

                showPaddingDialog = false
            },
        )
    }

    if (showCornerRadiusDialog) {
        EditCornerRadiusDialog(
            cornerRadius = gridItemSettings.cornerRadius,
            onDismissRequest = {
                showCornerRadiusDialog = false
            },
            onUpdateCornerRadius = {
                onUpdateGridItemSettings(
                    gridItemSettings.copy(
                        cornerRadius = it,
                    ),
                )

                showCornerRadiusDialog = false
            },
        )
    }

    if (showHorizontalAlignment) {
        RadioOptionsDialog(
            title = stringResource(R.string.horizontal_alignment),
            options = HorizontalAlignment.entries,
            selected = gridItemSettings.horizontalAlignment,
            label = {
                it.getHorizontalAlignmentTitle(context = context)
            },
            onDismissRequest = {
                showHorizontalAlignment = false
            },
            onUpdateClick = {
                onUpdateGridItemSettings(gridItemSettings.copy(horizontalAlignment = it))

                showHorizontalAlignment = false
            },
        )
    }

    if (showVerticalArrangement) {
        RadioOptionsDialog(
            title = stringResource(R.string.vertical_arrangement),
            options = VerticalArrangement.entries,
            selected = gridItemSettings.verticalArrangement,
            label = {
                it.getVerticalArrangementTitle(context = context)
            },
            onDismissRequest = {
                showVerticalArrangement = false
            },
            onUpdateClick = {
                onUpdateGridItemSettings(gridItemSettings.copy(verticalArrangement = it))

                showVerticalArrangement = false
            },
        )
    }
}

@Composable
fun TextColorSettingsRow(
    modifier: Modifier = Modifier,
    textColorTitle: String,
    customColorTitle: String,
    textColor: TextColor,
    customColor: Int,
    onClick: () -> Unit,
) {
    when (textColor) {
        TextColor.System,
        TextColor.Light,
        TextColor.Dark,
        -> {
            SettingsColumn(
                modifier = modifier,
                title = textColorTitle,
                subtitle = textColor.name,
                onClick = onClick,
            )
        }

        TextColor.Custom -> {
            CustomBackgroundColor(
                modifier = modifier,
                title = customColorTitle,
                customColor = customColor,
                onClick = onClick,
            )
        }
    }
}

@Composable
fun TextColor.getTitle(): String = when (this) {
    TextColor.System -> stringResource(commonR.string.system)
    TextColor.Light -> stringResource(commonR.string.light)
    TextColor.Dark -> stringResource(commonR.string.dark)
    TextColor.Custom -> stringResource(R.string.custom)
}

@Composable
private fun CustomBackgroundColor(
    modifier: Modifier = Modifier,
    title: String,
    customColor: Int,
    onClick: () -> Unit,
) {
    Row(
        modifier = modifier
            .clickable(onClick = onClick)
            .fillMaxWidth()
            .padding(15.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
        )

        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    color = Color(customColor),
                    shape = CircleShape,
                ),
        )
    }
}

@Composable
fun CustomBackgroundColor(
    modifier: Modifier = Modifier,
    index: Int,
    size: Int,
    title: String,
    customBackgroundColor: Int,
    onClick: () -> Unit,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        shape = settingsItemShape(
            index = index,
            size = size,
        ),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(15.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
            )

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = Color(customBackgroundColor),
                        shape = CircleShape,
                    ),
            )
        }
    }
}

private fun HorizontalAlignment.getHorizontalAlignmentTitle(context: Context): String = when (this) {
    HorizontalAlignment.Start -> context.getString(R.string.start)
    HorizontalAlignment.CenterHorizontally -> context.getString(R.string.center_horizontally)
    HorizontalAlignment.End -> context.getString(R.string.end)
}

private fun VerticalArrangement.getVerticalArrangementTitle(context: Context): String = when (this) {
    VerticalArrangement.Top -> context.getString(R.string.top)
    VerticalArrangement.Center -> context.getString(R.string.center)
    VerticalArrangement.Bottom -> context.getString(R.string.bottom)
}
