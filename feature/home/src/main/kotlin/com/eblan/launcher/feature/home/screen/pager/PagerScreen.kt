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

import android.content.ClipDescription
import android.content.Intent
import android.content.Intent.ACTION_SET_WALLPAPER
import android.content.Intent.createChooser
import android.content.Intent.parseUri
import android.graphics.Rect
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.mimeTypes
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.util.Consumer
import com.eblan.launcher.domain.model.AppDrawerSettings
import com.eblan.launcher.domain.model.Associate
import com.eblan.launcher.domain.model.EblanActionType
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
import com.eblan.launcher.domain.model.GridItemData
import com.eblan.launcher.domain.model.HomeSettings
import com.eblan.launcher.domain.model.MoveGridItemResult
import com.eblan.launcher.domain.model.PinItemRequestType
import com.eblan.launcher.domain.model.TextColor
import com.eblan.launcher.feature.home.component.GridLayout
import com.eblan.launcher.feature.home.component.GridPagerIndicator
import com.eblan.launcher.feature.home.model.Drag
import com.eblan.launcher.feature.home.model.GridItemSource
import com.eblan.launcher.feature.home.model.SharedElementKey
import com.eblan.launcher.feature.home.screen.PAGE_INDICATOR_HEIGHT
import com.eblan.launcher.feature.home.screen.application.ApplicationScreen
import com.eblan.launcher.feature.home.screen.folder.FolderGridItemPopup
import com.eblan.launcher.feature.home.screen.folder.FolderScreen
import com.eblan.launcher.feature.home.screen.resize.ResizeScreen
import com.eblan.launcher.feature.home.screen.shortcutconfig.ShortcutConfigScreen
import com.eblan.launcher.feature.home.screen.widget.AppWidgetScreen
import com.eblan.launcher.feature.home.screen.widget.WidgetScreen
import com.eblan.launcher.feature.home.util.calculatePage
import com.eblan.launcher.feature.home.util.getSystemTextColor
import com.eblan.launcher.ui.local.LocalAppWidgetHost
import com.eblan.launcher.ui.local.LocalFileManager
import com.eblan.launcher.ui.local.LocalIconKeyGenerator
import com.eblan.launcher.ui.local.LocalImageSerializer
import com.eblan.launcher.ui.local.LocalLauncherApps
import com.eblan.launcher.ui.local.LocalUserManager
import com.eblan.launcher.ui.local.LocalWallpaperManager
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
    lockMovement: Boolean,
    moveGridItemResult: MoveGridItemResult?,
    paddingValues: PaddingValues,
    pinGridItem: GridItem?,
    screenHeight: Int,
    screenWidth: Int,
    textColor: TextColor,
    resizeGridItem: GridItem?,
    gridItemSource: GridItemSource?,
    isVisibleOverlay: Boolean,
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
        conflictingGridItem: GridItem,
        movingGridItem: GridItem,
        data: GridItemData.Folder,
        dragX: Int,
        dragY: Int,
        columns: Int,
        rows: Int,
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
) {
    val context = LocalContext.current

    val layoutDirection = LocalLayoutDirection.current

    val androidLauncherAppsWrapper = LocalLauncherApps.current

    val androidWallpaperManagerWrapper = LocalWallpaperManager.current

    val view = LocalView.current

    val activity = LocalActivity.current as ComponentActivity

    val density = LocalDensity.current

    val androidUserManagerWrapper = LocalUserManager.current

    val androidImageSerializer = LocalImageSerializer.current

    val fileManager = LocalFileManager.current

    val androidAppWidgetHostWrapper = LocalAppWidgetHost.current

    val iconKeyGenerator = LocalIconKeyGenerator.current

    val scope = rememberCoroutineScope()

    val pagerScreenState = rememberPagerScreenState(
        gestureSettings = gestureSettings,
        homeSettings = homeSettings,
        screenHeight = screenHeight,
        screenWidth = screenWidth,
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

    val paddingValues = WindowInsets.safeDrawing.asPaddingValues()

    val appWidgetLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        pagerScreenState.handleAppWidgetLauncherResult(
            moveGridItemResult = moveGridItemResult,
            result = result,
        )
    }

    val shortcutConfigLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        scope.launch {
            handleShortcutConfigLauncherResult(
                androidImageSerializer = androidImageSerializer,
                moveGridItemResult = moveGridItemResult,
                result = result,
                fileManager = fileManager,
                onDeleteGridItem = onResetGridAfterDeleteGridItem,
                onUpdateGridItemsAfterMove = onUpdateGridItemsAfterMove,
                onResetGrid = onResetGrid,
            )
        }
    }

    val shortcutConfigIntentSenderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        scope.launch {
            handleShortcutConfigIntentSenderLauncherResult(
                androidImageSerializer = androidImageSerializer,
                androidLauncherAppsWrapper = androidLauncherAppsWrapper,
                androidUserManagerWrapper = androidUserManagerWrapper,
                fileManager = fileManager,
                moveGridItemResult = moveGridItemResult,
                result = result,
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

    val lastPopupFolderGridItem = folderPopups.lastOrNull()

    val currentGridItemSource = rememberUpdatedState(gridItemSource)
    val currentIsVisibleOverlay = rememberUpdatedState(isVisibleOverlay)
    val currentMoveGridItemResult = rememberUpdatedState(moveGridItemResult)
    val currentFolderPopups = rememberUpdatedState(folderPopups)

    LaunchedEffect(key1 = pinGridItem) {
        pagerScreenState.handlePinGridItemEffect(
            pinGridItem = pinGridItem,
            onUpdateGridItemSource = onUpdateGridItemSource,
            onUpdateIsVisibleOverlay = onUpdateIsVisibleOverlay,
            onUpdateMoveGridItemResult = onUpdateMoveGridItemResult,
        )
    }

    LifecycleEffect(
        syncData = experimentalSettings.syncData,
        userManagerWrapper = androidUserManagerWrapper,
        onManagedProfileResultChange = pagerScreenState::updateManagedProfileResult,
        onStartSyncData = onStartSyncData,
        onStatusBarNotificationsChange = pagerScreenState::updateStatusBarNotifications,
        onStopSyncData = onStopSyncData,
    )

    LaunchedEffect(key1 = pagerScreenState.dragIntOffset) {
        pagerScreenState.handleDragGridItemEffect(
            gridCurrentPage = gridCurrentPage,
            dockGridCurrentPage = dockGridCurrentPage,
            density = density,
            dockHeight = dockHeight,
            isGridScrollInProgress = gridHorizontalPagerState.isScrollInProgress,
            isDockScrollInProgress = dockGridHorizontalPagerState.isScrollInProgress,
            lockMovement = lockMovement,
            paddingValues = paddingValues,
            gridItemSource = currentGridItemSource,
            isVisibleOverlay = currentIsVisibleOverlay,
            moveGridItemResult = currentMoveGridItemResult,
            layoutDirection = layoutDirection,
            onMoveGridItem = onMoveGridItem,
        )
    }

    LaunchedEffect(key1 = pagerScreenState.drag) {
        pagerScreenState.handleDropGridItemEffect(
            moveGridItemResult = currentMoveGridItemResult,
            onLaunchShortcutConfigIntent = shortcutConfigLauncher::launch,
            onLaunchShortcutConfigIntentSenderRequest = shortcutConfigIntentSenderLauncher::launch,
            onLaunchWidgetIntent = appWidgetLauncher::launch,
            gridItemSource = currentGridItemSource,
            isVisibleOverlay = currentIsVisibleOverlay,
            onUpdateIsVisibleOverlay = onUpdateIsVisibleOverlay,
            onResetGridAfterDeleteGridItem = onResetGridAfterDeleteGridItem,
            onResetGrid = onResetGrid,
            onUpdateGridItemsAfterMove = onUpdateGridItemsAfterMove,
        )
    }

    LaunchedEffect(key1 = pagerScreenState.deleteAppWidgetId) {
        pagerScreenState.handleDeleteAppWidgetIdEffect(
            moveGridItemResult = moveGridItemResult,
            onResetGridAfterDeleteGridItem = onResetGridAfterDeleteGridItem,
        )
    }

    LaunchedEffect(key1 = pagerScreenState.updatedWidgetGridItem) {
        handleBoundWidgetEffect(
            activity = activity,
            androidAppWidgetHostWrapper = androidAppWidgetHostWrapper,
            gridItemSource = gridItemSource,
            moveGridItemResult = moveGridItemResult,
            updatedWidgetGridItem = pagerScreenState.updatedWidgetGridItem,
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

    LaunchedEffect(key1 = pagerScreenState.dragIntOffset) {
        pagerScreenState.handleAnimateScrollToPageEffect(
            density = density,
            paddingValues = paddingValues,
            gridItemSource = currentGridItemSource,
            layoutDirection = layoutDirection,
        )
    }

    LaunchedEffect(key1 = configureResultCode) {
        handleConfigureLauncherResultEffect(
            moveGridItemResult = moveGridItemResult,
            resultCode = configureResultCode,
            updatedGridItem = pagerScreenState.updatedWidgetGridItem,
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

    DisposableEffect(key1 = activity) {
        val listener = Consumer<Intent> { intent ->
            pagerScreenState.handleNewIntent(
                dockGridHorizontalPagerState = dockGridHorizontalPagerState,
                gridHorizontalPagerState = gridHorizontalPagerState,
                intent = intent,
                windowToken = view.windowToken,
            )
        }

        activity.addOnNewIntentListener(listener)

        onDispose {
            activity.removeOnNewIntentListener(listener)
        }
    }

    LaunchedEffect(key1 = pagerScreenState.isPressHome) {
        pagerScreenState.handleIsPressHome()
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

    BackHandler(
        enabled = pagerScreenState.swipeY.value == screenHeight.toFloat() &&
            !pagerScreenState.showGridItemPopup && !pagerScreenState.showSettingsPopup &&
            !pagerScreenState.showFolderGridItemPopup,
    ) {
        pagerScreenState.animateScrollToPages(
            dockGridHorizontalPagerState = dockGridHorizontalPagerState,
            gridHorizontalPagerState = gridHorizontalPagerState,
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
                        onVerticalDrag = { _, dragAmount ->
                            pagerScreenState.verticalDrag(dragAmount = dragAmount)
                        },
                        onDragEnd = {
                            pagerScreenState.swipeEblanAction()

                            pagerScreenState.resetSwipeOffset()
                        },
                        onDragCancel = {
                            pagerScreenState.verticalDragEnd()
                        },
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
                .padding(
                    top = paddingValues.calculateTopPadding(),
                    bottom = paddingValues.calculateBottomPadding(),
                )
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
                    content = {
                        val gridHeight = safeDrawingHeight - pageIndicatorHeightPx - dockHeightPx

                        val cellWidth = safeDrawingWidth / homeSettings.columns

                        val cellHeight = gridHeight / homeSettings.rows

                        val x = it.startColumn * cellWidth

                        val y = it.startRow * cellHeight

                        val width = it.columnSpan * cellWidth

                        val height = it.rowSpan * cellHeight

                        InteractiveGridItem(
                            sharedTransitionScope = this@SharedTransitionLayout,
                            drag = pagerScreenState.drag,
                            gridItem = it,
                            gridItemSettings = homeSettings.gridItemSettings,
                            hasShortcutHostPermission = hasShortcutHostPermission,
                            isScrollInProgress = gridHorizontalPagerState.isScrollInProgress,
                            statusBarNotifications = pagerScreenState.statusBarNotifications,
                            textColor = textColor,
                            isVisibleOverlay = isVisibleOverlay,
                            sharedElementKey = SharedElementKey(
                                id = it.id,
                                parent = SharedElementKey.Parent.Grid,
                            ),
                            isVisibleFolder = folderPopups.isNotEmpty(),
                            moveGridItemResult = moveGridItemResult,
                            lockMovement = lockMovement,
                            isDragging = pagerScreenState.isDragging,
                            showGridItemPopup = pagerScreenState.showGridItemPopup,
                            onOpenAppDrawer = pagerScreenState::openApplicationScreen,
                            onTapApplicationInfo = { serialNumber, componentName ->
                                val sourceBoundsX = x + leftPadding

                                val sourceBoundsY = y + topPadding

                                androidLauncherAppsWrapper.startMainActivity(
                                    serialNumber = serialNumber,
                                    componentName = componentName,
                                    sourceBounds = Rect(
                                        sourceBoundsX,
                                        sourceBoundsY,
                                        sourceBoundsX + width,
                                        sourceBoundsY + height,
                                    ),
                                )
                            },
                            onUpsertFolderPopupEntry = onUpsertFolderPopupEntry,
                            onTapShortcutConfig = { shortcutIntentUri ->
                                context.startActivity(parseUri(shortcutIntentUri, 0))
                            },
                            onTapShortcutInfo = { serialNumber, packageName, shortcutId ->
                                val sourceBoundsX = x + leftPadding

                                val sourceBoundsY = y + topPadding

                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
                                    androidLauncherAppsWrapper.startShortcut(
                                        serialNumber = serialNumber,
                                        packageName = packageName,
                                        id = shortcutId,
                                        sourceBounds = Rect(
                                            sourceBoundsX,
                                            sourceBoundsY,
                                            sourceBoundsX + width,
                                            sourceBoundsY + height,
                                        ),
                                    )
                                }
                            },
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
                        )
                    },
                )
            }

            GridPagerIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(PAGE_INDICATOR_HEIGHT),
                color = getSystemTextColor(
                    systemCustomTextColor = homeSettings.gridItemSettings.customTextColor,
                    systemTextColor = textColor,
                ),
                gridHorizontalPagerState = gridHorizontalPagerState,
                infiniteScroll = homeSettings.infiniteScroll,
                pageCount = homeSettings.pageCount,
            )

            HorizontalPager(
                state = dockGridHorizontalPagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(dockHeight)
                    .padding(
                        start = paddingValues.calculateStartPadding(layoutDirection),
                        end = paddingValues.calculateEndPadding(layoutDirection),
                    ),
                userScrollEnabled = !isVisibleOverlay,
            ) { index ->
                val page = calculatePage(
                    index = index,
                    infiniteScroll = homeSettings.dockInfiniteScroll,
                    pageCount = homeSettings.dockPageCount,
                )

                GridLayout(
                    modifier = Modifier.fillMaxSize(),
                    columns = homeSettings.dockColumns,
                    gridItems = dockGridItemsByPage[page],
                    rows = homeSettings.dockRows,
                    content = {
                        val cellWidth = safeDrawingWidth / homeSettings.dockColumns

                        val cellHeight = dockHeightPx / homeSettings.dockRows

                        val x = it.startColumn * cellWidth

                        val y = it.startRow * cellHeight

                        val width = it.columnSpan * cellWidth

                        val height = it.rowSpan * cellHeight

                        InteractiveGridItem(
                            sharedTransitionScope = this@SharedTransitionLayout,
                            drag = pagerScreenState.drag,
                            gridItem = it,
                            gridItemSettings = homeSettings.gridItemSettings,
                            hasShortcutHostPermission = hasShortcutHostPermission,
                            isScrollInProgress = dockGridHorizontalPagerState.isScrollInProgress,
                            statusBarNotifications = pagerScreenState.statusBarNotifications,
                            textColor = textColor,
                            isVisibleOverlay = isVisibleOverlay,
                            sharedElementKey = SharedElementKey(
                                id = it.id,
                                parent = SharedElementKey.Parent.Dock,
                            ),
                            isVisibleFolder = folderPopups.isNotEmpty(),
                            moveGridItemResult = moveGridItemResult,
                            lockMovement = lockMovement,
                            isDragging = pagerScreenState.isDragging,
                            showGridItemPopup = pagerScreenState.showGridItemPopup,
                            onOpenAppDrawer = pagerScreenState::openApplicationScreen,
                            onTapApplicationInfo = { serialNumber, componentName ->
                                val left = x + leftPadding

                                val top = y + dockTopLeft

                                androidLauncherAppsWrapper.startMainActivity(
                                    serialNumber = serialNumber,
                                    componentName = componentName,
                                    sourceBounds = Rect(
                                        left,
                                        top,
                                        left + width,
                                        top + height,
                                    ),
                                )
                            },
                            onUpsertFolderPopupEntry = onUpsertFolderPopupEntry,
                            onTapShortcutConfig = { shortcutIntentUri ->
                                context.startActivity(parseUri(shortcutIntentUri, 0))
                            },
                            onTapShortcutInfo = { serialNumber, packageName, shortcutId ->
                                val sourceBoundsX = x + leftPadding

                                val sourceBoundsY = y + dockTopLeft

                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
                                    androidLauncherAppsWrapper.startShortcut(
                                        serialNumber = serialNumber,
                                        packageName = packageName,
                                        id = shortcutId,
                                        sourceBounds = Rect(
                                            sourceBoundsX,
                                            sourceBoundsY,
                                            sourceBoundsX + width,
                                            sourceBoundsY + height,
                                        ),
                                    )
                                }
                            },
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
                        )
                    },
                )
            }
        }

        if (gridItemSource != null &&
            pagerScreenState.showGridItemPopup &&
            pagerScreenState.popupIntOffset != null &&
            pagerScreenState.popupIntSize != null &&
            moveGridItemResult != null
        ) {
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
                onInfo = { serialNumber, componentName ->
                    pagerScreenState.startAppDetailsActivity(
                        left = pagerScreenState.popupIntOffset?.x,
                        top = pagerScreenState.popupIntOffset?.y,
                        width = pagerScreenState.popupIntSize?.width,
                        height = pagerScreenState.popupIntSize?.height,
                        serialNumber = serialNumber,
                        componentName = componentName,
                    )
                },
                onResize = {
                    pagerScreenState.resize(
                        resizeGridItem = it,
                        onUpdateResizeGridItem = onUpdateResizeGridItem,
                    )
                },
                onTapShortcutInfo = { serialNumber, packageName, shortcutId ->
                    pagerScreenState.startPopupShortcut(
                        leftPadding = leftPadding,
                        topPadding = topPadding,
                        serialNumber = serialNumber,
                        packageName = packageName,
                        shortcutId = shortcutId,
                    )
                },
                onUpdateGridItemSource = onUpdateGridItemSource,
                onUpdateImageBitmap = pagerScreenState::updateOverlayImageBitmap,
                onUpdateOverlayBounds = pagerScreenState::updateOverlayBounds,
                onUpdateSharedElementKey = pagerScreenState::updateSharedElementKey,
                onWidgets = pagerScreenState::openAppWidgetScreen,
                onUpdateIsVisibleOverlay = onUpdateIsVisibleOverlay,
                onUpdateMoveGridItemResult = onUpdateMoveGridItemResult,
            )
        }

        if (pagerScreenState.showSettingsPopup &&
            pagerScreenState.settingsPopupIntOffset != null
        ) {
            SettingsPopup(
                gridItems = gridItems,
                hasSystemFeatureAppWidgets = hasSystemFeatureAppWidgets,
                popupSettingsIntOffset = pagerScreenState.settingsPopupIntOffset,
                onDismissRequest = pagerScreenState::dismissSettingsPopup,
                onEditPage = onEditPage,
                onSettings = onSettings,
                onShortcutConfigActivities = pagerScreenState::openShortcutConfigScreen,
                onWallpaper = {
                    val intent = Intent(ACTION_SET_WALLPAPER)

                    val chooser = createChooser(intent, "Set Wallpaper")

                    context.startActivity(chooser)
                },
                onWidgets = pagerScreenState::openWidgetScreen,
            )
        }

        folderPopups.forEach { popupFolderGridItem ->
            FolderScreen(
                sharedTransitionScope = this@SharedTransitionLayout,
                drag = pagerScreenState.drag,
                folderPopup = popupFolderGridItem,
                gridItemSettings = homeSettings.gridItemSettings,
                paddingValues = paddingValues,
                safeDrawingHeight = safeDrawingHeight,
                safeDrawingWidth = safeDrawingWidth,
                statusBarNotifications = pagerScreenState.statusBarNotifications,
                isVisibleOverlay = isVisibleOverlay,
                hasShortcutHostPermission = hasShortcutHostPermission,
                moveGridItemResult = moveGridItemResult,
                homeSettings = homeSettings,
                isDragging = pagerScreenState.isDragging,
                dragIntOffset = pagerScreenState.dragIntOffset,
                lockMovement = lockMovement,
                folderCellWidth = homeSettings.folderCellWidth,
                folderCellHeight = homeSettings.folderCellHeight,
                screenWidth = screenWidth,
                screenHeight = screenHeight,
                lastFolderPopup = lastPopupFolderGridItem,
                showFolderGridItemPopup = pagerScreenState.showFolderGridItemPopup,
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
            )
        }

        if (lastPopupFolderGridItem != null &&
            pagerScreenState.showFolderGridItemPopup &&
            pagerScreenState.popupIntOffset != null &&
            pagerScreenState.popupIntSize != null &&
            moveGridItemResult != null
        ) {
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
                lastFolderPopupEntry = lastPopupFolderGridItem.folderPopupEntry,
                onDeleteGridItem = onDeleteGridItem,
                onUpsertFolderPopupEntry = onUpsertFolderPopupEntry,
                onDeleteFolderPopupEntry = onDeleteFolderPopupEntry,
                onDismissRequest = pagerScreenState::dismissFolderGridItemPopup,
                onUpdateIsDragging = pagerScreenState::updateIsDragging,
                onEdit = onEditGridItem,
                onInfo = { serialNumber, componentName ->
                    pagerScreenState.startAppDetailsActivity(
                        left = lastPopupFolderGridItem.folderPopupEntry.x,
                        top = lastPopupFolderGridItem.folderPopupEntry.y,
                        width = lastPopupFolderGridItem.folderPopupEntry.width,
                        height = lastPopupFolderGridItem.folderPopupEntry.height,
                        serialNumber = serialNumber,
                        componentName = componentName,
                    )
                },
                onTapShortcutInfo = { serialNumber, packageName, shortcutId ->
                    pagerScreenState.startPopupShortcut(
                        leftPadding = leftPadding,
                        topPadding = topPadding,
                        serialNumber = serialNumber,
                        packageName = packageName,
                        shortcutId = shortcutId,
                    )
                },
                onUpdateGridItemSource = onUpdateGridItemSource,
                onUpdateImageBitmap = pagerScreenState::updateOverlayImageBitmap,
                onUpdateIsVisibleOverlay = onUpdateIsVisibleOverlay,
                onUpdateOverlayBounds = pagerScreenState::updateOverlayBounds,
                onUpdateSharedElementKey = pagerScreenState::updateSharedElementKey,
                onWidgets = pagerScreenState::openAppWidgetScreen,
                onUpdateMoveGridItemResult = onUpdateMoveGridItemResult,
            )
        }

        if (gestureSettings.swipeUp.eblanActionType == EblanActionType.OpenAppDrawer ||
            gestureSettings.swipeDown.eblanActionType == EblanActionType.OpenAppDrawer
        ) {
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
                isPressHome = pagerScreenState.isPressHome,
                managedProfileResult = pagerScreenState.managedProfileResult,
                paddingValues = paddingValues,
                screenHeight = screenHeight,
                swipeY = pagerScreenState.swipeY.value,
                isVisibleOverlay = isVisibleOverlay,
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
                isPressHome = pagerScreenState.isPressHome,
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
                isPressHome = pagerScreenState.isPressHome,
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
                isPressHome = pagerScreenState.isPressHome,
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

        if (pagerScreenState.isResizing && resizeGridItem != null) {
            ResizeScreen(
                homeSettings = homeSettings,
                lockMovement = lockMovement,
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
            onResetOverlay = pagerScreenState::resetOverlay,
        )
    }
}
