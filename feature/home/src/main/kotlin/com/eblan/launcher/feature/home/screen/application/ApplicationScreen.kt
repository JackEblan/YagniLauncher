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

import android.graphics.Rect
import android.os.Build
import android.os.UserHandle
import androidx.activity.compose.BackHandler
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberOverscrollEffect
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SearchBarValue
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.compose.ui.unit.sp
import coil3.compose.rememberAsyncImagePainter
import coil3.compose.rememberConstraintsSizeResolver
import coil3.request.ImageRequest
import coil3.request.addLastModifiedToFileCacheKey
import com.eblan.launcher.designsystem.icon.EblanLauncherIcons
import com.eblan.launcher.domain.model.AppDrawerSettings
import com.eblan.launcher.domain.model.Associate
import com.eblan.launcher.domain.model.EblanAction
import com.eblan.launcher.domain.model.EblanActionType
import com.eblan.launcher.domain.model.EblanAppWidgetProviderInfo
import com.eblan.launcher.domain.model.EblanApplicationInfo
import com.eblan.launcher.domain.model.EblanApplicationInfoGroup
import com.eblan.launcher.domain.model.EblanApplicationInfoOrder
import com.eblan.launcher.domain.model.EblanApplicationInfoTag
import com.eblan.launcher.domain.model.EblanShortcutInfo
import com.eblan.launcher.domain.model.EblanShortcutInfoByGroup
import com.eblan.launcher.domain.model.EblanUser
import com.eblan.launcher.domain.model.EblanUserType
import com.eblan.launcher.domain.model.GetEblanApplicationInfosByLabel
import com.eblan.launcher.domain.model.GridItem
import com.eblan.launcher.domain.model.GridItemData
import com.eblan.launcher.domain.model.ManagedProfileResult
import com.eblan.launcher.feature.home.component.scroll.OffsetNestedScrollConnection
import com.eblan.launcher.feature.home.component.scroll.OffsetOverscrollEffect
import com.eblan.launcher.feature.home.dialog.EblanApplicationInfoOrderDialog
import com.eblan.launcher.feature.home.model.Drag
import com.eblan.launcher.feature.home.model.GridItemSource
import com.eblan.launcher.feature.home.model.SharedElementKey
import com.eblan.launcher.feature.home.screen.application.draganddrop.DragAndDropEblanApplicationInfos
import com.eblan.launcher.feature.home.util.getHorizontalAlignment
import com.eblan.launcher.feature.home.util.getSystemTextColor
import com.eblan.launcher.feature.home.util.getVerticalArrangement
import com.eblan.launcher.framework.packagemanager.AndroidPackageManagerWrapper
import com.eblan.launcher.framework.usermanager.AndroidUserManagerWrapper
import com.eblan.launcher.ui.local.LocalLauncherApps
import com.eblan.launcher.ui.local.LocalPackageManager
import com.eblan.launcher.ui.local.LocalUserManager
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

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
    getEblanApplicationInfosByLabel: GetEblanApplicationInfosByLabel,
    hasShortcutHostPermission: Boolean,
    iconPackFilePaths: Map<String, String>,
    isPressHome: Boolean,
    managedProfileResult: ManagedProfileResult?,
    paddingValues: PaddingValues,
    screenHeight: Int,
    swipeY: Float,
    onDismiss: () -> Unit,
    onDragEnd: (Float) -> Unit,
    onDraggingGridItem: () -> Unit,
    onEditApplicationInfo: (
        serialNumber: Long,
        componentName: String,
    ) -> Unit,
    onGetEblanApplicationInfosByLabel: (String) -> Unit,
    onGetEblanApplicationInfosByTagIds: (List<Long>) -> Unit,
    onUpdateAppDrawerSettings: (AppDrawerSettings) -> Unit,
    onUpdateEblanApplicationInfos: (List<EblanApplicationInfo>) -> Unit,
    onUpdateGridItemSource: (GridItemSource) -> Unit,
    onUpdateImageBitmap: (ImageBitmap) -> Unit,
    onUpdateIsLongPressAndIsDragging: () -> Unit,
    onUpdateOverlayBounds: (
        intOffset: IntOffset,
        intSize: IntSize,
    ) -> Unit,
    onUpdateSharedElementKey: (SharedElementKey?) -> Unit,
    onVerticalDrag: (Float) -> Unit,
    onWidgets: (EblanApplicationInfoGroup) -> Unit,
    onDraggingShortcutInfoGridItem: () -> Unit,
    onUpdateAssociate: (Associate) -> Unit,
) {
    Surface(
        modifier = modifier
            .offset {
                IntOffset(x = 0, y = swipeY.roundToInt())
            }
            .fillMaxSize()
            .clip(RoundedCornerShape(cornerSize))
            .alpha(alpha),
    ) {
        Success(
            appDrawerSettings = appDrawerSettings,
            currentPage = currentPage,
            drag = drag,
            eblanAppWidgetProviderInfosGroup = eblanAppWidgetProviderInfosGroup,
            eblanApplicationInfoTags = eblanApplicationInfoTags,
            eblanShortcutInfosGroup = eblanShortcutInfosGroup,
            getEblanApplicationInfosByLabel = getEblanApplicationInfosByLabel,
            hasShortcutHostPermission = hasShortcutHostPermission,
            iconPackFilePaths = iconPackFilePaths,
            isPressHome = isPressHome,
            managedProfileResult = managedProfileResult,
            paddingValues = paddingValues,
            screenHeight = screenHeight,
            swipeY = swipeY,
            onDismiss = onDismiss,
            onDragEnd = onDragEnd,
            onDraggingGridItem = onDraggingGridItem,
            onEditApplicationInfo = onEditApplicationInfo,
            onGetEblanApplicationInfosByLabel = onGetEblanApplicationInfosByLabel,
            onGetEblanApplicationInfosByTagIds = onGetEblanApplicationInfosByTagIds,
            onUpdateAppDrawerSettings = onUpdateAppDrawerSettings,
            onUpdateEblanApplicationInfos = onUpdateEblanApplicationInfos,
            onUpdateGridItemSource = onUpdateGridItemSource,
            onUpdateImageBitmap = onUpdateImageBitmap,
            onUpdateIsLongPressAndIsDragging = onUpdateIsLongPressAndIsDragging,
            onUpdateOverlayBounds = onUpdateOverlayBounds,
            onUpdateSharedElementKey = onUpdateSharedElementKey,
            onVerticalDrag = onVerticalDrag,
            onWidgets = onWidgets,
            onDraggingShortcutInfoGridItem = onDraggingShortcutInfoGridItem,
            onUpdateAssociate = onUpdateAssociate,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class, FlowPreview::class)
@Composable
private fun SharedTransitionScope.Success(
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
    onDismiss: () -> Unit,
    onDragEnd: (Float) -> Unit,
    onDraggingGridItem: () -> Unit,
    onEditApplicationInfo: (
        serialNumber: Long,
        componentName: String,
    ) -> Unit,
    onGetEblanApplicationInfosByLabel: (String) -> Unit,
    onGetEblanApplicationInfosByTagIds: (List<Long>) -> Unit,
    onUpdateAppDrawerSettings: (AppDrawerSettings) -> Unit,
    onUpdateEblanApplicationInfos: (List<EblanApplicationInfo>) -> Unit,
    onUpdateGridItemSource: (GridItemSource) -> Unit,
    onUpdateImageBitmap: (ImageBitmap) -> Unit,
    onUpdateIsLongPressAndIsDragging: () -> Unit,
    onUpdateOverlayBounds: (
        intOffset: IntOffset,
        intSize: IntSize,
    ) -> Unit,
    onUpdateSharedElementKey: (SharedElementKey?) -> Unit,
    onVerticalDrag: (Float) -> Unit,
    onWidgets: (EblanApplicationInfoGroup) -> Unit,
    onDraggingShortcutInfoGridItem: () -> Unit,
    onUpdateAssociate: (Associate) -> Unit,
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

    val selectedTagIds = remember { mutableStateSetOf<Long>() }

    val scope = rememberCoroutineScope()

    var isRearrangeEblanApplicationInfo by remember { mutableStateOf(false) }

    var showEblanApplicationInfoOrderDialog by remember { mutableStateOf(false) }

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

            selectedTagIds.clear()
        }
    }

    LaunchedEffect(key1 = Unit) {
        snapshotFlow { selectedTagIds.toList() }.onEach { selectedTagIds ->
            onGetEblanApplicationInfosByTagIds(selectedTagIds)
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
        SearchBar(
            state = searchBarState,
            modifier = Modifier
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
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                showEblanApplicationInfoOrderDialog = true
                            },
                        ) {
                            Icon(
                                imageVector = EblanLauncherIcons.MoreVert,
                                contentDescription = null,
                            )
                        }
                    },
                    onSearch = { scope.launch { searchBarState.animateToCollapsed() } },
                    placeholder = { Text(text = "Search Applications") },
                )
            },
        )

        if (eblanApplicationInfoTags.isNotEmpty()) {
            LazyRow(modifier = Modifier.fillMaxWidth()) {
                items(eblanApplicationInfoTags) { eblanApplicationInfoTag ->
                    TagFilterChip(
                        eblanApplicationInfoTag = eblanApplicationInfoTag,
                        selectedTagIds = selectedTagIds,
                        onAddId = selectedTagIds::add,
                        onRemoveId = selectedTagIds::remove,
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
                    eblanApplicationInfoOrder = appDrawerSettings.eblanApplicationInfoOrder,
                    getEblanApplicationInfosByLabel = getEblanApplicationInfosByLabel,
                    iconPackFilePaths = iconPackFilePaths,
                    index = index,
                    isRearrangeEblanApplicationInfo = isRearrangeEblanApplicationInfo,
                    managedProfileResult = managedProfileResult,
                    paddingValues = paddingValues,
                    onDismiss = onDismiss,
                    onDismissDragAndDrop = {
                        isRearrangeEblanApplicationInfo = false
                    },
                    onDragEnd = onDragEnd,
                    onDraggingGridItem = onDraggingGridItem,
                    onUpdateEblanApplicationInfos = onUpdateEblanApplicationInfos,
                    onUpdateGridItemSource = onUpdateGridItemSource,
                    onUpdateImageBitmap = onUpdateImageBitmap,
                    onUpdateIsLongPressAndIsDragging = onUpdateIsLongPressAndIsDragging,
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
                    onUpdateAssociate = onUpdateAssociate,
                )
            }
        } else {
            EblanApplicationInfosPage(
                appDrawerSettings = appDrawerSettings,
                currentPage = currentPage,
                drag = drag,
                eblanApplicationInfoOrder = appDrawerSettings.eblanApplicationInfoOrder,
                getEblanApplicationInfosByLabel = getEblanApplicationInfosByLabel,
                iconPackFilePaths = iconPackFilePaths,
                index = 0,
                isRearrangeEblanApplicationInfo = isRearrangeEblanApplicationInfo,
                managedProfileResult = managedProfileResult,
                paddingValues = paddingValues,
                onDismiss = onDismiss,
                onDismissDragAndDrop = {
                    isRearrangeEblanApplicationInfo = false
                },
                onDragEnd = onDragEnd,
                onDraggingGridItem = onDraggingGridItem,
                onUpdateEblanApplicationInfos = onUpdateEblanApplicationInfos,
                onUpdateGridItemSource = onUpdateGridItemSource,
                onUpdateImageBitmap = onUpdateImageBitmap,
                onUpdateIsLongPressAndIsDragging = onUpdateIsLongPressAndIsDragging,
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
                onUpdateAssociate = onUpdateAssociate,
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
            onUpdateAssociate = onUpdateAssociate,
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
    onDismiss: () -> Unit,
    onDismissDragAndDrop: () -> Unit,
    onDragEnd: (Float) -> Unit,
    onDraggingGridItem: () -> Unit,
    onUpdateEblanApplicationInfos: (List<EblanApplicationInfo>) -> Unit,
    onUpdateGridItemSource: (GridItemSource) -> Unit,
    onUpdateImageBitmap: (ImageBitmap) -> Unit,
    onUpdateIsLongPressAndIsDragging: () -> Unit,
    onUpdateOverlayBounds: (
        intOffset: IntOffset,
        intSize: IntSize,
    ) -> Unit,
    onUpdatePopupMenu: (Boolean) -> Unit,
    onUpdateSharedElementKey: (SharedElementKey?) -> Unit,
    onVerticalDrag: (Float) -> Unit,
    onUpdateEblanApplicationInfo: (EblanApplicationInfo) -> Unit,
    onUpdateAssociate: (Associate) -> Unit,
) {
    val userManager = LocalUserManager.current

    val packageManager = LocalPackageManager.current

    val eblanUser = getEblanApplicationInfosByLabel.eblanApplicationInfos.keys.toList().getOrElse(
        index = index,
        defaultValue = {
            EblanUser(
                serialNumber = 0L,
                eblanUserType = EblanUserType.Personal,
                isPrivateSpaceEntryPointHidden = false,
            )
        },
    )

    val userHandle = userManager.getUserForSerialNumber(serialNumber = eblanUser.serialNumber)

    var isQuietModeEnabled by remember { mutableStateOf(false) }

    LaunchedEffect(key1 = userHandle) {
        if (userHandle != null) {
            isQuietModeEnabled = userManager.isQuietModeEnabled(userHandle = userHandle)
        }
    }

    LaunchedEffect(key1 = managedProfileResult) {
        if (managedProfileResult != null && managedProfileResult.serialNumber == eblanUser.serialNumber) {
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
                eblanUser = eblanUser,
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
                eblanUser = eblanUser,
                getEblanApplicationInfosByLabel = getEblanApplicationInfosByLabel,
                iconPackFilePaths = iconPackFilePaths,
                managedProfileResult = managedProfileResult,
                paddingValues = paddingValues,
                onDismiss = onDismiss,
                onDragEnd = onDragEnd,
                onDraggingGridItem = onDraggingGridItem,
                onUpdateGridItemSource = onUpdateGridItemSource,
                onUpdateImageBitmap = onUpdateImageBitmap,
                onUpdateIsLongPressAndIsDragging = onUpdateIsLongPressAndIsDragging,
                onUpdateOverlayBounds = onUpdateOverlayBounds,
                onUpdatePopupMenu = onUpdatePopupMenu,
                onUpdateSharedElementKey = onUpdateSharedElementKey,
                onVerticalDrag = onVerticalDrag,
                onUpdateEblanApplicationInfo = onUpdateEblanApplicationInfo,
                onUpdateAssociate = onUpdateAssociate,
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && packageManager.isDefaultLauncher() && eblanUser.serialNumber > 0 && userHandle != null) {
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

@Composable
private fun QuiteModeScreen(
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

@OptIn(ExperimentalLayoutApi::class, ExperimentalSharedTransitionApi::class)
@Composable
private fun SharedTransitionScope.EblanApplicationInfos(
    modifier: Modifier = Modifier,
    appDrawerSettings: AppDrawerSettings,
    currentPage: Int,
    drag: Drag,
    eblanUser: EblanUser,
    getEblanApplicationInfosByLabel: GetEblanApplicationInfosByLabel,
    iconPackFilePaths: Map<String, String>,
    managedProfileResult: ManagedProfileResult?,
    paddingValues: PaddingValues,
    onDismiss: () -> Unit,
    onDragEnd: (Float) -> Unit,
    onDraggingGridItem: () -> Unit,
    onUpdateGridItemSource: (GridItemSource) -> Unit,
    onUpdateImageBitmap: (ImageBitmap) -> Unit,
    onUpdateIsLongPressAndIsDragging: () -> Unit,
    onUpdateOverlayBounds: (
        intOffset: IntOffset,
        intSize: IntSize,
    ) -> Unit,
    onUpdatePopupMenu: (Boolean) -> Unit,
    onUpdateSharedElementKey: (SharedElementKey?) -> Unit,
    onVerticalDrag: (Float) -> Unit,
    onUpdateEblanApplicationInfo: (EblanApplicationInfo) -> Unit,
    onUpdateAssociate: (Associate) -> Unit,
) {
    val scope = rememberCoroutineScope()

    val overscrollEffect = remember(key1 = scope) {
        OffsetOverscrollEffect(
            scope = scope,
            onVerticalDrag = onVerticalDrag,
            onDragEnd = onDragEnd,
        )
    }

    val lazyGridState = rememberLazyGridState()

    val canOverscroll by remember(key1 = lazyGridState) {
        derivedStateOf {
            val lastVisibleIndex =
                lazyGridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0

            lastVisibleIndex < lazyGridState.layoutInfo.totalItemsCount - 1
        }
    }

    val nestedScrollConnection = remember {
        OffsetNestedScrollConnection(
            onVerticalDrag = onVerticalDrag,
            onDragEnd = onDragEnd,
        )
    }

    var isQuietModeEnabled by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .run {
                if (!canOverscroll) {
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
            overscrollEffect = if (canOverscroll) {
                overscrollEffect
            } else {
                rememberOverscrollEffect()
            },
        ) {
            when (eblanUser.eblanUserType) {
                EblanUserType.Personal -> {
                    items(getEblanApplicationInfosByLabel.eblanApplicationInfos[eblanUser].orEmpty()) { eblanApplicationInfo ->
                        key(eblanApplicationInfo.serialNumber, eblanApplicationInfo.componentName) {
                            EblanApplicationInfoItem(
                                appDrawerSettings = appDrawerSettings,
                                currentPage = currentPage,
                                drag = drag,
                                eblanApplicationInfo = eblanApplicationInfo,
                                iconPackFilePaths = iconPackFilePaths,
                                paddingValues = paddingValues,
                                onDismiss = onDismiss,
                                onDraggingGridItem = onDraggingGridItem,
                                onUpdateGridItemSource = onUpdateGridItemSource,
                                onUpdateImageBitmap = onUpdateImageBitmap,
                                onUpdateIsLongPressAndIsDragging = onUpdateIsLongPressAndIsDragging,
                                onUpdateOverlayBounds = onUpdateOverlayBounds,
                                onUpdatePopupMenu = onUpdatePopupMenu,
                                onUpdateSharedElementKey = onUpdateSharedElementKey,
                                onUpdateEblanApplicationInfo = onUpdateEblanApplicationInfo,
                                onUpdateAssociate = onUpdateAssociate,
                            )
                        }
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
                        onUpdateGridItemSource = onUpdateGridItemSource,
                        onUpdateIsQuietModeEnabled = { newIsQuiteModeEnabled ->
                            isQuietModeEnabled = newIsQuiteModeEnabled
                        },
                        onUpdateOverlayBounds = onUpdateOverlayBounds,
                        onUpdatePopupMenu = onUpdatePopupMenu,
                    )
                }

                else -> {
                    items(getEblanApplicationInfosByLabel.eblanApplicationInfos[eblanUser].orEmpty()) { eblanApplicationInfo ->
                        key(eblanApplicationInfo.serialNumber, eblanApplicationInfo.componentName) {
                            EblanApplicationInfoItem(
                                appDrawerSettings = appDrawerSettings,
                                currentPage = currentPage,
                                drag = drag,
                                eblanApplicationInfo = eblanApplicationInfo,
                                iconPackFilePaths = iconPackFilePaths,
                                paddingValues = paddingValues,
                                onDismiss = onDismiss,
                                onDraggingGridItem = onDraggingGridItem,
                                onUpdateGridItemSource = onUpdateGridItemSource,
                                onUpdateImageBitmap = onUpdateImageBitmap,
                                onUpdateIsLongPressAndIsDragging = onUpdateIsLongPressAndIsDragging,
                                onUpdateOverlayBounds = onUpdateOverlayBounds,
                                onUpdatePopupMenu = onUpdatePopupMenu,
                                onUpdateSharedElementKey = onUpdateSharedElementKey,
                                onUpdateEblanApplicationInfo = onUpdateEblanApplicationInfo,
                                onUpdateAssociate = onUpdateAssociate,
                            )
                        }
                    }
                }
            }
        }

        if (!WindowInsets.isImeVisible) {
            ScrollBarThumb(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .fillMaxHeight(),
                appDrawerSettings = appDrawerSettings,
                lazyGridState = lazyGridState,
                paddingValues = paddingValues,
                onScrollToItem = lazyGridState::scrollToItem,
            )
        }
    }
}

@OptIn(ExperimentalUuidApi::class, ExperimentalSharedTransitionApi::class)
@Composable
private fun SharedTransitionScope.EblanApplicationInfoItem(
    modifier: Modifier = Modifier,
    appDrawerSettings: AppDrawerSettings,
    currentPage: Int,
    drag: Drag,
    eblanApplicationInfo: EblanApplicationInfo,
    iconPackFilePaths: Map<String, String>,
    paddingValues: PaddingValues,
    onDismiss: () -> Unit,
    onDraggingGridItem: () -> Unit,
    onUpdateGridItemSource: (GridItemSource) -> Unit,
    onUpdateImageBitmap: (ImageBitmap) -> Unit,
    onUpdateIsLongPressAndIsDragging: () -> Unit,
    onUpdateOverlayBounds: (
        intOffset: IntOffset,
        intSize: IntSize,
    ) -> Unit,
    onUpdatePopupMenu: (Boolean) -> Unit,
    onUpdateSharedElementKey: (SharedElementKey?) -> Unit,
    onUpdateEblanApplicationInfo: (EblanApplicationInfo) -> Unit,
    onUpdateAssociate: (Associate) -> Unit,
) {
    var intOffset by remember { mutableStateOf(IntOffset.Zero) }

    var intSize by remember { mutableStateOf(IntSize.Zero) }

    val graphicsLayer = rememberGraphicsLayer()

    val scope = rememberCoroutineScope()

    val density = LocalDensity.current

    val launcherApps = LocalLauncherApps.current

    val textColor = getSystemTextColor(
        systemCustomTextColor = appDrawerSettings.gridItemSettings.customTextColor,
        systemTextColor = appDrawerSettings.gridItemSettings.textColor,
    )

    val appDrawerRowsHeight = appDrawerSettings.appDrawerRowsHeight.dp

    val maxLines = if (appDrawerSettings.gridItemSettings.singleLineLabel) 1 else Int.MAX_VALUE

    val icon = iconPackFilePaths[eblanApplicationInfo.componentName] ?: eblanApplicationInfo.icon

    val horizontalAlignment =
        getHorizontalAlignment(horizontalAlignment = appDrawerSettings.gridItemSettings.horizontalAlignment)

    val verticalArrangement =
        getVerticalArrangement(verticalArrangement = appDrawerSettings.gridItemSettings.verticalArrangement)

    val leftPadding = with(density) {
        paddingValues.calculateStartPadding(LayoutDirection.Ltr).roundToPx()
    }

    val topPadding = with(density) {
        paddingValues.calculateTopPadding().roundToPx()
    }

    var isLongPress by remember { mutableStateOf(false) }

    val applicationScreenId = remember { Uuid.random().toHexString() }

    val hasInteraction = isLongPress && (drag == Drag.Start || drag == Drag.Dragging)

    val sizeResolver = rememberConstraintsSizeResolver()

    val painter = rememberAsyncImagePainter(
        model = ImageRequest.Builder(LocalContext.current)
            .data(eblanApplicationInfo.customIcon ?: icon)
            .addLastModifiedToFileCacheKey(true)
            .size(sizeResolver)
            .build(),
    )

    LaunchedEffect(key1 = drag) {
        if (drag == Drag.Dragging && isLongPress) {
            onUpdatePopupMenu(false)

            onDismiss()

            val pagerScreenId = Uuid.random().toHexString()

            val data = GridItemData.ApplicationInfo(
                serialNumber = eblanApplicationInfo.serialNumber,
                componentName = eblanApplicationInfo.componentName,
                packageName = eblanApplicationInfo.packageName,
                icon = eblanApplicationInfo.icon,
                label = eblanApplicationInfo.label,
                customIcon = eblanApplicationInfo.customIcon,
                customLabel = eblanApplicationInfo.customLabel,
                index = -1,
                folderId = null,
            )

            val gridItem = GridItem(
                id = pagerScreenId,
                page = currentPage,
                startColumn = -1,
                startRow = -1,
                columnSpan = 1,
                rowSpan = 1,
                data = data,
                associate = Associate.Grid,
                override = false,
                gridItemSettings = appDrawerSettings.gridItemSettings,
                doubleTap = EblanAction(
                    eblanActionType = EblanActionType.None,
                    serialNumber = 0L,
                    componentName = "",
                ),
                swipeUp = EblanAction(
                    eblanActionType = EblanActionType.None,
                    serialNumber = 0L,
                    componentName = "",
                ),
                swipeDown = EblanAction(
                    eblanActionType = EblanActionType.None,
                    serialNumber = 0L,
                    componentName = "",
                ),
            )

            onUpdateGridItemSource(GridItemSource.New(gridItem = gridItem))

            onUpdateAssociate(gridItem.associate)

            onUpdateSharedElementKey(
                SharedElementKey(
                    id = pagerScreenId,
                    parent = SharedElementKey.Parent.Grid,
                ),
            )

            onUpdateIsLongPressAndIsDragging()

            onDraggingGridItem()
        } else if ((drag == Drag.End || drag == Drag.Cancel) && isLongPress) {
            isLongPress = false
        }
    }

    Column(
        modifier = modifier
            .pointerInput(key1 = drag) {
                detectTapGestures(
                    onTap = {
                        val sourceBoundsX = intOffset.x + leftPadding

                        val sourceBoundsY = intOffset.y + topPadding

                        launcherApps.startMainActivity(
                            serialNumber = eblanApplicationInfo.serialNumber,
                            componentName = eblanApplicationInfo.componentName,
                            sourceBounds = Rect(
                                sourceBoundsX,
                                sourceBoundsY,
                                sourceBoundsX + intSize.width,
                                sourceBoundsY + intSize.height,
                            ),
                        )
                    },
                    onLongPress = {
                        scope.launch {
                            onUpdateImageBitmap(graphicsLayer.toImageBitmap())

                            onUpdateOverlayBounds(
                                intOffset,
                                intSize,
                            )

                            onUpdateSharedElementKey(
                                SharedElementKey(
                                    id = applicationScreenId,
                                    parent = SharedElementKey.Parent.SwipeY,
                                ),
                            )

                            onUpdateEblanApplicationInfo(eblanApplicationInfo)

                            onUpdatePopupMenu(true)

                            isLongPress = true
                        }
                    },
                )
            }
            .height(appDrawerRowsHeight)
            .padding(appDrawerSettings.gridItemSettings.padding.dp)
            .background(
                color = Color(appDrawerSettings.gridItemSettings.customBackgroundColor),
                shape = RoundedCornerShape(size = appDrawerSettings.gridItemSettings.cornerRadius.dp),
            ),
        horizontalAlignment = horizontalAlignment,
        verticalArrangement = verticalArrangement,
    ) {
        if (!hasInteraction) {
            Box(modifier = Modifier.size(appDrawerSettings.gridItemSettings.iconSize.dp)) {
                Image(
                    painter = painter,
                    contentDescription = null,
                    modifier = Modifier
                        .then(sizeResolver)
                        .matchParentSize()
                        .drawWithContent {
                            graphicsLayer.record {
                                this@drawWithContent.drawContent()
                            }

                            drawLayer(graphicsLayer)
                        }
                        .onGloballyPositioned { layoutCoordinates ->
                            intOffset = layoutCoordinates.positionInRoot().round()

                            intSize = layoutCoordinates.size
                        }
                        .sharedElementWithCallerManagedVisibility(
                            rememberSharedContentState(
                                key = SharedElementKey(
                                    id = applicationScreenId,
                                    parent = SharedElementKey.Parent.SwipeY,
                                ),
                            ),
                            visible = drag == Drag.None || drag == Drag.Cancel || drag == Drag.End,
                        ),
                )

                if (eblanApplicationInfo.serialNumber != 0L) {
                    ElevatedCard(
                        modifier = Modifier
                            .size((appDrawerSettings.gridItemSettings.iconSize * 0.40).dp)
                            .align(Alignment.BottomEnd),
                    ) {
                        Icon(
                            imageVector = EblanLauncherIcons.Work,
                            contentDescription = null,
                            modifier = Modifier.padding(2.dp),
                        )
                    }
                }
            }

            if (appDrawerSettings.gridItemSettings.showLabel) {
                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = eblanApplicationInfo.customLabel ?: eblanApplicationInfo.label,
                    color = textColor,
                    textAlign = TextAlign.Center,
                    maxLines = maxLines,
                    fontSize = appDrawerSettings.gridItemSettings.textSize.sp,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun EblanApplicationInfoTabRow(
    modifier: Modifier = Modifier,
    currentPage: Int,
    eblanApplicationInfos: Map<EblanUser, List<EblanApplicationInfo>>,
    onAnimateScrollToPage: suspend (Int) -> Unit,
) {
    val scope = rememberCoroutineScope()

    SecondaryTabRow(
        modifier = modifier,
        selectedTabIndex = currentPage,
    ) {
        eblanApplicationInfos.keys.forEachIndexed { index, eblanUser ->
            Tab(
                selected = currentPage == index,
                onClick = {
                    scope.launch {
                        onAnimateScrollToPage(index)
                    }
                },
                text = {
                    Text(
                        text = eblanUser.eblanUserType.name,
                        maxLines = 1,
                    )
                },
            )
        }
    }
}

@Composable
private fun TagFilterChip(
    modifier: Modifier = Modifier,
    eblanApplicationInfoTag: EblanApplicationInfoTag,
    selectedTagIds: Set<Long>,
    onAddId: (Long) -> Unit,
    onRemoveId: (Long) -> Unit,
) {
    FilterChip(
        modifier = modifier.padding(5.dp),
        onClick = {
            if (eblanApplicationInfoTag.id in selectedTagIds) {
                onRemoveId(eblanApplicationInfoTag.id)
            } else {
                onAddId(eblanApplicationInfoTag.id)
            }
        },
        label = {
            Text(text = eblanApplicationInfoTag.name)
        },
        selected = eblanApplicationInfoTag.id in selectedTagIds,
        leadingIcon = if (eblanApplicationInfoTag.id in selectedTagIds) {
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
