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
package com.eblan.launcher.data.datastore.mapper

import com.eblan.launcher.data.datastore.proto.appdrawer.AppDrawerSettingsProto
import com.eblan.launcher.data.datastore.proto.appdrawer.AppDrawerTypeProto
import com.eblan.launcher.data.datastore.proto.appdrawer.EblanApplicationInfoOrderProto
import com.eblan.launcher.data.datastore.proto.experimental.ExperimentalSettingsProto
import com.eblan.launcher.data.datastore.proto.general.GeneralSettingsProto
import com.eblan.launcher.data.datastore.proto.general.ThemeProto
import com.eblan.launcher.data.datastore.proto.gesture.EblanActionProto
import com.eblan.launcher.data.datastore.proto.gesture.EblanActionTypeProto
import com.eblan.launcher.data.datastore.proto.gesture.GestureSettingsProto
import com.eblan.launcher.data.datastore.proto.home.GridItemLayoutTypeProto
import com.eblan.launcher.data.datastore.proto.home.GridItemSettingsProto
import com.eblan.launcher.data.datastore.proto.home.HomeSettingsProto
import com.eblan.launcher.data.datastore.proto.home.HorizontalAlignmentProto
import com.eblan.launcher.data.datastore.proto.home.TextColorProto
import com.eblan.launcher.data.datastore.proto.home.VerticalArrangementProto
import com.eblan.launcher.domain.model.AppDrawerSettings
import com.eblan.launcher.domain.model.AppDrawerType
import com.eblan.launcher.domain.model.EblanAction
import com.eblan.launcher.domain.model.EblanActionType
import com.eblan.launcher.domain.model.EblanApplicationInfoOrder
import com.eblan.launcher.domain.model.ExperimentalSettings
import com.eblan.launcher.domain.model.GeneralSettings
import com.eblan.launcher.domain.model.GestureSettings
import com.eblan.launcher.domain.model.GridItemLayoutType
import com.eblan.launcher.domain.model.GridItemSettings
import com.eblan.launcher.domain.model.HomeSettings
import com.eblan.launcher.domain.model.HorizontalAlignment
import com.eblan.launcher.domain.model.HorizontalArrangement
import com.eblan.launcher.domain.model.TextColor
import com.eblan.launcher.domain.model.Theme
import com.eblan.launcher.domain.model.VerticalAlignment
import com.eblan.launcher.domain.model.VerticalArrangement

internal fun HomeSettingsProto.toHomeSettings(): HomeSettings = HomeSettings(
    columns = columns,
    rows = rows,
    pageCount = pageCount,
    infiniteScroll = infiniteScroll,
    dockColumns = dockColumns,
    dockRows = dockRows,
    dockHeight = dockHeight,
    initialPage = initialPage,
    wallpaperScroll = wallpaperScroll,
    gridItemSettings = gridItemSettingsProto.toGridItemSettings(),
    lockScreenOrientation = lockScreenOrientation,
    dockPageCount = dockPageCount,
    dockInfiniteScroll = dockInfiniteScroll,
    dockInitialPage = dockInitialPage,
    addNewAppsToHomeScreen = addNewAppsToHomeScreen,
    folderCellWidth = folderCellWidth,
    folderCellHeight = folderCellHeight,
    maxFolderColumns = maxFolderColumns,
    maxFolderRows = maxFolderRows,
)

internal fun AppDrawerSettingsProto.toAppDrawerSettings(): AppDrawerSettings = AppDrawerSettings(
    appDrawerColumns = appDrawerColumns,
    appDrawerRowsHeight = appDrawerRowsHeight,
    gridItemSettings = gridItemSettingsProto.toGridItemSettings(),
    eblanApplicationInfoOrder = eblanApplicationInfoOrderProto.toEblanApplicationInfoOrder(),
    backgroundColor = backgroundColor.toTextColor(),
    customBackgroundColor = customBackgroundColor,
    appDrawerType = appDrawerTypeProto.toAppDrawerType(),
    horizontalAppDrawerColumns = horizontalAppDrawerColumns,
    horizontalAppDrawerRows = horizontalAppDrawerRows,
    excludeTaggedApps = excludeTaggedApps,
)

internal fun GridItemSettingsProto.toGridItemSettings(): GridItemSettings = GridItemSettings(
    iconSize = iconSize,
    textColor = textColorProto.toTextColor(),
    textSize = textSize,
    showLabel = showLabel,
    singleLineLabel = singleLineLabel,
    horizontalAlignment = horizontalAlignmentProto.toHorizontalAlignment(),
    verticalArrangement = verticalArrangementProto.toVerticalArrangement(),
    customTextColor = customTextColor,
    customBackgroundColor = customBackgroundColor,
    padding = padding,
    cornerRadius = cornerRadius,
    gridItemLayoutType = gridItemLayoutTypeProto.toGridItemLayoutType(),
    horizontalArrangement = HorizontalArrangement.CENTER,
    verticalAlignment = VerticalAlignment.CENTER_VERTICALLY,
)

