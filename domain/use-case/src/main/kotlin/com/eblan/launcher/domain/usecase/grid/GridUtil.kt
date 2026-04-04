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
import com.eblan.launcher.domain.model.GridItemData.Folder
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlin.math.ceil
import kotlin.math.min
import kotlin.math.sqrt

const val FOLDER_MAX_COLUMNS = 5

const val FOLDER_MAX_ROWS = 4

internal suspend fun FolderGridItemWrapper.asGridItem(): GridItem {
    val sortedApplicationInfoGridItems = applicationInfoGridItems.sortedBy { it.index }

    val gridItemsByPage = sortedApplicationInfoGridItems.getGridItemsByPage()

    val firstPageGridItems = gridItemsByPage[0] ?: emptyList()

    val (columns, rows) = getGridDimension(count = firstPageGridItems.size)

    val data = Folder(
        id = folderGridItem.id,
        label = folderGridItem.label,
        gridItems = sortedApplicationInfoGridItems,
        gridItemsByPage = gridItemsByPage,
        previewGridItemsByPage = gridItemsByPage.values.firstOrNull() ?: emptyList(),
        icon = folderGridItem.icon,
        columns = columns,
        rows = rows,
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

internal suspend fun List<ApplicationInfoGridItem>.getGridItemsByPage(): Map<Int, List<ApplicationInfoGridItem>> = chunked(FOLDER_MAX_COLUMNS * FOLDER_MAX_ROWS)
    .mapIndexed { pageIndex, pageItems ->
        currentCoroutineContext().ensureActive()

        pageIndex to pageItems
    }
    .toMap()

internal fun getGridDimension(count: Int): Pair<Int, Int> {
    if (count <= 0) return 0 to 0

    val columns = min(FOLDER_MAX_COLUMNS, ceil(sqrt(count.toDouble())).toInt())
    val rows = min(FOLDER_MAX_ROWS, ceil(count / columns.toDouble()).toInt())

    return columns to rows
}
