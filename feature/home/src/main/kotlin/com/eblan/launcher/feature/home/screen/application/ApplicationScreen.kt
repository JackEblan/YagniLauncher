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
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
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
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.addLastModifiedToFileCacheKey
import com.eblan.launcher.designsystem.icon.EblanLauncherIcons
import com.eblan.launcher.domain.model.AppDrawerSettings
import com.eblan.launcher.domain.model.AppDrawerType
import com.eblan.launcher.domain.model.Associate
import com.eblan.launcher.domain.model.EblanAction
import com.eblan.launcher.domain.model.EblanActionType
import com.eblan.launcher.domain.model.EblanAppWidgetProviderInfo
import com.eblan.launcher.domain.model.EblanApplicationInfo
import com.eblan.launcher.domain.model.EblanApplicationInfoGroup
import com.eblan.launcher.domain.model.EblanApplicationInfoTag
import com.eblan.launcher.domain.model.EblanShortcutInfo
import com.eblan.launcher.domain.model.EblanShortcutInfoByGroup
import com.eblan.launcher.domain.model.EblanUserPageKey
import com.eblan.launcher.domain.model.GetEblanApplicationInfosByLabel
import com.eblan.launcher.domain.model.GridItem
import com.eblan.launcher.domain.model.GridItemData
import com.eblan.launcher.domain.model.ManagedProfileResult
import com.eblan.launcher.domain.model.TextColor
import com.eblan.launcher.feature.home.model.Drag
import com.eblan.launcher.feature.home.model.GridItemSource
import com.eblan.launcher.feature.home.model.SharedElementKey
import com.eblan.launcher.feature.home.screen.application.horizontal.HorizontalApplicationScreen
import com.eblan.launcher.feature.home.screen.application.vertical.VerticalApplicationScreen
import com.eblan.launcher.feature.home.util.getHorizontalAlignment
import com.eblan.launcher.feature.home.util.getSystemTextColor
import com.eblan.launcher.feature.home.util.getVerticalArrangement
import com.eblan.launcher.framework.packagemanager.AndroidPackageManagerWrapper
import com.eblan.launcher.framework.usermanager.AndroidUserManagerWrapper
import com.eblan.launcher.ui.local.LocalLauncherApps
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
    Surface(
        modifier = modifier
            .offset {
                IntOffset(x = 0, y = swipeY.roundToInt())
            }
            .fillMaxSize()
            .clip(RoundedCornerShape(cornerSize))
            .alpha(alpha),
        color = when (appDrawerSettings.backgroundColor) {
            TextColor.System -> {
                MaterialTheme.colorScheme.surface
            }

            TextColor.Light -> {
                Color.White
            }

            TextColor.Dark -> {
                Color.Black
            }

            TextColor.Custom -> {
                Color(appDrawerSettings.customBackgroundColor)
            }
        },
    ) {
        when (appDrawerSettings.appDrawerType) {
            AppDrawerType.Vertical -> {
                VerticalApplicationScreen(
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
                    isVisibleOverlay = isVisibleOverlay,
                    onDismiss = onDismiss,
                    onDragEnd = onDragEnd,
                    onDraggingGridItem = onDraggingGridItem,
                    onEditApplicationInfo = onEditApplicationInfo,
                    onGetEblanApplicationInfosByLabel = onGetEblanApplicationInfosByLabel,
                    onGetEblanApplicationInfosByTagId = onGetEblanApplicationInfosByTagId,
                    onUpdateAppDrawerSettings = onUpdateAppDrawerSettings,
                    onUpdateEblanApplicationInfos = onUpdateEblanApplicationInfos,
                    onUpdateGridItemSource = onUpdateGridItemSource,
                    onUpdateImageBitmap = onUpdateImageBitmap,
                    onUpdateIsDragging = onUpdateIsDragging,
                    onUpdateOverlayBounds = onUpdateOverlayBounds,
                    onUpdateSharedElementKey = onUpdateSharedElementKey,
                    onVerticalDrag = onVerticalDrag,
                    onWidgets = onWidgets,
                    onDraggingShortcutInfoGridItem = onDraggingShortcutInfoGridItem,
                    onUpdateIsVisibleOverlay = onUpdateIsVisibleOverlay,
                )
            }

            AppDrawerType.Horizontal -> {
                HorizontalApplicationScreen(
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
                    isVisibleOverlay = isVisibleOverlay,
                    onDismiss = onDismiss,
                    onDragEnd = onDragEnd,
                    onDraggingGridItem = onDraggingGridItem,
                    onEditApplicationInfo = onEditApplicationInfo,
                    onGetEblanApplicationInfosByLabel = onGetEblanApplicationInfosByLabel,
                    onGetEblanApplicationInfosByTagId = onGetEblanApplicationInfosByTagId,
                    onUpdateGridItemSource = onUpdateGridItemSource,
                    onUpdateImageBitmap = onUpdateImageBitmap,
                    onUpdateIsDragging = onUpdateIsDragging,
                    onUpdateOverlayBounds = onUpdateOverlayBounds,
                    onUpdateSharedElementKey = onUpdateSharedElementKey,
                    onVerticalDrag = onVerticalDrag,
                    onWidgets = onWidgets,
                    onDraggingShortcutInfoGridItem = onDraggingShortcutInfoGridItem,
                    onUpdateIsVisibleOverlay = onUpdateIsVisibleOverlay,
                )
            }
        }
    }
}

