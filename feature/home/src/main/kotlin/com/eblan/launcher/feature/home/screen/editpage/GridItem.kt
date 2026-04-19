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
package com.eblan.launcher.feature.home.screen.editpage

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest.Builder
import coil3.request.addLastModifiedToFileCacheKey
import com.eblan.launcher.designsystem.icon.EblanLauncherIcons
import com.eblan.launcher.domain.model.GridItem
import com.eblan.launcher.domain.model.GridItemData
import com.eblan.launcher.domain.model.GridItemSettings
import com.eblan.launcher.domain.model.TextColor
import com.eblan.launcher.feature.home.util.getGridItemTextColor
import com.eblan.launcher.feature.home.util.getHorizontalAlignment
import com.eblan.launcher.feature.home.util.getSystemTextColor
import com.eblan.launcher.feature.home.util.getVerticalArrangement
import com.eblan.launcher.ui.local.LocalSettings

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun GridItemContent(
    modifier: Modifier = Modifier,
    gridItem: GridItem,
    gridItemSettings: GridItemSettings,
    hasShortcutHostPermission: Boolean,
    iconPackFilePaths: Map<String, String>,
    statusBarNotifications: Map<String, Int>,
    textColor: TextColor,
) {
    val currentGridItemSettings = if (gridItem.override) {
        gridItem.gridItemSettings
    } else {
        gridItemSettings
    }

    val currentTextColor = if (gridItem.override) {
        getGridItemTextColor(
            gridItemCustomTextColor = gridItem.gridItemSettings.customTextColor,
            gridItemTextColor = gridItem.gridItemSettings.textColor,
            systemCustomTextColor = gridItemSettings.customTextColor,
            systemTextColor = textColor,
        )
    } else {
        getSystemTextColor(
            systemCustomTextColor = gridItemSettings.customTextColor,
            systemTextColor = textColor,
        )
    }

    when (val data = gridItem.data) {
        is GridItemData.ApplicationInfo -> {
            ApplicationInfoGridItem(
                modifier = modifier,
                data = data,
                gridItemSettings = currentGridItemSettings,
                iconPackFilePaths = iconPackFilePaths,
                statusBarNotifications = statusBarNotifications,
                textColor = currentTextColor,
            )
        }

        is GridItemData.Widget -> {
            WidgetGridItem(modifier = modifier, data = data)
        }

        is GridItemData.ShortcutInfo -> {
            ShortcutInfoGridItem(
                modifier = modifier,
                data = data,
                gridItemSettings = currentGridItemSettings,
                hasShortcutHostPermission = hasShortcutHostPermission,
                textColor = currentTextColor,
            )
        }

        is GridItemData.Folder -> {
            FolderGridItem(
                modifier = modifier,
                data = data,
                gridItemSettings = currentGridItemSettings,
                iconPackFilePaths = iconPackFilePaths,
                textColor = currentTextColor,
            )
        }

        is GridItemData.ShortcutConfig -> {
            ShortcutConfigGridItem(
                modifier = modifier,
                data = data,
                gridItemSettings = currentGridItemSettings,
                textColor = currentTextColor,
            )
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun ApplicationInfoGridItem(
    modifier: Modifier = Modifier,
    data: GridItemData.ApplicationInfo,
    gridItemSettings: GridItemSettings,
    iconPackFilePaths: Map<String, String>,
    statusBarNotifications: Map<String, Int>,
    textColor: Color,
) {
    val settings = LocalSettings.current

    val horizontalAlignment =
        getHorizontalAlignment(horizontalAlignment = gridItemSettings.horizontalAlignment)

    val verticalArrangement =
        getVerticalArrangement(verticalArrangement = gridItemSettings.verticalArrangement)

    val maxLines = if (gridItemSettings.singleLineLabel) 1 else Int.MAX_VALUE

    val icon = iconPackFilePaths[data.componentName] ?: data.icon

    val hasNotifications =
        statusBarNotifications[data.packageName] != null && (
            statusBarNotifications[data.packageName]
                ?: 0
            ) > 0

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(gridItemSettings.padding.dp)
            .background(
                color = Color(gridItemSettings.customBackgroundColor),
                shape = RoundedCornerShape(size = gridItemSettings.cornerRadius.dp),
            ),
        horizontalAlignment = horizontalAlignment,
        verticalArrangement = verticalArrangement,
    ) {
        Box(modifier = Modifier.size(gridItemSettings.iconSize.dp)) {
            AsyncImage(
                model = Builder(LocalContext.current).data(data.customIcon ?: icon)
                    .addLastModifiedToFileCacheKey(true).build(),
                contentDescription = null,
                modifier = Modifier.matchParentSize(),
            )

            if (settings.isNotificationAccessGranted() && hasNotifications) {
                Box(
                    modifier = Modifier
                        .size((gridItemSettings.iconSize * 0.4).dp)
                        .align(Alignment.TopEnd)
                        .background(
                            color = MaterialTheme.colorScheme.primary,
                            shape = CircleShape,
                        ),
                )
            }

            if (data.serialNumber != 0L) {
                ElevatedCard(
                    modifier = Modifier
                        .size((gridItemSettings.iconSize * 0.4).dp)
                        .align(Alignment.BottomEnd),
                ) {
                    Icon(
                        imageVector = EblanLauncherIcons.Work,
                        contentDescription = null,
                        modifier = Modifier.padding(2.dp),
                    )
                }
            }
        }

        if (gridItemSettings.showLabel) {
            Text(
                text = data.customLabel ?: data.label,
                color = textColor,
                textAlign = TextAlign.Center,
                maxLines = maxLines,
                fontSize = gridItemSettings.textSize.sp,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun ShortcutInfoGridItem(
    modifier: Modifier = Modifier,
    data: GridItemData.ShortcutInfo,
    gridItemSettings: GridItemSettings,
    hasShortcutHostPermission: Boolean,
    textColor: Color,
) {
    val horizontalAlignment =
        getHorizontalAlignment(horizontalAlignment = gridItemSettings.horizontalAlignment)

    val verticalArrangement =
        getVerticalArrangement(verticalArrangement = gridItemSettings.verticalArrangement)

    val maxLines = if (gridItemSettings.singleLineLabel) 1 else Int.MAX_VALUE

    val customIcon = data.customIcon ?: data.icon

    val customShortLabel = data.customShortLabel ?: data.shortLabel

    val alpha = if (hasShortcutHostPermission && data.isEnabled) 1f else 0.3f

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(gridItemSettings.padding.dp)
            .background(
                color = Color(gridItemSettings.customBackgroundColor),
                shape = RoundedCornerShape(size = gridItemSettings.cornerRadius.dp),
            ),
        horizontalAlignment = horizontalAlignment,
        verticalArrangement = verticalArrangement,
    ) {
        Box(modifier = Modifier.size(gridItemSettings.iconSize.dp)) {
            AsyncImage(
                model = customIcon,
                modifier = Modifier
                    .matchParentSize()
                    .alpha(alpha),
                contentDescription = null,
            )

            AsyncImage(
                model = data.eblanApplicationInfoIcon,
                modifier = Modifier
                    .size((gridItemSettings.iconSize * 0.25).dp)
                    .align(Alignment.BottomEnd)
                    .alpha(alpha),
                contentDescription = null,
            )
        }

        if (gridItemSettings.showLabel) {
            Text(
                modifier = Modifier.alpha(alpha),
                text = customShortLabel,
                color = textColor,
                textAlign = TextAlign.Center,
                maxLines = maxLines,
                fontSize = gridItemSettings.textSize.sp,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun FolderGridItem(
    modifier: Modifier = Modifier,
    data: GridItemData.Folder,
    gridItemSettings: GridItemSettings,
    iconPackFilePaths: Map<String, String>,
    textColor: Color,
) {
    val horizontalAlignment =
        getHorizontalAlignment(horizontalAlignment = gridItemSettings.horizontalAlignment)

    val verticalArrangement =
        getVerticalArrangement(verticalArrangement = gridItemSettings.verticalArrangement)

    val maxLines = if (gridItemSettings.singleLineLabel) 1 else Int.MAX_VALUE

    val commonModifier = Modifier.size(gridItemSettings.iconSize.dp)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(gridItemSettings.padding.dp)
            .background(
                color = Color(gridItemSettings.customBackgroundColor),
                shape = RoundedCornerShape(size = gridItemSettings.cornerRadius.dp),
            ),
        horizontalAlignment = horizontalAlignment,
        verticalArrangement = verticalArrangement,
    ) {
        if (data.icon != null) {
            AsyncImage(
                model = data.icon,
                contentDescription = null,
                modifier = commonModifier,
            )
        } else {
            Box(
                modifier = commonModifier.background(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(5.dp),
                ),
            ) {
                FlowRow(
                    modifier = Modifier.matchParentSize(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalArrangement = Arrangement.SpaceEvenly,
                    maxItemsInEachRow = 3,
                    maxLines = 3,
                ) {
                    data.previewGridItemsByPage.forEach { applicationInfoFolderGridItem ->
                        key(applicationInfoFolderGridItem.id) {
                            val icon =
                                iconPackFilePaths[applicationInfoFolderGridItem.componentName]
                                    ?: applicationInfoFolderGridItem.icon

                            AsyncImage(
                                model = Builder(LocalContext.current)
                                    .data(applicationInfoFolderGridItem.customIcon ?: icon)
                                    .addLastModifiedToFileCacheKey(true).build(),
                                contentDescription = null,
                                modifier = Modifier.size((gridItemSettings.iconSize * 0.30).dp),
                            )
                        }
                    }
                }
            }
        }

        if (gridItemSettings.showLabel) {
            Text(
                text = data.label,
                color = textColor,
                textAlign = TextAlign.Center,
                maxLines = maxLines,
                fontSize = gridItemSettings.textSize.sp,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun WidgetGridItem(modifier: Modifier = Modifier, data: GridItemData.Widget) {
    AsyncImage(
        model = data.preview ?: data.icon,
        contentDescription = null,
        modifier = modifier.fillMaxSize(),
    )
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun ShortcutConfigGridItem(
    modifier: Modifier = Modifier,
    data: GridItemData.ShortcutConfig,
    gridItemSettings: GridItemSettings,
    textColor: Color,
) {
    val horizontalAlignment =
        getHorizontalAlignment(horizontalAlignment = gridItemSettings.horizontalAlignment)

    val verticalArrangement =
        getVerticalArrangement(verticalArrangement = gridItemSettings.verticalArrangement)

    val maxLines = if (gridItemSettings.singleLineLabel) 1 else Int.MAX_VALUE

    val icon = when {
        data.customIcon != null -> {
            data.customIcon
        }

        data.shortcutIntentIcon != null -> {
            data.shortcutIntentIcon
        }

        data.activityIcon != null -> {
            data.activityIcon
        }

        else -> {
            data.applicationIcon
        }
    }

    val label = when {
        data.customLabel != null -> {
            data.customLabel
        }

        data.shortcutIntentName != null -> {
            data.shortcutIntentName
        }

        data.activityLabel != null -> {
            data.activityLabel
        }

        else -> {
            data.applicationLabel
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(gridItemSettings.padding.dp)
            .background(
                color = Color(gridItemSettings.customBackgroundColor),
                shape = RoundedCornerShape(size = gridItemSettings.cornerRadius.dp),
            ),
        horizontalAlignment = horizontalAlignment,
        verticalArrangement = verticalArrangement,
    ) {
        Box(modifier = Modifier.size(gridItemSettings.iconSize.dp)) {
            AsyncImage(
                model = Builder(LocalContext.current).data(icon)
                    .addLastModifiedToFileCacheKey(true).build(),
                contentDescription = null,
                modifier = Modifier.matchParentSize(),
            )

            if (data.serialNumber != 0L) {
                ElevatedCard(
                    modifier = Modifier
                        .size((gridItemSettings.iconSize * 0.4).dp)
                        .align(Alignment.BottomEnd),
                ) {
                    Icon(
                        imageVector = EblanLauncherIcons.Work,
                        contentDescription = null,
                        modifier = Modifier.padding(2.dp),
                    )
                }
            }
        }

        if (gridItemSettings.showLabel) {
            Text(
                text = label.toString(),
                color = textColor,
                textAlign = TextAlign.Center,
                maxLines = maxLines,
                fontSize = gridItemSettings.textSize.sp,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
