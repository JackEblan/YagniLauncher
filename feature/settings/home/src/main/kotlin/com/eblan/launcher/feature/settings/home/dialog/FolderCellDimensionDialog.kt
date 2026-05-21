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
    var minCellWidth by remember { mutableStateOf("${homeSettings.minCellWidth}") }
    var minCellHeight by remember { mutableStateOf("${homeSettings.minCellHeight}") }
    var maxCellWidth by remember { mutableStateOf("${homeSettings.maxCellWidth}") }
    var maxCellHeight by remember { mutableStateOf("${homeSettings.maxCellHeight}") }

    var minCellWidthIsError by remember { mutableStateOf(false) }
    var minCellHeightIsError by remember { mutableStateOf(false) }
    var maxCellWidthIsError by remember { mutableStateOf(false) }
    var maxCellHeightIsError by remember { mutableStateOf(false) }

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
                            value = minCellWidth,
                            onValueChange = {
                                minCellWidth = it
                            },
                            modifier = Modifier.weight(1f),
                            label = {
                                Text(text = "Min Cell Width")
                            },
                            supportingText = {
                                if (minCellWidthIsError) {
                                    Text(text = "Min Cell Width is not valid")
                                }
                            },
                            isError = minCellWidthIsError,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                        )

                        Spacer(modifier = Modifier.width(5.dp))

                        TextField(
                            value = minCellHeight,
                            onValueChange = {
                                minCellHeight = it
                            },
                            modifier = Modifier.weight(1f),
                            label = {
                                Text(text = "Min Cell Height")
                            },
                            supportingText = {
                                if (minCellHeightIsError) {
                                    Text(text = "Min Cell Height is not valid")
                                }
                            },
                            isError = minCellHeightIsError,
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
                            value = maxCellWidth,
                            onValueChange = {
                                maxCellWidth = it
                            },
                            modifier = Modifier.weight(1f),
                            label = {
                                Text(text = "Max Cell Width")
                            },
                            supportingText = {
                                if (maxCellWidthIsError) {
                                    Text(text = "Max Cell Width is not valid")
                                }
                            },
                            isError = maxCellWidthIsError,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                        )

                        Spacer(modifier = Modifier.width(5.dp))

                        TextField(
                            value = maxCellHeight,
                            onValueChange = {
                                maxCellHeight = it
                            },
                            modifier = Modifier.weight(1f),
                            label = {
                                Text(text = "Max Cell Height")
                            },
                            supportingText = {
                                if (maxCellHeightIsError) {
                                    Text(text = "Max Cell Height is not valid")
                                }
                            },
                            isError = maxCellHeightIsError,
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
                                    minCellWidth.toInt()
                                } catch (_: NumberFormatException) {
                                    minCellWidthIsError = true
                                    0
                                }

                                val minCellHeight = try {
                                    minCellHeight.toInt()
                                } catch (_: NumberFormatException) {
                                    minCellHeightIsError = true
                                    0
                                }

                                val maxCellWidth = try {
                                    maxCellWidth.toInt()
                                } catch (_: NumberFormatException) {
                                    maxCellWidthIsError = true
                                    0
                                }

                                val maxCellHeight = try {
                                    maxCellHeight.toInt()
                                } catch (_: NumberFormatException) {
                                    maxCellHeightIsError = true
                                    0
                                }

                                if (minCellWidth > 0 && minCellHeight > 0 &&
                                    maxCellWidth > 0 && maxCellHeight > 0
                                ) {
                                    onUpdateHomeSettings(
                                        homeSettings.copy(
                                            minCellWidth = minCellWidth,
                                            minCellHeight = minCellHeight,
                                            maxCellWidth = maxCellWidth,
                                            maxCellHeight = maxCellHeight,
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
