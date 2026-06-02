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
package com.eblan.launcher.domain.usecase.grid

import com.eblan.launcher.domain.common.IconKeyGenerator
import com.eblan.launcher.domain.framework.FileManager
import com.eblan.launcher.domain.model.ApplicationInfoGridItem
import com.eblan.launcher.domain.model.FolderGridItem
import com.eblan.launcher.domain.model.FolderGridItemWrapper
import com.eblan.launcher.domain.model.GridItem
import com.eblan.launcher.domain.model.GridItemData
import com.eblan.launcher.domain.model.ShortcutConfigGridItem
import com.eblan.launcher.domain.model.ShortcutInfoGridItem
import com.eblan.launcher.domain.repository.FolderGridItemRepository
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.io.File
import kotlin.math.ceil
import kotlin.math.min
import kotlin.math.sqrt

internal suspend fun FolderGridItemWrapper.asGridItem(
    folderGridItemRepository: FolderGridItemRepository,
    maxFolderColumns: Int,
    maxFolderRows: Int,
    fileManager: FileManager,
    iconKeyGenerator: IconKeyGenerator,
    iconPackInfoPackageName: String,
): GridItem {
    val childFolderGridItems =
        folderGridItems.map {
            folderGridItemRepository.getFolderGridItemWrapper(
                id = it.id,
            )?.asPreviewGridItem(
                maxFolderColumns = maxFolderColumns,
                maxFolderRows = maxFolderRows,
                fileManager = fileManager,
                iconKeyGenerator = iconKeyGenerator,
                iconPackInfoPackageName = iconPackInfoPackageName,
            ) ?: it.asIconGridItem()
        }

    val gridItems =
        (
            applicationInfoGridItems.map {
                it.asGridItem(
                    fileManager = fileManager,
                    iconKeyGenerator = iconKeyGenerator,
                    iconPackInfoPackageName = iconPackInfoPackageName,
                )
            } + shortcutInfoGridItems.map {
                it.asGridItem()
            } + shortcutConfigGridItems.map {
                it.asGridItem()
            } + childFolderGridItems
            ).sortedBy {
            when (val data = it.data) {
                is GridItemData.ApplicationInfo -> data.index
                is GridItemData.ShortcutInfo -> data.index
                is GridItemData.ShortcutConfig -> data.index
                is GridItemData.Folder -> data.index
                else -> error("Unsupported folder grid item")
            }
        }

    val gridItemsByPage = gridItems.getGridItemsByPage(
        maxFolderColumns = maxFolderColumns,
        maxFolderRows = maxFolderRows,
    )

    val firstPageGridItems = gridItemsByPage.values.firstOrNull().orEmpty()

    val (columns, rows) = getGridDimension(
        count = firstPageGridItems.size,
        maxFolderColumns = maxFolderColumns,
        maxFolderRows = maxFolderRows,
    )

    val maxIndex = gridItems.maxOfOrNull {
        when (val data = it.data) {
            is GridItemData.ApplicationInfo -> data.index + 1
            is GridItemData.ShortcutInfo -> data.index + 1
            is GridItemData.ShortcutConfig -> data.index + 1
            is GridItemData.Folder -> data.index + 1
            else -> error("Unsupported folder grid item")
        }
    } ?: 0

    return GridItem(
        id = folderGridItem.id,
        page = folderGridItem.page,
        startColumn = folderGridItem.startColumn,
        startRow = folderGridItem.startRow,
        columnSpan = folderGridItem.columnSpan,
        rowSpan = folderGridItem.rowSpan,
        data = GridItemData.Folder(
            id = folderGridItem.id,
            label = folderGridItem.label,
            gridItems = gridItems,
            gridItemsByPage = gridItemsByPage,
            icon = folderGridItem.icon,
            columns = columns,
            rows = rows,
            maxIndex = maxIndex,
            index = folderGridItem.index,
            folderId = folderGridItem.folderId,
        ),
        associate = folderGridItem.associate,
        override = folderGridItem.override,
        gridItemSettings = folderGridItem.gridItemSettings,
        doubleTap = folderGridItem.doubleTap,
        swipeUp = folderGridItem.swipeUp,
        swipeDown = folderGridItem.swipeDown,
    )
}

