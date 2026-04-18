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

import android.graphics.Rect
import android.os.Build
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberOverscrollEffect
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.eblan.launcher.designsystem.icon.EblanLauncherIcons
import com.eblan.launcher.domain.model.AppDrawerSettings
import com.eblan.launcher.domain.model.EblanAppWidgetProviderInfo
import com.eblan.launcher.domain.model.EblanApplicationInfo
import com.eblan.launcher.domain.model.EblanApplicationInfoGroup
import com.eblan.launcher.domain.model.EblanApplicationInfoOrder
import com.eblan.launcher.domain.model.EblanApplicationInfoTag
import com.eblan.launcher.domain.model.EblanShortcutInfo
import com.eblan.launcher.domain.model.EblanShortcutInfoByGroup
import com.eblan.launcher.domain.model.EblanUser
import com.eblan.launcher.domain.model.EblanUserPageKey
import com.eblan.launcher.domain.model.EblanUserType
import com.eblan.launcher.domain.model.GetEblanApplicationInfosByLabel
import com.eblan.launcher.domain.model.ManagedProfileResult
import com.eblan.launcher.feature.home.component.OffsetNestedScrollConnection
import com.eblan.launcher.feature.home.component.OffsetOverscrollEffect
import com.eblan.launcher.feature.home.dialog.EblanApplicationInfoOrderDialog
import com.eblan.launcher.feature.home.model.Drag
import com.eblan.launcher.feature.home.model.GridItemSource
import com.eblan.launcher.feature.home.model.SharedElementKey
import com.eblan.launcher.feature.home.screen.application.ApplicationInfoPopup
import com.eblan.launcher.feature.home.screen.application.ApplicationScreenEffect
import com.eblan.launcher.feature.home.screen.application.ApplicationSearchBar
import com.eblan.launcher.feature.home.screen.application.EblanApplicationInfoItem
import com.eblan.launcher.feature.home.screen.application.EblanApplicationInfoTabRow
import com.eblan.launcher.feature.home.screen.application.PrivateApplicationInfoPopup
import com.eblan.launcher.feature.home.screen.application.QuiteModeScreen
import com.eblan.launcher.feature.home.screen.application.TagElevatedFilterChip
import com.eblan.launcher.feature.home.screen.application.privateSpace
import com.eblan.launcher.ui.local.LocalLauncherApps
import com.eblan.launcher.ui.local.LocalPackageManager
import com.eblan.launcher.ui.local.LocalUserManager
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class, FlowPreview::class)
@Composable
internal fun SharedTransitionScope.VerticalApplicationScreen(
    modifier: Modifier = Modifier,
    appDrawerSettings: AppDrawerSettings,
    currentPage: Int,
    drag: Drag,
    eblanAppWidgetProviderInfosGroup: Map<String, List<EblanAppWidgetProviderInfo>>,
    eblanApplicationInfoTags: List<EblanApplicationInfoTag>,
    eblanShortcutInfosGroup: Map<EblanShortcutInfoByGroup, List<EblanShortcutInfo>>,
    getEblanApplicationInfosByLabel: GetEblanApplicationInfosByLabel,
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
    val density = LocalDensity.current

    var showPopupApplicationMenu by remember { mutableStateOf(false) }

    var showPrivatePopupApplicationMenu by remember { mutableStateOf(false) }

    var popupIntOffset by remember { mutableStateOf(IntOffset.Zero) }

    var popupIntSize by remember { mutableStateOf(IntSize.Zero) }

    val launcherApps = LocalLauncherApps.current

    val leftPadding = with(density) {
        paddingValues.calculateStartPadding(LayoutDirection.Ltr).roundToPx()
    }

    val topPadding = with(density) {
        paddingValues.calculateTopPadding().roundToPx()
    }

    val horizontalPagerState = rememberPagerState(
        pageCount = {
            getEblanApplicationInfosByLabel.eblanApplicationInfos.keys.size
        },
    )

    val scope = rememberCoroutineScope()

    val searchBarState = rememberSearchBarState()

    val textFieldState = rememberTextFieldState()

    var selectedEblanApplicationInfoTagId by remember { mutableStateOf<Long?>(null) }

    var isRearrangeEblanApplicationInfo by remember { mutableStateOf(false) }

    var showEblanApplicationInfoOrderDialog by remember { mutableStateOf(false) }

    var selectedEblanApplicationInfo by remember { mutableStateOf<EblanApplicationInfo?>(null) }

    val lazyGridState = rememberLazyGridState()

    val eblanUserPageKeys = remember(key1 = getEblanApplicationInfosByLabel.eblanApplicationInfos) {
        getEblanApplicationInfosByLabel.eblanApplicationInfos.keys.distinctBy { it.eblanUser.serialNumber }
    }

    ApplicationScreenEffect(
        appDrawerSettings = appDrawerSettings,
        drag = drag,
        horizontalPagerState = horizontalPagerState,
        isPressHome = isPressHome,
        screenHeight = screenHeight,
        searchBarState = searchBarState,
        selectedEblanApplicationInfoTagId = selectedEblanApplicationInfoTagId,
        showPopupApplicationMenu = showPopupApplicationMenu,
        swipeY = swipeY,
        textFieldState = textFieldState,
        onDismiss = onDismiss,
        onGetEblanApplicationInfosByLabel = onGetEblanApplicationInfosByLabel,
        onGetEblanApplicationInfosByTagId = onGetEblanApplicationInfosByTagId,
        onShowPopupApplicationMenu = { newShowPopupApplicationMenu ->
            showPopupApplicationMenu = newShowPopupApplicationMenu
        },
        onUpdateSelectedEblanApplicationInfoTagId = { newSelectedEblanApplicationInfoTagId ->
            selectedEblanApplicationInfoTagId = newSelectedEblanApplicationInfoTagId
        },
        onResetScroll = {
            scope.launch {
                horizontalPagerState.scrollToPage(0)

                lazyGridState.scrollToItem(0)
            }
        },
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(
                top = paddingValues.calculateTopPadding(),
                start = paddingValues.calculateStartPadding(LayoutDirection.Ltr),
                end = paddingValues.calculateEndPadding(LayoutDirection.Ltr),
            ),
    ) {
        ApplicationSearchBar(
            searchBarState = searchBarState,
            textFieldState = textFieldState,
            swipeY = swipeY,
            showKeyboard = appDrawerSettings.showKeyboard,
            onUpdateShowEblanApplicationInfoOrderDialog = { newShowEblanApplicationInfoOrderDialog ->
                showEblanApplicationInfoOrderDialog = newShowEblanApplicationInfoOrderDialog
            },
        )

        if (eblanApplicationInfoTags.isNotEmpty()) {
            LazyRow(modifier = Modifier.fillMaxWidth()) {
                items(eblanApplicationInfoTags) { eblanApplicationInfoTag ->
                    TagElevatedFilterChip(
                        eblanApplicationInfoTag = eblanApplicationInfoTag,
                        selectedEblanApplicationInfoTag = selectedEblanApplicationInfoTagId,
                        onUpdateEblanApplicationInfoTag = { newEblanApplicationInfoTagId ->
                            selectedEblanApplicationInfoTagId = newEblanApplicationInfoTagId
                        },
                    )
                }
            }
        }

        if (eblanUserPageKeys.size > 1) {
            EblanApplicationInfoTabRow(
                currentPage = horizontalPagerState.currentPage,
                eblanUserPageKeys = eblanUserPageKeys,
                eblanApplicationInfos = getEblanApplicationInfosByLabel.eblanApplicationInfos,
                onAnimateScrollToPage = horizontalPagerState::animateScrollToPage,
            )
        }

        HorizontalPager(
            modifier = Modifier.fillMaxSize(),
            state = horizontalPagerState,
        ) { index ->
            EblanApplicationInfosPage(
                appDrawerSettings = appDrawerSettings,
                currentPage = currentPage,
                drag = drag,
                eblanApplicationInfoOrder = appDrawerSettings.eblanApplicationInfoOrder,
                getEblanApplicationInfosByLabel = getEblanApplicationInfosByLabel,
                iconPackFilePaths = iconPackFilePaths,
                index = index,
                isRearrangeEblanApplicationInfo = isRearrangeEblanApplicationInfo,
                managedProfileResult = managedProfileResult,
                paddingValues = paddingValues,
                isVisibleOverlay = isVisibleOverlay,
                showPopupApplicationMenu = showPopupApplicationMenu,
                lazyGridState = lazyGridState,
                onDismiss = onDismiss,
                onDismissDragAndDrop = {
                    isRearrangeEblanApplicationInfo = false
                },
                onDragEnd = onDragEnd,
                onDraggingGridItem = onDraggingGridItem,
                onUpdateEblanApplicationInfos = onUpdateEblanApplicationInfos,
                onUpdateGridItemSource = onUpdateGridItemSource,
                onUpdateImageBitmap = onUpdateImageBitmap,
                onUpdateIsDragging = onUpdateIsDragging,
                onUpdateOverlayBounds = { intOffset, intSize ->
                    onUpdateOverlayBounds(intOffset, intSize)

                    popupIntOffset = intOffset

                    popupIntSize = intSize
                },
                onUpdatePopupMenu = { newShowPopupApplicationMenu ->
                    showPopupApplicationMenu = newShowPopupApplicationMenu
                },
                onUpdatePrivatePopupMenu = { newShowPrivatePopupApplicationMenu ->
                    showPrivatePopupApplicationMenu = newShowPrivatePopupApplicationMenu
                },
                onUpdateSharedElementKey = onUpdateSharedElementKey,
                onVerticalDrag = onVerticalDrag,
                onUpdateEblanApplicationInfo = { eblanApplicationInfo ->
                    selectedEblanApplicationInfo = eblanApplicationInfo
                },
                onUpdateIsVisibleOverlay = onUpdateIsVisibleOverlay,
            )
        }
    }

    if (showPopupApplicationMenu && selectedEblanApplicationInfo != null) {
        ApplicationInfoPopup(
            currentPage = currentPage,
            drag = drag,
            eblanAppWidgetProviderInfos = eblanAppWidgetProviderInfosGroup,
            eblanShortcutInfosGroup = eblanShortcutInfosGroup,
            eblanApplicationInfo = selectedEblanApplicationInfo,
            gridItemSettings = appDrawerSettings.gridItemSettings,
            hasShortcutHostPermission = hasShortcutHostPermission,
            paddingValues = paddingValues,
            popupIntOffset = popupIntOffset,
            popupIntSize = popupIntSize,
            onDismissRequest = {
                showPopupApplicationMenu = false
            },
            onDraggingShortcutInfoGridItem = {
                showPopupApplicationMenu = false

                onDismiss()

                onDraggingShortcutInfoGridItem()
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
        )
    }

    if (showEblanApplicationInfoOrderDialog) {
        EblanApplicationInfoOrderDialog(
            eblanApplicationInfoOrder = appDrawerSettings.eblanApplicationInfoOrder,
            onDismissRequest = {
                showEblanApplicationInfoOrderDialog = false
            },
            onUpdateClick = { eblanApplicationInfoOrder, newIsRearrangeEblanApplicationInfo ->
                onUpdateAppDrawerSettings(appDrawerSettings.copy(eblanApplicationInfoOrder = eblanApplicationInfoOrder))

                isRearrangeEblanApplicationInfo = newIsRearrangeEblanApplicationInfo

                showEblanApplicationInfoOrderDialog = false
            },
        )
    }

    if (showPrivatePopupApplicationMenu && selectedEblanApplicationInfo != null) {
        PrivateApplicationInfoPopup(
            drag = drag,
            eblanShortcutInfosGroup = eblanShortcutInfosGroup,
            eblanApplicationInfo = selectedEblanApplicationInfo,
            hasShortcutHostPermission = hasShortcutHostPermission,
            paddingValues = paddingValues,
            popupIntOffset = popupIntOffset,
            popupIntSize = popupIntSize,
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
private fun SharedTransitionScope.EblanApplicationInfosPage(
    modifier: Modifier = Modifier,
    appDrawerSettings: AppDrawerSettings,
    currentPage: Int,
    drag: Drag,
    eblanApplicationInfoOrder: EblanApplicationInfoOrder,
    getEblanApplicationInfosByLabel: GetEblanApplicationInfosByLabel,
    iconPackFilePaths: Map<String, String>,
    index: Int,
    isRearrangeEblanApplicationInfo: Boolean,
    managedProfileResult: ManagedProfileResult?,
    paddingValues: PaddingValues,
    showPopupApplicationMenu: Boolean,
    isVisibleOverlay: Boolean,
    lazyGridState: LazyGridState,
    onDismiss: () -> Unit,
    onDismissDragAndDrop: () -> Unit,
    onDragEnd: (Float) -> Unit,
    onDraggingGridItem: () -> Unit,
    onUpdateEblanApplicationInfos: (List<EblanApplicationInfo>) -> Unit,
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
) {
    val userManager = LocalUserManager.current

    val packageManager = LocalPackageManager.current

    val eblanUserPageKey =
        getEblanApplicationInfosByLabel.eblanApplicationInfos.keys.toList().getOrElse(
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
                onUpdateRequestQuietModeEnabled = { newIsQuietModeEnabled ->
                    isQuietModeEnabled = newIsQuietModeEnabled
                },
                onVerticalDrag = onVerticalDrag,
            )
        } else if (isRearrangeEblanApplicationInfo && eblanApplicationInfoOrder == EblanApplicationInfoOrder.Index) {
            DragAndDropEblanApplicationInfos(
                appDrawerSettings = appDrawerSettings,
                eblanUserPageKey = eblanUserPageKey,
                getEblanApplicationInfosByLabel = getEblanApplicationInfosByLabel,
                iconPackFilePaths = iconPackFilePaths,
                paddingValues = paddingValues,
                onDismissDragAndDrop = onDismissDragAndDrop,
                onUpdateEblanApplicationInfos = onUpdateEblanApplicationInfos,
            )
        } else {
            EblanApplicationInfos(
                appDrawerSettings = appDrawerSettings,
                currentPage = currentPage,
                drag = drag,
                eblanUserPageKey = eblanUserPageKey,
                getEblanApplicationInfosByLabel = getEblanApplicationInfosByLabel,
                iconPackFilePaths = iconPackFilePaths,
                managedProfileResult = managedProfileResult,
                paddingValues = paddingValues,
                showPopupApplicationMenu = showPopupApplicationMenu,
                isVisibleOverlay = isVisibleOverlay,
                lazyGridState = lazyGridState,
                onDismiss = onDismiss,
                onDragEnd = onDragEnd,
                onDraggingGridItem = onDraggingGridItem,
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
                onScrollToItem = lazyGridState::scrollToItem,
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
private fun SharedTransitionScope.EblanApplicationInfos(
    modifier: Modifier = Modifier,
    appDrawerSettings: AppDrawerSettings,
    currentPage: Int,
    drag: Drag,
    eblanUserPageKey: EblanUserPageKey,
    getEblanApplicationInfosByLabel: GetEblanApplicationInfosByLabel,
    iconPackFilePaths: Map<String, String>,
    managedProfileResult: ManagedProfileResult?,
    paddingValues: PaddingValues,
    isVisibleOverlay: Boolean,
    showPopupApplicationMenu: Boolean,
    lazyGridState: LazyGridState,
    onDismiss: () -> Unit,
    onDragEnd: (Float) -> Unit,
    onDraggingGridItem: () -> Unit,
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
    onScrollToItem: suspend (Int) -> Unit,
) {
    val scope = rememberCoroutineScope()

    val overscrollEffect = remember(key1 = scope) {
        OffsetOverscrollEffect(
            scope = scope,
            onVerticalDrag = onVerticalDrag,
            onDragEnd = onDragEnd,
        )
    }

    val canScroll by remember(key1 = lazyGridState) {
        derivedStateOf {
            lazyGridState.canScrollForward || lazyGridState.canScrollBackward
        }
    }

    val nestedScrollConnection = remember {
        OffsetNestedScrollConnection(
            onVerticalDrag = onVerticalDrag,
            onDragEnd = onDragEnd,
        )
    }

    var isQuietModeEnabled by remember { mutableStateOf(false) }

    LaunchedEffect(key1 = lazyGridState.isScrollInProgress) {
        if (lazyGridState.isScrollInProgress && showPopupApplicationMenu) {
            onUpdatePopupMenu(false)
        }
    }

    Box(
        modifier = modifier
            .run {
                if (!canScroll) {
                    nestedScroll(nestedScrollConnection)
                } else {
                    this
                }
            }
            .fillMaxSize(),
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(count = appDrawerSettings.appDrawerColumns),
            state = lazyGridState,
            modifier = Modifier.matchParentSize(),
            contentPadding = PaddingValues(
                bottom = paddingValues.calculateBottomPadding(),
            ),
            overscrollEffect = if (canScroll) {
                overscrollEffect
            } else {
                rememberOverscrollEffect()
            },
        ) {
            when (eblanUserPageKey.eblanUser.eblanUserType) {
                EblanUserType.Personal -> {
                    items(
                        items = getEblanApplicationInfosByLabel.eblanApplicationInfos[eblanUserPageKey].orEmpty(),
                        key = { eblanApplicationInfo -> eblanApplicationInfo.serialNumber to eblanApplicationInfo.componentName },
                    ) { eblanApplicationInfo ->
                        EblanApplicationInfoItem(
                            appDrawerSettings = appDrawerSettings,
                            currentPage = currentPage,
                            drag = drag,
                            eblanApplicationInfo = eblanApplicationInfo,
                            iconPackFilePaths = iconPackFilePaths,
                            paddingValues = paddingValues,
                            isVisibleOverlay = isVisibleOverlay,
                            appDrawerType = appDrawerSettings.appDrawerType,
                            onDismiss = onDismiss,
                            onDraggingGridItem = onDraggingGridItem,
                            onUpdateGridItemSource = onUpdateGridItemSource,
                            onUpdateImageBitmap = onUpdateImageBitmap,
                            onUpdateIsDragging = onUpdateIsDragging,
                            onUpdateOverlayBounds = onUpdateOverlayBounds,
                            onUpdatePopupMenu = onUpdatePopupMenu,
                            onUpdateSharedElementKey = onUpdateSharedElementKey,
                            onUpdateEblanApplicationInfo = onUpdateEblanApplicationInfo,
                            onUpdateIsVisibleOverlay = onUpdateIsVisibleOverlay,
                            onScrollToItem = onScrollToItem,
                        )
                    }

                    privateSpace(
                        appDrawerSettings = appDrawerSettings,
                        drag = drag,
                        iconPackFilePaths = iconPackFilePaths,
                        isQuietModeEnabled = isQuietModeEnabled,
                        managedProfileResult = managedProfileResult,
                        paddingValues = paddingValues,
                        privateEblanApplicationInfos = getEblanApplicationInfosByLabel.privateEblanApplicationInfos,
                        privateEblanUser = getEblanApplicationInfosByLabel.privateEblanUser,
                        onUpdateIsQuietModeEnabled = { newIsQuiteModeEnabled ->
                            isQuietModeEnabled = newIsQuiteModeEnabled
                        },
                        onUpdateOverlayBounds = onUpdateOverlayBounds,
                        onUpdatePopupMenu = onUpdatePrivatePopupMenu,
                        onUpdateEblanApplicationInfo = onUpdateEblanApplicationInfo,
                    )
                }

                else -> {
                    items(
                        items = getEblanApplicationInfosByLabel.eblanApplicationInfos[eblanUserPageKey].orEmpty(),
                        key = { eblanApplicationInfo -> eblanApplicationInfo.serialNumber to eblanApplicationInfo.componentName },
                    ) { eblanApplicationInfo ->
                        EblanApplicationInfoItem(
                            appDrawerSettings = appDrawerSettings,
                            currentPage = currentPage,
                            drag = drag,
                            eblanApplicationInfo = eblanApplicationInfo,
                            iconPackFilePaths = iconPackFilePaths,
                            paddingValues = paddingValues,
                            isVisibleOverlay = isVisibleOverlay,
                            appDrawerType = appDrawerSettings.appDrawerType,
                            onDismiss = onDismiss,
                            onDraggingGridItem = onDraggingGridItem,
                            onUpdateGridItemSource = onUpdateGridItemSource,
                            onUpdateImageBitmap = onUpdateImageBitmap,
                            onUpdateIsDragging = onUpdateIsDragging,
                            onUpdateOverlayBounds = onUpdateOverlayBounds,
                            onUpdatePopupMenu = onUpdatePopupMenu,
                            onUpdateSharedElementKey = onUpdateSharedElementKey,
                            onUpdateEblanApplicationInfo = onUpdateEblanApplicationInfo,
                            onUpdateIsVisibleOverlay = onUpdateIsVisibleOverlay,
                            onScrollToItem = onScrollToItem,
                        )
                    }
                }
            }
        }

        if (!WindowInsets.isImeVisible && canScroll) {
            ScrollBarThumb(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .fillMaxHeight(),
                appDrawerColumns = appDrawerSettings.appDrawerColumns,
                lazyGridState = lazyGridState,
                paddingValues = paddingValues,
                onScrollToItem = lazyGridState::scrollToItem,
            )
        }
    }
}
