package com.eblan.launcher.domain.model

data class GridItems(
    val applicationInfoGridItems: List<ApplicationInfoGridItem>,
    val widgetGridItems: List<WidgetGridItem>,
    val shortcutInfoGridItems: List<ShortcutInfoGridItem>,
    val shortcutConfigGridItems: List<ShortcutConfigGridItem>,
    val folderGridItemWrappers: List<FolderGridItemWrapper>,
)