internal fun GeneralSettingsProto.toGeneralSettings(): GeneralSettings = GeneralSettings(
    theme = themeProto.toDarkThemeConfig(),
    dynamicTheme = dynamicTheme,
    iconPackInfoPackageName = iconPackInfoPackageName,
)

internal fun GridItemSettings.toGridItemSettingsProto(): GridItemSettingsProto = GridItemSettingsProto.newBuilder().setIconSize(iconSize)
    .setTextColorProto(textColor.toTextColorProto()).setTextSize(textSize)
    .setShowLabel(showLabel).setSingleLineLabel(singleLineLabel)
    .setHorizontalAlignmentProto(horizontalAlignment.toHorizontalAlignmentProto())
    .setVerticalArrangementProto(verticalArrangement.toVerticalArrangementProto())
    .setCustomTextColor(customTextColor).setCustomBackgroundColor(customBackgroundColor)
    .setPadding(padding).setCornerRadius(cornerRadius)
    .setGridItemLayoutTypeProto(gridItemLayoutType.toGridItemLayoutTypeProto()).build()

internal fun HomeSettings.toHomeSettingsProto(): HomeSettingsProto = HomeSettingsProto.newBuilder().setColumns(columns).setRows(rows).setPageCount(pageCount)
    .setInfiniteScroll(infiniteScroll).setDockColumns(dockColumns).setDockRows(dockRows)
    .setDockHeight(dockHeight).setInitialPage(initialPage).setWallpaperScroll(wallpaperScroll)
    .setGridItemSettingsProto(gridItemSettings.toGridItemSettingsProto())
    .setLockScreenOrientation(lockScreenOrientation).setDockPageCount(dockPageCount)
    .setDockInfiniteScroll(dockInfiniteScroll).setDockInitialPage(dockInitialPage)
    .setAddNewAppsToHomeScreen(addNewAppsToHomeScreen)
    .setFolderCellWidth(folderCellWidth)
    .setFolderCellHeight(folderCellHeight)
    .setMaxFolderColumns(maxFolderColumns)
    .setMaxFolderRows(maxFolderRows)
    .build()

internal fun AppDrawerSettings.toAppDrawerSettingsProto(): AppDrawerSettingsProto = AppDrawerSettingsProto.newBuilder().setAppDrawerColumns(appDrawerColumns)
    .setAppDrawerRowsHeight(appDrawerRowsHeight)
    .setGridItemSettingsProto(gridItemSettings.toGridItemSettingsProto())
    .setEblanApplicationInfoOrderProto(eblanApplicationInfoOrder.toEblanApplicationInfoOrderProto())
    .setBackgroundColor(backgroundColor.toTextColorProto())
    .setCustomBackgroundColor(customBackgroundColor)
    .setAppDrawerTypeProto(appDrawerType.toAppDrawerTypeProto())
    .setHorizontalAppDrawerColumns(horizontalAppDrawerColumns)
    .setHorizontalAppDrawerRows(horizontalAppDrawerRows)
    .setExcludeTaggedApps(excludeTaggedApps)
    .build()

internal fun GeneralSettings.toGeneralSettingsProto(): GeneralSettingsProto = GeneralSettingsProto.newBuilder().setThemeProto(theme.toThemeProto())
    .setDynamicTheme(dynamicTheme).setIconPackInfoPackageName(iconPackInfoPackageName).build()

internal fun GestureSettings.toGestureSettingsProto(): GestureSettingsProto = GestureSettingsProto.newBuilder().setDoubleTapProto(doubleTap.toEblanActionProto())
    .setSwipeUpProto(swipeUp.toEblanActionProto())
    .setSwipeDownProto(swipeDown.toEblanActionProto()).build()

internal fun ExperimentalSettings.toExperimentalSettingsProto(): ExperimentalSettingsProto = ExperimentalSettingsProto.newBuilder().setSyncData(syncData).setFirstLaunch(firstLaunch)
    .setLockMovement(lockMovement).build()

internal fun ExperimentalSettingsProto.toExperimentalSettings(): ExperimentalSettings = ExperimentalSettings(
    syncData = syncData,
    firstLaunch = firstLaunch,
    lockMovement = lockMovement,
)

internal fun EblanAction.toEblanActionProto(): EblanActionProto = EblanActionProto.newBuilder().setEblanActionTypeProto(eblanActionType.toEblanActionTypeProto())
    .setSerialNumber(serialNumber).setComponentName(componentName).build()

internal fun GestureSettingsProto.toGestureSettings(): GestureSettings = GestureSettings(
    doubleTap = doubleTapProto.toEblanAction(),
    swipeUp = swipeUpProto.toEblanAction(),
    swipeDown = swipeDownProto.toEblanAction(),
)

