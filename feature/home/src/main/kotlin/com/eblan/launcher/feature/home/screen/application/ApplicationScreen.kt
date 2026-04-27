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

import android.os.Build
import android.os.UserHandle
import androidx.activity.compose.BackHandler
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.eblan.launcher.designsystem.icon.EblanLauncherIcons
import com.eblan.launcher.domain.model.AppDrawerSettings
import com.eblan.launcher.domain.model.AppDrawerType
import com.eblan.launcher.domain.model.EblanAppWidgetProviderInfo
import com.eblan.launcher.domain.model.EblanApplicationInfo
import com.eblan.launcher.domain.model.EblanApplicationInfoGroup
import com.eblan.launcher.domain.model.EblanApplicationInfoTag
import com.eblan.launcher.domain.model.EblanShortcutInfo
import com.eblan.launcher.domain.model.EblanShortcutInfoByGroup
import com.eblan.launcher.domain.model.EblanUserPageKey
import com.eblan.launcher.domain.model.GetEblanApplicationInfosByLabelAndTag
import com.eblan.launcher.domain.model.ManagedProfileResult
import com.eblan.launcher.domain.model.TextColor
import com.eblan.launcher.feature.home.model.Drag
import com.eblan.launcher.feature.home.model.GridItemSource
import com.eblan.launcher.feature.home.model.SharedElementKey
import com.eblan.launcher.feature.home.screen.application.horizontal.HorizontalApplicationScreen
import com.eblan.launcher.feature.home.screen.application.list.ListApplicationScreen
import com.eblan.launcher.feature.home.screen.application.vertical.VerticalApplicationScreen
import com.eblan.launcher.framework.packagemanager.AndroidPackageManagerWrapper
import com.eblan.launcher.framework.usermanager.AndroidUserManagerWrapper
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun SharedTransitionScope.ApplicationScreen(
    modifier: Modifier = Modifier,
    alpha: Float,
    appDrawerSettings: AppDrawerSettings,
    cornerSize: Dp,
    currentPage: Int,
    drag: Drag,
    eblanAppWidgetProviderInfosGroup: Map<String, List<EblanAppWidgetProviderInfo>>,
    eblanApplicationInfoTags: List<EblanApplicationInfoTag>,
    eblanShortcutInfosGroup: Map<EblanShortcutInfoByGroup, List<EblanShortcutInfo>>,
    getEblanApplicationInfosByLabelAndTag: GetEblanApplicationInfosByLabelAndTag,
    hasShortcutHostPermission: Boolean,
    iconPackFilePaths: Map<String, String>,
    isPressHome: Boolean,
    managedProfileResult: ManagedProfileResult?,
    paddingValues: PaddingValues,
    screenHeight: Int,
    swipeY: Float,
    isVisibleOverlay: Boolean,
    onDismiss: () -> Unit,
    onDragEnd: (Float) -> Unit,
    onDraggingGridItem: () -> Unit,
    onEditApplicationInfo: (
        serialNumber: Long,
        componentName: String,
    ) -> Unit,
    onGetEblanApplicationInfosByLabel: (String) -> Unit,
    onGetEblanApplicationInfosByTagId: (Long?) -> Unit,
    onUpdateAppDrawerSettings: (AppDrawerSettings) -> Unit,
    onUpdateEblanApplicationInfos: (List<EblanApplicationInfo>) -> Unit,
    onUpdateGridItemSource: (GridItemSource) -> Unit,
    onUpdateImageBitmap: (ImageBitmap) -> Unit,
    onUpdateIsDragging: (Boolean) -> Unit,
    onUpdateOverlayBounds: (
        intOffset: IntOffset,
        intSize: IntSize,
    ) -> Unit,
    onUpdateSharedElementKey: (SharedElementKey?) -> Unit,
    onVerticalDrag: (Float) -> Unit,
    onWidgets: (EblanApplicationInfoGroup) -> Unit,
    onDraggingShortcutInfoGridItem: () -> Unit,
    onUpdateIsVisibleOverlay: (Boolean) -> Unit,
) {
    Surface(
        modifier = modifier
            .offset {
                IntOffset(x = 0, y = swipeY.roundToInt())
            }
            .fillMaxSize()
            .clip(RoundedCornerShape(cornerSize))
            .alpha(alpha),
        color = when (appDrawerSettings.backgroundColor) {
            TextColor.System -> {
                MaterialTheme.colorScheme.surface
            }

            TextColor.Light -> {
                Color.White
            }

            TextColor.Dark -> {
                Color.Black
            }

            TextColor.Custom -> {
                Color(appDrawerSettings.customBackgroundColor)
            }
        },
    ) {
        when (appDrawerSettings.appDrawerType) {
            AppDrawerType.Vertical -> {
                VerticalApplicationScreen(
                    appDrawerSettings = appDrawerSettings,
                    currentPage = currentPage,
                    drag = drag,
                    eblanAppWidgetProviderInfosGroup = eblanAppWidgetProviderInfosGroup,
                    eblanApplicationInfoTags = eblanApplicationInfoTags,
                    eblanShortcutInfosGroup = eblanShortcutInfosGroup,
                    getEblanApplicationInfosByLabelAndTag = getEblanApplicationInfosByLabelAndTag,
                    hasShortcutHostPermission = hasShortcutHostPermission,
                    iconPackFilePaths = iconPackFilePaths,
                    isPressHome = isPressHome,
                    managedProfileResult = managedProfileResult,
                    paddingValues = paddingValues,
                    screenHeight = screenHeight,
                    swipeY = swipeY,
                    isVisibleOverlay = isVisibleOverlay,
                    onDismiss = onDismiss,
                    onDragEnd = onDragEnd,
                    onDraggingGridItem = onDraggingGridItem,
                    onEditApplicationInfo = onEditApplicationInfo,
                    onGetEblanApplicationInfosByLabel = onGetEblanApplicationInfosByLabel,
                    onGetEblanApplicationInfosByTagId = onGetEblanApplicationInfosByTagId,
                    onUpdateAppDrawerSettings = onUpdateAppDrawerSettings,
                    onUpdateEblanApplicationInfos = onUpdateEblanApplicationInfos,
                    onUpdateGridItemSource = onUpdateGridItemSource,
                    onUpdateImageBitmap = onUpdateImageBitmap,
                    onUpdateIsDragging = onUpdateIsDragging,
                    onUpdateOverlayBounds = onUpdateOverlayBounds,
                    onUpdateSharedElementKey = onUpdateSharedElementKey,
                    onVerticalDrag = onVerticalDrag,
                    onWidgets = onWidgets,
                    onDraggingShortcutInfoGridItem = onDraggingShortcutInfoGridItem,
                    onUpdateIsVisibleOverlay = onUpdateIsVisibleOverlay,
                )
            }

            AppDrawerType.Horizontal -> {
                HorizontalApplicationScreen(
                    appDrawerSettings = appDrawerSettings,
                    currentPage = currentPage,
                    drag = drag,
                    eblanAppWidgetProviderInfosGroup = eblanAppWidgetProviderInfosGroup,
                    eblanApplicationInfoTags = eblanApplicationInfoTags,
                    eblanShortcutInfosGroup = eblanShortcutInfosGroup,
                    getEblanApplicationInfosByLabelAndTag = getEblanApplicationInfosByLabelAndTag,
                    hasShortcutHostPermission = hasShortcutHostPermission,
                    iconPackFilePaths = iconPackFilePaths,
                    isPressHome = isPressHome,
                    managedProfileResult = managedProfileResult,
                    paddingValues = paddingValues,
                    screenHeight = screenHeight,
                    swipeY = swipeY,
                    isVisibleOverlay = isVisibleOverlay,
                    onDismiss = onDismiss,
                    onDragEnd = onDragEnd,
                    onDraggingGridItem = onDraggingGridItem,
                    onEditApplicationInfo = onEditApplicationInfo,
                    onGetEblanApplicationInfosByLabel = onGetEblanApplicationInfosByLabel,
                    onGetEblanApplicationInfosByTagId = onGetEblanApplicationInfosByTagId,
                    onUpdateGridItemSource = onUpdateGridItemSource,
                    onUpdateImageBitmap = onUpdateImageBitmap,
                    onUpdateIsDragging = onUpdateIsDragging,
                    onUpdateOverlayBounds = onUpdateOverlayBounds,
                    onUpdateSharedElementKey = onUpdateSharedElementKey,
                    onVerticalDrag = onVerticalDrag,
                    onWidgets = onWidgets,
                    onDraggingShortcutInfoGridItem = onDraggingShortcutInfoGridItem,
                    onUpdateIsVisibleOverlay = onUpdateIsVisibleOverlay,
                )
            }

            AppDrawerType.List -> {
                ListApplicationScreen(
                    appDrawerSettings = appDrawerSettings,
                    currentPage = currentPage,
                    drag = drag,
                    eblanAppWidgetProviderInfosGroup = eblanAppWidgetProviderInfosGroup,
                    eblanApplicationInfoTags = eblanApplicationInfoTags,
                    eblanShortcutInfosGroup = eblanShortcutInfosGroup,
                    getEblanApplicationInfosByLabelAndTag = getEblanApplicationInfosByLabelAndTag,
                    hasShortcutHostPermission = hasShortcutHostPermission,
                    iconPackFilePaths = iconPackFilePaths,
                    isPressHome = isPressHome,
                    managedProfileResult = managedProfileResult,
                    paddingValues = paddingValues,
                    screenHeight = screenHeight,
                    swipeY = swipeY,
                    isVisibleOverlay = isVisibleOverlay,
                    onDismiss = onDismiss,
                    onDragEnd = onDragEnd,
                    onDraggingGridItem = onDraggingGridItem,
                    onEditApplicationInfo = onEditApplicationInfo,
                    onGetEblanApplicationInfosByLabel = onGetEblanApplicationInfosByLabel,
                    onGetEblanApplicationInfosByTagId = onGetEblanApplicationInfosByTagId,
                    onUpdateEblanApplicationInfos = onUpdateEblanApplicationInfos,
                    onUpdateGridItemSource = onUpdateGridItemSource,
                    onUpdateImageBitmap = onUpdateImageBitmap,
                    onUpdateIsDragging = onUpdateIsDragging,
                    onUpdateOverlayBounds = onUpdateOverlayBounds,
                    onUpdateSharedElementKey = onUpdateSharedElementKey,
                    onVerticalDrag = onVerticalDrag,
                    onWidgets = onWidgets,
                    onDraggingShortcutInfoGridItem = onDraggingShortcutInfoGridItem,
                    onUpdateIsVisibleOverlay = onUpdateIsVisibleOverlay,
                )
            }
        }
    }
}

