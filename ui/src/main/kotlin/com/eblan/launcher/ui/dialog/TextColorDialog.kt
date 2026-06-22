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
package com.eblan.launcher.ui.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.eblan.launcher.designsystem.component.EblanDialog
import com.eblan.launcher.designsystem.component.EblanRadioButton
import com.eblan.launcher.domain.model.TextColor

@Composable
fun TextColorDialog(
    title: String,
    modifier: Modifier = Modifier,
    textColor: TextColor,
    customTextColor: Int,
    onDismissRequest: () -> Unit,
    onUpdateClick: (
        textColor: TextColor,
        customColor: Int,
    ) -> Unit,
) {
    var selectedTextColor by remember { mutableStateOf(textColor) }

    var selectedCustomTextColor by remember { mutableIntStateOf(customTextColor) }

    var showColorPickerDialog by remember { mutableStateOf(false) }

    EblanDialog(
        modifier = modifier,
        onDismissRequest = onDismissRequest,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
        )

        Column(
            modifier = Modifier
                .selectableGroup()
                .fillMaxWidth(),
        ) {
            TextColor.entries.forEach { textColor ->
                EblanRadioButton(
                    selected = selectedTextColor == textColor,
                    text = textColor.name,
                    onClick = {
                        if (textColor == TextColor.Custom) {
                            showColorPickerDialog = true
                        } else {
                            selectedTextColor = textColor
                        }
                    },
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(
                onClick = onDismissRequest,
            ) {
                Text(text = "Cancel")
            }

            TextButton(
                onClick = {
                    onUpdateClick(
                        selectedTextColor,
                        selectedCustomTextColor,
                    )
                },
            ) {
                Text(text = "Update")
            }
        }
    }

    if (showColorPickerDialog) {
        ColorPickerDialog(
            title = title,
            customColor = customTextColor,
            onDismissRequest = {
                showColorPickerDialog = false
            },
            onSelectColor = { newCustomColor ->
                selectedTextColor = TextColor.Custom

                selectedCustomTextColor = newCustomColor

                showColorPickerDialog = false
            },
        )
    }
}
