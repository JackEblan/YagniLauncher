package com.eblan.launcher.data.room.model

import com.eblan.launcher.data.room.entity.ApplicationInfoGridItemEntity
import com.eblan.launcher.data.room.entity.FolderGridItemWrapperEntity
import com.eblan.launcher.data.room.entity.ShortcutConfigGridItemEntity
import com.eblan.launcher.data.room.entity.ShortcutInfoGridItemEntity
import com.eblan.launcher.data.room.entity.WidgetGridItemEntity

data class GridItemEntities(
    val applicationInfoGridItemEntities: List<ApplicationInfoGridItemEntity>,
    val widgetGridItemEntities: List<WidgetGridItemEntity>,
    val shortcutInfoGridItemEntities: List<ShortcutInfoGridItemEntity>,
    val shortcutConfigGridItemEntities: List<ShortcutConfigGridItemEntity>,
    val folderGridItemWrapperEntities: List<FolderGridItemWrapperEntity>,
)