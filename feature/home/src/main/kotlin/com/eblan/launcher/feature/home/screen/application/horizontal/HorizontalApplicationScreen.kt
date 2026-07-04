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
package com.eblan.launcher.feature.home.screen.application.horizontal

import android.graphics.Rect
import android.os.Build
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.eblan.launcher.designsystem.icon.EblanLauncherIcons
import com.eblan.launcher.domain.model.AppDrawerSettings
import com.eblan.launcher.domain.model.EblanAppWidgetProviderInfo
import com.eblan.launcher.domain.model.EblanApplicationInfo
import com.eblan.launcher.domain.model.EblanApplicationInfoGroup
import com.eblan.launcher.domain.model.EblanApplicationInfoTag
import com.eblan.launcher.domain.model.EblanShortcutInfo
import com.eblan.launcher.domain.model.EblanShortcutInfoByGroup
import com.eblan.launcher.domain.model.EblanUser
import com.eblan.launcher.domain.model.EblanUserPageKey
import com.eblan.launcher.domain.model.EblanUserType
import com.eblan.launcher.domain.model.GetEblanApplicationInfosByLabelAndTag
import com.eblan.launcher.domain.model.ManagedProfileResult
import com.eblan.launcher.domain.model.MoveGridItemResult
import com.eblan.launcher.feature.home.component.HorizontalAppDrawerGridLayout
import com.eblan.launcher.feature.home.model.Drag
import com.eblan.launcher.feature.home.model.GridItemSource
import com.eblan.launcher.feature.home.model.SharedElementKey
import com.eblan.launcher.feature.home.screen.application.ApplicationInfoPopup
import com.eblan.launcher.feature.home.screen.application.ApplicationScreenEffect
import com.eblan.launcher.feature.home.screen.application.ApplicationSearchBarWithoutMenu
import com.eblan.launcher.feature.home.screen.application.EblanApplicationInfoGridItem
import com.eblan.launcher.feature.home.screen.application.EblanApplicationInfoTabRow
import com.eblan.launcher.feature.home.screen.application.PrivateApplicationInfoPopup
import com.eblan.launcher.feature.home.screen.application.PrivateSpaceEblanApplicationInfoItem
import com.eblan.launcher.feature.home.screen.application.QuiteModeScreen
import com.eblan.launcher.feature.home.screen.application.TagElevatedFilterChip
import com.eblan.launcher.ui.local.LocalLauncherApps
import com.eblan.launcher.ui.local.LocalPackageManager
import com.eblan.launcher.ui.local.LocalUserManager
import kotlinx.coroutines.FlowPreview

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class, FlowPreview::class)
@Composable
internal fun HorizontalApplicationScreen(
    modifier: Modifier = Modifier,
    sharedTransitionScope: SharedTransitionScope,
    appDrawerSettings: AppDrawerSettings,
    drag: Drag,
    eblanAppWidgetProviderInfosGroup: Map<String, List<EblanAppWidgetProviderInfo>>,
    eblanApplicationInfoTags: List<EblanApplicationInfoTag>,
    eblanShortcutInfosGroup: Map<EblanShortcutInfoByGroup, List<EblanShortcutInfo>>,
    getEblanApplicationInfosByLabelAndTag: GetEblanApplicationInfosByLabelAndTag,
    hasShortcutHostPermission: Boolean,
    isPressHome: Boolean,
    managedProfileResult: ManagedProfileResult?,
    paddingValues: PaddingValues,
    screenHeight: Int,
    swipeY: Float,
    isVisibleOverlay: Boolean,
    onDismiss: () -> Unit,
    onDragEnd: () -> Unit,
    onEditApplicationInfo: (
        serialNumber: Long,
        componentName: String,
    ) -> Unit,
    onGetEblanApplicationInfosByLabel: (String) -> Unit,
    onGetEblanApplicationInfosByTagId: (Long?) -> Unit,
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
    onUpdateIsVisibleOverlay: (Boolean) -> Unit,
    onUpdateMoveGridItemResult: (MoveGridItemResult) -> Unit,
) {
    val density = LocalDensity.current

    val layoutDirection = LocalLayoutDirection.current

    val launcherApps = LocalLauncherApps.current

    var showPopupApplicationMenu by remember { mutableStateOf(false) }

    var showPrivatePopupApplicationMenu by remember { mutableStateOf(false) }

    var popupIntOffset by remember { mutableStateOf(IntOffset.Zero) }

    var popupIntSize by remember { mutableStateOf(IntSize.Zero) }

    val leftPadding = with(density) {
        paddingValues.calculateLeftPadding(layoutDirection).roundToPx()
    }

    val topPadding = with(density) {
        paddingValues.calculateTopPadding().roundToPx()
    }

    val horizontalPagerState = rememberPagerState(
        pageCount = {
            getEblanApplicationInfosByLabelAndTag.eblanApplicationInfoWithIconPackInfos.keys.size
        },
    )

    val searchBarState = rememberSearchBarState()

    val textFieldState = rememberTextFieldState()

    var selectedEblanApplicationInfoTagId by remember { mutableStateOf<Long?>(null) }

    var selectedEblanApplicationInfo by remember { mutableStateOf<EblanApplicationInfo?>(null) }

    val eblanUserPageKeys =
        remember(key1 = getEblanApplicationInfosByLabelAndTag.eblanApplicationInfoWithIconPackInfos) {
            getEblanApplicationInfosByLabelAndTag.eblanApplicationInfoWithIconPackInfos.keys.distinctBy { it.eblanUser.serialNumber }
        }

    ApplicationScreenEffect(
        horizontalPagerState = horizontalPagerState,
        isPressHome = isPressHome,
        screenHeight = screenHeight,
        selectedEblanApplicationInfoTagId = selectedEblanApplicationInfoTagId,
        showPopupApplicationMenu = showPopupApplicationMenu,
        swipeY = swipeY,
        textFieldState = textFieldState,
        onDismiss = onDismiss,
        onGetEblanApplicationInfosByLabel = onGetEblanApplicationInfosByLabel,
        onGetEblanApplicationInfosByTagId = onGetEblanApplicationInfosByTagId,
        onShowPopupApplicationMenu = {
            showPopupApplicationMenu = it
        },
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(paddingValues),
    ) {
        ApplicationSearchBarWithoutMenu(
            searchBarState = searchBarState,
            textFieldState = textFieldState,
        )

        if (eblanApplicationInfoTags.isNotEmpty()) {
            LazyRow(modifier = Modifier.fillMaxWidth()) {
                items(eblanApplicationInfoTags) { eblanApplicationInfoTag ->
                    TagElevatedFilterChip(
                        eblanApplicationInfoTag = eblanApplicationInfoTag,
                        selectedEblanApplicationInfoTag = selectedEblanApplicationInfoTagId,
                        onUpdateEblanApplicationInfoTag = {
                            selectedEblanApplicationInfoTagId = it
                        },
                    )
                }
            }
        }

        if (eblanUserPageKeys.size > 1) {
            EblanApplicationInfoTabRow(
                currentPage = horizontalPagerState.currentPage,
                eblanUserPageKeys = eblanUserPageKeys,
                eblanApplicationInfos = getEblanApplicationInfosByLabelAndTag.eblanApplicationInfoWithIconPackInfos,
                onAnimateScrollToPage = horizontalPagerState::animateScrollToPage,
            )
        }

        HorizontalPager(
            modifier = Modifier.fillMaxSize(),
            state = horizontalPagerState,
            userScrollEnabled = !isVisibleOverlay,
        ) { index ->
            EblanApplicationInfosPage(
                sharedTransitionScope = sharedTransitionScope,
                appDrawerSettings = appDrawerSettings,
                drag = drag,
                getEblanApplicationInfosByLabelAndTag = getEblanApplicationInfosByLabelAndTag,
                index = index,
                managedProfileResult = managedProfileResult,
                paddingValues = paddingValues,
                isVisibleOverlay = isVisibleOverlay,
                swipeY = swipeY,
                isScrollInProgress = horizontalPagerState.isScrollInProgress,
                onDismiss = onDismiss,
                onDragEnd = onDragEnd,
                onUpdateGridItemSource = onUpdateGridItemSource,
                onUpdateImageBitmap = onUpdateImageBitmap,
                onUpdateIsDragging = onUpdateIsDragging,
                onUpdateOverlayBounds = { intOffset, intSize ->
                    onUpdateOverlayBounds(intOffset, intSize)

                    popupIntOffset = intOffset

                    popupIntSize = intSize
                },
                onUpdatePopupMenu = {
                    showPopupApplicationMenu = it
                },
                onUpdatePrivatePopupMenu = {
                    showPrivatePopupApplicationMenu = it
                },
                onUpdateSharedElementKey = onUpdateSharedElementKey,
                onVerticalDrag = onVerticalDrag,
                onUpdateEblanApplicationInfo = {
                    selectedEblanApplicationInfo = it
                },
                onUpdateIsVisibleOverlay = onUpdateIsVisibleOverlay,
                onUpdateMoveGridItemResult = onUpdateMoveGridItemResult,
            )
        }
    }

    if (showPopupApplicationMenu && selectedEblanApplicationInfo != null) {
        ApplicationInfoPopup(
            eblanAppWidgetProviderInfos = eblanAppWidgetProviderInfosGroup,
            eblanShortcutInfosGroup = eblanShortcutInfosGroup,
            eblanApplicationInfo = selectedEblanApplicationInfo,
            gridItemSettings = appDrawerSettings.gridItemSettings,
            hasShortcutHostPermission = hasShortcutHostPermission,
            popupIntOffset = popupIntOffset,
            popupIntSize = popupIntSize,
            isVisibleOverlay = isVisibleOverlay,
            paddingValues = paddingValues,
            onDismissRequest = {
                showPopupApplicationMenu = false
            },
            onUpdateIsDragging = {
                showPopupApplicationMenu = false

                onDismiss()

                onUpdateIsDragging(it)
            },
            onEditApplicationInfo = onEditApplicationInfo,
            onTapShortcutInfo = { serialNumber, packageName, shortcutId ->
                val sourceBoundsX = popupIntOffset.x + leftPadding

                val sourceBoundsY = popupIntOffset.y + topPadding

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
                    launcherApps.startShortcut(
                        serialNumber = serialNumber,
                        packageName = packageName,
                        id = shortcutId,
                        sourceBounds = Rect(
                            sourceBoundsX,
                            sourceBoundsY,
                            sourceBoundsX + popupIntSize.width,
                            sourceBoundsY + popupIntSize.height,
                        ),
                    )
                }
            },
            onUpdateGridItemSource = onUpdateGridItemSource,
            onUpdateImageBitmap = onUpdateImageBitmap,
            onUpdateOverlayBounds = onUpdateOverlayBounds,
            onUpdateSharedElementKey = onUpdateSharedElementKey,
            onWidgets = onWidgets,
            onUpdateIsVisibleOverlay = onUpdateIsVisibleOverlay,
            onUpdateMoveGridItemResult = onUpdateMoveGridItemResult,
        )
    }

    if (showPrivatePopupApplicationMenu && selectedEblanApplicationInfo != null) {
        PrivateApplicationInfoPopup(
            drag = drag,
            eblanShortcutInfosGroup = eblanShortcutInfosGroup,
            eblanApplicationInfo = selectedEblanApplicationInfo,
            hasShortcutHostPermission = hasShortcutHostPermission,
            popupIntOffset = popupIntOffset,
            popupIntSize = popupIntSize,
            paddingValues = paddingValues,
            onDismissRequest = {
                showPrivatePopupApplicationMenu = false
            },
            onEditApplicationInfo = onEditApplicationInfo,
            onTapShortcutInfo = { serialNumber, packageName, shortcutId ->
                val sourceBoundsX = popupIntOffset.x + leftPadding

                val sourceBoundsY = popupIntOffset.y + topPadding

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
                    launcherApps.startShortcut(
                        serialNumber = serialNumber,
                        packageName = packageName,
                        id = shortcutId,
                        sourceBounds = Rect(
                            sourceBoundsX,
                            sourceBoundsY,
                            sourceBoundsX + popupIntSize.width,
                            sourceBoundsY + popupIntSize.height,
                        ),
                    )
                }
            },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalSharedTransitionApi::class)
