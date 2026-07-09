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
import android.graphics.Rect
import android.os.Build
import android.os.IBinder
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
import androidx.compose.runtime.State
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
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import com.eblan.launcher.domain.common.IconKeyGenerator
import com.eblan.launcher.domain.framework.FileManager
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
import com.eblan.launcher.ui.local.LocalIconKeyGenerator
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
    private val iconKeyGenerator: IconKeyGenerator,
    private val onGetPinGridItem: (PinItemRequestType) -> Unit,
    private val onResetPinGridItem: () -> Unit,
) {
    private var lastSwipeUpY by mutableFloatStateOf(initialSwipeUpY)

    private var lastSwipeDownY by mutableFloatStateOf(initialSwipeDownY)

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

    val widgetScreenSwipeY = Animatable(screenHeight.toFloat())

    val widgetScreenAlpha by derivedStateOf {
        ((screenHeight - widgetScreenSwipeY.value) / (screenHeight / 2)).coerceIn(0f, 1f)
    }

    val widgetScreenCornerSize by derivedStateOf {
        val progress = (widgetScreenSwipeY.value / screenHeight).coerceIn(0f, 1f)

        (20 * progress).dp
    }

    val shortcutConfigScreenSwipeY = Animatable(screenHeight.toFloat())

    val shortcutConfigScreenAlpha by derivedStateOf {
        ((screenHeight - shortcutConfigScreenSwipeY.value) / (screenHeight / 2)).coerceIn(0f, 1f)
    }

    val shortcutConfigScreenCornerSize by derivedStateOf {
        val progress = (shortcutConfigScreenSwipeY.value / screenHeight).coerceIn(0f, 1f)

        (20 * progress).dp
    }

    val appWidgetScreenSwipeY = Animatable(screenHeight.toFloat())

    var isCloseGridItemPopup by mutableStateOf(false)
        private set

    var isCloseFolderGridItemPopup by mutableStateOf(false)
        private set

    var showWidgetScreen by mutableStateOf(false)
        private set

    var showShortcutConfigScreen by mutableStateOf(false)
        private set

    private val touchSlop = with(density) {
        50.dp.toPx()
    }

    private var accumulatedDragOffset by mutableStateOf(Offset.Zero)

    private var lastAppWidgetId by mutableIntStateOf(AppWidgetManager.INVALID_APPWIDGET_ID)

    suspend fun handlePinGridItemEffect(
        pinGridItem: GridItem?,
        onUpdateGridItemSource: (GridItemSource) -> Unit,
        onUpdateIsVisibleOverlay: (Boolean) -> Unit,
        onUpdateMoveGridItemResult: (MoveGridItemResult) -> Unit,
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
            GridItemSource.Pin(pinItemRequest = pinItemRequest),
        )

        onUpdateMoveGridItemResult(
            MoveGridItemResult(
                isSuccess = false,
                movingGridItem = pinGridItem,
                conflictingGridItem = null,
            ),
        )

        onUpdateIsVisibleOverlay(true)

        isDragging = true
    }

    suspend fun handleDragGridItemEffect(
        gridCurrentPage: Int,
        dockGridCurrentPage: Int,
        density: Density,
        dockHeight: Dp,
        isGridScrollInProgress: Boolean,
        isDockScrollInProgress: Boolean,
        lockMovement: Boolean,
        paddingValues: PaddingValues,
        gridItemSource: State<GridItemSource?>,
        isVisibleOverlay: State<Boolean>,
        moveGridItemResult: State<MoveGridItemResult?>,
        layoutDirection: LayoutDirection,
        onMoveGridItem: (
            movingGridItem: GridItem,
            x: Int,
            y: Int,
            columns: Int,
            rows: Int,
            gridWidth: Int,
            gridHeight: Int,
        ) -> Unit,
    ) {
        handleDragGridItem(
            columns = homeSettings.columns,
            gridCurrentPage = gridCurrentPage,
            dockGridCurrentPage = dockGridCurrentPage,
            density = density,
            dockColumns = homeSettings.dockColumns,
            dockHeight = dockHeight,
            dockRows = homeSettings.dockRows,
            drag = drag,
            dragIntOffset = dragIntOffset,
            gridItemSource = gridItemSource,
            isDragging = isDragging,
            isVisibleOverlay = isVisibleOverlay,
            isGridScrollInProgress = isGridScrollInProgress,
            isDockScrollInProgress = isDockScrollInProgress,
            lockMovement = lockMovement,
            paddingValues = paddingValues,
            rows = homeSettings.rows,
            screenHeight = screenHeight,
            screenWidth = screenWidth,
            moveGridItemResult = moveGridItemResult,
            layoutDirection = layoutDirection,
            onMoveGridItem = onMoveGridItem,
            onUpdateAssociate = {
                associate = it
            },
            onUpdateSharedElementKey = {
                sharedElementKey = it
            },
        )
    }

    suspend fun handleDropGridItemEffect(
        moveGridItemResult: State<MoveGridItemResult?>,
        onLaunchShortcutConfigIntent: (Intent) -> Unit,
        onLaunchShortcutConfigIntentSenderRequest: (IntentSenderRequest) -> Unit,
        onLaunchWidgetIntent: (Intent) -> Unit,
        gridItemSource: State<GridItemSource?>,
        isVisibleOverlay: State<Boolean>,
        onUpdateIsVisibleOverlay: (Boolean) -> Unit,
        onResetGridAfterDeleteGridItem: (GridItem) -> Unit,
        onResetGrid: () -> Unit,
        onUpdateGridItemsAfterMove: (MoveGridItemResult) -> Unit,
    ) {
        handleDropGridItem(
            androidAppWidgetHostWrapper = androidAppWidgetHostWrapper,
            androidAppWidgetManagerWrapper = androidAppWidgetManagerWrapper,
            androidLauncherAppsWrapper = androidLauncherAppsWrapper,
            androidUserManagerWrapper = androidUserManagerWrapper,
            context = context,
            drag = drag,
            gridItemSource = gridItemSource,
            isDragging = isDragging,
            isVisibleOverlay = isVisibleOverlay,
            moveGridItemResult = moveGridItemResult,
            lockMovement = experimentalSettings.lockMovement,
            onResetGridAfterDeleteGridItem = onResetGridAfterDeleteGridItem,
            onResetGrid = onResetGrid,
            onUpdateGridItemsAfterMove = onUpdateGridItemsAfterMove,
            onLaunchShortcutConfigIntent = onLaunchShortcutConfigIntent,
            onLaunchShortcutConfigIntentSenderRequest = onLaunchShortcutConfigIntentSenderRequest,
            onLaunchWidgetIntent = onLaunchWidgetIntent,
            onUpdateAppWidgetId = {
                lastAppWidgetId = it
            },
            onUpdateIsDragging = {
                isDragging = it
            },
            onUpdateWidgetGridItem = {
                updatedWidgetGridItem = it
            },
            onUpdateIsVisibleOverlay = onUpdateIsVisibleOverlay,
        )
    }

    fun handleDeleteAppWidgetIdEffect(
        moveGridItemResult: MoveGridItemResult?,
        onResetGridAfterDeleteGridItem: (GridItem) -> Unit,
    ) {
        handleDeleteAppWidgetId(
            appWidgetId = lastAppWidgetId,
            deleteAppWidgetId = deleteAppWidgetId,
            moveGridItemResult = moveGridItemResult,
            onResetGridAfterDeleteGridItem = onResetGridAfterDeleteGridItem,
            onResetAppWidgetId = {
                lastAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

                deleteAppWidgetId = false
            },
        )
    }

    fun handleAnimateScrollToPageEffect(
        density: Density,
        paddingValues: PaddingValues,
        gridItemSource: State<GridItemSource?>,
        layoutDirection: LayoutDirection,
    ) {
        handleAnimateScrollToPage(
            associate = associate,
            density = density,
            dragIntOffset = dragIntOffset,
            gridItemSource = gridItemSource,
            isDragging = isDragging,
            paddingValues = paddingValues,
            screenWidth = screenWidth,
            layoutDirection = layoutDirection,
            onUpdateDockPageDirection = {
                dockPageDirection = it
            },
            onUpdateGridPageDirection = {
                gridPageDirection = it
            },
        )
    }

    fun handleHasDoubleTap() {
        if (!hasDoubleTap) return

        handleEblanAction(
            context = context,
            eblanAction = gestureSettings.doubleTap,
            launcherApps = androidLauncherAppsWrapper,
            onOpenAppDrawer = {
                scope.launch {
                    swipeY.animateTo(
                        targetValue = 0f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessLow,
                        ),
                    )
                }
            },
        )

        hasDoubleTap = false
    }

    fun handleNewIntent(
        dockGridHorizontalPagerState: PagerState,
        gridHorizontalPagerState: PagerState,
        intent: Intent,
        windowToken: IBinder,
    ) {
        handleActionMainIntent(
            dockGridHorizontalPagerState = dockGridHorizontalPagerState,
            gridHorizontalPagerState = gridHorizontalPagerState,
            intent = intent,
            windowToken = windowToken,
        )

        handleEblanActionIntent(intent = intent)
    }

    fun handleAppWidgetLauncherResult(
        moveGridItemResult: MoveGridItemResult?,
        result: ActivityResult,
    ) {
        handleAppWidgetLauncherResult(
            androidAppWidgetManagerWrapper = androidAppWidgetManagerWrapper,
            moveGridItemResult = moveGridItemResult,
            result = result,
            onDeleteAppWidgetId = {
                deleteAppWidgetId = true
            },
            onUpdateWidgetGridItem = {
                updatedWidgetGridItem = it
            },
        )
    }

    fun swipeEblanAction() {
        val swipeThreshold = 100f

        if (swipeUpY.value < screenHeight - swipeThreshold) {
            handleEblanAction(
                context = context,
                eblanAction = gestureSettings.swipeUp,
                launcherApps = androidLauncherAppsWrapper,
                onOpenAppDrawer = {},
            )
        }

        if (swipeDownY.value < screenHeight - swipeThreshold) {
            handleEblanAction(
                context = context,
                eblanAction = gestureSettings.swipeDown,
                launcherApps = androidLauncherAppsWrapper,
                onOpenAppDrawer = {},
            )
        }
    }

    fun resetSwipeOffset() {
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

    fun handleActionMainIntent(
        dockGridHorizontalPagerState: PagerState,
        gridHorizontalPagerState: PagerState,
        intent: Intent,
        windowToken: IBinder,
    ) {
        if (intent.action != Intent.ACTION_MAIN && !intent.hasCategory(Intent.CATEGORY_HOME)) {
            return
        }

        if ((intent.flags and Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT) != 0) {
            return
        }

        isPressHome = true

        if (swipeY.value < screenHeight.toFloat() ||
            widgetScreenSwipeY.value < screenHeight.toFloat() ||
            shortcutConfigScreenSwipeY.value < screenHeight.toFloat() ||
            eblanApplicationInfoGroup != null
        ) {
            return
        }

        animateScrollToPages(
            dockGridHorizontalPagerState = dockGridHorizontalPagerState,
            gridHorizontalPagerState = gridHorizontalPagerState,
        )

        if (homeSettings.wallpaperScroll) {
            val page = calculatePage(
                index = gridHorizontalPagerState.currentPage,
                infiniteScroll = homeSettings.infiniteScroll,
                pageCount = homeSettings.pageCount,
            )

            androidWallpaperManagerWrapper.setWallpaperOffsetSteps(
                xStep = 1f / (homeSettings.pageCount.toFloat() - 1),
                yStep = 1f,
            )

            androidWallpaperManagerWrapper.setWallpaperOffsets(
                windowToken = windowToken,
                xOffset = page / (homeSettings.pageCount.toFloat() - 1),
                yOffset = 0f,
            )
        }
    }

    fun animateScrollToPages(
        dockGridHorizontalPagerState: PagerState,
        gridHorizontalPagerState: PagerState,
    ) {
        fun getInfiniteScrollInitialPage(
            currentPage: Int,
            initialPage: Int,
            pageCount: Int,
            center: Int = Int.MAX_VALUE / 2,
        ): Int {
            var diff = initialPage - Math.floorMod(currentPage - center, pageCount)

            val halfCount = pageCount / 2

            if (diff > halfCount) {
                diff -= pageCount
            } else if (diff < -halfCount) {
                diff += pageCount
            }

            return currentPage + diff
        }

        scope.launch {
            gridHorizontalPagerState.animateScrollToPage(
                if (homeSettings.infiniteScroll) {
                    getInfiniteScrollInitialPage(
                        currentPage = gridHorizontalPagerState.currentPage,
                        initialPage = homeSettings.initialPage,
                        pageCount = homeSettings.pageCount,
                    )
                } else {
                    homeSettings.initialPage
                },
            )
        }

        scope.launch {
            dockGridHorizontalPagerState.animateScrollToPage(
                if (homeSettings.dockInfiniteScroll) {
                    getInfiniteScrollInitialPage(
                        currentPage = dockGridHorizontalPagerState.currentPage,
                        initialPage = homeSettings.dockInitialPage,
                        pageCount = homeSettings.dockPageCount,
                    )
                } else {
                    homeSettings.dockInitialPage
                },
            )
        }
    }

    fun handleEblanActionIntent(intent: Intent) {
        if (intent.action != EblanAction.ACTION) return

        val eblanAction = intent.getStringExtra(EblanAction.NAME)?.let { eblanAction ->
            Json.decodeFromString<EblanAction>(eblanAction)
        } ?: return

        handleEblanAction(
            context = context,
            eblanAction = eblanAction,
            launcherApps = androidLauncherAppsWrapper,
            onOpenAppDrawer = {
                scope.launch {
                    swipeY.animateTo(
                        targetValue = 0f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessLow,
                        ),
                    )
                }
            },
        )
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

        isCloseGridItemPopup = false
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

        isCloseFolderGridItemPopup = false
    }

    fun updateIsDragging(value: Boolean) {
        isDragging = value
    }

    fun updateIsResizing(value: Boolean) {
        isResizing = value
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

    fun resize(
        resizeGridItem: GridItem,
        onUpdateResizeGridItem: (GridItem) -> Unit,
    ) {
        isResizing = true

        onUpdateResizeGridItem(resizeGridItem)
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

    fun verticalDragApplicationScreen(dragAmount: Float) {
        scope.launch {
            swipeY.snapTo((swipeY.value + dragAmount).coerceIn(0f, screenHeight.toFloat()))
        }
    }

    fun openWidgetScreen() {
        scope.launch {
            showWidgetScreen = true

            widgetScreenSwipeY.animateTo(
                targetValue = 0f,
                animationSpec = tween(
                    easing = FastOutSlowInEasing,
                ),
            )
        }
    }

    fun dismissWidgetScreen() {
        scope.launch {
            widgetScreenSwipeY.animateTo(
                targetValue = screenHeight.toFloat(),
                animationSpec = tween(
                    easing = FastOutSlowInEasing,
                ),
            )

            showWidgetScreen = false

            if (isPressHome) {
                isPressHome = false
            }
        }
    }

    fun verticalDragWidgetScreen(dragAmount: Float) {
        scope.launch {
            widgetScreenSwipeY.snapTo(
                (widgetScreenSwipeY.value + dragAmount).coerceIn(
                    0f,
                    screenHeight.toFloat(),
                ),
            )
        }
    }

    fun verticalDragShortcutConfigScreen(dragAmount: Float) {
        scope.launch {
            shortcutConfigScreenSwipeY.snapTo(
                (shortcutConfigScreenSwipeY.value + dragAmount).coerceIn(
                    0f,
                    screenHeight.toFloat(),
                ),
            )
        }
    }

    fun openShortcutConfigScreen() {
        scope.launch {
            showShortcutConfigScreen = true

            shortcutConfigScreenSwipeY.animateTo(
                targetValue = 0f,
                animationSpec = tween(
                    easing = FastOutSlowInEasing,
                ),
            )
        }
    }

    fun dismissShortcutConfigScreen() {
        scope.launch {
            shortcutConfigScreenSwipeY.animateTo(
                targetValue = screenHeight.toFloat(),
                animationSpec = tween(
                    easing = FastOutSlowInEasing,
                ),
            )

            showShortcutConfigScreen = false

            if (isPressHome) {
                isPressHome = false
            }
        }
    }

    fun dismissAppWidgetScreen() {
        scope.launch {
            appWidgetScreenSwipeY.animateTo(
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

            appWidgetScreenSwipeY.animateTo(
                targetValue = 0f,
                animationSpec = tween(
                    easing = FastOutSlowInEasing,
                ),
            )
        }
    }

    fun startAppDetailsActivity(
        left: Int?,
        top: Int?,
        width: Int?,
        height: Int?,
        serialNumber: Long,
        componentName: String,
    ) {
        if (left != null && top != null && width != null && height != null) {
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
    }

    fun startPopupShortcut(
        leftPadding: Int,
        topPadding: Int,
        serialNumber: Long,
        packageName: String,
        shortcutId: String,
    ) {
        val x = popupIntOffset?.x

        val y = popupIntOffset?.y

        val width = popupIntSize?.width

        val height = popupIntSize?.height

        if (x != null && y != null && width != null && height != null) {
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
        }
    }

    fun verticalDragAppWidgetScreen(dragAmount: Float) {
        scope.launch {
            appWidgetScreenSwipeY.snapTo(
                (appWidgetScreenSwipeY.value + dragAmount).coerceIn(
                    0f,
                    screenHeight.toFloat(),
                ),
            )
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
                                    iconKeyGenerator.getHashedName(name = componentName),
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
                        val serialNumber =
                            androidUserManagerWrapper.getSerialNumberForUser(userHandle = shortcutInfo.userHandle)

                        val icon = androidLauncherAppsWrapper.getShortcutBadgedIconDrawable(
                            shortcutInfo = shortcutInfo,
                            density = 0,
                        )?.let { drawable ->
                            val directory = fileManager.getFilesDirectory(FileManager.SHORTCUTS_DIR)

                            val file = File(
                                directory,
                                iconKeyGenerator.getShortcutIconKey(
                                    serialNumber = serialNumber,
                                    packageName = shortcutInfo.`package`,
                                    id = shortcutInfo.id,
                                ),
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

    fun updateIsCloseGridItemPopup(value: Boolean) {
        isCloseGridItemPopup = value
    }

    fun updateIsCloseFolderGridItemPopup(value: Boolean) {
        isCloseFolderGridItemPopup = value
    }

    fun handleOnDragEndApplicationScreen() {
        handleApplyFling(swipeY = swipeY)
    }

    fun handleOnDragEndWidgetScreen() {
        handleApplyFling(swipeY = widgetScreenSwipeY)
    }

    fun handleOnDragEndShortcutConfigScreen() {
        handleApplyFling(swipeY = shortcutConfigScreenSwipeY)
    }

    fun handleOnDragEndAppWidgetScreen() {
        scope.launch {
            if (appWidgetScreenSwipeY.value > 200f) {
                appWidgetScreenSwipeY.animateTo(
                    targetValue = screenHeight.toFloat(),
                    animationSpec = tween(easing = FastOutSlowInEasing),
                )
            } else {
                appWidgetScreenSwipeY.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(easing = FastOutSlowInEasing),
                )
            }
        }
    }

    private fun handleApplyFling(swipeY: Animatable<Float, AnimationVector1D>) {
        scope.launch {
            if (swipeY.value > 200f) {
                swipeY.animateTo(
                    targetValue = screenHeight.toFloat(),
                    animationSpec = tween(easing = FastOutSlowInEasing),
                )
            } else {
                swipeY.animateTo(
                    targetValue = 0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessLow,
                    ),
                )
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
            iconKeyGenerator: IconKeyGenerator,
            onGetPinGridItem: (PinItemRequestType) -> Unit,
            onResetPinGridItem: () -> Unit,
        ): Saver<PagerScreenState, *> = listSaver(
            save = {
                listOf(
                    it.lastSwipeUpY,
                    it.lastSwipeDownY,
                )
            },
            restore = {
                PagerScreenState(
                    initialSwipeUpY = it[0],
                    initialSwipeDownY = it[1],
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
                    iconKeyGenerator = iconKeyGenerator,
                    onGetPinGridItem = onGetPinGridItem,
                    onResetPinGridItem = onResetPinGridItem,
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
    onGetPinGridItem: (PinItemRequestType) -> Unit,
    onResetPinGridItem: () -> Unit,
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

    val iconKeyGenerator = LocalIconKeyGenerator.current

    return rememberSaveable(
        screenWidth,
        screenHeight,
        gestureSettings,
        homeSettings,
        experimentalSettings,
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
            iconKeyGenerator = iconKeyGenerator,
            onGetPinGridItem = onGetPinGridItem,
            onResetPinGridItem = onResetPinGridItem,
        ),
    ) {
        PagerScreenState(
            initialSwipeUpY = screenHeight.toFloat(),
            initialSwipeDownY = screenHeight.toFloat(),
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
            iconKeyGenerator = iconKeyGenerator,
            onGetPinGridItem = onGetPinGridItem,
            onResetPinGridItem = onResetPinGridItem,
        )
    }
}