internal fun EblanActionProto.toEblanAction(): EblanAction = EblanAction(
    eblanActionType = eblanActionTypeProto.toEblanActionType(),
    serialNumber = serialNumber,
    componentName = componentName,
)

internal fun Theme.toThemeProto(): ThemeProto = when (this) {
    Theme.SYSTEM -> ThemeProto.THEME_SYSTEM
    Theme.LIGHT -> ThemeProto.THEME_LIGHT
    Theme.DARK -> ThemeProto.THEME_DARK
}

private fun EblanActionType.toEblanActionTypeProto(): EblanActionTypeProto = when (this) {
    EblanActionType.NONE -> EblanActionTypeProto.NONE
    EblanActionType.OPEN_APP_DRAWER -> EblanActionTypeProto.OPEN_APP_DRAWER
    EblanActionType.OPEN_NOTIFICATION_PANEL -> EblanActionTypeProto.OPEN_NOTIFICATION_PANEL
    EblanActionType.OPEN_APP -> EblanActionTypeProto.OPEN_APP
    EblanActionType.LOCK_SCREEN -> EblanActionTypeProto.LOCK_SCREEN
    EblanActionType.OPEN_QUICK_SETTINGS -> EblanActionTypeProto.OPEN_QUICK_SETTINGS
    EblanActionType.OPEN_RECENTS -> EblanActionTypeProto.OPEN_RECENTS
}

private fun EblanActionTypeProto.toEblanActionType(): EblanActionType = when (this) {
    EblanActionTypeProto.NONE, EblanActionTypeProto.UNRECOGNIZED -> EblanActionType.NONE
    EblanActionTypeProto.OPEN_APP_DRAWER -> EblanActionType.OPEN_APP_DRAWER
    EblanActionTypeProto.OPEN_NOTIFICATION_PANEL -> EblanActionType.OPEN_NOTIFICATION_PANEL
    EblanActionTypeProto.OPEN_APP -> EblanActionType.OPEN_APP
    EblanActionTypeProto.LOCK_SCREEN -> EblanActionType.LOCK_SCREEN
    EblanActionTypeProto.OPEN_QUICK_SETTINGS -> EblanActionType.OPEN_QUICK_SETTINGS
    EblanActionTypeProto.OPEN_RECENTS -> EblanActionType.OPEN_RECENTS
}

private fun ThemeProto.toDarkThemeConfig(): Theme = when (this) {
    ThemeProto.THEME_SYSTEM, ThemeProto.UNRECOGNIZED -> Theme.SYSTEM
    ThemeProto.THEME_LIGHT -> Theme.LIGHT
    ThemeProto.THEME_DARK -> Theme.DARK
}

private fun EblanApplicationInfoOrderProto.toEblanApplicationInfoOrder(): EblanApplicationInfoOrder = when (this) {
    EblanApplicationInfoOrderProto.ALPHABETICAL -> EblanApplicationInfoOrder.ALPHABETICAL
    EblanApplicationInfoOrderProto.INDEX -> EblanApplicationInfoOrder.INDEX
    EblanApplicationInfoOrderProto.UNRECOGNIZED -> EblanApplicationInfoOrder.ALPHABETICAL
}

private fun EblanApplicationInfoOrder.toEblanApplicationInfoOrderProto(): EblanApplicationInfoOrderProto = when (this) {
    EblanApplicationInfoOrder.ALPHABETICAL -> EblanApplicationInfoOrderProto.ALPHABETICAL
    EblanApplicationInfoOrder.INDEX -> EblanApplicationInfoOrderProto.INDEX
}

private fun TextColor.toTextColorProto(): TextColorProto = when (this) {
    TextColor.SYSTEM -> TextColorProto.TEXT_COLOR_SYSTEM
    TextColor.LIGHT -> TextColorProto.TEXT_COLOR_LIGHT
    TextColor.DARK -> TextColorProto.TEXT_COLOR_DARK
    TextColor.CUSTOM -> TextColorProto.TEXT_COLOR_CUSTOM
}

private fun TextColorProto.toTextColor(): TextColor = when (this) {
    TextColorProto.TEXT_COLOR_SYSTEM, TextColorProto.UNRECOGNIZED -> TextColor.SYSTEM
    TextColorProto.TEXT_COLOR_LIGHT -> TextColor.LIGHT
    TextColorProto.TEXT_COLOR_DARK -> TextColor.DARK
    TextColorProto.TEXT_COLOR_CUSTOM -> TextColor.CUSTOM
}

