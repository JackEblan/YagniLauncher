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
internal fun EditFolderCellDimensionDialog(
    modifier: Modifier = Modifier,
    homeSettings: HomeSettings,
    onDismissRequest: () -> Unit,
    onUpdateHomeSettings: (HomeSettings) -> Unit,
) {
    var cellWidth by remember {
        mutableStateOf("${homeSettings.folderCellWidth}")
    }

    var cellHeight by remember {
        mutableStateOf("${homeSettings.folderCellHeight}")
    }

    var firstError by remember { mutableStateOf(false) }
    var secondError by remember { mutableStateOf(false) }

    RowTextFieldsDialog(
        modifier = modifier,
        title = "Folder Cell Dimension",
        onDismissRequest = onDismissRequest,
        textFields = {
            TextField(
                value = cellWidth,
                onValueChange = {
                    cellWidth = it
                    firstError = false
                },
                modifier = Modifier.weight(1f),
                label = { Text(text = "Cell Width") },
                supportingText = if (firstError) {
                    {
                        Text(text = "Cell width is not valid")
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
                value = cellHeight,
                onValueChange = {
                    cellHeight = it
                    secondError = false
                },
                modifier = Modifier.weight(1f),
                label = { Text(text = "Cell Height") },
                supportingText = if (secondError) {
                    {
                        Text(text = "Cell height is not valid")
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
                Text(text = "Cancel")
            }

            TextButton(
                onClick = {
                    val newWidth = try {
                        cellWidth.toInt()
                    } catch (_: NumberFormatException) {
                        firstError = true
                        0
                    }

                    val newHeight = try {
                        cellHeight.toInt()
                    } catch (_: NumberFormatException) {
                        secondError = true
                        0
                    }

                    if (newWidth > 0 && newHeight > 0) {
                        onUpdateHomeSettings(
                            homeSettings.copy(
                                folderCellWidth = newWidth,
                                folderCellHeight = newHeight,
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
