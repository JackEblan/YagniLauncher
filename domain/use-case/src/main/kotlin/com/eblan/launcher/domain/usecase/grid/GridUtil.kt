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

import com.eblan.launcher.domain.model.ApplicationInfoGridItem
import com.eblan.launcher.domain.model.FolderGridItemWrapper
import com.eblan.launcher.domain.model.GridItem
import com.eblan.launcher.domain.model.GridItemData
import com.eblan.launcher.domain.model.GridItemData.Folder
import com.eblan.launcher.domain.model.ShortcutConfigGridItem
import com.eblan.launcher.domain.model.ShortcutInfoGridItem
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlin.math.ceil
import kotlin.math.min
import kotlin.math.sqrt

const val FOLDER_MAX_COLUMNS = 5

const val FOLDER_MAX_ROWS = 4

internal suspend fun FolderGridItemWrapper.asGridItem(): GridItem {
    val sortedApplicationInfoGridItems =
        applicationInfoGridItems.map { applicationInfoGridItem ->
            applicationInfoGridItem.asGridItem()
        }

    val sortedShortcutInfoGridItems =
        shortcutInfoGridItems.map { shortcutInfoGridItem ->
            shortcutInfoGridItem.asGridItem()
        }

    val sortedShortcutConfigGridItems =
        shortcutConfigGridItems.map { shortcutConfigGridItem ->
            shortcutConfigGridItem.asGridItem()
        }

    val gridItems =
        (sortedApplicationInfoGridItems + sortedShortcutInfoGridItems + sortedShortcutConfigGridItems).sortedBy { gridItem ->
            when (val data = gridItem.data) {
                is GridItemData.ApplicationInfo -> data.index
                is GridItemData.ShortcutInfo -> data.index
                is GridItemData.ShortcutConfig -> data.index
                else -> -1
            }
        }

    val gridItemsByPage = gridItems.getGridItemsByPage()

    val firstPageGridItems = gridItemsByPage[0] ?: emptyList()

    val (columns, rows) = getGridDimension(count = firstPageGridItems.size)

    val maxIndex = gridItems.maxOfOrNull { folderGridItem ->
        when (val data = folderGridItem.data) {
            is GridItemData.ApplicationInfo -> data.index + 1
            is GridItemData.ShortcutConfig -> data.index + 1
            is GridItemData.ShortcutInfo -> data.index + 1
            else -> error("Unsupported Folder GridItem ")
        }
    } ?: 0

    val data = Folder(
        id = folderGridItem.id,
        label = folderGridItem.label,
        gridItems = gridItems,
        gridItemsByPage = gridItemsByPage,
        previewGridItemsByPage = gridItemsByPage.values.firstOrNull() ?: emptyList(),
        icon = folderGridItem.icon,
        columns = columns,
        rows = rows,
        maxIndex = maxIndex,
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

private suspend fun List<GridItem>.getGridItemsByPage(): Map<Int, List<GridItem>> = chunked(FOLDER_MAX_COLUMNS * FOLDER_MAX_ROWS)
    .mapIndexed { pageIndex, pageItems ->
        currentCoroutineContext().ensureActive()

        pageIndex to pageItems
    }
    .toMap()

private fun getGridDimension(count: Int): Pair<Int, Int> {
    if (count <= 0) return 0 to 0

    val columns = min(FOLDER_MAX_COLUMNS, ceil(sqrt(count.toDouble())).toInt())
    val rows = min(FOLDER_MAX_ROWS, ceil(count / columns.toDouble()).toInt())

    return columns to rows
}

private fun ApplicationInfoGridItem.asGridItem(): GridItem = GridItem(
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
    ),
    associate = associate,
    override = override,
    gridItemSettings = gridItemSettings,
    doubleTap = doubleTap,
    swipeUp = swipeUp,
    swipeDown = swipeDown,
)

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
