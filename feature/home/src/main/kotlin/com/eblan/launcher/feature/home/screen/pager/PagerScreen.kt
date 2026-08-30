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

import android.content.BroadcastReceiver
import android.content.ClipDescription
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.content.pm.LauncherApps
import android.content.pm.ShortcutInfo
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.UserHandle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.mimeTypes
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.util.Consumer
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.eblan.launcher.domain.model.AppDrawerSettings
import com.eblan.launcher.domain.model.Associate
import com.eblan.launcher.domain.model.EblanAppWidgetProviderInfo
import com.eblan.launcher.domain.model.EblanApplicationInfo
import com.eblan.launcher.domain.model.EblanApplicationInfoGroup
import com.eblan.launcher.domain.model.EblanApplicationInfoTag
import com.eblan.launcher.domain.model.EblanShortcutConfig
import com.eblan.launcher.domain.model.EblanShortcutInfo
import com.eblan.launcher.domain.model.EblanShortcutInfoByGroup
import com.eblan.launcher.domain.model.EblanUser
import com.eblan.launcher.domain.model.ExperimentalSettings
import com.eblan.launcher.domain.model.FolderPopup
import com.eblan.launcher.domain.model.FolderPopupEntry
import com.eblan.launcher.domain.model.GestureSettings
import com.eblan.launcher.domain.model.GetEblanApplicationInfosByLabelAndTag
import com.eblan.launcher.domain.model.GridItem
import com.eblan.launcher.domain.model.HomeSettings
import com.eblan.launcher.domain.model.ManagedProfileResult
import com.eblan.launcher.domain.model.MoveGridItemResult
import com.eblan.launcher.domain.model.PinItemRequestType
import com.eblan.launcher.domain.model.PreviewFolder
import com.eblan.launcher.domain.model.TextColor
import com.eblan.launcher.feature.home.component.GridLayout
import com.eblan.launcher.feature.home.component.GridPagerIndicator
import com.eblan.launcher.feature.home.component.HomeHandler
import com.eblan.launcher.feature.home.model.Drag
import com.eblan.launcher.feature.home.model.GridItemSource
import com.eblan.launcher.feature.home.model.SharedElementKey
import com.eblan.launcher.feature.home.screen.application.ApplicationScreen
import com.eblan.launcher.feature.home.screen.folder.FolderGridItemPopup
import com.eblan.launcher.feature.home.screen.folder.FolderScreen
import com.eblan.launcher.feature.home.screen.resize.ResizeScreen
import com.eblan.launcher.feature.home.screen.shortcutconfig.ShortcutConfigScreen
import com.eblan.launcher.feature.home.screen.widget.AppWidgetScreen
import com.eblan.launcher.feature.home.screen.widget.WidgetScreen
import com.eblan.launcher.feature.home.util.PAGE_INDICATOR_HEIGHT
import com.eblan.launcher.feature.home.util.calculatePage
import com.eblan.launcher.feature.home.util.getTextColor
import com.eblan.launcher.framework.usermanager.AndroidUserManagerWrapper
import com.eblan.launcher.ui.local.LocalAppWidgetHost
import com.eblan.launcher.ui.local.LocalAppWidgetManager
import com.eblan.launcher.ui.local.LocalFileManager
import com.eblan.launcher.ui.local.LocalIconKeyGenerator
import com.eblan.launcher.ui.local.LocalImageSerializer
import com.eblan.launcher.ui.local.LocalLauncherApps
import com.eblan.launcher.ui.local.LocalPinItemRequest
import com.eblan.launcher.ui.local.LocalUserManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalLayoutApi::class)
@Composable
internal fun PagerScreen(
    modifier: Modifier = Modifier,
    appDrawerSettings: AppDrawerSettings,
    configureResultCode: Int?,
    dockGridItemsByPage: Map<Int, List<GridItem>>,
    eblanAppWidgetProviderInfos: Map<EblanApplicationInfoGroup, List<EblanAppWidgetProviderInfo>>,
    eblanAppWidgetProviderInfosGroup: Map<String, List<EblanAppWidgetProviderInfo>>,
    eblanApplicationInfoTags: List<EblanApplicationInfoTag>,
    eblanShortcutConfigs: Map<EblanUser, Map<EblanApplicationInfoGroup, List<EblanShortcutConfig>>>,
    eblanShortcutInfosGroup: Map<EblanShortcutInfoByGroup, List<EblanShortcutInfo>>,
    experimentalSettings: ExperimentalSettings,
    folderPopups: List<FolderPopup>,
    gestureSettings: GestureSettings,
    getEblanApplicationInfosByLabelAndTag: GetEblanApplicationInfosByLabelAndTag,
    gridItems: List<GridItem>,
    gridItemsByPage: Map<Int, List<GridItem>>,
    hasShortcutHostPermission: Boolean,
    hasSystemFeatureAppWidgets: Boolean,
    homeSettings: HomeSettings,
    moveGridItemResult: MoveGridItemResult?,
    paddingValues: PaddingValues,
    pinGridItem: GridItem?,
    screenHeight: Int,
    screenWidth: Int,
    textColor: TextColor,
    resizeGridItem: GridItem?,
    gridItemSource: GridItemSource?,
    isVisibleOverlay: Boolean,
    previewFolderGridItems: Map<String, PreviewFolder>,
    statusBarNotifications: Map<String, Int>,
    iconPackInfoFilePaths: Map<String, String?>,
    onDeleteGridItem: (GridItem) -> Unit,
    onResetGridAfterDeleteGridItem: (GridItem) -> Unit,
    onUpdateGridItemsAfterMove: (MoveGridItemResult) -> Unit,
    onDragEndAfterMoveFolder: () -> Unit,
    onEditApplicationInfo: (
        serialNumber: Long,
        componentName: String,
    ) -> Unit,
    onEditGridItem: (String) -> Unit,
    onEditPage: (
        gridItems: List<GridItem>,
        associate: Associate,
    ) -> Unit,
    onGetEblanAppWidgetProviderInfosByLabel: (String) -> Unit,
    onGetEblanApplicationInfosByLabel: (String) -> Unit,
    onGetEblanApplicationInfosByTagId: (Long?) -> Unit,
    onGetEblanShortcutConfigsByLabel: (String) -> Unit,
    onGetPinGridItem: (PinItemRequestType) -> Unit,
    onMoveFolderGridItem: (
        folderPopup: FolderPopup,
        movingGridItem: GridItem,
        dragX: Int,
        dragY: Int,
        gridWidth: Int,
        gridHeight: Int,
        currentPage: Int,
    ) -> Unit,
    onMoveFolderGridItemOutsideFolder: (GridItem) -> Unit,
    onMoveGridItem: (
        movingGridItem: GridItem,
        x: Int,
        y: Int,
        columns: Int,
        rows: Int,
        gridWidth: Int,
        gridHeight: Int,
    ) -> Unit,
    onResetConfigureResultCode: () -> Unit,
    onResetPinGridItem: () -> Unit,
    onResizeCancel: () -> Unit,
    onResizeEnd: () -> Unit,
    onResizeGridItem: (
        gridItem: GridItem,
        columns: Int,
        rows: Int,
    ) -> Unit,
    onSettings: () -> Unit,
    onStartSyncData: () -> Unit,
    onStopSyncData: () -> Unit,
    onUpdateAppDrawerSettings: (AppDrawerSettings) -> Unit,
    onUpdateEblanApplicationInfos: (List<EblanApplicationInfo>) -> Unit,
    onUpsertFolderPopupEntry: (FolderPopupEntry) -> Unit,
    onDeleteFolderPopupEntry: (FolderPopupEntry) -> Unit,
    onShowFolderWhenDragging: (
        folderPopupEntry: FolderPopupEntry,
        movingGridItem: GridItem,
    ) -> Unit,
    onUpdateShortcutConfigIntoShortcutInfoGridItem: (
        moveGridItemResult: MoveGridItemResult,
        pinItemRequestType: PinItemRequestType.ShortcutInfo,
    ) -> Unit,
    onUpdateGridItemSource: (GridItemSource) -> Unit,
    onUpdateIsVisibleOverlay: (Boolean) -> Unit,
    onUpdateMoveGridItemResult: (MoveGridItemResult) -> Unit,
    onUpdateResizeGridItem: (GridItem) -> Unit,
    onResetGrid: () -> Unit,
    onPackageRemoved: (
        serialNumber: Long,
        packageName: String,
    ) -> Unit,
    onPackageAdded: (
        serialNumber: Long,
        packageName: String,
    ) -> Unit,
    onPackageChanged: (
        serialNumber: Long,
        packageName: String,
    ) -> Unit,
    onShortcutsChanged: (
        serialNumber: Long,
        packageName: String,
    ) -> Unit,
    onResetFolderPopupEntries: () -> Unit,
) {
    val layoutDirection = LocalLayoutDirection.current

    val androidLauncherAppsWrapper = LocalLauncherApps.current

    val view = LocalView.current

    val activity = LocalActivity.current as ComponentActivity

    val density = LocalDensity.current

    val context = LocalContext.current

    val androidUserManagerWrapper = LocalUserManager.current

    val androidImageSerializer = LocalImageSerializer.current

    val androidAppWidgetManagerWrapper = LocalAppWidgetManager.current

    val fileManager = LocalFileManager.current

    val androidAppWidgetHostWrapper = LocalAppWidgetHost.current

    val iconKeyGenerator = LocalIconKeyGenerator.current

    val scope = rememberCoroutineScope()

    val pagerScreenState = rememberPagerScreenState(
        gestureSettings = gestureSettings,
        homeSettings = homeSettings,
        screenHeight = screenHeight,
        experimentalSettings = experimentalSettings,
        onGetPinGridItem = onGetPinGridItem,
        onResetPinGridItem = onResetPinGridItem,
    )

    val dockHeight = homeSettings.dockHeight.dp

    val dockHeightPx = with(density) {
        dockHeight.roundToPx()
    }

    val leftPadding = with(density) {
        paddingValues.calculateLeftPadding(layoutDirection).roundToPx()
    }

    val rightPadding = with(density) {
        paddingValues.calculateRightPadding(layoutDirection).roundToPx()
    }

    val topPadding = with(density) {
        paddingValues.calculateTopPadding().roundToPx()
    }

    val bottomPadding = with(density) {
        paddingValues.calculateBottomPadding().roundToPx()
    }

    val horizontalPadding = leftPadding + rightPadding

    val verticalPadding = topPadding + bottomPadding

    val safeDrawingWidth = screenWidth - horizontalPadding

    val safeDrawingHeight = screenHeight - verticalPadding

    val dockTopLeft = safeDrawingHeight - dockHeightPx

    val pageIndicatorHeightPx = with(density) {
        PAGE_INDICATOR_HEIGHT.roundToPx()
    }

    val gridHeight = safeDrawingHeight - pageIndicatorHeightPx - dockHeightPx

    val currentGridItemSource = rememberUpdatedState(gridItemSource)
    val currentIsVisibleOverlay = rememberUpdatedState(isVisibleOverlay)
    val currentMoveGridItemResult = rememberUpdatedState(moveGridItemResult)
    val currentFolderPopups = rememberUpdatedState(folderPopups)

    val appWidgetLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) {
        handleAppWidgetLauncherResult(
            androidAppWidgetManagerWrapper = androidAppWidgetManagerWrapper,
            moveGridItemResult = currentMoveGridItemResult,
            result = it,
            columns = homeSettings.columns,
            density = density,
            rows = homeSettings.rows,
            screenWidth = screenWidth,
            screenHeight = screenHeight,
            paddingValues = paddingValues,
            layoutDirection = layoutDirection,
            dockHeight = homeSettings.dockHeight,
            lastAppWidgetId = pagerScreenState.lastAppWidgetId,
            onUpdateWidgetGridItem = pagerScreenState::updateWidgetGridItem,
            onResetGridAfterDeleteGridItem = onResetGridAfterDeleteGridItem,
            onUpdateLastAppWidgetId = pagerScreenState::updateLastAppWidgetId,
        )
    }

    val shortcutConfigLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) {
        scope.launch {
            handleShortcutConfigLauncherResult(
                androidImageSerializer = androidImageSerializer,
                moveGridItemResult = currentMoveGridItemResult,
                result = it,
                fileManager = fileManager,
                onDeleteGridItem = onResetGridAfterDeleteGridItem,
                onUpdateGridItemsAfterMove = onUpdateGridItemsAfterMove,
                onResetGrid = onResetGrid,
            )
        }
    }

    val shortcutConfigIntentSenderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult(),
    ) {
        scope.launch {
            handleShortcutConfigIntentSenderLauncherResult(
                androidImageSerializer = androidImageSerializer,
                androidLauncherAppsWrapper = androidLauncherAppsWrapper,
                androidUserManagerWrapper = androidUserManagerWrapper,
                fileManager = fileManager,
                moveGridItemResult = currentMoveGridItemResult,
                result = it,
                iconKeyGenerator = iconKeyGenerator,
                onDeleteGridItem = onResetGridAfterDeleteGridItem,
                onUpdateShortcutConfigIntoShortcutInfoGridItem = onUpdateShortcutConfigIntoShortcutInfoGridItem,
            )
        }
    }

    val gridHorizontalPagerState = rememberPagerState(
        initialPage = if (homeSettings.infiniteScroll) {
            (Int.MAX_VALUE / 2) + homeSettings.initialPage
        } else {
            homeSettings.initialPage
        },
        pageCount = {
            if (homeSettings.infiniteScroll) {
                Int.MAX_VALUE
            } else {
                homeSettings.pageCount
            }
        },
    )

    val dockGridHorizontalPagerState = rememberPagerState(
        initialPage = if (homeSettings.dockInfiniteScroll) {
            (Int.MAX_VALUE / 2) + homeSettings.dockInitialPage
        } else {
            homeSettings.dockInitialPage
        },
        pageCount = {
            if (homeSettings.dockInfiniteScroll) {
                Int.MAX_VALUE
            } else {
                homeSettings.dockPageCount
            }
        },
    )

    val gridCurrentPage by remember(
        key1 = gridHorizontalPagerState,
        key2 = homeSettings,
    ) {
        derivedStateOf {
            calculatePage(
                index = gridHorizontalPagerState.currentPage,
                infiniteScroll = homeSettings.infiniteScroll,
                pageCount = homeSettings.pageCount,
            )
        }
    }

    val dockGridCurrentPage by remember(
        key1 = dockGridHorizontalPagerState,
        key2 = homeSettings,
    ) {
        derivedStateOf {
            calculatePage(
                index = dockGridHorizontalPagerState.currentPage,
                infiniteScroll = homeSettings.dockInfiniteScroll,
                pageCount = homeSettings.dockPageCount,
            )
        }
    }

    val managedProfileResult by rememberManagedProfileResult()

    val isVisibleGridItemPopup = gridItemSource != null &&
        pagerScreenState.showGridItemPopup &&
        pagerScreenState.popupIntOffset != null &&
        pagerScreenState.popupIntSize != null &&
        moveGridItemResult != null

    val isVisibleSettingsPopup =
        pagerScreenState.showSettingsPopup && pagerScreenState.settingsPopupIntOffset != null

    val isVisibleFolder = pagerScreenState.isVisibleFolder && folderPopups.isNotEmpty()

    val isVisibleFolderGridItemPopup = pagerScreenState.showFolderGridItemPopup &&
        pagerScreenState.popupIntOffset != null &&
        pagerScreenState.popupIntSize != null &&
        moveGridItemResult != null

    val isResizing = pagerScreenState.isResizing && resizeGridItem != null

    val shouldLockScreenOrientation = homeSettings.lockScreenOrientation ||
        isVisibleGridItemPopup ||
        isVisibleSettingsPopup ||
        isVisibleFolder ||
        isVisibleFolderGridItemPopup ||
        isResizing ||
        pagerScreenState.showApplicationScreen ||
        pagerScreenState.showWidgetScreen ||
        pagerScreenState.showShortcutConfigScreen ||
        pagerScreenState.eblanApplicationInfoGroup != null

    LaunchedEffect(
        key1 = pinGridItem,
        key2 = pagerScreenState,
    ) {
        pagerScreenState.handlePinGridItemEffect(
            pinGridItem = pinGridItem,
            onUpdateGridItemSource = onUpdateGridItemSource,
            onUpdateIsVisibleOverlay = onUpdateIsVisibleOverlay,
            onUpdateMoveGridItemResult = onUpdateMoveGridItemResult,
        )
    }

    SyncDataEffect(
        syncData = experimentalSettings.syncData,
        onStartSyncData = onStartSyncData,
        onStopSyncData = onStopSyncData,
        onPackageRemoved = onPackageRemoved,
        onPackageAdded = onPackageAdded,
        onPackageChanged = onPackageChanged,
        onShortcutsChanged = onShortcutsChanged,
    )

    LaunchedEffect(
        key1 = pagerScreenState.dragIntOffset,
        key2 = pagerScreenState,
        key3 = experimentalSettings,
    ) {
        handleDragGridItem(
            columns = homeSettings.columns,
            gridCurrentPage = gridCurrentPage,
            dockGridCurrentPage = dockGridCurrentPage,
            density = density,
            dockColumns = homeSettings.dockColumns,
            dockHeight = homeSettings.dockHeight,
            dockRows = homeSettings.dockRows,
            drag = pagerScreenState.drag,
            dragIntOffset = pagerScreenState.dragIntOffset,
            gridItemSource = currentGridItemSource,
            isDragging = pagerScreenState.isDragging,
            isVisibleOverlay = currentIsVisibleOverlay,
            isGridScrollInProgress = gridHorizontalPagerState.isScrollInProgress,
            isDockScrollInProgress = dockGridHorizontalPagerState.isScrollInProgress,
            lockMovement = experimentalSettings.lockMovement,
            paddingValues = paddingValues,
            rows = homeSettings.rows,
            screenHeight = screenHeight,
            screenWidth = screenWidth,
            moveGridItemResult = currentMoveGridItemResult,
            layoutDirection = layoutDirection,
            onMoveGridItem = onMoveGridItem,
            onUpdateAssociate = pagerScreenState::updateAssociate,
            onUpdateSharedElementKey = pagerScreenState::updateSharedElementKey,
        )
    }

    LaunchedEffect(
        key1 = pagerScreenState.drag,
        key2 = pagerScreenState,
        key3 = experimentalSettings,
    ) {
        handleDropGridItem(
            androidAppWidgetHostWrapper = androidAppWidgetHostWrapper,
            androidAppWidgetManagerWrapper = androidAppWidgetManagerWrapper,
            androidLauncherAppsWrapper = androidLauncherAppsWrapper,
            androidUserManagerWrapper = androidUserManagerWrapper,
            context = context,
            drag = pagerScreenState.drag,
            gridItemSource = currentGridItemSource,
            isDragging = pagerScreenState.isDragging,
            isVisibleOverlay = currentIsVisibleOverlay,
            moveGridItemResult = currentMoveGridItemResult,
            lockMovement = experimentalSettings.lockMovement,
            columns = homeSettings.columns,
            density = density,
            rows = homeSettings.rows,
            paddingValues = paddingValues,
            screenHeight = screenHeight,
            screenWidth = screenWidth,
            dockHeight = homeSettings.dockHeight,
            layoutDirection = layoutDirection,
            onResetGridAfterDeleteGridItem = onResetGridAfterDeleteGridItem,
            onResetGrid = onResetGrid,
            onUpdateGridItemsAfterMove = onUpdateGridItemsAfterMove,
            onLaunchShortcutConfigIntent = shortcutConfigLauncher::launch,
            onLaunchShortcutConfigIntentSenderRequest = shortcutConfigIntentSenderLauncher::launch,
            onLaunchWidgetIntent = appWidgetLauncher::launch,
            onUpdateAppWidgetId = pagerScreenState::updateLastAppWidgetId,
            onUpdateIsDragging = pagerScreenState::updateIsDragging,
            onUpdateWidgetGridItem = pagerScreenState::updateWidgetGridItem,
            onUpdateIsVisibleOverlay = onUpdateIsVisibleOverlay,
        )
    }

    LaunchedEffect(key1 = pagerScreenState.widgetGridItem) {
        handleBoundWidgetEffect(
            activity = activity,
            androidAppWidgetHostWrapper = androidAppWidgetHostWrapper,
            gridItemSource = currentGridItemSource,
            moveGridItemResult = currentMoveGridItemResult,
            widgetGridItem = pagerScreenState.widgetGridItem,
            onDeleteGridItem = onResetGridAfterDeleteGridItem,
            onUpdateGridItemsAfterMove = onUpdateGridItemsAfterMove,
            onResetGrid = onResetGrid,
        )
    }

    LaunchedEffect(key1 = gridHorizontalPagerState) {
        pagerScreenState.handleWallpaperScrollEffect(
            horizontalPagerState = gridHorizontalPagerState,
            windowToken = view.windowToken,
        )
    }

    LaunchedEffect(
        key1 = pagerScreenState.dragIntOffset,
        key2 = pagerScreenState,
    ) {
        handleAnimateScrollToPage(
            associate = pagerScreenState.associate,
            density = density,
            dragIntOffset = pagerScreenState.dragIntOffset,
            gridItemSource = currentGridItemSource,
            isDragging = pagerScreenState.isDragging,
            paddingValues = paddingValues,
            screenWidth = screenWidth,
            layoutDirection = layoutDirection,
            onUpdateDockPageDirection = pagerScreenState::updateDockPageDirection,
            onUpdateGridPageDirection = pagerScreenState::updateGridPageDirection,
        )
    }

    LaunchedEffect(
        key1 = configureResultCode,
        key2 = pagerScreenState.widgetGridItem,
    ) {
        handleConfigureLauncherResultEffect(
            moveGridItemResult = currentMoveGridItemResult,
            resultCode = configureResultCode,
            widgetGridItem = pagerScreenState.widgetGridItem,
            onDeleteGridItem = onDeleteGridItem,
            onUpdateGridItemsAfterMove = onUpdateGridItemsAfterMove,
            onResetConfigureResultCode = onResetConfigureResultCode,
            onResetGrid = onResetGrid,
        )
    }

    LaunchedEffect(key1 = pagerScreenState.gridPageDirection) {
        handlePageDirection(
            folderPopups = currentFolderPopups,
            pageDirection = pagerScreenState.gridPageDirection,
            currentPage = gridHorizontalPagerState.currentPage,
            onAnimateScrollToPage = gridHorizontalPagerState::animateScrollToPage,
        )
    }

    LaunchedEffect(key1 = pagerScreenState.dockPageDirection) {
        handlePageDirection(
            folderPopups = currentFolderPopups,
            pageDirection = pagerScreenState.dockPageDirection,
            currentPage = dockGridHorizontalPagerState.currentPage,
            onAnimateScrollToPage = dockGridHorizontalPagerState::animateScrollToPage,
        )
    }

    LaunchedEffect(key1 = pagerScreenState.hasDoubleTap) {
        pagerScreenState.handleHasDoubleTap()
    }

    LaunchedEffect(key1 = homeSettings.infiniteScroll) {
        gridHorizontalPagerState.scrollToPage(
            if (homeSettings.infiniteScroll) {
                (Int.MAX_VALUE / 2) + homeSettings.initialPage
            } else {
                homeSettings.initialPage
            },
        )
    }

    LaunchedEffect(key1 = homeSettings.dockInfiniteScroll) {
        dockGridHorizontalPagerState.scrollToPage(
            if (homeSettings.dockInfiniteScroll) {
                (Int.MAX_VALUE / 2) + homeSettings.dockInitialPage
            } else {
                homeSettings.dockInitialPage
            },
        )
    }

    DisposableEffect(
        key1 = activity,
        key2 = pagerScreenState,
    ) {
        val listener = Consumer<Intent> {
            pagerScreenState.handleEblanActionIntent(intent = it)
        }

        activity.addOnNewIntentListener(listener)

        onDispose {
            activity.removeOnNewIntentListener(listener)
        }
    }

    DisposableEffect(key1 = shouldLockScreenOrientation) {
        activity.requestedOrientation =
            if (shouldLockScreenOrientation) {
                ActivityInfo.SCREEN_ORIENTATION_LOCKED
            } else {
                ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }

        onDispose {
            activity.requestedOrientation =
                ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    BackHandler(enabled = pagerScreenState.isAvailableSystemNavigation) {
        pagerScreenState.handleSystemNavigation(
            dockGridHorizontalPagerState = dockGridHorizontalPagerState,
            gridHorizontalPagerState = gridHorizontalPagerState,
            windowToken = view.windowToken,
        )
    }

    HomeHandler(enabled = pagerScreenState.isAvailableSystemNavigation) {
        pagerScreenState.handleSystemNavigation(
            dockGridHorizontalPagerState = dockGridHorizontalPagerState,
            gridHorizontalPagerState = gridHorizontalPagerState,
            windowToken = view.windowToken,
        )
    }

    SharedTransitionLayout(
        modifier = modifier
            .pointerInput(key1 = pagerScreenState) {
                detectDragGesturesAfterLongPress(
                    onDragStart = pagerScreenState::dragStart,
                    onDragEnd = {
                        pagerScreenState.updateDrag(Drag.End)
                    },
                    onDragCancel = {
                        pagerScreenState.updateDrag(Drag.Cancel)
                    },
                    onDrag = { _, dragAmount ->
                        pagerScreenState.drag(dragAmount = dragAmount)
                    },
                )
            }
            .dragAndDropTarget(
                shouldStartDragAndDrop = {
                    it.mimeTypes().contains(ClipDescription.MIMETYPE_TEXT_PLAIN)
                },
                target = pagerScreenState.target,
            )
            .fillMaxSize(),
    ) {
        Column(
            modifier = Modifier
                .pointerInput(
                    key1 = isVisibleOverlay,
                    key2 = pagerScreenState,
                ) {
                    detectVerticalDragGestures(
                        onDragStart = {
                            pagerScreenState.verticalDragStart()
                        },
                        onVerticalDrag = { _, dragAmount ->
                            pagerScreenState.verticalDrag(dragAmount = dragAmount)
                        },
                        onDragEnd = pagerScreenState::verticalDragEnd,
                        onDragCancel = pagerScreenState::verticalDragCancel,
                    )
                }
                .pointerInput(key1 = pagerScreenState) {
                    detectTapGestures(
                        onDoubleTap = {
                            pagerScreenState.updateHasDoubleTap(value = true)
                        },
                        onLongPress = {
                            pagerScreenState.showSettingsPopup(offset = it)
                        },
                    )
                }
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding())
                .alpha(pagerScreenState.pagerScreenAlpha),
        ) {
            HorizontalPager(
                state = gridHorizontalPagerState,
                modifier = Modifier.weight(1f),
                userScrollEnabled = !isVisibleOverlay,
            ) { index ->
                val page = calculatePage(
                    index = index,
                    infiniteScroll = homeSettings.infiniteScroll,
                    pageCount = homeSettings.pageCount,
                )

                GridLayout(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            start = paddingValues.calculateStartPadding(layoutDirection),
                            end = paddingValues.calculateEndPadding(layoutDirection),
                        ),
                    columns = homeSettings.columns,
                    gridItems = gridItemsByPage[page],
                    rows = homeSettings.rows,
                    animate = isVisibleOverlay || isResizing,
                    content = {
                        InteractiveGridItem(
                            sharedTransitionScope = this@SharedTransitionLayout,
                            drag = pagerScreenState.drag,
                            gridItem = it,
                            gridItemSettings = homeSettings.gridItemSettings,
                            hasShortcutHostPermission = hasShortcutHostPermission,
                            isScrollInProgress = gridHorizontalPagerState.isScrollInProgress,
                            statusBarNotifications = statusBarNotifications,
                            textColor = textColor,
                            isVisibleOverlay = isVisibleOverlay,
                            isVisibleFolder = folderPopups.isNotEmpty(),
                            moveGridItemResult = moveGridItemResult,
                            lockMovement = experimentalSettings.lockMovement,
                            isDragging = pagerScreenState.isDragging,
                            showGridItemPopup = pagerScreenState.showGridItemPopup,
                            previewFolderGridItems = previewFolderGridItems,
                            cellWidth = safeDrawingWidth / homeSettings.columns,
                            cellHeight = gridHeight / homeSettings.rows,
                            leftPadding = leftPadding,
                            topOffset = topPadding,
                            sharedElementKey = SharedElementKey(
                                id = it.id,
                                parent = SharedElementKey.Parent.Grid,
                            ),
                            iconPackInfoFilePaths = iconPackInfoFilePaths,
                            onOpenAppDrawer = pagerScreenState::openApplicationScreen,
                            onUpsertFolderPopupEntry = onUpsertFolderPopupEntry,
                            onUpdateGridItemSource = onUpdateGridItemSource,
                            onUpdateImageBitmap = pagerScreenState::updateOverlayImageBitmap,
                            onUpdateIsDragging = pagerScreenState::updateIsDragging,
                            onUpdateOverlayBounds = pagerScreenState::updateOverlayBounds,
                            onUpdateSharedElementKey = pagerScreenState::updateSharedElementKey,
                            onShowGridItemPopup = pagerScreenState::showGridItemPopup,
                            onUpdateIsCloseGridItemPopup = pagerScreenState::updateIsCloseGridItemPopup,
                            onUpdateIsVisibleOverlay = onUpdateIsVisibleOverlay,
                            onUpdateMoveGridItemResult = onUpdateMoveGridItemResult,
                            onShowFolderWhenDragging = onShowFolderWhenDragging,
                            onResetGrid = onResetGrid,
                            onUpdateIsVisibleFolder = pagerScreenState::updateIsVisibleFolder,
                        )
                    },
                )
            }

            GridPagerIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(PAGE_INDICATOR_HEIGHT),
                color = getTextColor(
                    customTextColor = homeSettings.gridItemSettings.customTextColor,
                    textColor = textColor,
                ),
                gridHorizontalPagerState = gridHorizontalPagerState,
                infiniteScroll = homeSettings.infiniteScroll,
                pageCount = homeSettings.pageCount,
                swipeUp = gestureSettings.swipeUp,
                swipeDown = gestureSettings.swipeDown,
                showPageIndicator = homeSettings.showPageIndicator,
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(dockHeight + paddingValues.calculateBottomPadding()),
            ) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .padding(homeSettings.dockPadding.dp)
                        .background(
                            color = Color(homeSettings.dockCustomBackgroundColor),
                            shape = RoundedCornerShape(
                                topStart = homeSettings.dockTopStartCornerRadius.dp,
                                topEnd = homeSettings.dockTopEndCornerRadius.dp,
                                bottomStart = homeSettings.dockBottomStartCornerRadius.dp,
                                bottomEnd = homeSettings.dockBottomEndCornerRadius.dp,
                            ),
                        ),
                )

                HorizontalPager(
                    state = dockGridHorizontalPagerState,
                    modifier = Modifier
                        .matchParentSize()
                        .padding(
                            bottom = paddingValues.calculateBottomPadding(),
                        ),
                    userScrollEnabled = !isVisibleOverlay,
                ) { index ->
                    val page = calculatePage(
                        index = index,
                        infiniteScroll = homeSettings.dockInfiniteScroll,
                        pageCount = homeSettings.dockPageCount,
                    )

                    GridLayout(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(
                                start = paddingValues.calculateStartPadding(layoutDirection),
                                end = paddingValues.calculateEndPadding(layoutDirection),
                            ),
                        columns = homeSettings.dockColumns,
                        gridItems = dockGridItemsByPage[page],
                        rows = homeSettings.dockRows,
                        animate = isVisibleOverlay || isResizing,
                        content = {
                            InteractiveGridItem(
                                sharedTransitionScope = this@SharedTransitionLayout,
                                drag = pagerScreenState.drag,
                                gridItem = it,
                                gridItemSettings = homeSettings.gridItemSettings,
                                hasShortcutHostPermission = hasShortcutHostPermission,
                                isScrollInProgress = dockGridHorizontalPagerState.isScrollInProgress,
                                statusBarNotifications = statusBarNotifications,
                                textColor = textColor,
                                isVisibleOverlay = isVisibleOverlay,
                                isVisibleFolder = folderPopups.isNotEmpty(),
                                moveGridItemResult = moveGridItemResult,
                                lockMovement = experimentalSettings.lockMovement,
                                isDragging = pagerScreenState.isDragging,
                                showGridItemPopup = pagerScreenState.showGridItemPopup,
                                previewFolderGridItems = previewFolderGridItems,
                                cellWidth = safeDrawingWidth / homeSettings.dockColumns,
                                cellHeight = dockHeightPx / homeSettings.dockRows,
                                leftPadding = leftPadding,
                                topOffset = dockTopLeft,
                                sharedElementKey = SharedElementKey(
                                    id = it.id,
                                    parent = SharedElementKey.Parent.Dock,
                                ),
                                iconPackInfoFilePaths = iconPackInfoFilePaths,
                                onOpenAppDrawer = pagerScreenState::openApplicationScreen,
                                onUpsertFolderPopupEntry = onUpsertFolderPopupEntry,
                                onUpdateGridItemSource = onUpdateGridItemSource,
                                onUpdateImageBitmap = pagerScreenState::updateOverlayImageBitmap,
                                onUpdateIsDragging = pagerScreenState::updateIsDragging,
                                onUpdateOverlayBounds = pagerScreenState::updateOverlayBounds,
                                onUpdateSharedElementKey = pagerScreenState::updateSharedElementKey,
                                onShowGridItemPopup = pagerScreenState::showGridItemPopup,
                                onUpdateIsCloseGridItemPopup = pagerScreenState::updateIsCloseGridItemPopup,
                                onUpdateIsVisibleOverlay = onUpdateIsVisibleOverlay,
                                onUpdateMoveGridItemResult = onUpdateMoveGridItemResult,
                                onShowFolderWhenDragging = onShowFolderWhenDragging,
                                onResetGrid = onResetGrid,
                                onUpdateIsVisibleFolder = pagerScreenState::updateIsVisibleFolder,
                            )
                        },
                    )
                }
            }
        }

        if (isVisibleGridItemPopup) {
            GridItemPopup(
                eblanAppWidgetProviderInfosGroup = eblanAppWidgetProviderInfosGroup,
                eblanShortcutInfosGroup = eblanShortcutInfosGroup,
                gridItem = moveGridItemResult.movingGridItem,
                gridItemSettings = homeSettings.gridItemSettings,
                hasShortcutHostPermission = hasShortcutHostPermission,
                popupIntOffset = pagerScreenState.popupIntOffset,
                popupIntSize = pagerScreenState.popupIntSize,
                isVisibleOverlay = isVisibleOverlay,
                paddingValues = paddingValues,
                isCloseGridItemPopup = pagerScreenState.isCloseGridItemPopup,
                onDeleteGridItem = onDeleteGridItem,
                onDismissRequest = pagerScreenState::dismissGridItemPopup,
                onUpdateIsDragging = pagerScreenState::updateIsDragging,
                onEdit = onEditGridItem,
                onResize = onUpdateResizeGridItem,
                onUpdateGridItemSource = onUpdateGridItemSource,
                onUpdateImageBitmap = pagerScreenState::updateOverlayImageBitmap,
                onUpdateOverlayBounds = pagerScreenState::updateOverlayBounds,
                onUpdateSharedElementKey = pagerScreenState::updateSharedElementKey,
                onWidgets = pagerScreenState::openAppWidgetScreen,
                onUpdateIsVisibleOverlay = onUpdateIsVisibleOverlay,
                onUpdateMoveGridItemResult = onUpdateMoveGridItemResult,
                onUpdateIsResizing = pagerScreenState::updateIsResizing,
            )
        }

        if (isVisibleSettingsPopup) {
            SettingsPopup(
                gridItems = gridItems,
                hasSystemFeatureAppWidgets = hasSystemFeatureAppWidgets,
                popupSettingsIntOffset = pagerScreenState.settingsPopupIntOffset,
                onDismissRequest = pagerScreenState::dismissSettingsPopup,
                onEditPage = onEditPage,
                onSettings = onSettings,
                onShortcutConfigActivities = pagerScreenState::openShortcutConfigScreen,
                onWidgets = pagerScreenState::openWidgetScreen,
            )
        }

        if (isVisibleFolder) {
            folderPopups.forEach {
                FolderScreen(
                    sharedTransitionScope = this@SharedTransitionLayout,
                    drag = pagerScreenState.drag,
                    folderPopup = it,
                    gridItemSettings = homeSettings.gridItemSettings,
                    paddingValues = paddingValues,
                    safeDrawingHeight = safeDrawingHeight,
                    safeDrawingWidth = safeDrawingWidth,
                    statusBarNotifications = statusBarNotifications,
                    isVisibleOverlay = isVisibleOverlay,
                    hasShortcutHostPermission = hasShortcutHostPermission,
                    moveGridItemResult = moveGridItemResult,
                    homeSettings = homeSettings,
                    isDragging = pagerScreenState.isDragging,
                    dragIntOffset = pagerScreenState.dragIntOffset,
                    lockMovement = experimentalSettings.lockMovement,
                    folderCellWidth = homeSettings.folderCellWidth,
                    folderCellHeight = homeSettings.folderCellHeight,
                    screenWidth = screenWidth,
                    screenHeight = screenHeight,
                    folderPopups = folderPopups,
                    showFolderGridItemPopup = pagerScreenState.showFolderGridItemPopup,
                    previewFolderGridItems = previewFolderGridItems,
                    iconPackInfoFilePaths = iconPackInfoFilePaths,
                    onDeleteFolderPopupEntry = onDeleteFolderPopupEntry,
                    onMoveFolderGridItemOutsideFolder = onMoveFolderGridItemOutsideFolder,
                    onOpenAppDrawer = pagerScreenState::openApplicationScreen,
                    onUpdateImageBitmap = pagerScreenState::updateOverlayImageBitmap,
                    onUpdateIsDragging = pagerScreenState::updateIsDragging,
                    onUpdateOverlayBounds = pagerScreenState::updateOverlayBounds,
                    onUpdateSharedElementKey = pagerScreenState::updateSharedElementKey,
                    onShowGridItemPopup = pagerScreenState::showFolderGridItemPopup,
                    onUpdateIsCloseFolderGridItemPopup = pagerScreenState::updateIsCloseFolderGridItemPopup,
                    onUpdateIsVisibleOverlay = onUpdateIsVisibleOverlay,
                    onUpdateMoveGridItemResult = onUpdateMoveGridItemResult,
                    onUpsertFolderPopupEntry = onUpsertFolderPopupEntry,
                    onMoveFolderGridItem = onMoveFolderGridItem,
                    onDismissFolderGridItemPopup = pagerScreenState::dismissFolderGridItemPopup,
                    onResetGrid = onResetGrid,
                    onDragEndAfterMoveFolder = onDragEndAfterMoveFolder,
                    onUpdateIsVisibleFolder = pagerScreenState::updateIsVisibleFolder,
                )
            }
        }

        if (isVisibleFolderGridItemPopup) {
            FolderGridItemPopup(
                eblanAppWidgetProviderInfosGroup = eblanAppWidgetProviderInfosGroup,
                eblanShortcutInfosGroup = eblanShortcutInfosGroup,
                gridItemSettings = homeSettings.gridItemSettings,
                hasShortcutHostPermission = hasShortcutHostPermission,
                popupIntOffset = pagerScreenState.popupIntOffset,
                popupIntSize = pagerScreenState.popupIntSize,
                movingGridItem = moveGridItemResult.movingGridItem,
                isVisibleOverlay = isVisibleOverlay,
                paddingValues = paddingValues,
                isCloseFolderGridItemPopup = pagerScreenState.isCloseFolderGridItemPopup,
                onDeleteGridItem = onDeleteGridItem,
                onDismissRequest = pagerScreenState::dismissFolderGridItemPopup,
                onUpdateIsDragging = pagerScreenState::updateIsDragging,
                onEdit = onEditGridItem,
                onUpdateGridItemSource = onUpdateGridItemSource,
                onUpdateImageBitmap = pagerScreenState::updateOverlayImageBitmap,
                onUpdateIsVisibleOverlay = onUpdateIsVisibleOverlay,
                onUpdateOverlayBounds = pagerScreenState::updateOverlayBounds,
                onUpdateSharedElementKey = pagerScreenState::updateSharedElementKey,
                onWidgets = pagerScreenState::openAppWidgetScreen,
                onUpdateMoveGridItemResult = onUpdateMoveGridItemResult,
                onResetFolderPopupEntries = onResetFolderPopupEntries,
            )
        }

        if (pagerScreenState.showApplicationScreen) {
            ApplicationScreen(
                sharedTransitionScope = this@SharedTransitionLayout,
                alpha = pagerScreenState.applicationScreenAlpha,
                appDrawerSettings = appDrawerSettings,
                cornerSize = pagerScreenState.applicationScreenCornerSize,
                drag = pagerScreenState.drag,
                eblanAppWidgetProviderInfosGroup = eblanAppWidgetProviderInfosGroup,
                eblanApplicationInfoTags = eblanApplicationInfoTags,
                eblanShortcutInfosGroup = eblanShortcutInfosGroup,
                getEblanApplicationInfosByLabelAndTag = getEblanApplicationInfosByLabelAndTag,
                hasShortcutHostPermission = hasShortcutHostPermission,
                managedProfileResult = managedProfileResult,
                paddingValues = paddingValues,
                screenHeight = screenHeight,
                swipeY = pagerScreenState.applicationScreenSwipeY.value,
                isVisibleOverlay = isVisibleOverlay,
                systemTextColor = textColor,
                systemCustomTextColor = homeSettings.gridItemSettings.customTextColor,
                onDismiss = pagerScreenState::dismissApplicationScreen,
                onDragEnd = pagerScreenState::handleOnDragEndApplicationScreen,
                onEditApplicationInfo = onEditApplicationInfo,
                onGetEblanApplicationInfosByLabel = onGetEblanApplicationInfosByLabel,
                onGetEblanApplicationInfosByTagId = onGetEblanApplicationInfosByTagId,
                onUpdateAppDrawerSettings = onUpdateAppDrawerSettings,
                onUpdateEblanApplicationInfos = onUpdateEblanApplicationInfos,
                onUpdateGridItemSource = onUpdateGridItemSource,
                onUpdateImageBitmap = pagerScreenState::updateOverlayImageBitmap,
                onUpdateIsDragging = pagerScreenState::updateIsDragging,
                onUpdateOverlayBounds = pagerScreenState::updateOverlayBounds,
                onUpdateSharedElementKey = pagerScreenState::updateSharedElementKey,
                onVerticalDrag = pagerScreenState::verticalDragApplicationScreen,
                onWidgets = pagerScreenState::openAppWidgetScreen,
                onUpdateIsVisibleOverlay = onUpdateIsVisibleOverlay,
                onUpdateMoveGridItemResult = onUpdateMoveGridItemResult,
            )
        }

        if (pagerScreenState.showWidgetScreen) {
            WidgetScreen(
                columns = homeSettings.columns,
                drag = pagerScreenState.drag,
                eblanAppWidgetProviderInfos = eblanAppWidgetProviderInfos,
                gridItemSettings = homeSettings.gridItemSettings,
                paddingValues = paddingValues,
                rows = homeSettings.rows,
                screenHeight = screenHeight,
                screenWidth = screenWidth,
                swipeY = pagerScreenState.widgetScreenSwipeY.value,
                alpha = pagerScreenState.widgetScreenAlpha,
                cornerSize = pagerScreenState.widgetScreenCornerSize,
                onDismiss = pagerScreenState::dismissWidgetScreen,
                onGetEblanAppWidgetProviderInfosByLabel = onGetEblanAppWidgetProviderInfosByLabel,
                onUpdateOverlayBounds = pagerScreenState::updateOverlayBounds,
                onUpdateImageBitmap = pagerScreenState::updateOverlayImageBitmap,
                onUpdateGridItemSource = onUpdateGridItemSource,
                onUpdateSharedElementKey = pagerScreenState::updateSharedElementKey,
                onUpdateIsDragging = pagerScreenState::updateIsDragging,
                onVerticalDrag = pagerScreenState::verticalDragWidgetScreen,
                onDragEnd = pagerScreenState::handleOnDragEndWidgetScreen,
                onUpdateIsVisibleOverlay = onUpdateIsVisibleOverlay,
                onUpdateMoveGridItemResult = onUpdateMoveGridItemResult,
            )
        }

        if (pagerScreenState.showShortcutConfigScreen) {
            ShortcutConfigScreen(
                drag = pagerScreenState.drag,
                eblanShortcutConfigs = eblanShortcutConfigs,
                gridItemSettings = homeSettings.gridItemSettings,
                paddingValues = paddingValues,
                screenHeight = screenHeight,
                swipeY = pagerScreenState.shortcutConfigScreenSwipeY.value,
                alpha = pagerScreenState.shortcutConfigScreenAlpha,
                cornerSize = pagerScreenState.shortcutConfigScreenCornerSize,
                onDismiss = pagerScreenState::dismissShortcutConfigScreen,
                onGetEblanShortcutConfigsByLabel = onGetEblanShortcutConfigsByLabel,
                onUpdateOverlayBounds = pagerScreenState::updateOverlayBounds,
                onUpdateImageBitmap = pagerScreenState::updateOverlayImageBitmap,
                onUpdateGridItemSource = onUpdateGridItemSource,
                onUpdateSharedElementKey = pagerScreenState::updateSharedElementKey,
                onUpdateIsDragging = pagerScreenState::updateIsDragging,
                onVerticalDrag = pagerScreenState::verticalDragShortcutConfigScreen,
                onDragEnd = pagerScreenState::handleOnDragEndShortcutConfigScreen,
                onUpdateIsVisibleOverlay = onUpdateIsVisibleOverlay,
                onUpdateMoveGridItemResult = onUpdateMoveGridItemResult,
            )
        }

        if (pagerScreenState.eblanApplicationInfoGroup != null) {
            AppWidgetScreen(
                columns = homeSettings.columns,
                drag = pagerScreenState.drag,
                eblanAppWidgetProviderInfosGroup = eblanAppWidgetProviderInfosGroup,
                eblanApplicationInfoGroup = pagerScreenState.eblanApplicationInfoGroup,
                gridItemSettings = homeSettings.gridItemSettings,
                paddingValues = paddingValues,
                rows = homeSettings.rows,
                screenHeight = screenHeight,
                screenWidth = screenWidth,
                swipeY = pagerScreenState.appWidgetScreenSwipeY.value,
                onDismiss = pagerScreenState::dismissAppWidgetScreen,
                onDismissApplicationScreen = pagerScreenState::dismissApplicationScreen,
                onUpdateOverlayBounds = pagerScreenState::updateOverlayBounds,
                onUpdateImageBitmap = pagerScreenState::updateOverlayImageBitmap,
                onUpdateGridItemSource = onUpdateGridItemSource,
                onUpdateSharedElementKey = pagerScreenState::updateSharedElementKey,
                onUpdateIsDragging = pagerScreenState::updateIsDragging,
                onVerticalDrag = pagerScreenState::verticalDragAppWidgetScreen,
                onDragEnd = pagerScreenState::handleOnDragEndAppWidgetScreen,
                onUpdateIsVisibleOverlay = onUpdateIsVisibleOverlay,
                onUpdateMoveGridItemResult = onUpdateMoveGridItemResult,
            )
        }

        if (isResizing) {
            ResizeScreen(
                homeSettings = homeSettings,
                lockMovement = experimentalSettings.lockMovement,
                resizeGridItem = resizeGridItem,
                paddingValues = paddingValues,
                textColor = textColor,
                onResizeCancel = onResizeCancel,
                onResizeEnd = onResizeEnd,
                onResizeGridItem = onResizeGridItem,
                onUpdateIsResizing = pagerScreenState::updateIsResizing,
            )
        }

        OverlayImage(
            overlayImageBitmap = pagerScreenState.overlayImageBitmap,
            overlayIntOffset = pagerScreenState.overlayIntOffset,
            overlayIntSize = pagerScreenState.overlayIntSize,
            sharedElementKey = pagerScreenState.sharedElementKey,
            isVisibleOverlay = isVisibleOverlay,
            screenWidth = screenWidth,
            scale = pagerScreenState.overlayScale,
            onResetOverlay = pagerScreenState::resetOverlay,
        )
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun SharedTransitionScope.OverlayImage(
    modifier: Modifier = Modifier,
    overlayImageBitmap: ImageBitmap?,
    overlayIntOffset: IntOffset?,
    overlayIntSize: IntSize?,
    sharedElementKey: SharedElementKey?,
    isVisibleOverlay: Boolean,
    screenWidth: Int,
    scale: Float,
    onResetOverlay: () -> Unit,
) {
    if (overlayImageBitmap == null ||
        sharedElementKey == null ||
        overlayIntOffset == null ||
        overlayIntSize == null
    ) {
        return
    }

    val density = LocalDensity.current

    val layoutDirection = LocalLayoutDirection.current

    val size = with(density) {
        DpSize(width = overlayIntSize.width.toDp(), height = overlayIntSize.height.toDp())
    }

    val scale = remember { Animatable(scale) }

    LaunchedEffect(key1 = isVisibleOverlay) {
        if (isVisibleOverlay) {
            scale.animateTo(targetValue = 1f)
        } else {
            onResetOverlay()
        }
    }

    Image(
        modifier = modifier
            .offset {
                when (layoutDirection) {
                    LayoutDirection.Ltr -> overlayIntOffset

                    LayoutDirection.Rtl -> IntOffset(
                        x = screenWidth - overlayIntSize.width - overlayIntOffset.x,
                        y = overlayIntOffset.y,
                    )
                }
            }
            .size(size)
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            }
            .sharedElementWithCallerManagedVisibility(
                rememberSharedContentState(key = sharedElementKey),
                visible = isVisibleOverlay,
            ),
        bitmap = overlayImageBitmap,
        contentDescription = null,
    )
}