@Composable
private fun EblanApplicationInfosPage(
    modifier: Modifier = Modifier,
    sharedTransitionScope: SharedTransitionScope,
    appDrawerSettings: AppDrawerSettings,
    drag: Drag,
    getEblanApplicationInfosByLabelAndTag: GetEblanApplicationInfosByLabelAndTag,
    index: Int,
    managedProfileResult: ManagedProfileResult?,
    paddingValues: PaddingValues,
    swipeY: Float,
    isVisibleOverlay: Boolean,
    isScrollInProgress: Boolean,
    onDismiss: () -> Unit,
    onDragEnd: () -> Unit,
    onUpdateGridItemSource: (GridItemSource) -> Unit,
    onUpdateImageBitmap: (ImageBitmap) -> Unit,
    onUpdateIsDragging: (Boolean) -> Unit,
    onUpdateOverlayBounds: (
        intOffset: IntOffset,
        intSize: IntSize,
    ) -> Unit,
    onUpdatePopupMenu: (Boolean) -> Unit,
    onUpdatePrivatePopupMenu: (Boolean) -> Unit,
    onUpdateSharedElementKey: (SharedElementKey?) -> Unit,
    onVerticalDrag: (Float) -> Unit,
    onUpdateEblanApplicationInfo: (EblanApplicationInfo) -> Unit,
    onUpdateIsVisibleOverlay: (Boolean) -> Unit,
    onUpdateMoveGridItemResult: (MoveGridItemResult) -> Unit,
) {
    val userManager = LocalUserManager.current

    val packageManager = LocalPackageManager.current

    val eblanUserPageKey =
        getEblanApplicationInfosByLabelAndTag.eblanApplicationInfoWithIconPackInfos.keys.toList().getOrElse(
            index = index,
            defaultValue = {
                EblanUserPageKey(
                    eblanUser = EblanUser(
                        serialNumber = 0L,
                        eblanUserType = EblanUserType.Personal,
                        isPrivateSpaceEntryPointHidden = false,
                    ),
                    page = 0,
                )
            },
        )

    val userHandle =
        userManager.getUserForSerialNumber(serialNumber = eblanUserPageKey.eblanUser.serialNumber)

    var isQuietModeEnabled by remember { mutableStateOf(false) }

    LaunchedEffect(key1 = userHandle) {
        if (userHandle != null) {
            isQuietModeEnabled = userManager.isQuietModeEnabled(userHandle = userHandle)
        }
    }

    LaunchedEffect(key1 = managedProfileResult) {
        if (managedProfileResult != null && managedProfileResult.serialNumber == eblanUserPageKey.eblanUser.serialNumber) {
            isQuietModeEnabled = managedProfileResult.isQuiteModeEnabled
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (isQuietModeEnabled) {
            QuiteModeScreen(
                packageManager = packageManager,
                userHandle = userHandle,
                userManager = userManager,
                onDragEnd = onDragEnd,
                onUpdateRequestQuietModeEnabled = {
                    isQuietModeEnabled = it
                },
                onVerticalDrag = onVerticalDrag,
            )
        } else {
            EblanApplicationInfos(
                sharedTransitionScope = sharedTransitionScope,
                appDrawerSettings = appDrawerSettings,
                drag = drag,
                eblanUserPageKey = eblanUserPageKey,
                getEblanApplicationInfosByLabelAndTag = getEblanApplicationInfosByLabelAndTag,
                paddingValues = paddingValues,
                isVisibleOverlay = isVisibleOverlay,
                swipeY = swipeY,
                isScrollInProgress = isScrollInProgress,
                onDismiss = onDismiss,
                onDragEnd = onDragEnd,
                onUpdateGridItemSource = onUpdateGridItemSource,
                onUpdateImageBitmap = onUpdateImageBitmap,
                onUpdateIsDragging = onUpdateIsDragging,
                onUpdateOverlayBounds = onUpdateOverlayBounds,
                onUpdatePopupMenu = onUpdatePopupMenu,
                onUpdatePrivatePopupMenu = onUpdatePrivatePopupMenu,
                onUpdateSharedElementKey = onUpdateSharedElementKey,
                onVerticalDrag = onVerticalDrag,
                onUpdateEblanApplicationInfo = onUpdateEblanApplicationInfo,
                onUpdateIsVisibleOverlay = onUpdateIsVisibleOverlay,
                onUpdateMoveGridItemResult = onUpdateMoveGridItemResult,
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && packageManager.isDefaultLauncher() &&
                eblanUserPageKey.eblanUser.serialNumber > 0 && userHandle != null
            ) {
                FloatingActionButton(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(
                            end = 10.dp,
                            bottom = paddingValues.calculateBottomPadding() + 10.dp,
                        ),
                    onClick = {
                        userManager.requestQuietModeEnabled(
                            enableQuiteMode = true,
                            userHandle = userHandle,
                        )

                        isQuietModeEnabled = userManager.isQuietModeEnabled(userHandle)
                    },
                ) {
                    Icon(
                        imageVector = EblanLauncherIcons.WorkOff,
                        contentDescription = null,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalSharedTransitionApi::class)
@Composable
private fun EblanApplicationInfos(
    modifier: Modifier = Modifier,
    sharedTransitionScope: SharedTransitionScope,
    appDrawerSettings: AppDrawerSettings,
    drag: Drag,
    eblanUserPageKey: EblanUserPageKey,
    getEblanApplicationInfosByLabelAndTag: GetEblanApplicationInfosByLabelAndTag,
    paddingValues: PaddingValues,
    isVisibleOverlay: Boolean,
    swipeY: Float,
    isScrollInProgress: Boolean,
    onDismiss: () -> Unit,
    onDragEnd: () -> Unit,
    onUpdateGridItemSource: (GridItemSource) -> Unit,
    onUpdateImageBitmap: (ImageBitmap) -> Unit,
    onUpdateIsDragging: (Boolean) -> Unit,
    onUpdateOverlayBounds: (
        intOffset: IntOffset,
        intSize: IntSize,
    ) -> Unit,
    onUpdatePopupMenu: (Boolean) -> Unit,
    onUpdatePrivatePopupMenu: (Boolean) -> Unit,
    onUpdateSharedElementKey: (SharedElementKey?) -> Unit,
    onVerticalDrag: (Float) -> Unit,
    onUpdateEblanApplicationInfo: (EblanApplicationInfo) -> Unit,
    onUpdateIsVisibleOverlay: (Boolean) -> Unit,
    onUpdateMoveGridItemResult: (MoveGridItemResult) -> Unit,
) {
    HorizontalAppDrawerGridLayout(
        modifier = modifier
            .pointerInput(key1 = Unit) {
                detectVerticalDragGestures(
                    onVerticalDrag = { _, dragAmount ->
                        onVerticalDrag(dragAmount)
                    },
                    onDragEnd = onDragEnd,
                )
            }
            .fillMaxSize(),
        columns = appDrawerSettings.horizontalAppDrawerColumns,
        eblanApplicationInfoWithIconPackInfos = getEblanApplicationInfosByLabelAndTag.eblanApplicationInfoWithIconPackInfos[eblanUserPageKey],
        rows = appDrawerSettings.horizontalAppDrawerRows,
        content = {
            when (eblanUserPageKey.eblanUser.eblanUserType) {
                EblanUserType.Personal,
                EblanUserType.Clone,
                EblanUserType.Work,
                -> {
                    EblanApplicationInfoGridItem(
                        sharedTransitionScope = sharedTransitionScope,
                        appDrawerSettings = appDrawerSettings,
                        drag = drag,
                        eblanApplicationInfoWithIconPackInfo = it,
                        paddingValues = paddingValues,
                        isVisibleOverlay = isVisibleOverlay,
                        appDrawerType = appDrawerSettings.appDrawerType,
                        isSwiping = swipeY > 0f,
                        isScrollInProgress = isScrollInProgress,
                        onDismiss = onDismiss,
                        onUpdateGridItemSource = onUpdateGridItemSource,
                        onUpdateImageBitmap = onUpdateImageBitmap,
                        onUpdateIsDragging = onUpdateIsDragging,
                        onUpdateOverlayBounds = onUpdateOverlayBounds,
                        onUpdatePopupMenu = onUpdatePopupMenu,
                        onUpdateSharedElementKey = onUpdateSharedElementKey,
                        onUpdateEblanApplicationInfo = onUpdateEblanApplicationInfo,
                        onUpdateIsVisibleOverlay = onUpdateIsVisibleOverlay,
                        onUpdateMoveGridItemResult = onUpdateMoveGridItemResult,
                    )
                }

                EblanUserType.Private -> {
                    PrivateSpaceEblanApplicationInfoItem(
                        appDrawerSettings = appDrawerSettings,
                        eblanApplicationInfoWithIconPackInfo = it,
                        paddingValues = paddingValues,
                        isVisibleOverlay = isVisibleOverlay,
                        onUpdateOverlayBounds = onUpdateOverlayBounds,
                        onUpdatePopupMenu = onUpdatePrivatePopupMenu,
                        onUpdateEblanApplicationInfo = onUpdateEblanApplicationInfo,
                    )
                }
            }
        },
    )
}
