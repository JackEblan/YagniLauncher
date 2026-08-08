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
package com.eblan.launcher.feature.home.screen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.SoftwareKeyboardController
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.onEach
import kotlin.time.Duration.Companion.milliseconds

@Composable
@OptIn(ExperimentalMaterial3Api::class, FlowPreview::class)
internal fun ScreenEffect(
    isPressHome: Boolean,
    swipeY: Float,
    screenHeight: Int,
    onDismiss: () -> Unit,
    keyboardController: SoftwareKeyboardController?,
    textFieldState: TextFieldState,
    onChangeLabel: (String) -> Unit,
) {
    LaunchedEffect(key1 = isPressHome) {
        if (isPressHome && swipeY < screenHeight.toFloat()) {
            onDismiss()
        }
    }

    LaunchedEffect(key1 = textFieldState) {
        snapshotFlow { textFieldState.text }.debounce(500L.milliseconds).onEach {
            onChangeLabel(it.toString())
        }.collect()
    }

    LaunchedEffect(key1 = swipeY) {
        if (swipeY == screenHeight.toFloat()) {
            keyboardController?.hide()
        }
    }

    BackHandler(enabled = swipeY < screenHeight.toFloat()) {
        onDismiss()
    }
}