@Composable
private fun SyncDataEffect(
    syncData: Boolean,
    onStartSyncData: () -> Unit,
    onStopSyncData: () -> Unit,
    onPackageRemoved: (
        serialNumber: Long,
        packageName: String,
    ) -> Unit,
    onPackageAdded: (
        serialNumber: Long,
        packageName: String,
    ) -> Unit,
    onPackageChanged: (
        serialNumber: Long,
        packageName: String,
    ) -> Unit,
    onShortcutsChanged: (
        serialNumber: Long,
        packageName: String,
    ) -> Unit,
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    val appWidgetHost = LocalAppWidgetHost.current

    val pinItemRequestWrapper = LocalPinItemRequest.current

    val launcherAppsWrapper = LocalLauncherApps.current

    val userManagerWrapper = LocalUserManager.current

    DisposableEffect(
        key1 = lifecycleOwner,
        key2 = syncData,
    ) {
        val launcherAppsCallback = getLauncherAppsCallback(
            userManagerWrapper = userManagerWrapper,
            onPackageRemoved = onPackageRemoved,
            onPackageAdded = onPackageAdded,
            onPackageChanged = onPackageChanged,
            onShortcutsChanged = onShortcutsChanged,
        )

        val lifecycleEventObserver = LifecycleEventObserver { lifecycleOwner, event ->
            lifecycleOwner.lifecycleScope.launch {
                when (event) {
                    Lifecycle.Event.ON_START -> {
                        if (syncData &&
                            pinItemRequestWrapper.getPinItemRequest() == null
                        ) {
                            launcherAppsWrapper.registerCallback(
                                callback = launcherAppsCallback,
                                handler = Handler(Looper.getMainLooper()),
                            )

                            onStartSyncData()
                        }

                        appWidgetHost.startListening()
                    }

                    Lifecycle.Event.ON_STOP -> {
                        if (syncData &&
                            pinItemRequestWrapper.getPinItemRequest() == null
                        ) {
                            launcherAppsWrapper.unregisterCallback(callback = launcherAppsCallback)

                            onStopSyncData()
                        }

                        appWidgetHost.stopListening()
                    }

                    else -> Unit
                }
            }
        }

        lifecycleOwner.lifecycle.addObserver(lifecycleEventObserver)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(lifecycleEventObserver)

            launcherAppsWrapper.unregisterCallback(callback = launcherAppsCallback)

            onStopSyncData()

            appWidgetHost.stopListening()
        }
    }
}

