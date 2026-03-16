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
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.mimeTypes
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.util.Consumer
import com.eblan.launcher.domain.model.AppDrawerSettings
import com.eblan.launcher.domain.model.ApplicationInfoGridItem
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
import com.eblan.launcher.domain.model.GestureSettings
import com.eblan.launcher.domain.model.GetEblanApplicationInfosByLabel
import com.eblan.launcher.domain.model.GridItem
import com.eblan.launcher.domain.model.GridItemData
import com.eblan.launcher.domain.model.HomeSettings
import com.eblan.launcher.domain.model.MoveGridItemResult
import com.eblan.launcher.domain.model.PinItemRequestType
import com.eblan.launcher.domain.model.TextColor
import com.eblan.launcher.feature.home.component.grid.GridLayout
import com.eblan.launcher.feature.home.component.indicator.PageIndicator
import com.eblan.launcher.feature.home.model.Drag
import com.eblan.launcher.feature.home.screen.application.ApplicationScreen
import com.eblan.launcher.feature.home.screen.folder.FolderScreen
import com.eblan.launcher.feature.home.screen.resize.ResizeScreen
import com.eblan.launcher.feature.home.screen.shortcutconfig.ShortcutConfigScreen
import com.eblan.launcher.feature.home.screen.widget.AppWidgetScreen
import com.eblan.launcher.feature.home.screen.widget.WidgetScreen
import com.eblan.launcher.feature.home.util.PAGE_INDICATOR_HEIGHT
import com.eblan.launcher.feature.home.util.calculatePage
import com.eblan.launcher.feature.home.util.getSystemTextColor
import com.eblan.launcher.feature.home.util.handleApplyFling
import com.eblan.launcher.feature.home.util.handleWallpaperScrollEffect
import com.eblan.launcher.ui.local.LocalAppWidgetHost
import com.eblan.launcher.ui.local.LocalFileManager
import com.eblan.launcher.ui.local.LocalImageSerializer
import com.eblan.launcher.ui.local.LocalLauncherApps
import com.eblan.launcher.ui.local.LocalUserManager
import com.eblan.launcher.ui.local.LocalWallpaperManager
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
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
    folderGridItem: GridItem?,
    gestureSettings: GestureSettings,
    getEblanApplicationInfosByLabel: GetEblanApplicationInfosByLabel,
    gridItems: List<GridItem>,
    gridItemsByPage: Map<Int, List<GridItem>>,
    hasShortcutHostPermission: Boolean,
    hasSystemFeatureAppWidgets: Boolean,
    homeSettings: HomeSettings,
    iconPackFilePaths: Map<String, String>,
    lockMovement: Boolean,
    moveGridItemResult: MoveGridItemResult?,
    paddingValues: PaddingValues,
    pinGridItem: GridItem?,
    screenHeight: Int,
    screenWidth: Int,
    textColor: TextColor,
    resizeGridItem: GridItem?,
    onDeleteApplicationInfoGridItem: (ApplicationInfoGridItem) -> Unit,
    onDeleteGridItem: (GridItem) -> Unit,
    onDeleteGridItemCache: (GridItem) -> Unit,
    onDeleteWidgetGridItemCache: (
        gridItem: GridItem,
        appWidgetId: Int,
    ) -> Unit,
    onDragCancelAfterMove: () -> Unit,
    onDragEndAfterMove: (MoveGridItemResult) -> Unit,
    onDragEndAfterMoveFolder: () -> Unit,
    onDragEndAfterMoveWidgetGridItem: (MoveGridItemResult) -> Unit,
    onDraggingGridItem: (List<GridItem>) -> Unit,
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
    onGetEblanApplicationInfosByTagIds: (List<Long>) -> Unit,
    onGetEblanShortcutConfigsByLabel: (String) -> Unit,
    onGetPinGridItem: (PinItemRequestType) -> Unit,
    onMoveFolderGridItem: (
        folderGridItem: GridItem,
        applicationInfoGridItems: List<ApplicationInfoGridItem>,
        movingApplicationInfoGridItem: ApplicationInfoGridItem,
        dragX: Int,
        dragY: Int,
        columns: Int,
        rows: Int,
        gridWidth: Int,
        gridHeight: Int,
        currentPage: Int,
    ) -> Unit,
    onMoveFolderGridItemOutsideFolder: (
        folderGridItem: GridItem,
        movingApplicationInfoGridItem: ApplicationInfoGridItem,
        applicationInfoGridItems: List<ApplicationInfoGridItem>,
    ) -> Unit,
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
    onResizeEnd: (GridItem) -> Unit,
    onResizeGridItem: (
        gridItem: GridItem,
        columns: Int,
        rows: Int,
    ) -> Unit,
    onSettings: () -> Unit,
    onShowFolderWhenDragging: (
        id: String,
        conflictingGridItem: GridItem,
        movingGridItem: GridItem,
    ) -> Unit,
    onStartSyncData: () -> Unit,
    onStopSyncData: () -> Unit,
    onUpdateAppDrawerSettings: (AppDrawerSettings) -> Unit,
    onUpdateEblanApplicationInfos: (List<EblanApplicationInfo>) -> Unit,
    onUpdateFolderGridItemId: (String?) -> Unit,
    onUpdateShortcutConfigGridItemDataCache: (
        byteArray: ByteArray?,
        moveGridItemResult: MoveGridItemResult,
        gridItem: GridItem,
        data: GridItemData.ShortcutConfig,
    ) -> Unit,
    onUpdateShortcutConfigIntoShortcutInfoGridItem: (
        moveGridItemResult: MoveGridItemResult,
        pinItemRequestType: PinItemRequestType.ShortcutInfo,
    ) -> Unit,
) {
    val context = LocalContext.current

    val androidLauncherAppsWrapper = LocalLauncherApps.current

    val androidWallpaperManagerWrapper = LocalWallpaperManager.current

    val view = LocalView.current

    val activity = LocalActivity.current as ComponentActivity

    val density = LocalDensity.current

    val androidUserManagerWrapper = LocalUserManager.current

    val androidImageSerializer = LocalImageSerializer.current

    val fileManager = LocalFileManager.current

    val androidAppWidgetHostWrapper = LocalAppWidgetHost.current

    val scope = rememberCoroutineScope()

    val pagerScreenState = rememberPagerScreenState(
        gestureSettings = gestureSettings,
        homeSettings = homeSettings,
        screenHeight = screenHeight,
        screenWidth = screenWidth,
        experimentalSettings = experimentalSettings,
        onDeleteGridItemCache = onDeleteGridItemCache,
        onDeleteWidgetGridItemCache = onDeleteWidgetGridItemCache,
        onDragCancelAfterMove = onDragCancelAfterMove,
        onDragEndAfterMove = onDragEndAfterMove,
        onDragEndAfterMoveFolder = onDragEndAfterMoveFolder,
        onDraggingGridItem = onDraggingGridItem,
        onGetPinGridItem = onGetPinGridItem,
        onMoveFolderGridItem = onMoveFolderGridItem,
        onMoveFolderGridItemOutsideFolder = onMoveFolderGridItemOutsideFolder,
        onMoveGridItem = onMoveGridItem,
        onResetPinGridItem = onResetPinGridItem,
        onShowFolderWhenDragging = onShowFolderWhenDragging,
        onUpdateFolderGridItemId = onUpdateFolderGridItemId,
    )

    val dockHeight = homeSettings.dockHeight.dp

    val dockHeightPx = with(density) {
        dockHeight.roundToPx()
    }

    val leftPadding = with(density) {
        paddingValues.calculateStartPadding(LayoutDirection.Ltr).roundToPx()
    }

    val rightPadding = with(density) {
        paddingValues.calculateEndPadding(LayoutDirection.Ltr).roundToPx()
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
        pagerScreenState.handleAppWidgetLauncherResult(result = result)
    }

    val shortcutConfigLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        scope.launch {
            handleShortcutConfigLauncherResult(
                androidImageSerializer = androidImageSerializer,
                gridItemSource = pagerScreenState.gridItemSource,
                moveGridItemResult = moveGridItemResult,
                result = result,
                onDeleteGridItemCache = onDeleteGridItemCache,
                onUpdateShortcutConfigGridItemDataCache = onUpdateShortcutConfigGridItemDataCache,
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
                gridItemSource = pagerScreenState.gridItemSource,
                moveGridItemResult = moveGridItemResult,
                result = result,
                onDeleteGridItemCache = onDeleteGridItemCache,
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

    val dockCurrentPage by remember(
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

    val currentPage by remember(
        key1 = gridHorizontalPagerState,
        key2 = dockGridHorizontalPagerState,
        key3 = homeSettings,
    ) {
        derivedStateOf {
            when (pagerScreenState.associate) {
                Associate.Grid -> {
                    gridCurrentPage
                }

                Associate.Dock -> {
                    dockCurrentPage
                }

                null -> {
                    0
                }
            }
        }
    }

    val folderGridHorizontalPagerState = rememberPagerState(
        pageCount = {
            when (val data = folderGridItem?.data) {
                is GridItemData.Folder -> {
                    data.gridItemsByPage.size
                }

                else -> 0
            }
        },
    )

    LaunchedEffect(key1 = pinGridItem) {
        pagerScreenState.handlePinGridItemEffect(
            gridItems = gridItems,
            pinGridItem = pinGridItem,
            onDraggingGridItem = onDraggingGridItem,
        )
    }

    LifecycleEffect(
        syncDataEnabled = experimentalSettings.syncData,
        userManagerWrapper = androidUserManagerWrapper,
        onManagedProfileResultChange = pagerScreenState::updateManagedProfileResult,
        onStartSyncData = onStartSyncData,
        onStatusBarNotificationsChange = pagerScreenState::updateStatusBarNotifications,
        onStopSyncData = onStopSyncData,
    )

    LaunchedEffect(key1 = pagerScreenState.dragIntOffset) {
        pagerScreenState.handleDragGridItemEffect(
            currentPage = currentPage,
            density = density,
            dockHeight = dockHeight,
            folderCurrentPage = folderGridHorizontalPagerState.currentPage,
            folderGridItem = folderGridItem,
            isGridScrollInProgress = gridHorizontalPagerState.isScrollInProgress,
            isDockScrollInProgress = dockGridHorizontalPagerState.isScrollInProgress,
            lockMovement = lockMovement,
            paddingValues = paddingValues,
        )
    }

    LaunchedEffect(key1 = pagerScreenState.drag) {
        pagerScreenState.handleDropGridItemEffect(
            moveGridItemResult = moveGridItemResult,
            onLaunchShortcutConfigIntent = shortcutConfigLauncher::launch,
            onLaunchShortcutConfigIntentSenderRequest = shortcutConfigIntentSenderLauncher::launch,
            onLaunchWidgetIntent = appWidgetLauncher::launch,
        )
    }

    LaunchedEffect(key1 = pagerScreenState.deleteAppWidgetId) {
        pagerScreenState.handleDeleteAppWidgetIdEffect()
    }

    LaunchedEffect(key1 = pagerScreenState.updatedWidgetGridItem) {
        handleBoundWidgetEffect(
            activity = activity,
            androidAppWidgetHostWrapper = androidAppWidgetHostWrapper,
            gridItemSource = pagerScreenState.gridItemSource,
            moveGridItemResult = moveGridItemResult,
            updatedWidgetGridItem = pagerScreenState.updatedWidgetGridItem,
            onDeleteGridItemCache = onDeleteGridItemCache,
            onDeleteWidgetGridItemCache = onDeleteWidgetGridItemCache,
            onDragEndAfterMoveWidgetGridItem = onDragEndAfterMoveWidgetGridItem,
        )
    }

    LaunchedEffect(key1 = gridHorizontalPagerState) {
        handleWallpaperScrollEffect(
            horizontalPagerState = gridHorizontalPagerState,
            infiniteScroll = homeSettings.infiniteScroll,
            pageCount = homeSettings.pageCount,
            wallpaperManagerWrapper = androidWallpaperManagerWrapper,
            wallpaperScroll = homeSettings.wallpaperScroll,
            windowToken = view.windowToken,
        )
    }

    LaunchedEffect(key1 = moveGridItemResult) {
        pagerScreenState.handleConflictingGridItemEffect(
            density = density,
            dockHeight = dockHeight,
            moveGridItemResult = moveGridItemResult,
            paddingValues = paddingValues,
        )
    }

    LaunchedEffect(key1 = pagerScreenState.dragIntOffset) {
        pagerScreenState.handleAnimateScrollToPageEffect(
            density = density,
            folderGridItem = folderGridItem,
            paddingValues = paddingValues,
        )
    }

    LaunchedEffect(key1 = configureResultCode) {
        handleConfigureLauncherResultEffect(
            moveGridItemResult = moveGridItemResult,
            resultCode = configureResultCode,
            updatedGridItem = pagerScreenState.updatedWidgetGridItem,
            onDeleteWidgetGridItemCache = onDeleteWidgetGridItemCache,
            onDragEndAfterMoveWidgetGridItem = onDragEndAfterMoveWidgetGridItem,
            onResetConfigureResultCode = onResetConfigureResultCode,
        )
    }

    LaunchedEffect(key1 = pagerScreenState.gridPageDirection) {
        handlePageDirection(
            pageDirection = pagerScreenState.gridPageDirection,
            pagerState = gridHorizontalPagerState,
        )
    }

    LaunchedEffect(key1 = pagerScreenState.dockPageDirection) {
        handlePageDirection(
            pageDirection = pagerScreenState.dockPageDirection,
            pagerState = dockGridHorizontalPagerState,
        )
    }

    LaunchedEffect(key1 = pagerScreenState.folderPageDirection) {
        handlePageDirection(
            pageDirection = pagerScreenState.folderPageDirection,
            pagerState = folderGridHorizontalPagerState,
        )
    }

    LaunchedEffect(key1 = pagerScreenState.hasDoubleTap) {
        pagerScreenState.handleHasDoubleTap()
    }

    DisposableEffect(key1 = activity) {
        val listener = Consumer<Intent> { intent ->
            scope.launch {
                pagerScreenState.handleNewIntent(
                    gridHorizontalPagerState = gridHorizontalPagerState,
                    dockGridHorizontalPagerState = dockGridHorizontalPagerState,
                    intent = intent,
                    windowToken = view.windowToken,
                )
            }
        }

        activity.addOnNewIntentListener(listener)

        onDispose {
            activity.removeOnNewIntentListener(listener)
        }
    }

    LaunchedEffect(key1 = pagerScreenState.swipeUpY) {
        snapshotFlow { pagerScreenState.swipeUpY.value }.onEach { y ->
            pagerScreenState.updateLastSwipeUpY(value = y)
        }.collect()
    }

    LaunchedEffect(key1 = pagerScreenState.swipeDownY) {
        snapshotFlow { pagerScreenState.swipeDownY.value }.onEach { y ->
            pagerScreenState.updateLastSwipeDownY(value = y)
        }.collect()
    }

    LaunchedEffect(key1 = pagerScreenState.isPressHome) {
        pagerScreenState.handleIsPressHome()
    }

    SharedTransitionLayout(
        modifier = modifier
            .pointerInput(Unit) {
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
                shouldStartDragAndDrop = { event ->
                    event.mimeTypes().contains(ClipDescription.MIMETYPE_TEXT_PLAIN)
                },
                target = pagerScreenState.target,
            )
            .fillMaxSize(),
    ) {
        Column(
            modifier = modifier
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onVerticalDrag = { _, dragAmount ->
                            pagerScreenState.verticalDrag(dragAmount = dragAmount)
                        },
                        onDragEnd = {
                            pagerScreenState.swipeEblanAction(
                                context = context,
                                gestureSettings = gestureSettings,
                                launcherApps = androidLauncherAppsWrapper,
                                screenHeight = screenHeight,
                                swipeDownY = pagerScreenState.swipeDownY.value,
                                swipeUpY = pagerScreenState.swipeUpY.value,
                            )

                            pagerScreenState.resetSwipeOffset(
                                gestureSettings = gestureSettings,
                                screenHeight = screenHeight,
                                swipeDownY = pagerScreenState.swipeDownY,
                                swipeUpY = pagerScreenState.swipeUpY,
                            )
                        },
                        onDragCancel = {
                            pagerScreenState.verticalDragEnd()
                        },
                    )
                }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = {
                            pagerScreenState.updateHasDoubleTap(value = true)
                        },
                        onLongPress = { offset ->
                            pagerScreenState.showSettingsPopup(offset = offset)
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
                            start = paddingValues.calculateStartPadding(LayoutDirection.Ltr),
                            end = paddingValues.calculateEndPadding(LayoutDirection.Ltr),
                        ),
                    columns = homeSettings.columns,
                    gridItems = gridItemsByPage[page],
                    rows = homeSettings.rows,
                    content = { gridItem ->
                        val gridHeight = safeDrawingHeight - pageIndicatorHeightPx - dockHeightPx

                        val cellWidth = safeDrawingWidth / homeSettings.columns

                        val cellHeight = gridHeight / homeSettings.rows

                        val x = gridItem.startColumn * cellWidth

                        val y = gridItem.startRow * cellHeight

                        val width = gridItem.columnSpan * cellWidth

                        val height = gridItem.rowSpan * cellHeight

                        InteractiveGridItemContent(
                            drag = pagerScreenState.drag,
                            gridItem = gridItem,
                            gridItemSettings = homeSettings.gridItemSettings,
                            gridItemSource = pagerScreenState.gridItemSource,
                            hasShortcutHostPermission = hasShortcutHostPermission,
                            iconPackFilePaths = iconPackFilePaths,
                            isLongPress = pagerScreenState.isLongPress,
                            isScrollInProgress = gridHorizontalPagerState.isScrollInProgress,
                            statusBarNotifications = pagerScreenState.statusBarNotifications,
                            textColor = textColor,
                            onDraggingGridItem = {
                                onDraggingGridItem(gridItems)
                            },
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
                            onTapFolderGridItem = {
                                pagerScreenState.showFolder(
                                    height = height,
                                    id = gridItem.id,
                                    width = width,
                                    x = x,
                                    y = y,
                                )
                            },
                            onTapShortcutConfig = { uri ->
                                context.startActivity(parseUri(uri, 0))
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
                            onUpdateGridItemSource = pagerScreenState::updateGridItemSource,
                            onUpdateImageBitmap = pagerScreenState::updateOverlayImageBitmap,
                            onUpdateIsDragging = pagerScreenState::updateIsDragging,
                            onUpdateIsLongPress = pagerScreenState::updateIsLongPress,
                            onUpdateOverlayBounds = pagerScreenState::updateOverlayBounds,
                            onUpdateSharedElementKey = pagerScreenState::updateSharedElementKey,
                            onShowGridItemPopup = pagerScreenState::showGridItemPopup,
                            onDismissGridItemPopup = pagerScreenState::dismissGridItemPopup,
                        )
                    },
                )
            }

            PageIndicator(
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
                        start = paddingValues.calculateStartPadding(LayoutDirection.Ltr),
                        end = paddingValues.calculateEndPadding(LayoutDirection.Ltr),
                    ),
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
                    content = { gridItem ->
                        val cellWidth = safeDrawingWidth / homeSettings.dockColumns

                        val cellHeight = dockHeightPx / homeSettings.dockRows

                        val x = gridItem.startColumn * cellWidth

                        val y = gridItem.startRow * cellHeight

                        val width = gridItem.columnSpan * cellWidth

                        val height = gridItem.rowSpan * cellHeight

                        InteractiveGridItemContent(
                            drag = pagerScreenState.drag,
                            gridItem = gridItem,
                            gridItemSettings = homeSettings.gridItemSettings,
                            gridItemSource = pagerScreenState.gridItemSource,
                            hasShortcutHostPermission = hasShortcutHostPermission,
                            iconPackFilePaths = iconPackFilePaths,
                            isLongPress = pagerScreenState.isLongPress,
                            isScrollInProgress = dockGridHorizontalPagerState.isScrollInProgress,
                            statusBarNotifications = pagerScreenState.statusBarNotifications,
                            textColor = textColor,
                            onDraggingGridItem = {
                                onDraggingGridItem(gridItems)
                            },
                            onOpenAppDrawer = pagerScreenState::openApplicationScreen,
                            onTapApplicationInfo = { serialNumber, componentName ->
                                val sourceBoundsX = x + leftPadding

                                val sourceBoundsY = y + dockTopLeft

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
                            onTapFolderGridItem = {
                                pagerScreenState.showFolder(
                                    height = height,
                                    id = gridItem.id,
                                    width = width,
                                    x = x,
                                    y = y + dockTopLeft,
                                )
                            },
                            onTapShortcutConfig = { uri ->
                                context.startActivity(parseUri(uri, 0))
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
                            onUpdateGridItemSource = pagerScreenState::updateGridItemSource,
                            onUpdateImageBitmap = pagerScreenState::updateOverlayImageBitmap,
                            onUpdateIsDragging = pagerScreenState::updateIsDragging,
                            onUpdateIsLongPress = pagerScreenState::updateIsLongPress,
                            onUpdateOverlayBounds = pagerScreenState::updateOverlayBounds,
                            onUpdateSharedElementKey = pagerScreenState::updateSharedElementKey,
                            onShowGridItemPopup = pagerScreenState::showGridItemPopup,
                            onDismissGridItemPopup = pagerScreenState::dismissGridItemPopup,
                        )
                    },
                )
            }
        }

        if (pagerScreenState.showGridItemPopup &&
            pagerScreenState.popupIntOffset != null &&
            pagerScreenState.popupIntSize != null
        ) {
            GridItemPopup(
                currentPage = currentPage,
                drag = pagerScreenState.drag,
                eblanAppWidgetProviderInfosGroup = eblanAppWidgetProviderInfosGroup,
                eblanShortcutInfosGroup = eblanShortcutInfosGroup,
                gridItem = pagerScreenState.gridItemSource?.gridItem,
                gridItemSettings = homeSettings.gridItemSettings,
                hasShortcutHostPermission = hasShortcutHostPermission,
                paddingValues = paddingValues,
                popupIntOffset = pagerScreenState.popupIntOffset,
                popupIntSize = pagerScreenState.popupIntSize,
                onDeleteGridItem = onDeleteGridItem,
                onDismissRequest = pagerScreenState::dismissGridItemPopup,
                onDraggingShortcutInfoGridItem = {
                    pagerScreenState.draggingShortcutInfoGridItem(gridItems = gridItems)
                },
                onEdit = onEditGridItem,
                onInfo = { serialNumber, componentName ->
                    val left = pagerScreenState.popupIntOffset?.x

                    val top = pagerScreenState.popupIntOffset?.y

                    val width = pagerScreenState.popupIntSize?.width

                    val height = pagerScreenState.popupIntSize?.height

                    if (left != null && top != null &&
                        width != null && height != null
                    ) {
                        androidLauncherAppsWrapper.startAppDetailsActivity(
                            serialNumber = serialNumber,
                            componentName = componentName,
                            sourceBounds = Rect(
                                left,
                                top,
                                left + width,
                                top + height,
                            ),
                        )
                    }
                },
                onResize = {
                    pagerScreenState.resize(gridItems = gridItems)
                },
                onTapShortcutInfo = { serialNumber, packageName, shortcutId ->
                    val popupIntOffsetX = pagerScreenState.popupIntOffset?.x

                    val popupIntOffsetY = pagerScreenState.popupIntOffset?.y

                    val popupIntSizeWidth = pagerScreenState.popupIntSize?.width

                    val popupIntSizeHeight = pagerScreenState.popupIntSize?.height

                    if (popupIntOffsetX != null && popupIntOffsetY != null &&
                        popupIntSizeWidth != null && popupIntSizeHeight != null
                    ) {
                        val sourceBoundsX = popupIntOffsetX + leftPadding

                        val sourceBoundsY = popupIntOffsetY + topPadding

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
                            androidLauncherAppsWrapper.startShortcut(
                                serialNumber = serialNumber,
                                packageName = packageName,
                                id = shortcutId,
                                sourceBounds = Rect(
                                    sourceBoundsX,
                                    sourceBoundsY,
                                    sourceBoundsX + popupIntSizeWidth,
                                    sourceBoundsY + popupIntSizeHeight,
                                ),
                            )
                        }
                    }
                },
                onUpdateGridItemSource = pagerScreenState::updateGridItemSource,
                onUpdateImageBitmap = pagerScreenState::updateOverlayImageBitmap,
                onUpdateOverlayBounds = pagerScreenState::updateOverlayBounds,
                onUpdateSharedElementKey = pagerScreenState::updateSharedElementKey,
                onWidgets = pagerScreenState::openAppWidgetScreen,
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

        if (folderGridItem != null &&
            pagerScreenState.folderPopupIntOffset != null &&
            pagerScreenState.folderPopupIntSize != null
        ) {
            FolderScreen(
                drag = pagerScreenState.drag,
                folderGridHorizontalPagerState = folderGridHorizontalPagerState,
                folderGridItem = folderGridItem,
                folderPopupIntOffset = pagerScreenState.folderPopupIntOffset,
                folderPopupIntSize = pagerScreenState.folderPopupIntSize,
                gridItemSettings = homeSettings.gridItemSettings,
                gridItemSource = pagerScreenState.gridItemSource,
                homeSettings = homeSettings,
                iconPackFilePaths = iconPackFilePaths,
                isLongPress = pagerScreenState.isLongPress,
                paddingValues = paddingValues,
                safeDrawingHeight = safeDrawingHeight,
                safeDrawingWidth = safeDrawingWidth,
                statusBarNotifications = pagerScreenState.statusBarNotifications,
                textColor = textColor,
                onDismissRequest = pagerScreenState::dismissFolder,
                onDraggingGridItem = {
                    onDraggingGridItem(gridItems)
                },
                onOpenAppDrawer = pagerScreenState::openApplicationScreen,
                onUpdateGridItemSource = pagerScreenState::updateGridItemSource,
                onUpdateImageBitmap = pagerScreenState::updateOverlayImageBitmap,
                onUpdateIsDragging = pagerScreenState::updateIsDragging,
                onUpdateIsLongPress = pagerScreenState::updateIsLongPress,
                onUpdateOverlayBounds = pagerScreenState::updateOverlayBounds,
                onUpdateSharedElementKey = pagerScreenState::updateSharedElementKey,
                onShowGridItemPopup = pagerScreenState::showFolderGridItemPopup,
                onDismissGridItemPopup = pagerScreenState::dismissFolderGridItemPopup,
            )
        }

        if (pagerScreenState.showFolderGridItemPopup &&
            pagerScreenState.popupIntOffset != null &&
            pagerScreenState.popupIntSize != null
        ) {
            FolderGridItemPopup(
                gridItemSource = pagerScreenState.gridItemSource,
                paddingValues = paddingValues,
                popupIntOffset = pagerScreenState.popupIntOffset,
                popupIntSize = pagerScreenState.popupIntSize,
                onDeleteApplicationInfoGridItem = onDeleteApplicationInfoGridItem,
                onDismissRequest = pagerScreenState::dismissFolderGridItemPopup,
                onEdit = onEditGridItem,
                modifier = modifier,
                currentPage = currentPage,
                drag = pagerScreenState.drag,
                eblanAppWidgetProviderInfosGroup = eblanAppWidgetProviderInfosGroup,
                eblanShortcutInfosGroup = eblanShortcutInfosGroup,
                gridItemSettings = homeSettings.gridItemSettings,
                hasShortcutHostPermission = hasShortcutHostPermission,
                onDraggingShortcutInfoGridItem = {
                    pagerScreenState.draggingShortcutInfoGridItem(gridItems = gridItems)
                },
                onTapShortcutInfo = { serialNumber, packageName, shortcutId ->
                    val popupIntOffsetX = pagerScreenState.popupIntOffset?.x

                    val popupIntOffsetY = pagerScreenState.popupIntOffset?.y

                    val popupIntSizeWidth = pagerScreenState.popupIntSize?.width

                    val popupIntSizeHeight = pagerScreenState.popupIntSize?.height

                    if (popupIntOffsetX != null && popupIntOffsetY != null &&
                        popupIntSizeWidth != null && popupIntSizeHeight != null
                    ) {
                        val sourceBoundsX = popupIntOffsetX + leftPadding

                        val sourceBoundsY = popupIntOffsetY + topPadding

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
                            androidLauncherAppsWrapper.startShortcut(
                                serialNumber = serialNumber,
                                packageName = packageName,
                                id = shortcutId,
                                sourceBounds = Rect(
                                    sourceBoundsX,
                                    sourceBoundsY,
                                    sourceBoundsX + popupIntSizeWidth,
                                    sourceBoundsY + popupIntSizeHeight,
                                ),
                            )
                        }
                    }
                },
                onUpdateGridItemSource = pagerScreenState::updateGridItemSource,
                onUpdateImageBitmap = pagerScreenState::updateOverlayImageBitmap,
                onUpdateOverlayBounds = pagerScreenState::updateOverlayBounds,
                onUpdateSharedElementKey = pagerScreenState::updateSharedElementKey,
                onWidgets = pagerScreenState::openAppWidgetScreen,
                onDismissFolder = {
                    pagerScreenState.showFolder(
                        height = 0,
                        id = null,
                        width = 0,
                        x = 0,
                        y = 0,
                    )
                },
            )
        }

        if (gestureSettings.swipeUp.eblanActionType == EblanActionType.OpenAppDrawer || gestureSettings.swipeDown.eblanActionType == EblanActionType.OpenAppDrawer) {
            ApplicationScreen(
                alpha = pagerScreenState.applicationScreenAlpha,
                appDrawerSettings = appDrawerSettings,
                cornerSize = pagerScreenState.applicationScreenCornerSize,
                currentPage = currentPage,
                drag = pagerScreenState.drag,
                eblanAppWidgetProviderInfosGroup = eblanAppWidgetProviderInfosGroup,
                eblanApplicationInfoTags = eblanApplicationInfoTags,
                eblanShortcutInfosGroup = eblanShortcutInfosGroup,
                getEblanApplicationInfosByLabel = getEblanApplicationInfosByLabel,
                hasShortcutHostPermission = hasShortcutHostPermission,
                iconPackFilePaths = iconPackFilePaths,
                isPressHome = pagerScreenState.isPressHome,
                managedProfileResult = pagerScreenState.managedProfileResult,
                paddingValues = paddingValues,
                screenHeight = screenHeight,
                swipeY = pagerScreenState.swipeY.value,
                onDismiss = pagerScreenState::dismissApplicationScreen,
                onDragEnd = { remaining ->
                    scope.launch {
                        handleApplyFling(
                            offsetY = pagerScreenState.swipeY,
                            remaining = remaining,
                            screenHeight = screenHeight,
                        )
                    }
                },
                onDraggingGridItem = {
                    onDraggingGridItem(gridItems)
                },
                onEditApplicationInfo = onEditApplicationInfo,
                onGetEblanApplicationInfosByLabel = onGetEblanApplicationInfosByLabel,
                onGetEblanApplicationInfosByTagIds = onGetEblanApplicationInfosByTagIds,
                onUpdateAppDrawerSettings = onUpdateAppDrawerSettings,
                onUpdateEblanApplicationInfos = onUpdateEblanApplicationInfos,
                onUpdateGridItemSource = pagerScreenState::updateGridItemSource,
                onUpdateImageBitmap = pagerScreenState::updateOverlayImageBitmap,
                onUpdateIsLongPressAndIsDragging = pagerScreenState::updateIsLongPressAndIsDragging,
                onUpdateOverlayBounds = pagerScreenState::updateOverlayBounds,
                onUpdateSharedElementKey = pagerScreenState::updateSharedElementKey,
                onVerticalDrag = pagerScreenState::verticalDragApplicationScreen,
                onWidgets = pagerScreenState::openAppWidgetScreen,
                onDraggingShortcutInfoGridItem = {
                    pagerScreenState.draggingShortcutInfoGridItem(gridItems = gridItems)
                },
            )
        }

        WidgetScreen(
            columns = homeSettings.columns,
            currentPage = currentPage,
            drag = pagerScreenState.drag,
            eblanAppWidgetProviderInfos = eblanAppWidgetProviderInfos,
            gridItemSettings = homeSettings.gridItemSettings,
            isPressHome = pagerScreenState.isPressHome,
            paddingValues = paddingValues,
            rows = homeSettings.rows,
            screenHeight = screenHeight,
            screenWidth = screenWidth,
            offsetY = pagerScreenState.widgetScreenOffsetY.value,
            alpha = pagerScreenState.widgetScreenAlpha,
            cornerSize = pagerScreenState.widgetScreenCornerSize,
            onDismiss = pagerScreenState::dismissWidgetScreen,
            onDraggingGridItem = {
                onDraggingGridItem(gridItems)
            },
            onGetEblanAppWidgetProviderInfosByLabel = onGetEblanAppWidgetProviderInfosByLabel,
            onUpdateOverlayBounds = pagerScreenState::updateOverlayBounds,
            onUpdateImageBitmap = pagerScreenState::updateOverlayImageBitmap,
            onUpdateGridItemSource = pagerScreenState::updateGridItemSource,
            onUpdateSharedElementKey = pagerScreenState::updateSharedElementKey,
            onUpdateIsLongPressAndIsDragging = pagerScreenState::updateIsLongPressAndIsDragging,
            onVerticalDrag = pagerScreenState::verticalDragWidgetScreen,
            onDragEnd = { remaining ->
                scope.launch {
                    handleApplyFling(
                        offsetY = pagerScreenState.widgetScreenOffsetY,
                        remaining = remaining,
                        screenHeight = screenHeight,
                    )
                }
            },
        )

        ShortcutConfigScreen(
            currentPage = currentPage,
            drag = pagerScreenState.drag,
            eblanShortcutConfigs = eblanShortcutConfigs,
            gridItemSettings = homeSettings.gridItemSettings,
            isPressHome = pagerScreenState.isPressHome,
            paddingValues = paddingValues,
            screenHeight = screenHeight,
            offsetY = pagerScreenState.shortcutConfigScreenOffsetY.value,
            alpha = pagerScreenState.shortcutConfigScreenAlpha,
            cornerSize = pagerScreenState.shortcutConfigScreenCornerSize,
            onDismiss = pagerScreenState::dismissShortcutConfigScreen,
            onDraggingGridItem = {
                onDraggingGridItem(gridItems)
            },
            onGetEblanShortcutConfigsByLabel = onGetEblanShortcutConfigsByLabel,
            onUpdateOverlayBounds = pagerScreenState::updateOverlayBounds,
            onUpdateImageBitmap = pagerScreenState::updateOverlayImageBitmap,
            onUpdateGridItemSource = pagerScreenState::updateGridItemSource,
            onUpdateSharedElementKey = pagerScreenState::updateSharedElementKey,
            onUpdateIsLongPressAndIsDragging = pagerScreenState::updateIsLongPressAndIsDragging,
            onVerticalDrag = pagerScreenState::verticalDragShortcutConfigScreen,
            onDragEnd = { remaining ->
                scope.launch {
                    handleApplyFling(
                        offsetY = pagerScreenState.shortcutConfigScreenOffsetY,
                        remaining = remaining,
                        screenHeight = screenHeight,
                    )
                }
            },
        )

        AppWidgetScreen(
            columns = homeSettings.columns,
            currentPage = currentPage,
            drag = pagerScreenState.drag,
            eblanAppWidgetProviderInfosGroup = eblanAppWidgetProviderInfosGroup,
            eblanApplicationInfoGroup = pagerScreenState.eblanApplicationInfoGroup,
            gridItemSettings = homeSettings.gridItemSettings,
            isPressHome = pagerScreenState.isPressHome,
            paddingValues = paddingValues,
            rows = homeSettings.rows,
            screenHeight = screenHeight,
            screenWidth = screenWidth,
            offsetY = pagerScreenState.appWidgetScreenOffsetY.value,
            onDismiss = pagerScreenState::dismissAppWidgetScreen,
            onDismissApplicationScreen = pagerScreenState::dismissApplicationScreen,
            onDraggingGridItem = {
                onDraggingGridItem(gridItems)
            },
            onUpdateOverlayBounds = pagerScreenState::updateOverlayBounds,
            onUpdateImageBitmap = pagerScreenState::updateOverlayImageBitmap,
            onUpdateGridItemSource = pagerScreenState::updateGridItemSource,
            onUpdateSharedElementKey = pagerScreenState::updateSharedElementKey,
            onUpdateIsLongPressAndIsDragging = pagerScreenState::updateIsLongPressAndIsDragging,
            onVerticalDrag = pagerScreenState::verticalDragAppWidgetScreen,
            onDragEnd = {
                scope.launch {
                    handleApplyFling(
                        offsetY = pagerScreenState.appWidgetScreenOffsetY,
                        remaining = 0f,
                        screenHeight = screenHeight,
                    )
                }
            },
        )

        if (pagerScreenState.isResizing && pagerScreenState.gridItemSource != null) {
            ResizeScreen(
                gridItem = pagerScreenState.gridItemSource?.gridItem,
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
            drag = pagerScreenState.drag,
            overlayImageBitmap = pagerScreenState.overlayImageBitmap,
            overlayIntOffset = pagerScreenState.overlayIntOffset,
            overlayIntSize = pagerScreenState.overlayIntSize,
            sharedElementKey = pagerScreenState.sharedElementKey,
            onResetOverlay = pagerScreenState::resetOverlay,
        )
    }
}