private suspend fun FolderGridItemWrapper.asPreviewGridItem(
    maxFolderColumns: Int,
    maxFolderRows: Int,
    fileManager: FileManager,
    iconKeyGenerator: IconKeyGenerator,
    iconPackInfoPackageName: String,
): GridItem {
    val gridItems =
        (
            applicationInfoGridItems.map {
                it.asGridItem(
                    fileManager = fileManager,
                    iconKeyGenerator = iconKeyGenerator,
                    iconPackInfoPackageName = iconPackInfoPackageName,
                )
            } + shortcutInfoGridItems.map {
                it.asGridItem()
            } + shortcutConfigGridItems.map {
                it.asGridItem()
            } + folderGridItems.map {
                it.asIconGridItem()
            }
            ).sortedBy {
            when (val data = it.data) {
                is GridItemData.ApplicationInfo -> data.index
                is GridItemData.ShortcutInfo -> data.index
                is GridItemData.ShortcutConfig -> data.index
                is GridItemData.Folder -> data.index
                else -> error("Unsupported folder grid item")
            }
        }

    val gridItemsByPage = gridItems.getGridItemsByPage(
        maxFolderColumns = maxFolderColumns,
        maxFolderRows = maxFolderRows,
    )

    val firstPageGridItems = gridItemsByPage.values.firstOrNull() ?: emptyList()

    val (columns, rows) = getGridDimension(
        count = firstPageGridItems.size,
        maxFolderColumns = maxFolderColumns,
        maxFolderRows = maxFolderRows,
    )

    val maxIndex = gridItems.maxOfOrNull {
        when (val data = it.data) {
            is GridItemData.ApplicationInfo -> data.index + 1
            is GridItemData.ShortcutConfig -> data.index + 1
            is GridItemData.ShortcutInfo -> data.index + 1
            is GridItemData.Folder -> data.index + 1
            else -> error("Unsupported folder grid item")
        }
    } ?: 0

    val data = GridItemData.Folder(
        id = folderGridItem.id,
        label = folderGridItem.label,
        gridItems = gridItems,
        gridItemsByPage = gridItemsByPage,
        icon = folderGridItem.icon,
        columns = columns,
        rows = rows,
        maxIndex = maxIndex,
        index = folderGridItem.index,
        folderId = folderGridItem.folderId,
    )

    return GridItem(
        id = folderGridItem.id,
        page = folderGridItem.page,
        startColumn = folderGridItem.startColumn,
        startRow = folderGridItem.startRow,
        columnSpan = folderGridItem.columnSpan,
        rowSpan = folderGridItem.rowSpan,
        data = data,
        associate = folderGridItem.associate,
        override = folderGridItem.override,
        gridItemSettings = folderGridItem.gridItemSettings,
        doubleTap = folderGridItem.doubleTap,
        swipeUp = folderGridItem.swipeUp,
        swipeDown = folderGridItem.swipeDown,
    )
}

private suspend fun List<GridItem>.getGridItemsByPage(
    maxFolderColumns: Int,
    maxFolderRows: Int,
): Map<Int, List<GridItem>> = chunked(maxFolderColumns * maxFolderRows)
    .mapIndexed { index, gridItems ->
        currentCoroutineContext().ensureActive()

        index to gridItems
    }
    .toMap()

private fun getGridDimension(
    count: Int,
    maxFolderColumns: Int,
    maxFolderRows: Int,
): Pair<Int, Int> {
    if (count <= 0) return 0 to 0

    val columns = min(maxFolderColumns, ceil(sqrt(count.toDouble())).toInt())
    val rows = min(maxFolderRows, ceil(count / columns.toDouble()).toInt())

    return columns to rows
}