@Composable
private fun rememberManagedProfileResult(): State<ManagedProfileResult?> {
    val context = LocalContext.current

    val userManagerWrapper = LocalUserManager.current

    return produceState(
        initialValue = null,
        key1 = context,
        key2 = userManagerWrapper,
    ) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(
                context: Context,
                intent: Intent,
            ) {
                val userHandle =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(
                            Intent.EXTRA_USER,
                            UserHandle::class.java,
                        )
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(Intent.EXTRA_USER)
                    }

                if (userHandle != null) {
                    value = ManagedProfileResult(
                        serialNumber =
                        userManagerWrapper.getSerialNumberForUser(
                            userHandle = userHandle,
                        ),
                        isQuiteModeEnabled =
                        userManagerWrapper.isQuietModeEnabled(
                            userHandle = userHandle,
                        ),
                    )
                }
            }
        }

        ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter().apply {
                addAction(Intent.ACTION_MANAGED_PROFILE_AVAILABLE)
                addAction(Intent.ACTION_MANAGED_PROFILE_UNAVAILABLE)
                addAction(Intent.ACTION_MANAGED_PROFILE_REMOVED)
                addAction(Intent.ACTION_MANAGED_PROFILE_ADDED)
                addAction(Intent.ACTION_MANAGED_PROFILE_UNLOCKED)
            },
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )

        awaitDispose {
            context.unregisterReceiver(receiver)
        }
    }
}

