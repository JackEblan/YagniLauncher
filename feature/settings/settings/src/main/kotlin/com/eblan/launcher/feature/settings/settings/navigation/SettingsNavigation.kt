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
package com.eblan.launcher.feature.settings.settings.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.eblan.launcher.domain.model.SettingsRoute
import com.eblan.launcher.feature.settings.settings.SettingsRoute

fun NavGraphBuilder.settingsScreen(
    settingsRoute: SettingsRoute,
    onAppDrawer: () -> Unit,
    onExperimental: () -> Unit,
    onFinish: () -> Unit,
    onGeneral: () -> Unit,
    onGestures: () -> Unit,
    onHome: () -> Unit,
) {
    composable<SettingsRouteData> {
        SettingsRoute(
            settingsRoute = settingsRoute,
            onAppDrawer = onAppDrawer,
            onExperimental = onExperimental,
            onFinish = onFinish,
            onGeneral = onGeneral,
            onGestures = onGestures,
            onHome = onHome,
        )
    }
}
