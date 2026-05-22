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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import com.eblan.launcher.designsystem.component.EblanDialogContainer
import com.eblan.launcher.domain.model.HomeSettings

@Composable
internal fun FolderCellDimensionDialog(
    modifier: Modifier = Modifier,
    homeSettings: HomeSettings,
    onDismissRequest: () -> Unit,
    onUpdateHomeSettings: (HomeSettings) -> Unit,
) {
    var minFolderCellWidth by remember { mutableStateOf("${homeSettings.minFolderCellWidth}") }
    var minFolderCellHeight by remember { mutableStateOf("${homeSettings.minFolderCellHeight}") }
    var maxFolderCellWidth by remember { mutableStateOf("${homeSettings.maxFolderCellWidth}") }
    var maxFolderCellHeight by remember { mutableStateOf("${homeSettings.maxFolderCellHeight}") }

    var minFolderCellWidthIsError by remember { mutableStateOf(false) }
    var minFolderCellHeightIsError by remember { mutableStateOf(false) }
    var maxFolderCellWidthIsError by remember { mutableStateOf(false) }
    var maxFolderCellHeightIsError by remember { mutableStateOf(false) }

    EblanDialogContainer(
        content = {
            Column(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(10.dp),
            ) {
                Text(text = "Folder Cell Dimension", style = MaterialTheme.typography.titleLarge)

                Spacer(modifier = Modifier.height(10.dp))

                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        modifier = Modifier.padding(15.dp),
                        text = "Min",
                        style = MaterialTheme.typography.bodySmall,
                    )

                    Row(modifier = Modifier.fillMaxWidth()) {
                        TextField(
                            value = minFolderCellWidth,
                            onValueChange = {
                                minFolderCellWidth = it
                            },
                            modifier = Modifier.weight(1f),
                            label = {
                                Text(text = "Min Cell Width")
                            },
                            supportingText = {
                                if (minFolderCellWidthIsError) {
                                    Text(text = "Min Cell Width is not valid")
                                }
                            },
                            isError = minFolderCellWidthIsError,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                        )

                        Spacer(modifier = Modifier.width(5.dp))

                        TextField(
                            value = minFolderCellHeight,
                            onValueChange = {
                                minFolderCellHeight = it
                            },
                            modifier = Modifier.weight(1f),
                            label = {
                                Text(text = "Min Cell Height")
                            },
                            supportingText = {
                                if (minFolderCellHeightIsError) {
                                    Text(text = "Min Cell Height is not valid")
                                }
                            },
                            isError = minFolderCellHeightIsError,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                        )
                    }

                    Text(
                        modifier = Modifier.padding(15.dp),
                        text = "Max",
                        style = MaterialTheme.typography.bodySmall,
                    )

                    Row(modifier = Modifier.fillMaxWidth()) {
                        TextField(
                            value = maxFolderCellWidth,
                            onValueChange = {
                                maxFolderCellWidth = it
                            },
                            modifier = Modifier.weight(1f),
                            label = {
                                Text(text = "Max Cell Width")
                            },
                            supportingText = {
                                if (maxFolderCellWidthIsError) {
                                    Text(text = "Max Cell Width is not valid")
                                }
                            },
                            isError = maxFolderCellWidthIsError,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                        )

                        Spacer(modifier = Modifier.width(5.dp))

                        TextField(
                            value = maxFolderCellHeight,
                            onValueChange = {
                                maxFolderCellHeight = it
                            },
                            modifier = Modifier.weight(1f),
                            label = {
                                Text(text = "Max Cell Height")
                            },
                            supportingText = {
                                if (maxFolderCellHeightIsError) {
                                    Text(text = "Max Cell Height is not valid")
                                }
                            },
                            isError = maxFolderCellHeightIsError,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

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
                                val minCellWidth = try {
                                    minFolderCellWidth.toInt()
                                } catch (_: NumberFormatException) {
                                    minFolderCellWidthIsError = true
                                    0
                                }

                                val minCellHeight = try {
                                    minFolderCellHeight.toInt()
                                } catch (_: NumberFormatException) {
                                    minFolderCellHeightIsError = true
                                    0
                                }

                                val maxCellWidth = try {
                                    maxFolderCellWidth.toInt()
                                } catch (_: NumberFormatException) {
                                    maxFolderCellWidthIsError = true
                                    0
                                }

                                val maxCellHeight = try {
                                    maxFolderCellHeight.toInt()
                                } catch (_: NumberFormatException) {
                                    maxFolderCellHeightIsError = true
                                    0
                                }

                                if (minCellWidth > 0 && minCellHeight > 0 &&
                                    maxCellWidth > 0 && maxCellHeight > 0
                                ) {
                                    onUpdateHomeSettings(
                                        homeSettings.copy(
                                            minFolderCellWidth = minCellWidth,
                                            minFolderCellHeight = minCellHeight,
                                            maxFolderCellWidth = maxCellWidth,
                                            maxFolderCellHeight = maxCellHeight,
                                        ),
                                    )

                                    onDismissRequest()
                                }
                            },
                        ) {
                            Text(text = "Update")
                        }
                    }
                }
            }
        },
        onDismissRequest = onDismissRequest,
    )
}