@Composable
internal fun QuiteModeScreen(
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

@OptIn(ExperimentalUuidApi::class, ExperimentalSharedTransitionApi::class)
@Composable
internal fun SharedTransitionScope.EblanApplicationInfoItem(
    modifier: Modifier = Modifier,
    appDrawerSettings: AppDrawerSettings,
    currentPage: Int,
    drag: Drag,
    eblanApplicationInfo: EblanApplicationInfo,
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

    val alpha = if (isLongPress) 0f else 1f

    LaunchedEffect(key1 = drag) {
        when (drag) {
            Drag.Dragging if isLongPress -> {
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

                onUpdateIsDragging(true)

                onDraggingGridItem()
            }

            Drag.Cancel, Drag.End -> {
                if (isLongPress && isVisibleOverlay) {
                    onUpdateIsVisibleOverlay(false)

                    isLongPress = false
                }
            }

            else -> Unit
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

                            onUpdateIsVisibleOverlay(true)

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
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(eblanApplicationInfo.customIcon ?: icon)
                .addLastModifiedToFileCacheKey(true)
                .build(),
            contentDescription = null,
            modifier = Modifier
                .size(appDrawerSettings.gridItemSettings.iconSize.dp)
                .alpha(alpha)
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
                .run {
                    if (!isLongPress) {
                        sharedElementWithCallerManagedVisibility(
                            rememberSharedContentState(
                                key = SharedElementKey(
                                    id = applicationScreenId,
                                    parent = SharedElementKey.Parent.SwipeY,
                                ),
                            ),
                            visible = !isVisibleOverlay,
                        )
                    } else {
                        this
                    }
                },
        )

        if (appDrawerSettings.gridItemSettings.showLabel) {
            Spacer(modifier = Modifier.height(10.dp))

            Text(
                modifier = Modifier.alpha(alpha),
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

@Composable
internal fun TagElevatedFilterChip(
    modifier: Modifier = Modifier,
    eblanApplicationInfoTag: EblanApplicationInfoTag,
    selectedEblanApplicationInfoTag: Long?,
    onUpdateEblanApplicationInfoTag: (Long?) -> Unit,
) {
    ElevatedFilterChip(
        modifier = modifier.padding(5.dp),
        onClick = {
            if (eblanApplicationInfoTag.id == selectedEblanApplicationInfoTag) {
                onUpdateEblanApplicationInfoTag(null)
            } else {
                onUpdateEblanApplicationInfoTag(eblanApplicationInfoTag.id)
            }
        },
        label = {
            Text(text = eblanApplicationInfoTag.name)
        },
        selected = eblanApplicationInfoTag.id == selectedEblanApplicationInfoTag,
        leadingIcon = if (eblanApplicationInfoTag.id == selectedEblanApplicationInfoTag) {
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

@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun EblanApplicationInfoTabRow(
    modifier: Modifier = Modifier,
    currentPage: Int,
    eblanApplicationInfos: Map<EblanUserPageKey, List<EblanApplicationInfo>>,
    onAnimateScrollToPage: suspend (Int) -> Unit,
) {
    val scope = rememberCoroutineScope()

    val currentEblanUserPageKey = eblanApplicationInfos.keys.toList()[currentPage]

    val eblanUserPageKeys = remember(key1 = eblanApplicationInfos) {
        eblanApplicationInfos.keys.distinctBy { it.eblanUser.serialNumber }
    }

    val selectedTabIndex = remember(
        key1 = eblanUserPageKeys,
        key2 = currentEblanUserPageKey,
    ) {
        eblanUserPageKeys.indexOfFirst {
            it.eblanUser.serialNumber == currentEblanUserPageKey.eblanUser.serialNumber
        }
    }

    SecondaryTabRow(
        modifier = modifier,
        selectedTabIndex = selectedTabIndex,
    ) {
        eblanUserPageKeys.forEach { eblanUserPageKey ->
            Tab(
                selected = currentEblanUserPageKey == eblanUserPageKey,
                onClick = {
                    scope.launch {
                        onAnimateScrollToPage(
                            eblanApplicationInfos.keys.indexOfFirst {
                                it.eblanUser.serialNumber == eblanUserPageKey.eblanUser.serialNumber
                            },
                        )
                    }
                },
                text = {
                    Text(
                        text = eblanUserPageKey.eblanUser.eblanUserType.name,
                        maxLines = 1,
                    )
                },
            )
        }
    }
}
