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

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps.PinItemRequest
import android.os.Build
import android.os.IBinder
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.toAndroidDragEvent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import com.eblan.launcher.domain.framework.FileManager
import com.eblan.launcher.domain.model.ApplicationInfoGridItem
import com.eblan.launcher.domain.model.Associate
import com.eblan.launcher.domain.model.EblanAction
import com.eblan.launcher.domain.model.EblanActionType
import com.eblan.launcher.domain.model.EblanApplicationInfoGroup
import com.eblan.launcher.domain.model.ExperimentalSettings
import com.eblan.launcher.domain.model.GestureSettings
import com.eblan.launcher.domain.model.GridItem
import com.eblan.launcher.domain.model.HomeSettings
import com.eblan.launcher.domain.model.ManagedProfileResult
import com.eblan.launcher.domain.model.MoveGridItemResult
import com.eblan.launcher.domain.model.PinItemRequestType
import com.eblan.launcher.feature.home.model.Drag
import com.eblan.launcher.feature.home.model.GridItemSource
import com.eblan.launcher.feature.home.model.PageDirection
import com.eblan.launcher.feature.home.model.SharedElementKey
import com.eblan.launcher.feature.home.util.calculatePage
import com.eblan.launcher.feature.home.util.handleEblanAction
import com.eblan.launcher.framework.imageserializer.AndroidImageSerializer
import com.eblan.launcher.framework.launcherapps.AndroidLauncherAppsWrapper
import com.eblan.launcher.framework.launcherapps.PinItemRequestWrapper
import com.eblan.launcher.framework.usermanager.AndroidUserManagerWrapper
import com.eblan.launcher.framework.wallpapermanager.AndroidWallpaperManagerWrapper
import com.eblan.launcher.framework.widgetmanager.AndroidAppWidgetHostWrapper
import com.eblan.launcher.framework.widgetmanager.AndroidAppWidgetManagerWrapper
import com.eblan.launcher.ui.local.LocalAppWidgetHost
import com.eblan.launcher.ui.local.LocalAppWidgetManager
import com.eblan.launcher.ui.local.LocalFileManager
import com.eblan.launcher.ui.local.LocalImageSerializer
import com.eblan.launcher.ui.local.LocalLauncherApps
import com.eblan.launcher.ui.local.LocalPinItemRequest
import com.eblan.launcher.ui.local.LocalUserManager
import com.eblan.launcher.ui.local.LocalWallpaperManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.math.roundToInt

/**
 * The [PagerScreen] is so huge that we have to do this
 * 2k LOC is unacceptable and this is only what we've got
 */
