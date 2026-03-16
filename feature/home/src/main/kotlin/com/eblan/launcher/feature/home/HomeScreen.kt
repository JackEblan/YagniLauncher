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
package com.eblan.launcher.feature.home

import android.Manifest
import android.content.pm.ActivityInfo
import android.os.Build
import androidx.activity.compose.LocalActivity
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eblan.launcher.domain.model.AppDrawerSettings
import com.eblan.launcher.domain.model.ApplicationInfoGridItem
import com.eblan.launcher.domain.model.Associate
import com.eblan.launcher.domain.model.EblanAppWidgetProviderInfo
import com.eblan.launcher.domain.model.EblanApplicationInfo
import com.eblan.launcher.domain.model.EblanApplicationInfoGroup
import com.eblan.launcher.domain.model.EblanApplicationInfoTag
import com.eblan.launcher.domain.model.EblanShortcutConfig
import com.eblan.launcher.domain.model.EblanShortcutInfo
import com.eblan.launcher.domain.model.EblanShortcutInfoByGroup
import com.eblan.launcher.domain.model.EblanUser
import com.eblan.launcher.domain.model.EditPageData
import com.eblan.launcher.domain.model.GetEblanApplicationInfosByLabel
import com.eblan.launcher.domain.model.GridItem
import com.eblan.launcher.domain.model.GridItemData
import com.eblan.launcher.domain.model.HomeData
import com.eblan.launcher.domain.model.MoveGridItemResult
import com.eblan.launcher.domain.model.PageItem
import com.eblan.launcher.domain.model.PinItemRequestType
import com.eblan.launcher.feature.home.model.HomeUiState
import com.eblan.launcher.feature.home.model.Screen
import com.eblan.launcher.feature.home.screen.editpage.EditPageScreen
import com.eblan.launcher.feature.home.screen.loading.LoadingScreen
import com.eblan.launcher.feature.home.screen.pager.PagerScreen
import com.eblan.launcher.ui.dialog.TextDialog
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale

@Composable
internal fun HomeRoute(
    modifier: Modifier = Modifier,
    configureResultCode: Int?,
    viewModel: HomeViewModel = hiltViewModel(),
    onEditApplicationInfo: (
        serialNumber: Long,
        componentName: String,
    ) -> Unit,
    onEditGridItem: (String) -> Unit,
    onResetConfigureResultCode: () -> Unit,
    onSettings: () -> Unit,
) {
    val homeUiState by viewModel.homeUiState.collectAsStateWithLifecycle()

    val screen by viewModel.screen.collectAsStateWithLifecycle()

    val movedGridItemResult by viewModel.movedGridItemResult.collectAsStateWithLifecycle()

    val editPageData by viewModel.editPageData.collectAsStateWithLifecycle()

    val pinGridItem by viewModel.pinGridItem.collectAsStateWithLifecycle()

    val getEblanApplicationInfos by viewModel.getEblanApplicationInfosByLabel.collectAsStateWithLifecycle()

    val eblanShortcutConfigs by viewModel.eblanShortcutConfigs.collectAsStateWithLifecycle()

    val eblanAppWidgetProviderInfos by viewModel.eblanAppWidgetProviderInfos.collectAsStateWithLifecycle()

    val eblanShortcutInfosGroup by viewModel.eblanShortcutInfosGroup.collectAsStateWithLifecycle()

    val eblanAppWidgetProviderInfosGroup by viewModel.eblanAppWidgetProviderInfosGroup.collectAsStateWithLifecycle()

    val iconPackFilePaths by viewModel.iconPackFilePaths.collectAsStateWithLifecycle()

    val eblanApplicationInfoTags by viewModel.eblanApplicationInfoTags.collectAsStateWithLifecycle()

    val folderGridItem by viewModel.folderGridItem.collectAsStateWithLifecycle()

    val resizeGridItem by viewModel.resizeGridItem.collectAsStateWithLifecycle()

    HomeScreen(
        modifier = modifier,
        configureResultCode = configureResultCode,
        eblanAppWidgetProviderInfos = eblanAppWidgetProviderInfos,
        eblanAppWidgetProviderInfosGroup = eblanAppWidgetProviderInfosGroup,
        eblanApplicationInfoTags = eblanApplicationInfoTags,
        eblanShortcutConfigs = eblanShortcutConfigs,
        eblanShortcutInfosGroup = eblanShortcutInfosGroup,
        editPageData = editPageData,
        folderGridItem = folderGridItem,
        getEblanApplicationInfosByLabel = getEblanApplicationInfos,
        homeUiState = homeUiState,
        iconPackFilePaths = iconPackFilePaths,
        movedGridItemResult = movedGridItemResult,
        pinGridItem = pinGridItem,
        screen = screen,
        resizeGridItem = resizeGridItem,
        onCancelGridCache = viewModel::cancelGridCache,
        onDeleteApplicationInfoGridItem = viewModel::deleteApplicationInfoGridItem,
        onDeleteGridItem = viewModel::deleteGridItem,
        onDeleteGridItemCache = viewModel::deleteGridItemCache,
        onDeleteWidgetGridItemCache = viewModel::deleteWidgetGridItemCache,
        onEditApplicationInfo = onEditApplicationInfo,
        onEditGridItem = onEditGridItem,
        onEditPage = viewModel::showPageCache,
        onGetEblanAppWidgetProviderInfosByLabel = viewModel::getEblanAppWidgetProviderInfosByLabel,
        onGetEblanApplicationInfosByLabel = viewModel::getEblanApplicationInfosByLabel,
        onGetEblanApplicationInfosByTagIds = viewModel::getEblanApplicationInfosByTagId,
        onGetEblanShortcutConfigsByLabel = viewModel::getEblanShortcutConfigsByLabel,
        onGetPinGridItem = viewModel::getPinGridItem,
        onMoveFolderGridItem = viewModel::moveFolderGridItem,
        onMoveFolderGridItemOutsideFolder = viewModel::moveFolderGridItemOutsideFolder,
        onMoveGridItem = viewModel::moveGridItem,
        onResetConfigureResultCode = onResetConfigureResultCode,
        onResetGridCacheAfterMove = viewModel::resetGridCacheAfterMove,
        onResetGridCacheAfterMoveFolder = viewModel::resetGridCacheAfterMoveFolder,
        onResetGridCacheAfterMoveWidgetGridItem = viewModel::resetGridCacheAfterMoveWidgetGridItem,
        onResetGridCacheAfterResize = viewModel::resetGridCacheAfterResize,
        onResetPinGridItem = viewModel::resetPinGridItem,
        onResizeGridItem = viewModel::resizeGridItem,
        onSaveEditPage = viewModel::saveEditPage,
        onSettings = onSettings,
        onShowFolderWhenDragging = viewModel::showFolderWhenDragging,
        onShowGridCache = viewModel::showGridCache,
        onStartSyncData = viewModel::startSyncData,
        onStopSyncData = viewModel::stopSyncData,
        onUpdateAppDrawerSettings = viewModel::updateAppDrawerSettings,
        onUpdateEblanApplicationInfos = viewModel::updateEblanApplicationInfos,
        onUpdateFolderGridItemId = viewModel::updateFolderGridItemId,
        onUpdateScreen = viewModel::updateScreen,
        onUpdateShortcutConfigGridItemDataCache = viewModel::updateShortcutConfigGridItemDataCache,
        onUpdateShortcutConfigIntoShortcutInfoGridItem = viewModel::updateShortcutConfigIntoShortcutInfoGridItem,
    )
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun HomeScreen(
    modifier: Modifier = Modifier,
    configureResultCode: Int?,
    eblanAppWidgetProviderInfos: Map<EblanApplicationInfoGroup, List<EblanAppWidgetProviderInfo>>,
    eblanAppWidgetProviderInfosGroup: Map<String, List<EblanAppWidgetProviderInfo>>,
    eblanApplicationInfoTags: List<EblanApplicationInfoTag>,
    eblanShortcutConfigs: Map<EblanUser, Map<EblanApplicationInfoGroup, List<EblanShortcutConfig>>>,
    eblanShortcutInfosGroup: Map<EblanShortcutInfoByGroup, List<EblanShortcutInfo>>,
    editPageData: EditPageData?,
    folderGridItem: GridItem?,
    getEblanApplicationInfosByLabel: GetEblanApplicationInfosByLabel,
    homeUiState: HomeUiState,
    iconPackFilePaths: Map<String, String>,
    movedGridItemResult: MoveGridItemResult?,
    pinGridItem: GridItem?,
    screen: Screen,
    resizeGridItem: GridItem?,
    onCancelGridCache: () -> Unit,
    onDeleteApplicationInfoGridItem: (ApplicationInfoGridItem) -> Unit,
    onDeleteGridItem: (GridItem) -> Unit,
    onDeleteGridItemCache: (GridItem) -> Unit,
    onDeleteWidgetGridItemCache: (
        gridItem: GridItem,
        appWidgetId: Int,
    ) -> Unit,
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
    onResetGridCacheAfterMove: (MoveGridItemResult) -> Unit,
    onResetGridCacheAfterMoveFolder: () -> Unit,
    onResetGridCacheAfterMoveWidgetGridItem: (MoveGridItemResult) -> Unit,
    onResetGridCacheAfterResize: (GridItem) -> Unit,
    onResetPinGridItem: () -> Unit,
    onResizeGridItem: (
        gridItem: GridItem,
        columns: Int,
        rows: Int,
    ) -> Unit,
    onSaveEditPage: (
        id: Int,
        pageItems: List<PageItem>,
        pageItemsToDelete: List<PageItem>,
        associate: Associate,
    ) -> Unit,
    onSettings: () -> Unit,
    onShowFolderWhenDragging: (
        id: String,
        conflictingGridItem: GridItem,
        movingGridItem: GridItem,
    ) -> Unit,
    onShowGridCache: (List<GridItem>) -> Unit,
    onStartSyncData: () -> Unit,
    onStopSyncData: () -> Unit,
    onUpdateAppDrawerSettings: (AppDrawerSettings) -> Unit,
    onUpdateEblanApplicationInfos: (List<EblanApplicationInfo>) -> Unit,
    onUpdateFolderGridItemId: (String?) -> Unit,
    onUpdateScreen: (Screen) -> Unit,
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
    val paddingValues = WindowInsets.safeDrawing.asPaddingValues()

    var screenIntSize by remember { mutableStateOf(IntSize.Zero) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { intSize ->
                screenIntSize = intSize
            },
    ) {
        if (homeUiState is HomeUiState.Success && screenIntSize != IntSize.Zero) {
            Success(
                configureResultCode = configureResultCode,
                eblanAppWidgetProviderInfos = eblanAppWidgetProviderInfos,
                eblanAppWidgetProviderInfosGroup = eblanAppWidgetProviderInfosGroup,
                eblanApplicationInfoTags = eblanApplicationInfoTags,
                eblanShortcutConfigs = eblanShortcutConfigs,
                eblanShortcutInfosGroup = eblanShortcutInfosGroup,
                editPageData = editPageData,
                folderGridItem = folderGridItem,
                getEblanApplicationInfosByLabel = getEblanApplicationInfosByLabel,
                homeData = homeUiState.homeData,
                iconPackFilePaths = iconPackFilePaths,
                movedGridItemResult = movedGridItemResult,
                paddingValues = paddingValues,
                pinGridItem = pinGridItem,
                screen = screen,
                screenHeight = screenIntSize.height,
                screenWidth = screenIntSize.width,
                resizeGridItem = resizeGridItem,
                onCancelGridCache = onCancelGridCache,
                onDeleteApplicationInfoGridItem = onDeleteApplicationInfoGridItem,
                onDeleteGridItem = onDeleteGridItem,
                onDeleteGridItemCache = onDeleteGridItemCache,
                onDeleteWidgetGridItemCache = onDeleteWidgetGridItemCache,
                onEditApplicationInfo = onEditApplicationInfo,
                onEditGridItem = onEditGridItem,
                onEditPage = onEditPage,
                onGetEblanAppWidgetProviderInfosByLabel = onGetEblanAppWidgetProviderInfosByLabel,
                onGetEblanApplicationInfosByLabel = onGetEblanApplicationInfosByLabel,
                onGetEblanApplicationInfosByTagIds = onGetEblanApplicationInfosByTagIds,
                onGetEblanShortcutConfigsByLabel = onGetEblanShortcutConfigsByLabel,
                onGetPinGridItem = onGetPinGridItem,
                onMoveFolderGridItem = onMoveFolderGridItem,
                onMoveFolderGridItemOutsideFolder = onMoveFolderGridItemOutsideFolder,
                onMoveGridItem = onMoveGridItem,
                onResetConfigureResultCode = onResetConfigureResultCode,
                onResetGridCacheAfterMove = onResetGridCacheAfterMove,
                onResetGridCacheAfterMoveFolder = onResetGridCacheAfterMoveFolder,
                onResetGridCacheAfterMoveWidgetGridItem = onResetGridCacheAfterMoveWidgetGridItem,
                onResetGridCacheAfterResize = onResetGridCacheAfterResize,
                onResetPinGridItem = onResetPinGridItem,
                onResizeGridItem = onResizeGridItem,
                onSaveEditPage = onSaveEditPage,
                onSettings = onSettings,
                onShowFolderWhenDragging = onShowFolderWhenDragging,
                onShowGridCache = onShowGridCache,
                onStartSyncData = onStartSyncData,
                onStopSyncData = onStopSyncData,
                onUpdateAppDrawerSettings = onUpdateAppDrawerSettings,
                onUpdateEblanApplicationInfos = onUpdateEblanApplicationInfos,
                onUpdateFolderGridItemId = onUpdateFolderGridItemId,
                onUpdateScreen = onUpdateScreen,
                onUpdateShortcutConfigGridItemDataCache = onUpdateShortcutConfigGridItemDataCache,
                onUpdateShortcutConfigIntoShortcutInfoGridItem = onUpdateShortcutConfigIntoShortcutInfoGridItem,
            )
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class, ExperimentalSharedTransitionApi::class)
@Composable
private fun Success(
    modifier: Modifier = Modifier,
    configureResultCode: Int?,
    eblanAppWidgetProviderInfos: Map<EblanApplicationInfoGroup, List<EblanAppWidgetProviderInfo>>,
    eblanAppWidgetProviderInfosGroup: Map<String, List<EblanAppWidgetProviderInfo>>,
    eblanApplicationInfoTags: List<EblanApplicationInfoTag>,
    eblanShortcutConfigs: Map<EblanUser, Map<EblanApplicationInfoGroup, List<EblanShortcutConfig>>>,
    eblanShortcutInfosGroup: Map<EblanShortcutInfoByGroup, List<EblanShortcutInfo>>,
    editPageData: EditPageData?,
    folderGridItem: GridItem?,
    getEblanApplicationInfosByLabel: GetEblanApplicationInfosByLabel,
    homeData: HomeData,
    iconPackFilePaths: Map<String, String>,
    movedGridItemResult: MoveGridItemResult?,
    paddingValues: PaddingValues,
    pinGridItem: GridItem?,
    screen: Screen,
    screenHeight: Int,
    screenWidth: Int,
    resizeGridItem: GridItem?,
    onCancelGridCache: () -> Unit,
    onDeleteApplicationInfoGridItem: (ApplicationInfoGridItem) -> Unit,
    onDeleteGridItem: (GridItem) -> Unit,
    onDeleteGridItemCache: (GridItem) -> Unit,
    onDeleteWidgetGridItemCache: (
        gridItem: GridItem,
        appWidgetId: Int,
    ) -> Unit,
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
    onResetGridCacheAfterMove: (MoveGridItemResult) -> Unit,
    onResetGridCacheAfterMoveFolder: () -> Unit,
    onResetGridCacheAfterMoveWidgetGridItem: (MoveGridItemResult) -> Unit,
    onResetGridCacheAfterResize: (GridItem) -> Unit,
    onResetPinGridItem: () -> Unit,
    onResizeGridItem: (
        gridItem: GridItem,
        columns: Int,
        rows: Int,
    ) -> Unit,
    onSaveEditPage: (
        id: Int,
        pageItems: List<PageItem>,
        pageItemsToDelete: List<PageItem>,
        associate: Associate,
    ) -> Unit,
    onSettings: () -> Unit,
    onShowFolderWhenDragging: (
        id: String,
        conflictingGridItem: GridItem,
        movingGridItem: GridItem,
    ) -> Unit,
    onShowGridCache: (List<GridItem>) -> Unit,
    onStartSyncData: () -> Unit,
    onStopSyncData: () -> Unit,
    onUpdateAppDrawerSettings: (AppDrawerSettings) -> Unit,
    onUpdateEblanApplicationInfos: (List<EblanApplicationInfo>) -> Unit,
    onUpdateFolderGridItemId: (String?) -> Unit,
    onUpdateScreen: (Screen) -> Unit,
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
    val activity = LocalActivity.current

    LaunchedEffect(key1 = Unit) {
        if (homeData.userData.homeSettings.lockScreenOrientation) {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED
        }
    }

    AnimatedContent(
        modifier = modifier,
        targetState = screen,
    ) { targetState ->
        when (targetState) {
            Screen.Pager -> {
                PagerScreen(
                    appDrawerSettings = homeData.userData.appDrawerSettings,
                    configureResultCode = configureResultCode,
                    dockGridItemsByPage = homeData.dockGridItemsByPage,
                    eblanAppWidgetProviderInfos = eblanAppWidgetProviderInfos,
                    eblanAppWidgetProviderInfosGroup = eblanAppWidgetProviderInfosGroup,
                    eblanApplicationInfoTags = eblanApplicationInfoTags,
                    eblanShortcutConfigs = eblanShortcutConfigs,
                    eblanShortcutInfosGroup = eblanShortcutInfosGroup,
                    experimentalSettings = homeData.userData.experimentalSettings,
                    folderGridItem = folderGridItem,
                    gestureSettings = homeData.userData.gestureSettings,
                    getEblanApplicationInfosByLabel = getEblanApplicationInfosByLabel,
                    gridItems = homeData.gridItems,
                    gridItemsByPage = homeData.gridItemsByPage,
                    hasShortcutHostPermission = homeData.hasShortcutHostPermission,
                    hasSystemFeatureAppWidgets = homeData.hasSystemFeatureAppWidgets,
                    homeSettings = homeData.userData.homeSettings,
                    iconPackFilePaths = iconPackFilePaths,
                    lockMovement = homeData.userData.experimentalSettings.lockMovement,
                    moveGridItemResult = movedGridItemResult,
                    paddingValues = paddingValues,
                    pinGridItem = pinGridItem,
                    screenHeight = screenHeight,
                    screenWidth = screenWidth,
                    textColor = homeData.textColor,
                    resizeGridItem = resizeGridItem,
                    onDeleteApplicationInfoGridItem = onDeleteApplicationInfoGridItem,
                    onDeleteGridItem = onDeleteGridItem,
                    onDeleteGridItemCache = onDeleteGridItemCache,
                    onDeleteWidgetGridItemCache = onDeleteWidgetGridItemCache,
                    onDragCancelAfterMove = onCancelGridCache,
                    onDragEndAfterMove = onResetGridCacheAfterMove,
                    onDragEndAfterMoveFolder = onResetGridCacheAfterMoveFolder,
                    onDragEndAfterMoveWidgetGridItem = onResetGridCacheAfterMoveWidgetGridItem,
                    onDraggingGridItem = onShowGridCache,
                    onEditApplicationInfo = onEditApplicationInfo,
                    onEditGridItem = onEditGridItem,
                    onEditPage = onEditPage,
                    onGetEblanAppWidgetProviderInfosByLabel = onGetEblanAppWidgetProviderInfosByLabel,
                    onGetEblanApplicationInfosByLabel = onGetEblanApplicationInfosByLabel,
                    onGetEblanApplicationInfosByTagIds = onGetEblanApplicationInfosByTagIds,
                    onGetEblanShortcutConfigsByLabel = onGetEblanShortcutConfigsByLabel,
                    onGetPinGridItem = onGetPinGridItem,
                    onMoveFolderGridItem = onMoveFolderGridItem,
                    onMoveFolderGridItemOutsideFolder = onMoveFolderGridItemOutsideFolder,
                    onMoveGridItem = onMoveGridItem,
                    onResetConfigureResultCode = onResetConfigureResultCode,
                    onResetPinGridItem = onResetPinGridItem,
                    onResizeCancel = onCancelGridCache,
                    onResizeEnd = onResetGridCacheAfterResize,
                    onResizeGridItem = onResizeGridItem,
                    onSettings = onSettings,
                    onShowFolderWhenDragging = onShowFolderWhenDragging,
                    onStartSyncData = onStartSyncData,
                    onStopSyncData = onStopSyncData,
                    onUpdateAppDrawerSettings = onUpdateAppDrawerSettings,
                    onUpdateEblanApplicationInfos = onUpdateEblanApplicationInfos,
                    onUpdateFolderGridItemId = onUpdateFolderGridItemId,
                    onUpdateShortcutConfigGridItemDataCache = onUpdateShortcutConfigGridItemDataCache,
                    onUpdateShortcutConfigIntoShortcutInfoGridItem = onUpdateShortcutConfigIntoShortcutInfoGridItem,
                )
            }

            Screen.Loading -> {
                LoadingScreen()
            }

            Screen.EditPage -> {
                EditPageScreen(
                    editPageData = editPageData,
                    hasShortcutHostPermission = homeData.hasShortcutHostPermission,
                    homeSettings = homeData.userData.homeSettings,
                    iconPackFilePaths = iconPackFilePaths,
                    paddingValues = paddingValues,
                    screenHeight = screenHeight,
                    textColor = homeData.textColor,
                    onSaveEditPage = onSaveEditPage,
                    onUpdateScreen = onUpdateScreen,
                )
            }
        }
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        PostNotificationPermissionEffect()
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun PostNotificationPermissionEffect(modifier: Modifier = Modifier) {
    val notificationsPermissionState =
        rememberPermissionState(permission = Manifest.permission.POST_NOTIFICATIONS)

    var showTextDialog by remember { mutableStateOf(false) }

    LaunchedEffect(key1 = notificationsPermissionState) {
        if (notificationsPermissionState.status.shouldShowRationale) {
            showTextDialog = true
        } else {
            notificationsPermissionState.launchPermissionRequest()
        }
    }

    if (showTextDialog) {
        TextDialog(
            modifier = modifier,
            title = "Notification Permission",
            text = "Allow notification permission so we can inform you about data sync status and important crash reports.",
            onClick = {
                notificationsPermissionState.launchPermissionRequest()

                showTextDialog = false
            },
            onDismissRequest = {
                showTextDialog = false
            },
        )
    }
}
