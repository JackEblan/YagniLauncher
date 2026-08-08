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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.eblan.launcher.designsystem.component.EblanDialog
import com.eblan.launcher.domain.model.HomeSettings
import com.eblan.launcher.feature.settings.home.R
import com.eblan.launcher.common.R as commonR

@Composable
internal fun EditDockGridDialog(
    modifier: Modifier = Modifier,
    homeSettings: HomeSettings,
    onDismissRequest: () -> Unit,
    onUpdateHomeSettings: (HomeSettings) -> Unit,
) {
    var dockColumns by remember { mutableStateOf("${homeSettings.dockColumns}") }
    var dockRows by remember { mutableStateOf("${homeSettings.dockRows}") }

    var firstError by remember { mutableStateOf(false) }
    var secondError by remember { mutableStateOf(false) }

    EblanDialog(
        modifier = modifier,
        onDismissRequest = onDismissRequest,
    ) {
        Text(
            text = stringResource(R.string.dock_grid),
            style = MaterialTheme.typography.titleLarge,
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TextField(
                value = dockColumns,
                onValueChange = {
                    dockColumns = it
                    firstError = false
                },
                modifier = Modifier.weight(1f),
                label = { Text(text = stringResource(commonR.string.columns)) },
                supportingText = if (firstError) {
                    {
                        Text(text = stringResource(R.string.dock_columns_is_not_valid))
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
                value = dockRows,
                onValueChange = {
                    dockRows = it
                    secondError = false
                },
                modifier = Modifier.weight(1f),
                label = { Text(text = stringResource(commonR.string.rows)) },
                supportingText = if (secondError) {
                    {
                        Text(text = stringResource(R.string.dock_rows_is_not_valid))
                    }
                } else {
                    null
                },
                isError = secondError,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                ),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(
                onClick = onDismissRequest,
            ) {
                Text(text = stringResource(commonR.string.cancel))
            }

            TextButton(
                onClick = {
                    val newDockColumns = dockColumns.toIntOrNull()
                    val newDockRows = dockRows.toIntOrNull()

                    firstError = newDockColumns == null || newDockColumns <= 0
                    secondError = newDockRows == null || newDockRows <= 0

                    if (newDockColumns != null && newDockRows != null &&
                        newDockColumns > 0 && newDockRows > 0
                    ) {
                        onUpdateHomeSettings(
                            homeSettings.copy(
                                dockColumns = newDockColumns,
                                dockRows = newDockRows,
                            ),
                        )

                        onDismissRequest()
                    }
                },
            ) {
                Text(text = stringResource(commonR.string.update))
            }
        }
    }
}