@OptIn(ExperimentalFoundationApi::class)
internal class PagerScreenState(
    initialSwipeUpY: Float,
    initialSwipeDownY: Float,
    initialFolderX: Int,
    initialFolderY: Int,
    initialFolderWidth: Int,
    initialFolderHeight: Int,
    private val screenWidth: Int,
    private val screenHeight: Int,
    private val fileManager: FileManager,
    private val androidImageSerializer: AndroidImageSerializer,
    private val androidLauncherAppsWrapper: AndroidLauncherAppsWrapper,
    private val scope: CoroutineScope,
    private val context: Context,
    private val androidUserManagerWrapper: AndroidUserManagerWrapper,
    private val pinItemRequestWrapper: PinItemRequestWrapper,
    private val gestureSettings: GestureSettings,
    private val homeSettings: HomeSettings,
    private val androidAppWidgetHostWrapper: AndroidAppWidgetHostWrapper,
    private val androidAppWidgetManagerWrapper: AndroidAppWidgetManagerWrapper,
    private val androidWallpaperManagerWrapper: AndroidWallpaperManagerWrapper,
    private val density: Density,
    private val experimentalSettings: ExperimentalSettings,
    private val onGetPinGridItem: (PinItemRequestType) -> Unit,
    private val onResetPinGridItem: () -> Unit,
    private val onMoveFolderGridItem: (
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
    private val onMoveFolderGridItemOutsideFolder: (
        folderGridItem: GridItem,
        movingApplicationInfoGridItem: ApplicationInfoGridItem,
        applicationInfoGridItems: List<ApplicationInfoGridItem>,
    ) -> Unit,
    private val onMoveGridItem: (
        movingGridItem: GridItem,
        x: Int,
        y: Int,
        columns: Int,
        rows: Int,
        gridWidth: Int,
        gridHeight: Int,
    ) -> Unit,
    private val onDeleteGridItemCache: (GridItem) -> Unit,
    private val onDragCancelAfterMove: () -> Unit,
    private val onDragEndAfterMove: (MoveGridItemResult) -> Unit,
    private val onDragEndAfterMoveFolder: () -> Unit,
    private val onDeleteWidgetGridItemCache: (
        gridItem: GridItem,
        appWidgetId: Int,
    ) -> Unit,
    private val onShowFolderWhenDragging: (
        id: String,
        conflictingGridItem: GridItem,
        movingGridItem: GridItem,
    ) -> Unit,
    private val onUpdateFolderGridItemId: (String?) -> Unit,
    private val onDraggingGridItem: (List<GridItem>) -> Unit,
) {
    private var lastSwipeUpY by mutableFloatStateOf(initialSwipeUpY)

    private var lastSwipeDownY by mutableFloatStateOf(initialSwipeDownY)

    private var lastFolderPopupX by mutableIntStateOf(initialFolderX)

    private var lastFolderPopupY by mutableIntStateOf(initialFolderY)

    private var lastFolderPopupWidth by mutableIntStateOf(initialFolderWidth)

    private var lastFolderPopupHeight by mutableIntStateOf(initialFolderHeight)

    var hasDoubleTap by mutableStateOf(false)
        private set

    var isPressHome by mutableStateOf(false)
        private set

    var eblanApplicationInfoGroup by mutableStateOf<EblanApplicationInfoGroup?>(null)
        private set

    var showGridItemPopup by mutableStateOf(false)
        private set

    var showSettingsPopup by mutableStateOf(false)
        private set

    var showFolderGridItemPopup by mutableStateOf(false)
        private set

    var isLongPress by mutableStateOf(false)
        private set

    var isDragging by mutableStateOf(false)
        private set

    var isResizing by mutableStateOf(false)
        private set

    var settingsPopupIntOffset by mutableStateOf<IntOffset?>(null)
        private set

    var popupIntOffset by mutableStateOf<IntOffset?>(null)
        private set

    var popupIntSize by mutableStateOf<IntSize?>(null)
        private set

    var deleteAppWidgetId by mutableStateOf(false)
        private set

    var updatedWidgetGridItem by mutableStateOf<GridItem?>(null)
        private set

    var gridPageDirection by mutableStateOf<PageDirection?>(null)
        private set

    var dockPageDirection by mutableStateOf<PageDirection?>(null)
        private set

    var folderPageDirection by mutableStateOf<PageDirection?>(null)
        private set

    var gridItemSource by mutableStateOf<GridItemSource?>(null)
        private set

    var dragIntOffset by mutableStateOf(IntOffset.Zero)
        private set

    var overlayIntOffset by mutableStateOf<IntOffset?>(null)
        private set

    var overlayIntSize by mutableStateOf<IntSize?>(null)
        private set

    var overlayImageBitmap by mutableStateOf<ImageBitmap?>(null)
        private set

    var drag by mutableStateOf(Drag.None)
        private set

    var sharedElementKey by mutableStateOf<SharedElementKey?>(null)
        private set

    var managedProfileResult by mutableStateOf<ManagedProfileResult?>(null)
        private set

    var statusBarNotifications by mutableStateOf<Map<String, Int>>(emptyMap())
        private set

    var associate by mutableStateOf<Associate?>(null)
        private set

    val swipeUpY = Animatable(initialSwipeUpY)

    val swipeDownY = Animatable(initialSwipeDownY)

    val target = object : DragAndDropTarget {
        override fun onStarted(event: DragAndDropEvent) {
            val offset = with(event.toAndroidDragEvent()) {
                IntOffset(x = x.roundToInt(), y = y.roundToInt())
            }

            drag = Drag.Start

            dragIntOffset = offset

            scope.launch {
                handlePinItemRequest(pinItemRequest = pinItemRequestWrapper.getPinItemRequest())
            }
        }

        override fun onEnded(event: DragAndDropEvent) {
            drag = Drag.End

            val pinItemRequest = pinItemRequestWrapper.getPinItemRequest()

            if (pinItemRequest != null) {
                onResetPinGridItem()

                pinItemRequestWrapper.updatePinItemRequest(null)
            }
        }

        override fun onMoved(event: DragAndDropEvent) {
            val offset = with(event.toAndroidDragEvent()) {
                IntOffset(x = x.roundToInt(), y = y.roundToInt())
            }

            drag = Drag.Dragging

            dragIntOffset = offset
        }

        override fun onDrop(event: DragAndDropEvent): Boolean = true
    }

    val swipeY by derivedStateOf {
        if (swipeUpY.value < screenHeight.toFloat() && gestureSettings.swipeUp.eblanActionType == EblanActionType.OpenAppDrawer) {
            swipeUpY
        } else if (swipeDownY.value < screenHeight.toFloat() && gestureSettings.swipeDown.eblanActionType == EblanActionType.OpenAppDrawer) {
            swipeDownY
        } else {
            Animatable(screenHeight.toFloat())
        }
    }

    val isApplicationScreenVisible by derivedStateOf {
        swipeY.value < screenHeight.toFloat()
    }

    val applicationScreenAlpha by derivedStateOf {
        ((screenHeight - swipeY.value) / (screenHeight / 2)).coerceIn(0f, 1f)
    }

    val applicationScreenCornerSize by derivedStateOf {
        val progress = (swipeY.value / screenHeight).coerceIn(0f, 1f)

        (20 * progress).dp
    }

    val pagerScreenAlpha by derivedStateOf {
        val threshold = screenHeight / 2

        ((swipeY.value - threshold) / threshold).coerceIn(0f, 1f)
    }

    var folderPopupIntOffset by mutableStateOf<IntOffset?>(
        IntOffset(
            x = lastFolderPopupX,
            y = lastFolderPopupY,
        ),
    )

    var folderPopupIntSize by mutableStateOf<IntSize?>(
        IntSize(
            width = lastFolderPopupWidth,
            height = lastFolderPopupHeight,
        ),
    )

    val widgetScreenOffsetY = Animatable(screenHeight.toFloat())

    val widgetScreenAlpha by derivedStateOf {
        ((screenHeight - widgetScreenOffsetY.value) / (screenHeight / 2)).coerceIn(0f, 1f)
    }

    val widgetScreenCornerSize by derivedStateOf {
        val progress = (widgetScreenOffsetY.value / screenHeight).coerceIn(0f, 1f)

        (20 * progress).dp
    }

    val shortcutConfigScreenOffsetY = Animatable(screenHeight.toFloat())

    val shortcutConfigScreenAlpha by derivedStateOf {
        ((screenHeight - shortcutConfigScreenOffsetY.value) / (screenHeight / 2)).coerceIn(0f, 1f)
    }

    val shortcutConfigScreenCornerSize by derivedStateOf {
        val progress = (shortcutConfigScreenOffsetY.value / screenHeight).coerceIn(0f, 1f)

        (20 * progress).dp
    }

    val appWidgetScreenOffsetY = Animatable(screenHeight.toFloat())

    private val touchSlop = with(density) {
        50.dp.toPx()
    }

    private var accumulatedDragOffset by mutableStateOf(Offset.Zero)

    private var folderTitleHeightPx by mutableIntStateOf(0)

    private var lastAppWidgetId by mutableIntStateOf(AppWidgetManager.INVALID_APPWIDGET_ID)

    suspend fun handlePinGridItemEffect(
        gridItems: List<GridItem>,
        pinGridItem: GridItem?,
        onDraggingGridItem: (List<GridItem>) -> Unit,
    ) {
        handlePinGridItem(
            isApplicationScreenVisible = isApplicationScreenVisible,
            pinGridItem = pinGridItem,
            pinItemRequestWrapper = pinItemRequestWrapper,
            screenHeight = screenHeight,
            swipeY = swipeY,
            onDraggingGridItem = {
                isLongPress = true

                isDragging = true

                onDraggingGridItem(gridItems)
            },
            onUpdateGridItemSource = { newGridItemSource ->
                gridItemSource = newGridItemSource
            },
        )
    }

    suspend fun handleDragGridItemEffect(
        currentPage: Int,
        density: Density,
        dockHeight: Dp,
        folderCurrentPage: Int,
        folderGridItem: GridItem?,
        isGridScrollInProgress: Boolean,
        isDockScrollInProgress: Boolean,
        lockMovement: Boolean,
        paddingValues: PaddingValues,
    ) {
        handleDragGridItem(
            columns = homeSettings.columns,
            currentPage = currentPage,
            density = density,
            dockColumns = homeSettings.dockColumns,
            dockHeight = dockHeight,
            dockRows = homeSettings.dockRows,
            drag = drag,
            dragIntOffset = dragIntOffset,
            folderCurrentPage = folderCurrentPage,
            folderGridItem = folderGridItem,
            folderPopupIntOffset = folderPopupIntOffset,
            folderPopupIntSize = folderPopupIntSize,
            folderTitleHeightPx = folderTitleHeightPx,
            gridItemSource = gridItemSource,
            isDragging = isDragging,
            isLongPress = isLongPress,
            isGridScrollInProgress = isGridScrollInProgress,
            isDockScrollInProgress = isDockScrollInProgress,
            lockMovement = lockMovement,
            paddingValues = paddingValues,
            rows = homeSettings.rows,
            screenHeight = screenHeight,
            screenWidth = screenWidth,
            onMoveFolderGridItem = onMoveFolderGridItem,
            onMoveFolderGridItemOutsideFolder = onMoveFolderGridItemOutsideFolder,
            onMoveGridItem = onMoveGridItem,
            onUpdateAssociate = { newAssociate ->
                associate = newAssociate
            },
            onUpdateGridItemSource = { newGridItemSource ->
                gridItemSource = newGridItemSource

                associate = newGridItemSource.gridItem.associate
            },
            onUpdateSharedElementKey = { newSharedElementKey ->
                sharedElementKey = newSharedElementKey
            },
        )
    }

    suspend fun handleDropGridItemEffect(
        moveGridItemResult: MoveGridItemResult?,
        onLaunchShortcutConfigIntent: (Intent) -> Unit,
        onLaunchShortcutConfigIntentSenderRequest: (IntentSenderRequest) -> Unit,
        onLaunchWidgetIntent: (Intent) -> Unit,
    ) {
        handleDropGridItem(
            androidAppWidgetHostWrapper = androidAppWidgetHostWrapper,
            androidAppWidgetManagerWrapper = androidAppWidgetManagerWrapper,
            androidLauncherAppsWrapper = androidLauncherAppsWrapper,
            androidUserManagerWrapper = androidUserManagerWrapper,
            drag = drag,
            gridItemSource = gridItemSource,
            isDragging = isDragging,
            isLongPress = isLongPress,
            moveGridItemResult = moveGridItemResult,
            lockMovement = experimentalSettings.lockMovement,
            onDeleteGridItemCache = onDeleteGridItemCache,
            onDragCancelAfterMove = onDragCancelAfterMove,
            onDragEndAfterMove = onDragEndAfterMove,
            onDragEndAfterMoveFolder = onDragEndAfterMoveFolder,
            onLaunchShortcutConfigIntent = onLaunchShortcutConfigIntent,
            onLaunchShortcutConfigIntentSenderRequest = onLaunchShortcutConfigIntentSenderRequest,
            onLaunchWidgetIntent = onLaunchWidgetIntent,
            onToast = {
                Toast.makeText(
                    context,
                    "Layout was canceled due to an invalid position or interruption",
                    Toast.LENGTH_LONG,
                ).show()
            },
            onUpdateAppWidgetId = { appWidgetId ->
                lastAppWidgetId = appWidgetId
            },
            onUpdateIsDragging = { newIsDragging ->
                isDragging = newIsDragging
            },
            onUpdateIsLongPress = { newIsLongPress ->
                isLongPress = newIsLongPress
            },
            onUpdateWidgetGridItem = { gridItem ->
                updatedWidgetGridItem = gridItem
            },
        )
    }

    fun handleDeleteAppWidgetIdEffect() {
        handleDeleteAppWidgetId(
            appWidgetId = lastAppWidgetId,
            deleteAppWidgetId = deleteAppWidgetId,
            gridItemSource = gridItemSource,
            onDeleteWidgetGridItemCache = onDeleteWidgetGridItemCache,
            onResetAppWidgetId = {
                lastAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

                deleteAppWidgetId = false
            },
        )
    }

    suspend fun handleConflictingGridItemEffect(
        density: Density,
        dockHeight: Dp,
        moveGridItemResult: MoveGridItemResult?,
        paddingValues: PaddingValues,
    ) {
        handleConflictingGridItem(
            columns = homeSettings.columns,
            dockRows = homeSettings.dockRows,
            dockColumns = homeSettings.dockColumns,
            density = density,
            dockHeight = dockHeight,
            drag = drag,
            gridItemSource = gridItemSource,
            isDragging = isDragging,
            isLongPress = isLongPress,
            moveGridItemResult = moveGridItemResult,
            paddingValues = paddingValues,
            rows = homeSettings.rows,
            screenHeight = screenHeight,
            screenWidth = screenWidth,
            lockMovement = experimentalSettings.lockMovement,
            onShowFolderWhenDragging = onShowFolderWhenDragging,
            onUpdateFolderPopupBounds = { intOffset, intSize ->
                lastFolderPopupX = intOffset.x
                lastFolderPopupY = intOffset.y

                lastFolderPopupWidth = intSize.width
                lastFolderPopupHeight = intSize.height

                folderPopupIntOffset = intOffset

                folderPopupIntSize = intSize
            },
            onUpdateGridItemSource = { newGridItemSource ->
                gridItemSource = newGridItemSource
            },
            onUpdateSharedElementKey = { newSharedElementKey ->
                sharedElementKey = newSharedElementKey
            },
        )
    }

    fun handleAnimateScrollToPageEffect(
        density: Density,
        folderGridItem: GridItem?,
        paddingValues: PaddingValues,
    ) {
        handleAnimateScrollToPage(
            associate = associate,
            columns = homeSettings.columns,
            density = density,
            dragIntOffset = dragIntOffset,
            folderGridItem = folderGridItem,
            folderPopupIntOffset = folderPopupIntOffset,
            folderPopupIntSize = folderPopupIntSize,
            gridItemSource = gridItemSource,
            isDragging = isDragging,
            paddingValues = paddingValues,
            screenWidth = screenWidth,
            onUpdateDockPageDirection = { pageDirection ->
                dockPageDirection = pageDirection
            },
            onUpdateFolderPageDirection = { pageDirection ->
                folderPageDirection = pageDirection
            },
            onUpdateGridPageDirection = { pageDirection ->
                gridPageDirection = pageDirection
            },
        )
    }

    suspend fun handleHasDoubleTap() {
        handleHasDoubleTap(
            androidLauncherAppsWrapper = androidLauncherAppsWrapper,
            context = context,
            gestureSettings = gestureSettings,
            hasDoubleTap = hasDoubleTap,
            onOpenAppDrawer = {
                swipeY.animateTo(
                    targetValue = 0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessLow,
                    ),
                )
            },
        )

        hasDoubleTap = false
    }

    suspend fun handleNewIntent(
        gridHorizontalPagerState: PagerState,
        dockGridHorizontalPagerState: PagerState,
        intent: Intent,
        windowToken: IBinder,
    ) {
        handleActionMainIntent(
            eblanApplicationInfoGroup = eblanApplicationInfoGroup,
            gridHorizontalPagerState = gridHorizontalPagerState,
            gridInfiniteScroll = homeSettings.infiniteScroll,
            gridInitialPage = homeSettings.initialPage,
            intent = intent,
            pageCount = homeSettings.pageCount,
            screenHeight = screenHeight,
            swipeY = swipeY.value,
            widgetScreenOffsetY = widgetScreenOffsetY.value,
            shortcutConfigScreenOffsetY = shortcutConfigScreenOffsetY.value,
            wallpaperManagerWrapper = androidWallpaperManagerWrapper,
            wallpaperScroll = homeSettings.wallpaperScroll,
            windowToken = windowToken,
            onHome = {
                isPressHome = true
            },
            dockGridHorizontalPagerState = dockGridHorizontalPagerState,
            dockInfiniteScroll = homeSettings.dockInfiniteScroll,
            dockInitialPage = homeSettings.dockInitialPage,
        )

        handleEblanActionIntent(
            context = context,
            intent = intent,
            launcherApps = androidLauncherAppsWrapper,
            onOpenAppDrawer = {
                swipeY.animateTo(
                    targetValue = 0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessLow,
                    ),
                )
            },
        )
    }

    fun handleAppWidgetLauncherResult(result: ActivityResult) {
        handleAppWidgetLauncherResult(
            androidAppWidgetManagerWrapper = androidAppWidgetManagerWrapper,
            gridItemSource = gridItemSource,
            result = result,
            onDeleteAppWidgetId = {
                deleteAppWidgetId = true
            },
            onUpdateWidgetGridItem = { gridItem ->
                updatedWidgetGridItem = gridItem
            },
        )
    }

    fun swipeEblanAction(
        context: Context,
        gestureSettings: GestureSettings,
        launcherApps: AndroidLauncherAppsWrapper,
        screenHeight: Int,
        swipeDownY: Float,
        swipeUpY: Float,
    ) {
        scope.launch {
            val swipeThreshold = 100f

            if (swipeUpY < screenHeight - swipeThreshold) {
                handleEblanAction(
                    context = context,
                    eblanAction = gestureSettings.swipeUp,
                    launcherApps = launcherApps,
                    onOpenAppDrawer = {},
                )
            }

            if (swipeDownY < screenHeight - swipeThreshold) {
                handleEblanAction(
                    context = context,
                    eblanAction = gestureSettings.swipeDown,
                    launcherApps = launcherApps,
                    onOpenAppDrawer = {},
                )
            }
        }
    }

    fun resetSwipeOffset(
        gestureSettings: GestureSettings,
        screenHeight: Int,
        swipeDownY: Animatable<Float, AnimationVector1D>,
        swipeUpY: Animatable<Float, AnimationVector1D>,
    ) {
        suspend fun animateOffset(
            eblanAction: EblanAction,
            swipeY: Animatable<Float, AnimationVector1D>,
        ) {
            if (eblanAction.eblanActionType == EblanActionType.OpenAppDrawer) {
                val targetValue = if (swipeY.value < screenHeight - 200f) {
                    0f
                } else {
                    screenHeight.toFloat()
                }

                swipeY.animateTo(
                    targetValue = targetValue,
                    animationSpec = tween(
                        easing = FastOutSlowInEasing,
                    ),
                )
            } else {
                swipeY.snapTo(screenHeight.toFloat())
            }
        }

        scope.launch {
            animateOffset(
                eblanAction = gestureSettings.swipeUp,
                swipeY = swipeUpY,
            )

            animateOffset(
                eblanAction = gestureSettings.swipeDown,
                swipeY = swipeDownY,
            )
        }
    }

    suspend fun handleActionMainIntent(
        eblanApplicationInfoGroup: EblanApplicationInfoGroup?,
        gridHorizontalPagerState: PagerState,
        dockGridHorizontalPagerState: PagerState,
        gridInfiniteScroll: Boolean,
        dockInfiniteScroll: Boolean,
        gridInitialPage: Int,
        dockInitialPage: Int,
        intent: Intent,
        pageCount: Int,
        screenHeight: Int,
        swipeY: Float,
        widgetScreenOffsetY: Float,
        shortcutConfigScreenOffsetY: Float,
        wallpaperManagerWrapper: AndroidWallpaperManagerWrapper,
        wallpaperScroll: Boolean,
        windowToken: IBinder,
        onHome: () -> Unit,
    ) {
        if (intent.action != Intent.ACTION_MAIN && !intent.hasCategory(Intent.CATEGORY_HOME)) {
            return
        }

        if ((intent.flags and Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT) != 0) {
            return
        }

        onHome()

        if (swipeY < screenHeight.toFloat() ||
            widgetScreenOffsetY < screenHeight.toFloat() ||
            shortcutConfigScreenOffsetY < screenHeight.toFloat() ||
            eblanApplicationInfoGroup != null
        ) {
            return
        }

        gridHorizontalPagerState.scrollToPage(
            if (gridInfiniteScroll) {
                (Int.MAX_VALUE / 2) + gridInitialPage
            } else {
                gridInitialPage
            },
        )

        dockGridHorizontalPagerState.scrollToPage(
            if (dockInfiniteScroll) {
                (Int.MAX_VALUE / 2) + dockInitialPage
            } else {
                dockInitialPage
            },
        )

        if (wallpaperScroll) {
            val page = calculatePage(
                index = gridHorizontalPagerState.currentPage,
                infiniteScroll = gridInfiniteScroll,
                pageCount = pageCount,
            )

            wallpaperManagerWrapper.setWallpaperOffsetSteps(
                xStep = 1f / (pageCount.toFloat() - 1),
                yStep = 1f,
            )

            wallpaperManagerWrapper.setWallpaperOffsets(
                windowToken = windowToken,
                xOffset = page / (pageCount.toFloat() - 1),
                yOffset = 0f,
            )
        }
    }

    suspend fun handleEblanActionIntent(
        context: Context,
        intent: Intent,
        launcherApps: AndroidLauncherAppsWrapper,
        onOpenAppDrawer: suspend () -> Unit,
    ) {
        if (intent.action != EblanAction.ACTION) return

        val eblanAction = intent.getStringExtra(EblanAction.NAME)?.let { eblanAction ->
            Json.decodeFromString<EblanAction>(eblanAction)
        } ?: return

        handleEblanAction(
            context = context,
            eblanAction = eblanAction,
            launcherApps = launcherApps,
            onOpenAppDrawer = onOpenAppDrawer,
        )
    }

    suspend fun handleHasDoubleTap(
        androidLauncherAppsWrapper: AndroidLauncherAppsWrapper,
        context: Context,
        gestureSettings: GestureSettings,
        hasDoubleTap: Boolean,
        onOpenAppDrawer: suspend () -> Unit,
    ) {
        if (!hasDoubleTap) return

        handleEblanAction(
            context = context,
            eblanAction = gestureSettings.doubleTap,
            launcherApps = androidLauncherAppsWrapper,
            onOpenAppDrawer = onOpenAppDrawer,
        )
    }

    suspend fun handlePinGridItem(
        isApplicationScreenVisible: Boolean,
        pinGridItem: GridItem?,
        pinItemRequestWrapper: PinItemRequestWrapper,
        screenHeight: Int,
        swipeY: Animatable<Float, AnimationVector1D>,
        onDraggingGridItem: () -> Unit,
        onUpdateGridItemSource: (GridItemSource) -> Unit,
    ) {
        if (pinGridItem == null) return

        val pinItemRequest = pinItemRequestWrapper.getPinItemRequest() ?: return

        if (isApplicationScreenVisible) {
            swipeY.animateTo(
                targetValue = screenHeight.toFloat(),
                animationSpec = tween(
                    easing = FastOutSlowInEasing,
                ),
            )
        }

        onUpdateGridItemSource(
            GridItemSource.Pin(
                gridItem = pinGridItem,
                pinItemRequest = pinItemRequest,
            ),
        )

        onDraggingGridItem()
    }

    fun dragStart(offset: Offset) {
        drag = Drag.Start

        dragIntOffset = offset.round()

        accumulatedDragOffset = Offset.Zero
    }

    fun drag(dragAmount: Offset) {
        accumulatedDragOffset += dragAmount

        if (accumulatedDragOffset.getDistance() >= touchSlop) {
            drag = Drag.Dragging
        }

        dragIntOffset += dragAmount.round()

        overlayIntOffset = overlayIntOffset?.plus(dragAmount.round())
    }

    fun showFolder(
        height: Int,
        id: String?,
        width: Int,
        x: Int,
        y: Int,
    ) {
        lastFolderPopupX = x
        lastFolderPopupY = y

        lastFolderPopupWidth = width
        lastFolderPopupHeight = height

        folderPopupIntOffset = IntOffset(
            x = x,
            y = y,
        )

        folderPopupIntSize = IntSize(
            width = width,
            height = height,
        )

        onUpdateFolderGridItemId(id)
    }

    fun dismissFolder() {
        folderPopupIntOffset = null

        folderPopupIntSize = null

        onUpdateFolderGridItemId(null)
    }

    fun updateOverlayBounds(
        intOffset: IntOffset,
        intSize: IntSize,
    ) {
        overlayIntOffset = intOffset

        overlayIntSize = intSize
    }

    fun resetOverlay() {
        overlayImageBitmap = null

        sharedElementKey = null

        overlayIntOffset = null

        overlayIntSize = null

        drag = Drag.None
    }

    fun updateLastSwipeUpY(value: Float) {
        lastSwipeUpY = value
    }

    fun updateLastSwipeDownY(value: Float) {
        lastSwipeDownY = value
    }

    fun updateHasDoubleTap(value: Boolean) {
        hasDoubleTap = value
    }

    fun showGridItemPopup(
        intOffset: IntOffset,
        intSize: IntSize,
    ) {
        popupIntOffset = intOffset

        popupIntSize = intSize

        showGridItemPopup = true
    }

    fun dismissGridItemPopup() {
        popupIntOffset = null

        popupIntSize = null

        showGridItemPopup = false
    }

    fun showFolderGridItemPopup(
        intOffset: IntOffset,
        intSize: IntSize,
    ) {
        popupIntOffset = intOffset

        popupIntSize = intSize

        showFolderGridItemPopup = true
    }

    fun dismissFolderGridItemPopup() {
        popupIntOffset = null

        popupIntSize = null

        showFolderGridItemPopup = false
    }

    fun updateIsLongPress(value: Boolean) {
        isLongPress = value
    }

    fun updateIsDragging(value: Boolean) {
        isDragging = value
    }

    fun updateIsResizing(value: Boolean) {
        isResizing = value
    }

    fun updateGridItemSource(value: GridItemSource) {
        gridItemSource = value

        associate = value.gridItem.associate
    }

    fun updateOverlayImageBitmap(value: ImageBitmap?) {
        overlayImageBitmap = value
    }

    fun updateDrag(value: Drag) {
        drag = value
    }

    fun updateSharedElementKey(value: SharedElementKey?) {
        sharedElementKey = value
    }

    fun updateManagedProfileResult(value: ManagedProfileResult?) {
        managedProfileResult = value
    }

    fun updateStatusBarNotifications(value: Map<String, Int>) {
        statusBarNotifications = value
    }

    fun handleIsPressHome() {
        if (isPressHome) {
            showGridItemPopup = false

            showSettingsPopup = false
        }
    }

    fun verticalDrag(dragAmount: Float) {
        scope.launch {
            swipeUpY.snapTo(swipeUpY.value + dragAmount)

            swipeDownY.snapTo(swipeDownY.value - dragAmount)
        }
    }

    fun verticalDragEnd() {
        scope.launch {
            swipeUpY.animateTo(screenHeight.toFloat())

            swipeDownY.animateTo(screenHeight.toFloat())
        }
    }

    fun showSettingsPopup(offset: Offset) {
        settingsPopupIntOffset = offset.round()

        showSettingsPopup = true
    }

    fun dismissSettingsPopup() {
        settingsPopupIntOffset = null

        showSettingsPopup = false
    }

    fun openApplicationScreen() {
        scope.launch {
            swipeY.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessLow,
                ),
            )
        }
    }

    fun draggingShortcutInfoGridItem(gridItems: List<GridItem>) {
        isLongPress = true

        isDragging = true

        onDraggingGridItem(gridItems)
    }

    fun resize(gridItems: List<GridItem>) {
        isResizing = true

        onDraggingGridItem(gridItems)
    }

    fun dismissApplicationScreen() {
        scope.launch {
            swipeY.animateTo(
                targetValue = screenHeight.toFloat(),
                animationSpec = tween(
                    easing = FastOutSlowInEasing,
                ),
            )

            if (isPressHome) {
                isPressHome = false
            }
        }
    }

    fun updateIsLongPressAndIsDragging() {
        isLongPress = true

        isDragging = true
    }

    fun verticalDragApplicationScreen(dragAmount: Float) {
        scope.launch {
            swipeY.snapTo(swipeY.value + dragAmount)
        }
    }

    fun openWidgetScreen() {
        scope.launch {
            widgetScreenOffsetY.animateTo(
                targetValue = 0f,
                animationSpec = tween(
                    easing = FastOutSlowInEasing,
                ),
            )
        }
    }

    fun dismissWidgetScreen() {
        scope.launch {
            widgetScreenOffsetY.animateTo(
                targetValue = screenHeight.toFloat(),
                animationSpec = tween(
                    easing = FastOutSlowInEasing,
                ),
            )

            if (isPressHome) {
                isPressHome = false
            }
        }
    }

    fun verticalDragWidgetScreen(dragAmount: Float) {
        scope.launch {
            widgetScreenOffsetY.snapTo(widgetScreenOffsetY.value + dragAmount)
        }
    }

    fun verticalDragShortcutConfigScreen(dragAmount: Float) {
        scope.launch {
            shortcutConfigScreenOffsetY.snapTo(shortcutConfigScreenOffsetY.value + dragAmount)
        }
    }

    fun openShortcutConfigScreen() {
        scope.launch {
            shortcutConfigScreenOffsetY.animateTo(
                targetValue = 0f,
                animationSpec = tween(
                    easing = FastOutSlowInEasing,
                ),
            )
        }
    }

    fun dismissShortcutConfigScreen() {
        scope.launch {
            shortcutConfigScreenOffsetY.animateTo(
                targetValue = screenHeight.toFloat(),
                animationSpec = tween(
                    easing = FastOutSlowInEasing,
                ),
            )

            if (isPressHome) {
                isPressHome = false
            }
        }
    }

    fun dismissAppWidgetScreen() {
        scope.launch {
            appWidgetScreenOffsetY.animateTo(
                targetValue = screenHeight.toFloat(),
                animationSpec = tween(
                    easing = FastOutSlowInEasing,
                ),
            )

            eblanApplicationInfoGroup = null

            if (isPressHome) {
                isPressHome = false
            }
        }
    }

    fun openAppWidgetScreen(value: EblanApplicationInfoGroup) {
        scope.launch {
            eblanApplicationInfoGroup = value

            appWidgetScreenOffsetY.animateTo(
                targetValue = 0f,
                animationSpec = tween(
                    easing = FastOutSlowInEasing,
                ),
            )
        }
    }

    fun verticalDragAppWidgetScreen(dragAmount: Float) {
        scope.launch {
            appWidgetScreenOffsetY.snapTo(appWidgetScreenOffsetY.value + dragAmount)
        }
    }

    suspend fun handlePinItemRequest(pinItemRequest: PinItemRequest?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && pinItemRequest != null) {
            when (pinItemRequest.requestType) {
                PinItemRequest.REQUEST_TYPE_APPWIDGET -> {
                    val appWidgetProviderInfo = pinItemRequest.getAppWidgetProviderInfo(context)

                    if (appWidgetProviderInfo != null) {
                        val componentName = appWidgetProviderInfo.provider.flattenToString()

                        val preview =
                            appWidgetProviderInfo.loadPreviewImage(context, 0)?.let { drawable ->
                                val directory =
                                    fileManager.getFilesDirectory(FileManager.WIDGETS_DIR)

                                val file = File(
                                    directory,
                                    fileManager.getHashedFileName(name = componentName),
                                )

                                androidImageSerializer.createDrawablePath(
                                    drawable = drawable,
                                    file = file,
                                )

                                file.absolutePath
                            }

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            onGetPinGridItem(
                                PinItemRequestType.Widget(
                                    appWidgetId = 0,
                                    componentName = componentName,
                                    packageName = appWidgetProviderInfo.provider.packageName,
                                    serialNumber = androidUserManagerWrapper.getSerialNumberForUser(
                                        userHandle = appWidgetProviderInfo.profile,
                                    ),
                                    configure = appWidgetProviderInfo.configure.flattenToString(),
                                    minWidth = appWidgetProviderInfo.minWidth,
                                    minHeight = appWidgetProviderInfo.minHeight,
                                    resizeMode = appWidgetProviderInfo.resizeMode,
                                    minResizeWidth = appWidgetProviderInfo.minResizeWidth,
                                    minResizeHeight = appWidgetProviderInfo.minResizeHeight,
                                    maxResizeWidth = appWidgetProviderInfo.maxResizeWidth,
                                    maxResizeHeight = appWidgetProviderInfo.maxResizeHeight,
                                    targetCellHeight = appWidgetProviderInfo.targetCellHeight,
                                    targetCellWidth = appWidgetProviderInfo.targetCellWidth,
                                    preview = preview,
                                ),
                            )
                        } else {
                            onGetPinGridItem(
                                PinItemRequestType.Widget(
                                    appWidgetId = 0,
                                    componentName = appWidgetProviderInfo.provider.flattenToString(),
                                    packageName = appWidgetProviderInfo.provider.packageName,
                                    serialNumber = androidUserManagerWrapper.getSerialNumberForUser(
                                        userHandle = appWidgetProviderInfo.profile,
                                    ),
                                    configure = appWidgetProviderInfo.configure.flattenToString(),
                                    minWidth = appWidgetProviderInfo.minWidth,
                                    minHeight = appWidgetProviderInfo.minHeight,
                                    resizeMode = appWidgetProviderInfo.resizeMode,
                                    minResizeWidth = appWidgetProviderInfo.minResizeWidth,
                                    minResizeHeight = appWidgetProviderInfo.minResizeHeight,
                                    maxResizeWidth = 0,
                                    maxResizeHeight = 0,
                                    targetCellHeight = 0,
                                    targetCellWidth = 0,
                                    preview = preview,
                                ),
                            )
                        }
                    }
                }

                PinItemRequest.REQUEST_TYPE_SHORTCUT -> {
                    val shortcutInfo = pinItemRequest.shortcutInfo

                    if (shortcutInfo != null) {
                        val icon = androidLauncherAppsWrapper.getShortcutIconDrawable(
                            shortcutInfo = shortcutInfo,
                            density = 0,
                        )?.let { drawable ->
                            val directory = fileManager.getFilesDirectory(FileManager.SHORTCUTS_DIR)

                            val file = File(
                                directory,
                                fileManager.getHashedFileName(name = shortcutInfo.id),
                            )

                            androidImageSerializer.createDrawablePath(
                                drawable = drawable,
                                file = file,
                            )

                            file.absolutePath
                        }

                        onGetPinGridItem(
                            PinItemRequestType.ShortcutInfo(
                                serialNumber = androidUserManagerWrapper.getSerialNumberForUser(
                                    userHandle = shortcutInfo.userHandle,
                                ),
                                shortcutId = shortcutInfo.id,
                                packageName = shortcutInfo.`package`,
                                shortLabel = shortcutInfo.shortLabel.toString(),
                                longLabel = shortcutInfo.longLabel.toString(),
                                isEnabled = shortcutInfo.isEnabled,
                                disabledMessage = shortcutInfo.disabledMessage?.toString(),
                                icon = icon,
                            ),
                        )
                    }
                }
            }
        }
    }

    companion object {
        fun Saver(
            screenWidth: Int,
            screenHeight: Int,
            fileManager: FileManager,
            androidImageSerializer: AndroidImageSerializer,
            androidLauncherAppsWrapper: AndroidLauncherAppsWrapper,
            scope: CoroutineScope,
            context: Context,
            androidUserManagerWrapper: AndroidUserManagerWrapper,
            pinItemRequestWrapper: PinItemRequestWrapper,
            gestureSettings: GestureSettings,
            homeSettings: HomeSettings,
            androidAppWidgetHostWrapper: AndroidAppWidgetHostWrapper,
            androidAppWidgetManagerWrapper: AndroidAppWidgetManagerWrapper,
            androidWallpaperManagerWrapper: AndroidWallpaperManagerWrapper,
            density: Density,
            experimentalSettings: ExperimentalSettings,
            onGetPinGridItem: (PinItemRequestType) -> Unit,
            onResetPinGridItem: () -> Unit,
            onMoveFolderGridItem: (folderGridItem: GridItem, applicationInfoGridItems: List<ApplicationInfoGridItem>, movingApplicationInfoGridItem: ApplicationInfoGridItem, dragX: Int, dragY: Int, columns: Int, rows: Int, gridWidth: Int, gridHeight: Int, currentPage: Int) -> Unit,
            onMoveFolderGridItemOutsideFolder: (folderGridItem: GridItem, movingApplicationInfoGridItem: ApplicationInfoGridItem, applicationInfoGridItems: List<ApplicationInfoGridItem>) -> Unit,
            onMoveGridItem: (movingGridItem: GridItem, x: Int, y: Int, columns: Int, rows: Int, gridWidth: Int, gridHeight: Int) -> Unit,
            onDeleteGridItemCache: (GridItem) -> Unit,
            onDragCancelAfterMove: () -> Unit,
            onDragEndAfterMove: (MoveGridItemResult) -> Unit,
            onDragEndAfterMoveFolder: () -> Unit,
            onDeleteWidgetGridItemCache: (gridItem: GridItem, appWidgetId: Int) -> Unit,
            onShowFolderWhenDragging: (
                id: String,
                conflictingGridItem: GridItem,
                movingGridItem: GridItem,
            ) -> Unit,
            onUpdateFolderGridItemId: (String?) -> Unit,
            onDraggingGridItem: (List<GridItem>) -> Unit,
        ): Saver<PagerScreenState, *> = listSaver(
            save = {
                listOf(
                    it.lastSwipeUpY,
                    it.lastSwipeDownY,
                    it.lastFolderPopupX,
                    it.lastFolderPopupY,
                    it.lastFolderPopupWidth,
                    it.lastFolderPopupHeight,
                )
            },
            restore = { saved ->
                PagerScreenState(
                    initialSwipeUpY = saved[0] as Float,
                    initialSwipeDownY = saved[1] as Float,
                    initialFolderX = saved[2] as Int,
                    initialFolderY = saved[3] as Int,
                    initialFolderWidth = saved[4] as Int,
                    initialFolderHeight = saved[5] as Int,
                    screenWidth = screenWidth,
                    screenHeight = screenHeight,
                    fileManager = fileManager,
                    androidImageSerializer = androidImageSerializer,
                    androidLauncherAppsWrapper = androidLauncherAppsWrapper,
                    scope = scope,
                    context = context,
                    androidUserManagerWrapper = androidUserManagerWrapper,
                    pinItemRequestWrapper = pinItemRequestWrapper,
                    gestureSettings = gestureSettings,
                    homeSettings = homeSettings,
                    androidAppWidgetHostWrapper = androidAppWidgetHostWrapper,
                    androidAppWidgetManagerWrapper = androidAppWidgetManagerWrapper,
                    androidWallpaperManagerWrapper = androidWallpaperManagerWrapper,
                    density = density,
                    experimentalSettings = experimentalSettings,
                    onGetPinGridItem = onGetPinGridItem,
                    onResetPinGridItem = onResetPinGridItem,
                    onMoveFolderGridItem = onMoveFolderGridItem,
                    onMoveFolderGridItemOutsideFolder = onMoveFolderGridItemOutsideFolder,
                    onMoveGridItem = onMoveGridItem,
                    onDeleteGridItemCache = onDeleteGridItemCache,
                    onDragCancelAfterMove = onDragCancelAfterMove,
                    onDragEndAfterMove = onDragEndAfterMove,
                    onDragEndAfterMoveFolder = onDragEndAfterMoveFolder,
                    onDeleteWidgetGridItemCache = onDeleteWidgetGridItemCache,
                    onShowFolderWhenDragging = onShowFolderWhenDragging,
                    onUpdateFolderGridItemId = onUpdateFolderGridItemId,
                    onDraggingGridItem = onDraggingGridItem,
                )
            },
        )
    }
}

