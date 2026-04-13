package com.eblan.launcher.feature.home.screen.application.horizontal

import android.graphics.Rect
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SearchBarState
import androidx.compose.material3.SearchBarValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
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
import com.eblan.launcher.domain.model.GetEblanApplicationInfosByLabel
import com.eblan.launcher.domain.model.ManagedProfileResult
import com.eblan.launcher.feature.home.model.Drag
import com.eblan.launcher.feature.home.model.GridItemSource
import com.eblan.launcher.feature.home.model.SharedElementKey
import com.eblan.launcher.feature.home.screen.application.ApplicationInfoPopup
import com.eblan.launcher.feature.home.screen.application.EblanApplicationInfoItem
import com.eblan.launcher.feature.home.screen.application.EblanApplicationInfoTabRow
import com.eblan.launcher.feature.home.screen.application.QuiteModeScreen
import com.eblan.launcher.feature.home.screen.application.TagElevatedFilterChip
import com.eblan.launcher.ui.local.LocalLauncherApps
import com.eblan.launcher.ui.local.LocalPackageManager
import com.eblan.launcher.ui.local.LocalUserManager
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class, FlowPreview::class)
@Composable
internal fun SharedTransitionScope.HorizontalApplicationScreen(
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

    val appDrawerRowsHeight = with(density) {
        appDrawerSettings.appDrawerRowsHeight.dp.roundToPx()
    }

    val searchBarState = rememberSearchBarState()

    val textFieldState = rememberTextFieldState()

    var selectedEblanApplicationInfoTagId by remember { mutableStateOf<Long?>(null) }

    var selectedEblanApplicationInfo by remember { mutableStateOf<EblanApplicationInfo?>(null) }

    LaunchedEffect(key1 = textFieldState) {
        snapshotFlow { textFieldState.text }.debounce(500L).onEach { text ->
            onGetEblanApplicationInfosByLabel(text.toString())

            showPopupApplicationMenu = false
        }.collect()
    }

    LaunchedEffect(key1 = swipeY) {
        if (swipeY.roundToInt() >= screenHeight && textFieldState.text.isNotEmpty()) {
            onGetEblanApplicationInfosByLabel("")

            textFieldState.clearText()

            selectedEblanApplicationInfoTagId = null
        }

        if (swipeY.roundToInt() > 0 && showPopupApplicationMenu) {
            showPopupApplicationMenu = false
        }
    }

    LaunchedEffect(key1 = Unit) {
        snapshotFlow { selectedEblanApplicationInfoTagId }.onEach { selectedEblanApplicationInfoTag ->
            onGetEblanApplicationInfosByTagId(selectedEblanApplicationInfoTag)
        }.collect()
    }

    LaunchedEffect(key1 = isPressHome) {
        if (isPressHome) {
            showPopupApplicationMenu = false

            searchBarState.animateToCollapsed()

            onDismiss()
        }
    }

    LaunchedEffect(key1 = drag) {
        if (drag == Drag.Start && searchBarState.currentValue == SearchBarValue.Expanded) {
            searchBarState.animateToCollapsed()
        }
    }

    LaunchedEffect(key1 = horizontalPagerState.isScrollInProgress) {
        if (horizontalPagerState.isScrollInProgress && showPopupApplicationMenu) {
            showPopupApplicationMenu = false
        }
    }

    BackHandler(enabled = swipeY < screenHeight.toFloat()) {
        showPopupApplicationMenu = false

        onDismiss()
    }

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

        if (getEblanApplicationInfosByLabel.eblanApplicationInfos.keys.size > 1) {
            EblanApplicationInfoTabRow(
                currentPage = horizontalPagerState.currentPage,
                eblanApplicationInfos = getEblanApplicationInfosByLabel.eblanApplicationInfos,
                onAnimateScrollToPage = horizontalPagerState::animateScrollToPage,
            )

            HorizontalPager(
                modifier = Modifier.fillMaxSize(),
                state = horizontalPagerState,
            ) { index ->
                EblanApplicationInfosPage(
                    appDrawerSettings = appDrawerSettings,
                    currentPage = currentPage,
                    drag = drag,
                    getEblanApplicationInfosByLabel = getEblanApplicationInfosByLabel,
                    iconPackFilePaths = iconPackFilePaths,
                    index = index,
                    managedProfileResult = managedProfileResult,
                    paddingValues = paddingValues,
                    isVisibleOverlay = isVisibleOverlay,
                    onDismiss = onDismiss,
                    onDragEnd = onDragEnd,
                    onDraggingGridItem = onDraggingGridItem,
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
                    onUpdateSharedElementKey = onUpdateSharedElementKey,
                    onVerticalDrag = onVerticalDrag,
                    onUpdateEblanApplicationInfo = { eblanApplicationInfo ->
                        selectedEblanApplicationInfo = eblanApplicationInfo
                    },
                    onUpdateIsVisibleOverlay = onUpdateIsVisibleOverlay,
                )
            }
        } else {
            EblanApplicationInfosPage(
                appDrawerSettings = appDrawerSettings,
                currentPage = currentPage,
                drag = drag,
                getEblanApplicationInfosByLabel = getEblanApplicationInfosByLabel,
                iconPackFilePaths = iconPackFilePaths,
                index = 0,
                managedProfileResult = managedProfileResult,
                paddingValues = paddingValues,
                isVisibleOverlay = isVisibleOverlay,
                onDismiss = onDismiss,
                onDragEnd = onDragEnd,
                onDraggingGridItem = onDraggingGridItem,
                onUpdateGridItemSource = onUpdateGridItemSource,
                onUpdateImageBitmap = onUpdateImageBitmap,
                onUpdateIsDragging = onUpdateIsDragging,
                onUpdateOverlayBounds = { intOffset, intSize ->
                    onUpdateOverlayBounds(intOffset, intSize)

                    popupIntOffset = intOffset

                    popupIntSize = IntSize(
                        width = intSize.width,
                        height = appDrawerRowsHeight,
                    )
                },
                onUpdatePopupMenu = { newShowPopupApplicationMenu ->
                    showPopupApplicationMenu = newShowPopupApplicationMenu
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ApplicationSearchBar(
    modifier: Modifier = Modifier,
    searchBarState: SearchBarState,
    textFieldState: TextFieldState,
    swipeY: Float,
    showKeyboard: Boolean,
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    val scope = rememberCoroutineScope()

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(key1 = swipeY) {
        if (swipeY.roundToInt() == 0 && showKeyboard) {
            focusRequester.requestFocus()

            keyboardController?.show()
        }
    }

    SearchBar(
        state = searchBarState,
        modifier = modifier
            .focusRequester(focusRequester)
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

@OptIn(ExperimentalLayoutApi::class, ExperimentalSharedTransitionApi::class)
@Composable
private fun SharedTransitionScope.EblanApplicationInfosPage(
    modifier: Modifier = Modifier,
    appDrawerSettings: AppDrawerSettings,
    currentPage: Int,
    drag: Drag,
    getEblanApplicationInfosByLabel: GetEblanApplicationInfosByLabel,
    iconPackFilePaths: Map<String, String>,
    index: Int,
    managedProfileResult: ManagedProfileResult?,
    paddingValues: PaddingValues,
    isVisibleOverlay: Boolean,
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
        } else {
            EblanApplicationInfos(
                appDrawerSettings = appDrawerSettings,
                currentPage = currentPage,
                drag = drag,
                eblanUserPageKey = eblanUserPageKey,
                getEblanApplicationInfosByLabel = getEblanApplicationInfosByLabel,
                iconPackFilePaths = iconPackFilePaths,
                paddingValues = paddingValues,
                isVisibleOverlay = isVisibleOverlay,
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
    paddingValues: PaddingValues,
    isVisibleOverlay: Boolean,
    onDismiss: () -> Unit,
    onDraggingGridItem: () -> Unit,
    onUpdateGridItemSource: (GridItemSource) -> Unit,
    onUpdateImageBitmap: (ImageBitmap) -> Unit,
    onUpdateIsDragging: (Boolean) -> Unit,
    onUpdateOverlayBounds: (
        intOffset: IntOffset,
        intSize: IntSize,
    ) -> Unit,
    onUpdatePopupMenu: (Boolean) -> Unit,
    onUpdateSharedElementKey: (SharedElementKey?) -> Unit,
    onUpdateEblanApplicationInfo: (EblanApplicationInfo) -> Unit,
    onUpdateIsVisibleOverlay: (Boolean) -> Unit,
) {
    FlowRow(
        modifier = modifier.fillMaxSize(),
        maxItemsInEachRow = appDrawerSettings.horizontalAppDrawerColumns,
        maxLines = appDrawerSettings.horizontalAppDrawerRows,
    ) {
        getEblanApplicationInfosByLabel.eblanApplicationInfos[eblanUserPageKey]?.forEach { eblanApplicationInfo ->
            key(eblanApplicationInfo.serialNumber, eblanApplicationInfo.componentName) {
                EblanApplicationInfoItem(
                    appDrawerSettings = appDrawerSettings,
                    currentPage = currentPage,
                    drag = drag,
                    eblanApplicationInfo = eblanApplicationInfo,
                    iconPackFilePaths = iconPackFilePaths,
                    paddingValues = paddingValues,
                    isVisibleOverlay = isVisibleOverlay,
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
                )
            }
        }
    }
}
