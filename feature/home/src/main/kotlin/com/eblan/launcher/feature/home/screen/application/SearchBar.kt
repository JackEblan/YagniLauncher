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

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SearchBarState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.eblan.launcher.designsystem.icon.EblanLauncherIcons
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ApplicationSearchBar(
    modifier: Modifier = Modifier,
    searchBarState: SearchBarState,
    textFieldState: TextFieldState,
    onUpdateShowEblanApplicationInfoOrderDialog: (Boolean) -> Unit,
) {
    val scope = rememberCoroutineScope()

    SearchBar(
        state = searchBarState,
        modifier = modifier
            .fillMaxWidth()
            .padding(10.dp),
        inputField = {
            SearchBarDefaults.InputField(
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
                            onUpdateShowEblanApplicationInfoOrderDialog(true)
                        },
                    ) {
                        Icon(
                            imageVector = EblanLauncherIcons.MoreVert,
                            contentDescription = null,
                        )
                    }
                },
                onSearch = {
                    scope.launch {
                        searchBarState.animateToCollapsed()
                    }
                },
                placeholder = {
                    Text(text = "Search Applications")
                },
            )
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ApplicationSearchBarWithoutMenu(
    modifier: Modifier = Modifier,
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
                textFieldState = textFieldState,
                searchBarState = searchBarState,
                leadingIcon = {
                    Icon(
                        imageVector = EblanLauncherIcons.Search,
                        contentDescription = null,
                    )
                },
                onSearch = { scope.launch { searchBarState.animateToCollapsed() } },
                placeholder = { Text(text = "Search Applications") },
            )
        },
    )
}