private suspend fun ApplicationInfoGridItem.asGridItem(
    fileManager: FileManager,
    iconKeyGenerator: IconKeyGenerator,
    iconPackInfoPackageName: String,
): GridItem {
    val iconPacksDirectory = fileManager.getFilesDirectory(
        FileManager.ICON_PACKS_DIR,
    )

    val iconPackDirectory = File(
        iconPacksDirectory,
        iconPackInfoPackageName,
    )

    val iconPackInfoFilePath = File(
        iconPackDirectory,
        iconKeyGenerator.getHashedName(name = componentName),
    )

    return GridItem(
        id = id,
        page = page,
        startColumn = startColumn,
        startRow = startRow,
        columnSpan = columnSpan,
        rowSpan = rowSpan,
        data = GridItemData.ApplicationInfo(
            serialNumber = serialNumber,
            componentName = componentName,
            packageName = packageName,
            icon = icon,
            label = label,
            customIcon = customIcon,
            customLabel = customLabel,
            index = index,
            folderId = folderId,
            iconPackInfoFilePath = if (iconPackInfoFilePath.exists()) {
                iconPackInfoFilePath.absolutePath
            } else {
                null
            },
        ),
        associate = associate,
        override = override,
        gridItemSettings = gridItemSettings,
        doubleTap = doubleTap,
        swipeUp = swipeUp,
        swipeDown = swipeDown,
    )
}

private fun ShortcutInfoGridItem.asGridItem(): GridItem = GridItem(
    id = id,
    page = page,
    startColumn = startColumn,
    startRow = startRow,
    columnSpan = columnSpan,
    rowSpan = rowSpan,
    data = GridItemData.ShortcutInfo(
        shortcutId = shortcutId,
        packageName = packageName,
        serialNumber = serialNumber,
        shortLabel = shortLabel,
        longLabel = longLabel,
        icon = icon,
        isEnabled = isEnabled,
        eblanApplicationInfoIcon = eblanApplicationInfoIcon,
        customIcon = customIcon,
        customShortLabel = customShortLabel,
        index = index,
        folderId = folderId,
    ),
    associate = associate,
    override = override,
    gridItemSettings = gridItemSettings,
    doubleTap = doubleTap,
    swipeUp = swipeUp,
    swipeDown = swipeDown,
)

private fun ShortcutConfigGridItem.asGridItem(): GridItem = GridItem(
    id = id,
    page = page,
    startColumn = startColumn,
    startRow = startRow,
    columnSpan = columnSpan,
    rowSpan = rowSpan,
    data = GridItemData.ShortcutConfig(
        serialNumber = serialNumber,
        componentName = componentName,
        packageName = packageName,
        activityIcon = activityIcon,
        activityLabel = activityLabel,
        applicationIcon = applicationIcon,
        applicationLabel = applicationLabel,
        shortcutIntentName = shortcutIntentName,
        shortcutIntentIcon = shortcutIntentIcon,
        shortcutIntentUri = shortcutIntentUri,
        customIcon = customIcon,
        customLabel = customLabel,
        index = index,
        folderId = folderId,
    ),
    associate = associate,
    override = override,
    gridItemSettings = gridItemSettings,
    doubleTap = doubleTap,
    swipeUp = swipeUp,
    swipeDown = swipeDown,
)

private fun FolderGridItem.asIconGridItem(): GridItem = GridItem(
    id = id,
    page = page,
    startColumn = startColumn,
    startRow = startRow,
    columnSpan = columnSpan,
    rowSpan = rowSpan,
    data = GridItemData.Folder(
        id = id,
        label = label,
        gridItems = emptyList(),
        gridItemsByPage = emptyMap(),
        icon = icon,
        columns = 0,
        rows = 0,
        maxIndex = 0,
        index = index,
        folderId = folderId,
    ),
    associate = associate,
    override = override,
    gridItemSettings = gridItemSettings,
    doubleTap = doubleTap,
    swipeUp = swipeUp,
    swipeDown = swipeDown,
)
