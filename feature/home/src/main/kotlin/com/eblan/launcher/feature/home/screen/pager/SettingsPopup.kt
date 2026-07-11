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
package com.eblan.launcher.feature.home.screen.pager

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import com.eblan.launcher.designsystem.icon.EblanLauncherIcons
import com.eblan.launcher.domain.model.Associate
import com.eblan.launcher.domain.model.GridItem
import com.eblan.launcher.feature.home.R
import com.eblan.launcher.common.R as commonR

@Composable
internal fun SettingsPopup(
    gridItems: List<GridItem>,
    hasSystemFeatureAppWidgets: Boolean,
    popupSettingsIntOffset: IntOffset?,
    onDismissRequest: () -> Unit,
    onEditPage: (
        gridItems: List<GridItem>,
        associate: Associate,
    ) -> Unit,
    onSettings: () -> Unit,
    onShortcutConfigActivities: () -> Unit,
    onWallpaper: () -> Unit,
    onWidgets: () -> Unit,
) {
    requireNotNull(popupSettingsIntOffset)

    val transitionState = remember {
        MutableTransitionState(false).apply {
            targetState = true
        }
    }

    LaunchedEffect(
        key1 = transitionState.targetState,
        key2 = transitionState.isIdle,
    ) {
        if (!transitionState.targetState && transitionState.isIdle) {
            onDismissRequest()
        }
    }

    BackHandler(enabled = transitionState.targetState) {
        transitionState.targetState = false
    }

    Popup(
        popupPositionProvider = SettingsPopupPositionProvider(
            x = popupSettingsIntOffset.x,
            y = popupSettingsIntOffset.y,
        ),
        onDismissRequest = {
            transitionState.targetState = false
        },
    ) {
        AnimatedVisibility(
            visibleState = transitionState,
            enter = fadeIn(tween()) + scaleIn(initialScale = 0.8f, animationSpec = tween()),
            exit = fadeOut(tween()) + scaleOut(targetScale = 0.8f, animationSpec = tween()),
        ) {
            SettingsMenu(
                hasSystemFeatureAppWidgets = hasSystemFeatureAppWidgets,
                onEditDockPage = {
                    onEditPage(
                        gridItems,
                        Associate.DOCK,
                    )

                    transitionState.targetState = false
                },
                onEditPage = {
                    onEditPage(
                        gridItems,
                        Associate.GRID,
                    )

                    transitionState.targetState = false
                },
                onSettings = {
                    onSettings()

                    transitionState.targetState = false
                },
                onShortcutConfigActivities = {
                    onShortcutConfigActivities()

                    transitionState.targetState = false
                },
                onWallpaper = {
                    onWallpaper()

                    transitionState.targetState = false
                },
                onWidgets = {
                    onWidgets()

                    transitionState.targetState = false
                },
            )
        }
    }
}

@Composable
private fun SettingsMenu(
    modifier: Modifier = Modifier,
    hasSystemFeatureAppWidgets: Boolean,
    onEditDockPage: () -> Unit,
    onEditPage: () -> Unit,
    onSettings: () -> Unit,
    onShortcutConfigActivities: () -> Unit,
    onWallpaper: () -> Unit,
    onWidgets: () -> Unit,
) {
    Surface(
        modifier = modifier.width(IntrinsicSize.Max),
        shape = RoundedCornerShape(10.dp),
        shadowElevation = 2.dp,
        content = {
            Column {
                PopupMenuRow(
                    imageVector = EblanLauncherIcons.Settings,
                    title = stringResource(commonR.string.settings),
                    onClick = onSettings,
                )

                Spacer(modifier = Modifier.height(5.dp))

                PopupMenuRow(
                    imageVector = EblanLauncherIcons.Pages,
                    title = stringResource(R.string.edit_pages),
                    onClick = onEditPage,
                )

                Spacer(modifier = Modifier.height(5.dp))

                PopupMenuRow(
                    imageVector = EblanLauncherIcons.Pages,
                    title = stringResource(R.string.edit_dock_pages),
                    onClick = onEditDockPage,
                )

                if (hasSystemFeatureAppWidgets) {
                    Spacer(modifier = Modifier.height(5.dp))

                    PopupMenuRow(
                        imageVector = EblanLauncherIcons.Widgets,
                        title = stringResource(R.string.widgets),
                        onClick = onWidgets,
                    )
                }

                Spacer(modifier = Modifier.height(5.dp))

                PopupMenuRow(
                    imageVector = EblanLauncherIcons.Shortcut,
                    title = stringResource(R.string.shortcuts),
                    onClick = onShortcutConfigActivities,
                )

                Spacer(modifier = Modifier.height(5.dp))

                PopupMenuRow(
                    imageVector = EblanLauncherIcons.Image,
                    title = stringResource(R.string.wallpaper),
                    onClick = onWallpaper,
                )
            }
        },
    )
}

@Composable
private fun PopupMenuRow(
    modifier: Modifier = Modifier,
    imageVector: ImageVector,
    title: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = modifier
            .clickable(onClick = onClick)
            .fillMaxWidth()
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = null,
        )

        Spacer(modifier = Modifier.width(10.dp))

        Text(text = title)
    }
}
