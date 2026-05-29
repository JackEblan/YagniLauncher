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
package com.eblan.launcher.feature.settings.appdrawer.dialog

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import com.eblan.launcher.domain.model.AppDrawerSettings
import com.eblan.launcher.ui.dialog.RowTextFieldsDialog

@Composable
internal fun EditHorizontalGridDialog(
    modifier: Modifier = Modifier,
    appDrawerSettings: AppDrawerSettings,
    onDismissRequest: () -> Unit,
    onUpdateAppDrawerSettings: (AppDrawerSettings) -> Unit,
) {
    var columns by remember {
        mutableStateOf("${appDrawerSettings.horizontalAppDrawerColumns}")
    }

    var rows by remember {
        mutableStateOf("${appDrawerSettings.horizontalAppDrawerRows}")
    }

    var firstError by remember { mutableStateOf(false) }
    var secondError by remember { mutableStateOf(false) }

    RowTextFieldsDialog(
        modifier = modifier,
        title = "Horizontal Grid",
        onDismissRequest = onDismissRequest,
        textFields = {
            TextField(
                value = columns,
                onValueChange = { columns = it },
                modifier = Modifier.weight(1f),
                label = { Text(text = "Columns") },
                supportingText = if (firstError) {
                    {
                        Text(text = "Columns is not valid")
                    }
                } else {
                    null
                },
                isError = firstError,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                ),
            )

            TextField(
                value = rows,
                onValueChange = { rows = it },
                modifier = Modifier.weight(1f),
                label = { Text(text = "Rows") },
                supportingText = if (secondError) {
                    {
                        Text(text = "Rows is not valid")
                    }
                } else {
                    null
                },
                isError = secondError,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                ),
            )
        },
        bottomActions = {
            TextButton(
                onClick = onDismissRequest,
            ) {
                Text("Cancel")
            }

            TextButton(
                onClick = {
                    val newColumns = try {
                        if (columns.toInt() > 2) {
                            firstError = false
                            columns.toInt()
                        } else {
                            firstError = true
                            0
                        }
                    } catch (_: NumberFormatException) {
                        firstError = true
                        0
                    }

                    val newRows = try {
                        if (rows.toInt() > 2) {
                            secondError = false
                            rows.toInt()
                        } else {
                            secondError = true
                            0
                        }
                    } catch (_: NumberFormatException) {
                        secondError = true
                        0
                    }

                    if (newColumns > 0 && newRows > 0) {
                        onUpdateAppDrawerSettings(
                            appDrawerSettings.copy(
                                horizontalAppDrawerColumns = newColumns,
                                horizontalAppDrawerRows = newRows,
                            ),
                        )

                        onDismissRequest()
                    }
                },
            ) {
                Text(text = "Update")
            }
        },
    )
}
