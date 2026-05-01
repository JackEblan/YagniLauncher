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
            gridCacheRepository.updateGridItemData(
                id = moveGridItemResult.movingGridItem.id,
                data = moveGridItemResult.movingGridItem.data,
            )

            val gridItems = gridCacheRepository.gridItemsCacheFlow.first().toMutableList()

            groupConflictingGridItemsIntoFolder(
                gridItems = gridItems,
                moveGridItemResult = moveGridItemResult,
            )

            gridRepository.updateGridItems(gridItems = gridItems)
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    private fun groupConflictingGridItemsIntoFolder(
        gridItems: MutableList<GridItem>,
        moveGridItemResult: MoveGridItemResult,
    ) {
        val conflictingGridItem = moveGridItemResult.conflictingGridItem ?: return

        val conflictingIndex = gridItems.indexOfFirst { it.id == conflictingGridItem.id }

        val movingGridItem = moveGridItemResult.movingGridItem

        val movingIndex =
            gridItems.indexOfFirst { it.id == movingGridItem.id }

        if (conflictingIndex == -1 || movingIndex == -1) return

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
        val folderGridItems = data.gridItems.toMutableList()

        val index = folderGridItems.maxOfOrNull { folderGridItem ->
            when (val folderData = folderGridItem.data) {
                is GridItemData.ApplicationInfo -> folderData.index + 1
                is GridItemData.ShortcutConfig -> folderData.index + 1
                is GridItemData.ShortcutInfo -> folderData.index + 1
                else -> return
            }
        } ?: 0

        val newData = when (val folderData = movingGridItem.data) {
            is GridItemData.ApplicationInfo -> folderData.copy(
                index = index,
                folderId = data.id,
            )

            is GridItemData.ShortcutInfo -> folderData.copy(
                index = index,
                folderId = data.id,
            )

            is GridItemData.ShortcutConfig -> folderData.copy(
                index = index,
                folderId = data.id,
            )

            is GridItemData.Folder,
            is GridItemData.Widget,
            -> error("Unsupported folder item type: ${folderData::class.simpleName}")
        }

        val updatedMovingGridItem = movingGridItem.copy(data = newData)

        folderGridItems.add(updatedMovingGridItem)

        val conflictingData = data.copy(
            gridItems = folderGridItems,
            previewGridItemsByPage = data.gridItemsByPage.values.firstOrNull()
                ?.plus(updatedMovingGridItem)
                ?: listOf(updatedMovingGridItem),
        )

        gridItems[conflictingIndex] = conflictingGridItem.copy(data = conflictingData)
        gridItems.removeAt(movingIndex)
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

        val conflictingData = when (val data = conflictingGridItem.data) {
            is GridItemData.ApplicationInfo -> data.copy(folderId = id, index = 0)
            is GridItemData.ShortcutInfo -> data.copy(folderId = id, index = 0)
            is GridItemData.ShortcutConfig -> data.copy(folderId = id, index = 0)
            else -> error("Unsupported folder item type: ${data::class.simpleName}")
        }

        val movingData = when (val data = movingGridItem.data) {
            is GridItemData.ApplicationInfo -> data.copy(folderId = id, index = 1)
            is GridItemData.ShortcutInfo -> data.copy(folderId = id, index = 1)
            is GridItemData.ShortcutConfig -> data.copy(folderId = id, index = 1)
            else -> error("Unsupported folder item type: ${data::class.simpleName}")
        }

        val folderGridItems = listOf(
            conflictingGridItem.copy(data = conflictingData),
            movingGridItem.copy(data = movingData),
        )

        gridItems[conflictingIndex] = conflictingGridItem.copy(data = conflictingData)

        gridItems[movingIndex] = movingGridItem.copy(data = movingData)

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
}