@Composable
internal fun QuiteModeScreen(
    modifier: Modifier = Modifier,
    packageManager: AndroidPackageManagerWrapper,
    userHandle: UserHandle?,
    userManager: AndroidUserManagerWrapper,
    onDragEnd: (Float) -> Unit,
    onUpdateRequestQuietModeEnabled: (Boolean) -> Unit,
    onVerticalDrag: (Float) -> Unit,
) {
    Column(
        modifier = modifier
            .pointerInput(key1 = Unit) {
                detectVerticalDragGestures(
                    onVerticalDrag = { _, dragAmount ->
                        onVerticalDrag(dragAmount)
                    },
                    onDragEnd = {
                        onDragEnd(0f)
                    },
                )
            }
            .fillMaxSize()
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = "Work apps are paused", style = MaterialTheme.typography.titleLarge)

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "You won't receive notifications from your work apps",
            textAlign = TextAlign.Center,
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && packageManager.isDefaultLauncher() && userHandle != null) {
            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = {
                    userManager.requestQuietModeEnabled(
                        enableQuiteMode = false,
                        userHandle = userHandle,
                    )

                    onUpdateRequestQuietModeEnabled(userManager.isQuietModeEnabled(userHandle = userHandle))
                },
            ) {
                Text(text = "Unpause")
            }
        }
    }
}

