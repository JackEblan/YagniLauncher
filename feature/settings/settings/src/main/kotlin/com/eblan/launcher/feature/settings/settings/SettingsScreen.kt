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
package com.eblan.launcher.feature.settings.settings

import android.content.Intent
import android.provider.Settings.ACTION_HOME_SETTINGS
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.eblan.launcher.designsystem.icon.EblanLauncherIcons
import com.eblan.launcher.ui.local.LocalPackageManager

@Composable
internal fun SettingsRoute(
    modifier: Modifier = Modifier,
    onAppDrawer: () -> Unit,
    onExperimental: () -> Unit,
    onFinish: () -> Unit,
    onGeneral: () -> Unit,
    onGestures: () -> Unit,
    onHome: () -> Unit,
) {
    SettingsScreen(
        modifier = modifier,
        onAppDrawer = onAppDrawer,
        onExperimental = onExperimental,
        onFinish = onFinish,
        onGeneral = onGeneral,
        onGestures = onGestures,
        onHome = onHome,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsScreen(
    modifier: Modifier = Modifier,
    onAppDrawer: () -> Unit,
    onExperimental: () -> Unit,
    onFinish: () -> Unit,
    onGeneral: () -> Unit,
    onGestures: () -> Unit,
    onHome: () -> Unit,
) {
    val context = LocalContext.current

    val packageManager = LocalPackageManager.current

    BackHandler {
        onFinish()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(text = "Settings")
                },
                navigationIcon = {
                    IconButton(onClick = onFinish) {
                        Icon(
                            imageVector = EblanLauncherIcons.ArrowBack,
                            contentDescription = null,
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        Column(
            modifier = modifier
                .verticalScroll(rememberScrollState())
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            AlphaWarningCard()

            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 15.dp),
            ) {
                if (!packageManager.isDefaultLauncher()) {
                    SettingsRow(
                        imageVector = EblanLauncherIcons.Info,
                        subtitle = "Choose Yagni Launcher",
                        title = "Default Launcher",
                        onClick = {
                            context.startActivity(Intent(ACTION_HOME_SETTINGS))
                        },
                    )

                    HorizontalDivider(modifier = Modifier.fillMaxWidth())
                }

                SettingsRow(
                    imageVector = EblanLauncherIcons.Settings,
                    subtitle = "Themes, icon packs",
                    title = "General",
                    onClick = onGeneral,
                )

                HorizontalDivider(modifier = Modifier.fillMaxWidth())

                SettingsRow(
                    imageVector = EblanLauncherIcons.Home,
                    subtitle = "Grid, icon, dock, and more",
                    title = "Home",
                    onClick = onHome,
                )

                HorizontalDivider(modifier = Modifier.fillMaxWidth())

                SettingsRow(
                    imageVector = EblanLauncherIcons.Apps,
                    subtitle = "Columns and rows count",
                    title = "App Drawer",
                    onClick = onAppDrawer,
                )

                HorizontalDivider(modifier = Modifier.fillMaxWidth())

                SettingsRow(
                    imageVector = EblanLauncherIcons.Gesture,
                    subtitle = "Swipe gesture actions",
                    title = "Gestures",
                    onClick = onGestures,
                )

                HorizontalDivider(modifier = Modifier.fillMaxWidth())

                SettingsRow(
                    imageVector = EblanLauncherIcons.DeveloperMode,
                    subtitle = "Advanced options for power users",
                    title = "Experimental",
                    onClick = onExperimental,
                )
            }
        }
    }
}

@Composable
private fun SettingsRow(
    modifier: Modifier = Modifier,
    imageVector: ImageVector,
    subtitle: String,
    title: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = modifier
            .clickable(onClick = onClick)
            .fillMaxWidth()
            .padding(15.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = null,
        )

        Spacer(modifier = Modifier.width(20.dp))

        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
            )

            Spacer(modifier = Modifier.height(5.dp))

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun AlphaWarningCard(modifier: Modifier = Modifier) {
    val uriHandler = LocalUriHandler.current

    val repoUrl = "https://github.com/JackEblan/YagniLauncher"

    val kofiUrl = "https://ko-fi.com/I3I01OJG21"

    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(10.dp),
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Thank you for using Yagni Launcher Alpha!",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )

            Text(
                text = "Development began in January 2025 with regular weekly updates. " +
                    "This is my most complex project to date, and I dedicate significant effort to its improvement.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )

            Text(
                text = "If you find value in the application, your support would be greatly appreciated " +
                    "through a donation on Ko-Fi or by starring the repository on GitHub.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth(0.9f),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(
                    onClick = { uriHandler.openUri(kofiUrl) },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = "Support on Ko-Fi",
                        textAlign = TextAlign.Center,
                    )
                }

                OutlinedButton(
                    onClick = { uriHandler.openUri(repoUrl) },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = "Star on GitHub",
                        textAlign = TextAlign.Center,
                    )
                }
            }

            Text(
                text = "Note: This informational card will be removed in future stable releases",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}
