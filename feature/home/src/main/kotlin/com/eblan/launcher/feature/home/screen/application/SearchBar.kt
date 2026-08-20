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
package com.eblan.launcher.feature.home.screen.application

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SearchBarState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.eblan.launcher.designsystem.icon.EblanLauncherIcons
import com.eblan.launcher.domain.model.EblanApplicationInfoOrder
import com.eblan.launcher.feature.home.R
import kotlinx.coroutines.launch
import com.eblan.launcher.common.R as commonR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ApplicationSearchBar(
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester,
    searchBarState: SearchBarState,
    textFieldState: TextFieldState,
    eblanApplicationInfoOrder: EblanApplicationInfoOrder,
    isRearrangeEblanApplicationInfo: Boolean,
    onUpdateEblanApplicationInfoOrder: (EblanApplicationInfoOrder) -> Unit,
    onUpdateIsRearrangeEblanApplicationInfo: (Boolean) -> Unit,
) {
    val scope = rememberCoroutineScope()

    var showEblanApplicationInfoOrderMenu by remember { mutableStateOf(false) }

    SearchBar(
        state = searchBarState,
        modifier = modifier
            .fillMaxWidth()
            .padding(10.dp),
        inputField = {
            SearchBarDefaults.InputField(
                modifier = Modifier.focusRequester(focusRequester),
                textFieldState = textFieldState,
                searchBarState = searchBarState,
                leadingIcon = {
                    Icon(
                        imageVector = EblanLauncherIcons.Search,
                        contentDescription = null,
                    )
                },
                trailingIcon = {
                    Row {
                        IconButton(
                            onClick = {
                                textFieldState.clearText()
                            },
                        ) {
                            Icon(
                                imageVector = EblanLauncherIcons.Close,
                                contentDescription = null,
                            )
                        }

                        Box {
                            IconButton(
                                onClick = {
                                    showEblanApplicationInfoOrderMenu = true
                                },
                            ) {
                                Icon(
                                    imageVector = EblanLauncherIcons.MoreVert,
                                    contentDescription = null,
                                )
                            }

                            EblanApplicationInfoOrderMenu(
                                expanded = showEblanApplicationInfoOrderMenu,
                                onDismissRequest = {
                                    showEblanApplicationInfoOrderMenu = false
                                },
                                eblanApplicationInfoOrder = eblanApplicationInfoOrder,
                                isRearrangeEblanApplicationInfo =
                                isRearrangeEblanApplicationInfo,
                                onUpdateEblanApplicationInfoOrder = {
                                    onUpdateEblanApplicationInfoOrder(it)
                                },
                                onUpdateIsRearrangeEblanApplicationInfo = {
                                    onUpdateIsRearrangeEblanApplicationInfo(it)
                                },
                            )
                        }
                    }
                },
                onSearch = {
                    scope.launch {
                        searchBarState.animateToCollapsed()
                    }
                },
                placeholder = {
                    Text(
                        text = stringResource(
                            commonR.string.search_applications,
                        ),
                    )
                },
            )
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ApplicationSearchBarWithoutMenu(
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester,
    searchBarState: SearchBarState,
    textFieldState: TextFieldState,
) {
    val scope = rememberCoroutineScope()

    SearchBar(
        state = searchBarState,
        modifier = modifier
            .fillMaxWidth()
            .padding(10.dp),
        inputField = {
            SearchBarDefaults.InputField(
                modifier = Modifier.focusRequester(focusRequester),
                textFieldState = textFieldState,
                searchBarState = searchBarState,
                leadingIcon = {
                    Icon(
                        imageVector = EblanLauncherIcons.Search,
                        contentDescription = null,
                    )
                },
                trailingIcon = {
                    IconButton(
                        onClick = {
                            textFieldState.clearText()
                        },
                    ) {
                        Icon(
                            imageVector = EblanLauncherIcons.Close,
                            contentDescription = null,
                        )
                    }
                },
                onSearch = { scope.launch { searchBarState.animateToCollapsed() } },
                placeholder = { Text(text = stringResource(commonR.string.search_applications)) },
            )
        },
    )
}

@Composable
private fun EblanApplicationInfoOrderMenu(
    modifier: Modifier = Modifier,
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    eblanApplicationInfoOrder: EblanApplicationInfoOrder,
    isRearrangeEblanApplicationInfo: Boolean,
    onUpdateEblanApplicationInfoOrder: (EblanApplicationInfoOrder) -> Unit,
    onUpdateIsRearrangeEblanApplicationInfo: (Boolean) -> Unit,
) {
    DropdownMenu(
        modifier = modifier,
        expanded = expanded,
        onDismissRequest = onDismissRequest,
    ) {
        Text(
            modifier = Modifier.padding(5.dp),
            text = stringResource(R.string.sort_applications),
            style = MaterialTheme.typography.bodySmall,
        )

        EblanApplicationInfoOrder.entries.forEach {
            DropdownMenuItem(
                text = {
                    Text(text = it.getTitle())
                },
                leadingIcon = {
                    RadioButton(
                        selected = eblanApplicationInfoOrder == it,
                        onClick = null,
                    )
                },
                onClick = {
                    onUpdateEblanApplicationInfoOrder(it)
                },
            )
        }

        HorizontalDivider()

        DropdownMenuItem(
            text = {
                Text(
                    text = stringResource(
                        R.string.rearrange_applications,
                    ),
                )
            },
            enabled = eblanApplicationInfoOrder == EblanApplicationInfoOrder.Index,
            trailingIcon = {
                Switch(
                    checked = isRearrangeEblanApplicationInfo,
                    enabled = eblanApplicationInfoOrder == EblanApplicationInfoOrder.Index,
                    onCheckedChange = null,
                )
            },
            onClick = {
                onUpdateIsRearrangeEblanApplicationInfo(!isRearrangeEblanApplicationInfo)
            },
        )
    }
}

@Composable
private fun EblanApplicationInfoOrder.getTitle(): String = when (this) {
    EblanApplicationInfoOrder.Alphabetical -> stringResource(R.string.alphabetical)
    EblanApplicationInfoOrder.Index -> stringResource(R.string.index)
}
