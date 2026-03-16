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

import com.eblan.launcher.domain.common.dispatcher.Dispatcher
import com.eblan.launcher.domain.common.dispatcher.EblanDispatchers
import com.eblan.launcher.domain.model.ApplicationInfoGridItem
import com.eblan.launcher.domain.model.GridItem
import com.eblan.launcher.domain.model.GridItemData
import com.eblan.launcher.domain.model.MoveGridItemResult
import com.eblan.launcher.domain.repository.ApplicationInfoGridItemRepository
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
    private val applicationInfoGridItemRepository: ApplicationInfoGridItemRepository,
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
                )
            }

            gridRepository.updateGridItems(gridItems = gridItems)
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    private suspend fun groupConflictingGridItemsIntoFolder(
        gridItems: MutableList<GridItem>,
        conflictingGridItem: GridItem,
        movingGridItem: GridItem,
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
                )
            }

            else -> {
                createNewFolder(
                    conflictingGridItem = conflictingGridItem,
                    movingGridItem = movingGridItem,
                    gridItems = gridItems,
                )
            }
        }
    }

    private suspend fun addMovingGridItemIntoFolder(
        data: GridItemData.Folder,
        movingGridItem: GridItem,
        gridItems: MutableList<GridItem>,
        conflictingGridItem: GridItem,
        conflictingIndex: Int,
    ) {
        val movingData = movingGridItem.data as? GridItemData.ApplicationInfo
            ?: error("Expected GridItemData.ApplicationInfo")

        val applicationInfoGridItems = data.gridItems.toMutableList()

        val newData = movingData.copy(
            index = applicationInfoGridItems.lastIndex + 1,
            folderId = data.id,
        )

        val applicationInfoGridItem = movingGridItem.asApplicationInfoGridItem(data = newData)

        applicationInfoGridItems.add(applicationInfoGridItem)

        val previewGridItemsByPage =
            data.gridItemsByPage.values.firstOrNull()?.plus(applicationInfoGridItem) ?: emptyList()

        val conflictingData = data.copy(
            gridItems = applicationInfoGridItems,
            previewGridItemsByPage = previewGridItemsByPage,
        )

        gridItems[conflictingIndex] = conflictingGridItem.copy(data = conflictingData)
        gridItems.remove(movingGridItem)

        gridCacheRepository.updateGridItemData(id = conflictingGridItem.id, data = conflictingData)
        gridCacheRepository.deleteGridItemById(id = movingGridItem.id)

        applicationInfoGridItemRepository.deleteApplicationInfoGridItemById(id = movingGridItem.id)
    }

    @OptIn(ExperimentalUuidApi::class)
    private suspend fun createNewFolder(
        conflictingGridItem: GridItem,
        movingGridItem: GridItem,
        gridItems: MutableList<GridItem>,
    ) {
        val id = Uuid.random().toHexString()

        val conflictingData = conflictingGridItem.data as? GridItemData.ApplicationInfo
            ?: error("Expected GridItemData.ApplicationInfo")

        val movingData = movingGridItem.data as? GridItemData.ApplicationInfo
            ?: error("Expected GridItemData.ApplicationInfo")

        val conflictingApplicationInfoGridItem =
            conflictingGridItem.asApplicationInfoGridItem(
                data = conflictingData.copy(
                    folderId = id,
                    index = 0,
                ),
            )

        val movingApplicationInfoGridItem =
            movingGridItem.asApplicationInfoGridItem(
                data = movingData.copy(
                    folderId = id,
                    index = 1,
                ),
            )

        val folderGridItems = listOf(
            conflictingApplicationInfoGridItem,
            movingApplicationInfoGridItem,
        )

        val newGridItem = conflictingGridItem.copy(
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
        )

        gridItems.remove(conflictingGridItem)
        gridItems.remove(movingGridItem)
        gridItems.add(newGridItem)

        gridCacheRepository.deleteGridItemById(id = conflictingGridItem.id)
        gridCacheRepository.deleteGridItemById(id = movingGridItem.id)
        gridCacheRepository.insertGridItem(gridItem = newGridItem)

        applicationInfoGridItemRepository.deleteApplicationInfoGridItemById(id = conflictingGridItem.id)
        applicationInfoGridItemRepository.deleteApplicationInfoGridItemById(id = movingGridItem.id)
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