private fun getLauncherAppsCallback(
    userManagerWrapper: AndroidUserManagerWrapper,
    onPackageRemoved: (
        serialNumber: Long,
        packageName: String,
    ) -> Unit,
    onPackageAdded: (
        serialNumber: Long,
        packageName: String,
    ) -> Unit,
    onPackageChanged: (
        serialNumber: Long,
        packageName: String,
    ) -> Unit,
    onShortcutsChanged: (
        serialNumber: Long,
        packageName: String,
    ) -> Unit,
): LauncherApps.Callback = object : LauncherApps.Callback() {
    override fun onPackageRemoved(packageName: String?, user: UserHandle?) {
        if (packageName != null && user != null) {
            onPackageRemoved(
                userManagerWrapper.getSerialNumberForUser(userHandle = user),
                packageName,
            )
        }
    }

    override fun onPackageAdded(packageName: String?, user: UserHandle?) {
        if (packageName != null && user != null) {
            onPackageAdded(
                userManagerWrapper.getSerialNumberForUser(userHandle = user),
                packageName,
            )
        }
    }

    override fun onPackageChanged(packageName: String?, user: UserHandle?) {
        if (packageName != null && user != null) {
            onPackageChanged(
                userManagerWrapper.getSerialNumberForUser(userHandle = user),
                packageName,
            )
        }
    }

    override fun onPackagesAvailable(
        packageNames: Array<out String>?,
        user: UserHandle?,
        replacing: Boolean,
    ) {
    }

    override fun onPackagesUnavailable(
        packageNames: Array<out String>?,
        user: UserHandle?,
        replacing: Boolean,
    ) {
    }

    override fun onShortcutsChanged(
        packageName: String,
        shortcuts: MutableList<ShortcutInfo>,
        user: UserHandle,
    ) {
        onShortcutsChanged(
            userManagerWrapper.getSerialNumberForUser(userHandle = user),
            packageName,
        )
    }
}
