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

import androidx.compose.foundation.layout.fillMaxWidth
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

@Composable
internal fun EditIconSizeDialog(
    modifier: Modifier = Modifier,
    iconSize: Int,
    onDismissRequest: () -> Unit,
    onUpdateIconSize: (Int) -> Unit,
) {
    var value by remember {
        mutableStateOf("$iconSize")
    }

    var isError by remember { mutableStateOf(false) }

    TextFieldDialog(
        modifier = modifier,
        title = "Icon Size",
        onDismissRequest = onDismissRequest,
        textField = {
            TextField(
                value = value,
                onValueChange = {
                    value = it
                    isError = false
                },
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text(text = "Icon Size")
                },
                isError = isError,
                supportingText = if (isError) {
                    {
                        Text(text = "Icon Size is not valid")
                    }
                } else {
                    null
                },
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
                    try {
                        onUpdateIconSize(
                            value.toInt().coerceAtLeast(1),
                        )
                    } catch (_: NumberFormatException) {
                        isError = true
                    }
                },
            ) {
                Text(text = "Update")
            }
        },
    )
}