@Composable
internal fun rememberPagerScreenState(
    gestureSettings: GestureSettings,
    homeSettings: HomeSettings,
    screenHeight: Int,
    screenWidth: Int,
    experimentalSettings: ExperimentalSettings,
    onDeleteGridItemCache: (GridItem) -> Unit,
    onDeleteWidgetGridItemCache: (
        gridItem: GridItem,
        appWidgetId: Int,
    ) -> Unit,
    onDragCancelAfterMove: () -> Unit,
    onDragEndAfterMove: (MoveGridItemResult) -> Unit,
    onDragEndAfterMoveFolder: () -> Unit,
    onDraggingGridItem: (List<GridItem>) -> Unit,
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
    onResetPinGridItem: () -> Unit,
    onShowFolderWhenDragging: (
        id: String,
        conflictingGridItem: GridItem,
        movingGridItem: GridItem,
    ) -> Unit,
    onUpdateFolderGridItemId: (String?) -> Unit,
): PagerScreenState {
    val scope = rememberCoroutineScope()

    val context = LocalContext.current

    val androidLauncherAppsWrapper = LocalLauncherApps.current

    val androidWallpaperManagerWrapper = LocalWallpaperManager.current

    val density = LocalDensity.current

    val androidAppWidgetManagerWrapper = LocalAppWidgetManager.current

    val androidUserManagerWrapper = LocalUserManager.current

    val androidImageSerializer = LocalImageSerializer.current

    val fileManager = LocalFileManager.current

    val androidAppWidgetHostWrapper = LocalAppWidgetHost.current

    val pinItemRequestWrapper = LocalPinItemRequest.current

    return rememberSaveable(
        saver = PagerScreenState.Saver(
            screenWidth = screenWidth,
            screenHeight = screenHeight,
            fileManager = fileManager,
            androidImageSerializer = androidImageSerializer,
            androidLauncherAppsWrapper = androidLauncherAppsWrapper,
            scope = scope,
            context = context,
            androidUserManagerWrapper = androidUserManagerWrapper,
            pinItemRequestWrapper = pinItemRequestWrapper,
            gestureSettings = gestureSettings,
            homeSettings = homeSettings,
            androidAppWidgetHostWrapper = androidAppWidgetHostWrapper,
            androidAppWidgetManagerWrapper = androidAppWidgetManagerWrapper,
            androidWallpaperManagerWrapper = androidWallpaperManagerWrapper,
            density = density,
            experimentalSettings = experimentalSettings,
            onGetPinGridItem = onGetPinGridItem,
            onResetPinGridItem = onResetPinGridItem,
            onMoveFolderGridItem = onMoveFolderGridItem,
            onMoveFolderGridItemOutsideFolder = onMoveFolderGridItemOutsideFolder,
            onMoveGridItem = onMoveGridItem,
            onDeleteGridItemCache = onDeleteGridItemCache,
            onDragCancelAfterMove = onDragCancelAfterMove,
            onDragEndAfterMove = onDragEndAfterMove,
            onDragEndAfterMoveFolder = onDragEndAfterMoveFolder,
            onDeleteWidgetGridItemCache = onDeleteWidgetGridItemCache,
            onShowFolderWhenDragging = onShowFolderWhenDragging,
            onUpdateFolderGridItemId = onUpdateFolderGridItemId,
            onDraggingGridItem = onDraggingGridItem,
        ),
    ) {
        PagerScreenState(
            initialSwipeUpY = screenHeight.toFloat(),
            initialSwipeDownY = screenHeight.toFloat(),
            initialFolderX = 0,
            initialFolderY = 0,
            initialFolderWidth = 0,
            initialFolderHeight = 0,
            screenWidth = screenWidth,
            screenHeight = screenHeight,
            fileManager = fileManager,
            androidImageSerializer = androidImageSerializer,
            androidLauncherAppsWrapper = androidLauncherAppsWrapper,
            scope = scope,
            context = context,
            androidUserManagerWrapper = androidUserManagerWrapper,
            pinItemRequestWrapper = pinItemRequestWrapper,
            gestureSettings = gestureSettings,
            homeSettings = homeSettings,
            androidAppWidgetHostWrapper = androidAppWidgetHostWrapper,
            androidAppWidgetManagerWrapper = androidAppWidgetManagerWrapper,
            androidWallpaperManagerWrapper = androidWallpaperManagerWrapper,
            density = density,
            experimentalSettings = experimentalSettings,
            onGetPinGridItem = onGetPinGridItem,
            onResetPinGridItem = onResetPinGridItem,
            onMoveFolderGridItem = onMoveFolderGridItem,
            onMoveFolderGridItemOutsideFolder = onMoveFolderGridItemOutsideFolder,
            onMoveGridItem = onMoveGridItem,
            onDeleteGridItemCache = onDeleteGridItemCache,
            onDragCancelAfterMove = onDragCancelAfterMove,
            onDragEndAfterMove = onDragEndAfterMove,
            onDragEndAfterMoveFolder = onDragEndAfterMoveFolder,
            onDeleteWidgetGridItemCache = onDeleteWidgetGridItemCache,
            onShowFolderWhenDragging = onShowFolderWhenDragging,
            onUpdateFolderGridItemId = onUpdateFolderGridItemId,
            onDraggingGridItem = onDraggingGridItem,
        )
    }
}
