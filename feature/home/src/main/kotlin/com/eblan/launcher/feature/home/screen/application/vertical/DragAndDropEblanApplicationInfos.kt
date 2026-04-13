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
package com.eblan.launcher.feature.home.screen.application.vertical

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.addLastModifiedToFileCacheKey
import com.eblan.launcher.designsystem.icon.EblanLauncherIcons
import com.eblan.launcher.domain.model.AppDrawerSettings
import com.eblan.launcher.domain.model.EblanApplicationInfo
import com.eblan.launcher.domain.model.EblanUserPageKey
import com.eblan.launcher.domain.model.GetEblanApplicationInfosByLabel
import com.eblan.launcher.feature.home.util.getHorizontalAlignment
import com.eblan.launcher.feature.home.util.getSystemTextColor
import com.eblan.launcher.feature.home.util.getVerticalArrangement
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalLayoutApi::class, ExperimentalSharedTransitionApi::class)
@Composable
internal fun DragAndDropEblanApplicationInfos(
    modifier: Modifier = Modifier,
    appDrawerSettings: AppDrawerSettings,
    eblanUserPageKey: EblanUserPageKey,
    getEblanApplicationInfosByLabel: GetEblanApplicationInfosByLabel,
    iconPackFilePaths: Map<String, String>,
    paddingValues: PaddingValues,
    onDismissDragAndDrop: () -> Unit,
    onUpdateEblanApplicationInfos: (List<EblanApplicationInfo>) -> Unit,
) {
    val lazyGridState = rememberLazyGridState()

    val eblanApplicationInfos =
        getEblanApplicationInfosByLabel.eblanApplicationInfos[eblanUserPageKey].orEmpty()

    var currentEblanApplicationInfos by remember { mutableStateOf(eblanApplicationInfos) }

    val gridDragDropState = rememberGridDragDropState(lazyGridState) { from, to ->
        currentEblanApplicationInfos = currentEblanApplicationInfos.toMutableList().apply {
            add(
                index = to,
                element = removeAt(from),
            )
        }
    }

    val isAtTop by remember(key1 = lazyGridState) {
        derivedStateOf {
            lazyGridState.firstVisibleItemIndex == 0 && lazyGridState.firstVisibleItemScrollOffset == 0
        }
    }

    var isDismiss by remember { mutableStateOf(false) }

    LaunchedEffect(key1 = eblanApplicationInfos) {
        if (isDismiss) {
            onDismissDragAndDrop()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(count = appDrawerSettings.appDrawerColumns),
            state = lazyGridState,
            modifier = Modifier
                .dragContainer(gridDragDropState = gridDragDropState)
                .matchParentSize(),
            contentPadding = PaddingValues(
                bottom = paddingValues.calculateBottomPadding(),
            ),
        ) {
            itemsIndexed(
                items = currentEblanApplicationInfos,
                key = { _, eblanApplicationInfo -> eblanApplicationInfo.componentName },
            ) { index, eblanApplicationInfo ->
                DraggableItem(
                    dragDropState = gridDragDropState,
                    index = index,
                ) {
                    EblanApplicationInfoItem(
                        eblanApplicationInfo = eblanApplicationInfo,
                        appDrawerSettings = appDrawerSettings,
                        iconPackFilePaths = iconPackFilePaths,
                    )
                }
            }
        }

        if (!WindowInsets.isImeVisible) {
            ScrollBarThumb(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .fillMaxHeight(),
                appDrawerSettings = appDrawerSettings,
                lazyGridState = lazyGridState,
                paddingValues = paddingValues,
                onScrollToItem = lazyGridState::scrollToItem,
            )
        }

        AnimatedVisibility(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(paddingValues),
            visible = isAtTop,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            ActionButtons(
                onCancel = onDismissDragAndDrop,
                onSave = {
                    onUpdateEblanApplicationInfos(currentEblanApplicationInfos)

                    isDismiss = true
                },
            )
        }
    }
}

@OptIn(ExperimentalUuidApi::class, ExperimentalSharedTransitionApi::class)
@Composable
private fun EblanApplicationInfoItem(
    modifier: Modifier = Modifier,
    eblanApplicationInfo: EblanApplicationInfo,
    appDrawerSettings: AppDrawerSettings,
    iconPackFilePaths: Map<String, String>,
) {
    val textColor = getSystemTextColor(
        systemCustomTextColor = appDrawerSettings.gridItemSettings.customTextColor,
        systemTextColor = appDrawerSettings.gridItemSettings.textColor,
    )

    val appDrawerRowsHeight = appDrawerSettings.appDrawerRowsHeight.dp

    val maxLines = if (appDrawerSettings.gridItemSettings.singleLineLabel) 1 else Int.MAX_VALUE

    val icon = iconPackFilePaths[eblanApplicationInfo.componentName] ?: eblanApplicationInfo.icon

    val horizontalAlignment =
        getHorizontalAlignment(horizontalAlignment = appDrawerSettings.gridItemSettings.horizontalAlignment)

    val verticalArrangement =
        getVerticalArrangement(verticalArrangement = appDrawerSettings.gridItemSettings.verticalArrangement)

    Column(
        modifier = modifier
            .height(appDrawerRowsHeight)
            .padding(appDrawerSettings.gridItemSettings.padding.dp)
            .background(
                color = Color(appDrawerSettings.gridItemSettings.customBackgroundColor),
                shape = RoundedCornerShape(size = appDrawerSettings.gridItemSettings.cornerRadius.dp),
            ),
        horizontalAlignment = horizontalAlignment,
        verticalArrangement = verticalArrangement,
    ) {
        Box(
            modifier = Modifier.size(appDrawerSettings.gridItemSettings.iconSize.dp),
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(eblanApplicationInfo.customIcon ?: icon)
                    .addLastModifiedToFileCacheKey(true).build(),
                contentDescription = null,
                modifier = Modifier.matchParentSize(),
            )

            if (eblanApplicationInfo.serialNumber != 0L) {
                ElevatedCard(
                    modifier = Modifier
                        .size((appDrawerSettings.gridItemSettings.iconSize * 0.40).dp)
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

        if (appDrawerSettings.gridItemSettings.showLabel) {
            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = eblanApplicationInfo.customLabel ?: eblanApplicationInfo.label,
                color = textColor,
                textAlign = TextAlign.Center,
                maxLines = maxLines,
                fontSize = appDrawerSettings.gridItemSettings.textSize.sp,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ActionButtons(
    modifier: Modifier = Modifier,
    onCancel: () -> Unit,
    onSave: () -> Unit,
) {
    Surface(
        modifier = modifier.padding(10.dp),
        shape = RoundedCornerShape(30.dp),
        tonalElevation = 10.dp,
    ) {
        Row(
            modifier = Modifier.padding(5.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            IconButton(onClick = onCancel) {
                Icon(
                    imageVector = EblanLauncherIcons.Close,
                    contentDescription = null,
                )
            }

            IconButton(onClick = onSave) {
                Icon(
                    imageVector = EblanLauncherIcons.Save,
                    contentDescription = null,
                )
            }
        }
    }
}
