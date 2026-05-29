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
package com.eblan.launcher.feature.settings.home.dialog

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
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
import com.eblan.launcher.domain.model.HomeSettings
import com.eblan.launcher.ui.dialog.RowTextFieldsDialog

@Composable
internal fun EditGridDialog(
    modifier: Modifier = Modifier,
    homeSettings: HomeSettings,
    onDismissRequest: () -> Unit,
    onUpdateHomeSettings: (HomeSettings) -> Unit,
) {
    var columns by remember {
        mutableStateOf("${homeSettings.columns}")
    }

    var rows by remember {
        mutableStateOf("${homeSettings.rows}")
    }

    var firstError by remember { mutableStateOf(false) }
    var secondError by remember { mutableStateOf(false) }

    RowTextFieldsDialog(
        modifier = modifier,
        title = "Grid",
        onDismissRequest = onDismissRequest,
        actions = {
            TextButton(
                onClick = onDismissRequest,
            ) {
                Text("Cancel")
            }

            TextButton(
                onClick = {
                    val newColumns = try {
                        columns.toInt()
                    } catch (_: NumberFormatException) {
                        firstError = true
                        0
                    }

                    val newRows = try {
                        rows.toInt()
                    } catch (_: NumberFormatException) {
                        secondError = true
                        0
                    }

                    if (newColumns > 0 && newRows > 0) {
                        onUpdateHomeSettings(
                            homeSettings.copy(
                                columns = newColumns,
                                rows = newRows,
                            ),
                        )

                        onDismissRequest()
                    }
                },
            ) {
                Text("Update")
            }
        },
        textFields = {
            TextField(
                value = columns,
                onValueChange = { columns = it },
                modifier = Modifier.weight(1f),
                label = { Text("Columns") },
                isError = firstError,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                ),
            )

            TextField(
                value = rows,
                onValueChange = { rows = it },
                modifier = Modifier.weight(1f),
                label = { Text("Rows") },
                isError = secondError,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                ),
            )
        },
    )
}
