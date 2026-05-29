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
package com.eblan.launcher.feature.editgriditem.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.eblan.launcher.designsystem.icon.EblanLauncherIcons
import com.eblan.launcher.domain.model.GridItem
import com.eblan.launcher.domain.model.GridItemData
import com.eblan.launcher.ui.dialog.TextFieldDialog

@Composable
internal fun EditShortcutConfigCustomLabelDialog(
    modifier: Modifier = Modifier,
    gridItem: GridItem,
    data: GridItemData.ShortcutConfig,
    onDismissRequest: () -> Unit,
    onUpdateGridItem: (GridItem) -> Unit,
) {
    var value by remember {
        mutableStateOf(data.customLabel ?: "")
    }

    var isError by remember { mutableStateOf(false) }

    TextFieldDialog(
        modifier = modifier,
        title = "Custom Label",
        onDismissRequest = onDismissRequest,
        actions = {
            TextButton(onClick = onDismissRequest) {
                Text("Cancel")
            }

            TextButton(
                onClick = {
                    if (value.isNotBlank()) {
                        onUpdateGridItem(
                            gridItem.copy(
                                data = data.copy(customLabel = value),
                            ),
                        )
                        onDismissRequest()
                    } else {
                        isError = true
                    }
                },
            ) {
                Text("Update")
            }
        },
        textField = {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Custom Label",
                        style = MaterialTheme.typography.titleLarge,
                    )

                    IconButton(
                        enabled = value.isNotBlank(),
                        onClick = {
                            onUpdateGridItem(
                                gridItem.copy(
                                    data = data.copy(customLabel = null),
                                ),
                            )
                            onDismissRequest()
                        },
                    ) {
                        Icon(
                            imageVector = EblanLauncherIcons.Delete,
                            contentDescription = null,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                TextField(
                    value = value,
                    onValueChange = {
                        value = it
                        isError = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Custom Label") },
                    isError = isError,
                    supportingText = {
                        if (isError) Text("Custom Label is not valid")
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                    ),
                    singleLine = true,
                )
            }
        },
    )
}