private fun HorizontalAlignment.toHorizontalAlignmentProto(): HorizontalAlignmentProto = when (this) {
    HorizontalAlignment.START -> HorizontalAlignmentProto.HORIZONTAL_ALIGNMENT_START
    HorizontalAlignment.CENTER_HORIZONTALLY -> HorizontalAlignmentProto.HORIZONTAL_ALIGNMENT_CENTER_HORIZONTALLY
    HorizontalAlignment.END -> HorizontalAlignmentProto.HORIZONTAL_ALIGNMENT_END
}

private fun HorizontalAlignmentProto.toHorizontalAlignment(): HorizontalAlignment = when (this) {
    HorizontalAlignmentProto.HORIZONTAL_ALIGNMENT_START -> HorizontalAlignment.START
    HorizontalAlignmentProto.HORIZONTAL_ALIGNMENT_CENTER_HORIZONTALLY, HorizontalAlignmentProto.UNRECOGNIZED -> HorizontalAlignment.CENTER_HORIZONTALLY
    HorizontalAlignmentProto.HORIZONTAL_ALIGNMENT_END -> HorizontalAlignment.END
}

private fun VerticalArrangement.toVerticalArrangementProto(): VerticalArrangementProto = when (this) {
    VerticalArrangement.TOP -> VerticalArrangementProto.VERTICAL_ARRANGEMENT_TOP
    VerticalArrangement.CENTER -> VerticalArrangementProto.VERTICAL_ARRANGEMENT_CENTER
    VerticalArrangement.BOTTOM -> VerticalArrangementProto.VERTICAL_ARRANGEMENT_BOTTOM
}

private fun VerticalArrangementProto.toVerticalArrangement(): VerticalArrangement = when (this) {
    VerticalArrangementProto.VERTICAL_ARRANGEMENT_TOP -> VerticalArrangement.TOP
    VerticalArrangementProto.VERTICAL_ARRANGEMENT_CENTER, VerticalArrangementProto.UNRECOGNIZED -> VerticalArrangement.CENTER
    VerticalArrangementProto.VERTICAL_ARRANGEMENT_BOTTOM -> VerticalArrangement.BOTTOM
}

private fun AppDrawerType.toAppDrawerTypeProto(): AppDrawerTypeProto = when (this) {
    AppDrawerType.VERTICAL -> AppDrawerTypeProto.VERTICAL
    AppDrawerType.HORIZONTAL -> AppDrawerTypeProto.HORIZONTAL
    AppDrawerType.LIST -> AppDrawerTypeProto.LIST
}

private fun AppDrawerTypeProto.toAppDrawerType(): AppDrawerType = when (this) {
    AppDrawerTypeProto.VERTICAL, AppDrawerTypeProto.UNRECOGNIZED -> AppDrawerType.VERTICAL
    AppDrawerTypeProto.HORIZONTAL -> AppDrawerType.HORIZONTAL
    AppDrawerTypeProto.LIST -> AppDrawerType.LIST
}

private fun GridItemLayoutTypeProto.toGridItemLayoutType(): GridItemLayoutType = when (this) {
    GridItemLayoutTypeProto.START_ICON_END_LABEL -> GridItemLayoutType.START_ICON_END_LABEL
    GridItemLayoutTypeProto.START_LABEL_END_ICON -> GridItemLayoutType.START_LABEL_END_ICON
    GridItemLayoutTypeProto.TOP_ICON_BOTTOM_LABEL -> GridItemLayoutType.TOP_ICON_BOTTOM_LABEL
    GridItemLayoutTypeProto.TOP_LABEL_BOTTOM_ICON -> GridItemLayoutType.TOP_LABEL_BOTTOM_ICON
    GridItemLayoutTypeProto.ICON_ONLY -> GridItemLayoutType.ICON_ONLY
    GridItemLayoutTypeProto.LABEL_ONLY -> GridItemLayoutType.LABEL_ONLY
    GridItemLayoutTypeProto.UNRECOGNIZED -> GridItemLayoutType.TOP_ICON_BOTTOM_LABEL
}

private fun GridItemLayoutType.toGridItemLayoutTypeProto(): GridItemLayoutTypeProto = when (this) {
    GridItemLayoutType.START_ICON_END_LABEL -> GridItemLayoutTypeProto.START_ICON_END_LABEL
    GridItemLayoutType.START_LABEL_END_ICON -> GridItemLayoutTypeProto.START_LABEL_END_ICON
    GridItemLayoutType.TOP_ICON_BOTTOM_LABEL -> GridItemLayoutTypeProto.TOP_ICON_BOTTOM_LABEL
    GridItemLayoutType.TOP_LABEL_BOTTOM_ICON -> GridItemLayoutTypeProto.TOP_LABEL_BOTTOM_ICON
    GridItemLayoutType.ICON_ONLY -> GridItemLayoutTypeProto.ICON_ONLY
    GridItemLayoutType.LABEL_ONLY -> GridItemLayoutTypeProto.LABEL_ONLY
}
