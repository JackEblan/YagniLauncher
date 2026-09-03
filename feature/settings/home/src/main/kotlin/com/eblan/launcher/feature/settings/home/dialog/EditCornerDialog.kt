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
import com.eblan.launcher.feature.settings.home.R
import com.eblan.launcher.common.R as commonR
import com.eblan.launcher.ui.R as uiR

@Composable
internal fun EditCornerDialog(
    modifier: Modifier = Modifier,
    top: Int,
    start: Int,
    bottom: Int,
    end: Int,
    onDismissRequest: () -> Unit,
    onUpdateCorner: (
        dockTop: Int,
        dockStart: Int,
        dockBottom: Int,
        dockEnd: Int,
    ) -> Unit,
) {
    var currentTop by remember { mutableStateOf("$top") }
    var currentStart by remember { mutableStateOf("$start") }
    var currentBottom by remember { mutableStateOf("$bottom") }
    var currentEnd by remember { mutableStateOf("$end") }

    var isErrorTop by remember { mutableStateOf(false) }
    var isErrorStart by remember { mutableStateOf(false) }
    var isErrorBottom by remember { mutableStateOf(false) }
    var isErrorEnd by remember { mutableStateOf(false) }

    EblanDialog(
        modifier = modifier,
        onDismissRequest = onDismissRequest,
    ) {
        Text(
            text = stringResource(R.string.dock_corner_radius),
            style = MaterialTheme.typography.titleLarge,
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TextField(
                value = currentTop,
                onValueChange = {
                    currentTop = it
                    isErrorTop = false
                },
                modifier = Modifier.weight(1f),
                label = { Text(text = stringResource(uiR.string.top)) },
                supportingText = if (isErrorTop) {
                    {
                        Text(text = stringResource(R.string.top_is_not_valid))
                    }
                } else {
                    null
                },
                isError = isErrorTop,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                ),
            )

            TextField(
                value = currentStart,
                onValueChange = {
                    currentStart = it
                    isErrorStart = false
                },
                modifier = Modifier.weight(1f),
                label = { Text(text = stringResource(uiR.string.start)) },
                supportingText = if (isErrorStart) {
                    {
                        Text(text = stringResource(R.string.start_is_not_valid))
                    }
                } else {
                    null
                },
                isError = isErrorStart,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                ),
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TextField(
                value = currentBottom,
                onValueChange = {
                    currentBottom = it
                    isErrorBottom = false
                },
                modifier = Modifier.weight(1f),
                label = { Text(text = stringResource(uiR.string.bottom)) },
                supportingText = if (isErrorBottom) {
                    {
                        Text(text = stringResource(R.string.bottom_is_not_valid))
                    }
                } else {
                    null
                },
                isError = isErrorBottom,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                ),
            )

            TextField(
                value = currentEnd,
                onValueChange = {
                    currentEnd = it
                    isErrorEnd = false
                },
                modifier = Modifier.weight(1f),
                label = { Text(text = stringResource(uiR.string.end)) },
                supportingText = if (isErrorEnd) {
                    {
                        Text(text = stringResource(R.string.end_is_not_valid))
                    }
                } else {
                    null
                },
                isError = isErrorEnd,
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
                    val newTop = currentTop.toIntOrNull()
                    val newStart = currentStart.toIntOrNull()
                    val newBottom =
                        currentBottom.toIntOrNull()
                    val newEnd =
                        currentEnd.toIntOrNull()

                    isErrorTop = newTop == null ||
                        newTop < 0
                    isErrorStart = newStart == null ||
                        newStart < 0
                    isErrorBottom = newBottom == null ||
                        newBottom < 0
                    isErrorEnd = newEnd == null ||
                        newEnd < 0

                    if (newTop != null && newTop >= 0 &&
                        newStart != null && newStart >= 0 &&
                        newBottom != null && newBottom >= 0 &&
                        newEnd != null && newEnd >= 0
                    ) {
                        onUpdateCorner(
                            newTop,
                            newStart,
                            newBottom,
                            newEnd,
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