@Composable
internal fun TagElevatedFilterChip(
    modifier: Modifier = Modifier,
    eblanApplicationInfoTag: EblanApplicationInfoTag,
    selectedEblanApplicationInfoTag: Long?,
    onUpdateEblanApplicationInfoTag: (Long?) -> Unit,
) {
    ElevatedFilterChip(
        modifier = modifier.padding(5.dp),
        onClick = {
            if (eblanApplicationInfoTag.id == selectedEblanApplicationInfoTag) {
                onUpdateEblanApplicationInfoTag(null)
            } else {
                onUpdateEblanApplicationInfoTag(eblanApplicationInfoTag.id)
            }
        },
        label = {
            Text(text = eblanApplicationInfoTag.name)
        },
        selected = eblanApplicationInfoTag.id == selectedEblanApplicationInfoTag,
        leadingIcon = if (eblanApplicationInfoTag.id == selectedEblanApplicationInfoTag) {
            {
                Icon(
                    imageVector = EblanLauncherIcons.Done,
                    contentDescription = null,
                    modifier = Modifier.size(FilterChipDefaults.IconSize),
                )
            }
        } else {
            null
        },
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun EblanApplicationInfoTabRow(
    modifier: Modifier = Modifier,
    currentPage: Int,
    eblanUserPageKeys: List<EblanUserPageKey>,
    eblanApplicationInfos: Map<EblanUserPageKey, List<EblanApplicationInfo>>,
    onAnimateScrollToPage: suspend (Int) -> Unit,
) {
    val scope = rememberCoroutineScope()

    val currentEblanUserPageKey = eblanApplicationInfos.keys.toList()[currentPage]

    val selectedTabIndex = remember(
        key1 = eblanUserPageKeys,
        key2 = currentEblanUserPageKey,
    ) {
        eblanUserPageKeys.indexOfFirst {
            it.eblanUser.serialNumber == currentEblanUserPageKey.eblanUser.serialNumber
        }
    }

    SecondaryTabRow(
        modifier = modifier,
        selectedTabIndex = selectedTabIndex,
    ) {
        eblanUserPageKeys.forEach { eblanUserPageKey ->
            Tab(
                selected = currentEblanUserPageKey == eblanUserPageKey,
                onClick = {
                    scope.launch {
                        onAnimateScrollToPage(
                            eblanApplicationInfos.keys.indexOfFirst {
                                it.eblanUser.serialNumber == eblanUserPageKey.eblanUser.serialNumber
                            },
                        )
                    }
                },
                text = {
                    Text(
                        text = eblanUserPageKey.eblanUser.eblanUserType.name,
                        maxLines = 1,
                    )
                },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, FlowPreview::class)
@Composable
internal fun ApplicationScreenEffect(
    horizontalPagerState: PagerState,
    isPressHome: Boolean,
    screenHeight: Int,
    selectedEblanApplicationInfoTagId: Long?,
    showPopupApplicationMenu: Boolean,
    swipeY: Float,
    textFieldState: TextFieldState,
    onDismiss: () -> Unit,
    onGetEblanApplicationInfosByLabel: (String) -> Unit,
    onGetEblanApplicationInfosByTagId: (Long?) -> Unit,
    onShowPopupApplicationMenu: (Boolean) -> Unit,
) {
    LaunchedEffect(key1 = textFieldState) {
        snapshotFlow { textFieldState.text }.debounce(500L).onEach { text ->
            onGetEblanApplicationInfosByLabel(text.toString())

            onShowPopupApplicationMenu(false)
        }.collect()
    }

    LaunchedEffect(key1 = selectedEblanApplicationInfoTagId) {
        onGetEblanApplicationInfosByTagId(selectedEblanApplicationInfoTagId)
    }

    LaunchedEffect(key1 = isPressHome) {
        if (isPressHome) {
            onDismiss()
        }
    }

    LaunchedEffect(key1 = horizontalPagerState.isScrollInProgress) {
        if (horizontalPagerState.isScrollInProgress && showPopupApplicationMenu) {
            onShowPopupApplicationMenu(false)
        }
    }

    BackHandler(enabled = swipeY < screenHeight.toFloat()) {
        onDismiss()
    }
}
