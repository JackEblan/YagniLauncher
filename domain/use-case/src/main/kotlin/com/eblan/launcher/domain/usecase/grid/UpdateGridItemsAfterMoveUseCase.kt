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

import com.eblan.launcher.domain.common.Dispatcher
import com.eblan.launcher.domain.common.EblanDispatchers
import com.eblan.launcher.domain.model.ApplicationInfoGridItem
import com.eblan.launcher.domain.model.GridItem
import com.eblan.launcher.domain.model.GridItemData
import com.eblan.launcher.domain.model.MoveGridItemResult
import com.eblan.launcher.domain.repository.GridCacheRepository
import com.eblan.launcher.domain.repository.GridRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class UpdateGridItemsAfterMoveUseCase @Inject constructor(
    private val gridCacheRepository: GridCacheRepository,
    private val gridRepository: GridRepository,
    @param:Dispatcher(EblanDispatchers.Default) private val defaultDispatcher: CoroutineDispatcher,
) {
    suspend operator fun invoke(moveGridItemResult: MoveGridItemResult) {
        withContext(defaultDispatcher) {
            val gridItems = gridCacheRepository.gridItemsCache.first().toMutableList()

            val conflictingGridItem = moveGridItemResult.conflictingGridItem

            val movingIndex =
                gridItems.indexOfFirst { it.id == moveGridItemResult.movingGridItem.id }

            if (movingIndex != -1 && conflictingGridItem != null) {
                groupConflictingGridItemsIntoFolder(
                    gridItems = gridItems,
                    conflictingGridItem = conflictingGridItem,
                    movingGridItem = gridItems[movingIndex],
                    movingIndex = movingIndex,
                )
            }

            gridRepository.updateGridItems(gridItems = gridItems)
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    private fun groupConflictingGridItemsIntoFolder(
        gridItems: MutableList<GridItem>,
        conflictingGridItem: GridItem,
        movingGridItem: GridItem,
        movingIndex: Int,
    ) {
        val conflictingIndex = gridItems.indexOfFirst { it.id == conflictingGridItem.id }

        when (val data = conflictingGridItem.data) {
            is GridItemData.Folder -> {
                addMovingGridItemIntoFolder(
                    data = data,
                    movingGridItem = movingGridItem,
                    gridItems = gridItems,
                    conflictingGridItem = conflictingGridItem,
                    conflictingIndex = conflictingIndex,
                    movingIndex = movingIndex,
                )
            }

            else -> {
                createNewFolder(
                    conflictingGridItem = conflictingGridItem,
                    movingGridItem = movingGridItem,
                    gridItems = gridItems,
                    conflictingIndex = conflictingIndex,
                    movingIndex = movingIndex,
                )
            }
        }
    }

    private fun addMovingGridItemIntoFolder(
        data: GridItemData.Folder,
        movingGridItem: GridItem,
        gridItems: MutableList<GridItem>,
        conflictingGridItem: GridItem,
        conflictingIndex: Int,
        movingIndex: Int,
    ) {
        val movingData = movingGridItem.data as? GridItemData.ApplicationInfo
            ?: error("Expected GridItemData.ApplicationInfo")

        val applicationInfoGridItems = data.gridItems.toMutableList()

        val newMovingData = movingData.copy(
            index = applicationInfoGridItems.lastIndex + 1,
            folderId = data.id,
        )

        val applicationInfoGridItem = movingGridItem.asApplicationInfoGridItem(data = newMovingData)

        applicationInfoGridItems.add(applicationInfoGridItem)

        val previewGridItemsByPage =
            data.gridItemsByPage.values.firstOrNull()?.plus(applicationInfoGridItem) ?: emptyList()

        val conflictingData = data.copy(
            gridItems = applicationInfoGridItems,
            previewGridItemsByPage = previewGridItemsByPage,
        )

        gridItems[conflictingIndex] = conflictingGridItem.copy(data = conflictingData)
        gridItems[movingIndex] = movingGridItem.copy(data = movingData)
        gridItems.remove(movingGridItem)
    }

    @OptIn(ExperimentalUuidApi::class)
    private fun createNewFolder(
        conflictingGridItem: GridItem,
        movingGridItem: GridItem,
        gridItems: MutableList<GridItem>,
        conflictingIndex: Int,
        movingIndex: Int,
    ) {
        val id = Uuid.random().toHexString()

        val conflictingData = conflictingGridItem.data as? GridItemData.ApplicationInfo
            ?: error("Expected GridItemData.ApplicationInfo")

        val movingData = movingGridItem.data as? GridItemData.ApplicationInfo
            ?: error("Expected GridItemData.ApplicationInfo")

        val newConflictingData = conflictingData.copy(
            folderId = id,
            index = 0,
        )

        val newMovingData = movingData.copy(
            folderId = id,
            index = 1,
        )

        val conflictingApplicationInfoGridItem =
            conflictingGridItem.asApplicationInfoGridItem(data = newConflictingData)

        val movingApplicationInfoGridItem =
            movingGridItem.asApplicationInfoGridItem(data = newMovingData)

        val folderGridItems = listOf(
            conflictingApplicationInfoGridItem,
            movingApplicationInfoGridItem,
        )

        gridItems[conflictingIndex] = conflictingGridItem.copy(data = newConflictingData)

        gridItems[movingIndex] = movingGridItem.copy(data = newMovingData)

        gridItems.add(
            conflictingGridItem.copy(
                id = id,
                data = GridItemData.Folder(
                    id = id,
                    label = "Unknown",
                    gridItems = folderGridItems,
                    gridItemsByPage = mapOf(0 to folderGridItems),
                    previewGridItemsByPage = folderGridItems,
                    icon = null,
                    columns = 1,
                    rows = 2,
                ),
            ),
        )
    }

    private fun GridItem.asApplicationInfoGridItem(data: GridItemData.ApplicationInfo): ApplicationInfoGridItem = ApplicationInfoGridItem(
        id = id,
        page = page,
        startColumn = startColumn,
        startRow = startRow,
        columnSpan = columnSpan,
        rowSpan = rowSpan,
        associate = associate,
        componentName = data.componentName,
        packageName = data.packageName,
        icon = data.icon,
        label = data.label,
        override = override,
        serialNumber = data.serialNumber,
        customIcon = data.customIcon,
        customLabel = data.customLabel,
        gridItemSettings = gridItemSettings,
        doubleTap = doubleTap,
        swipeUp = swipeUp,
        swipeDown = swipeDown,
        index = data.index,
        folderId = data.folderId,
    )
}
