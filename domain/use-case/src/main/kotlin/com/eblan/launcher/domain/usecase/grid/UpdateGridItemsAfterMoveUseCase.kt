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
import com.eblan.launcher.domain.repository.GridRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class UpdateGridItemsAfterMoveUseCase @Inject constructor(
    private val gridRepository: GridRepository,
    @param:Dispatcher(EblanDispatchers.Default) private val defaultDispatcher: CoroutineDispatcher,
) {
    suspend operator fun invoke(moveGridItemResult: MoveGridItemResult) {
        withContext(defaultDispatcher) {
            val conflictingGridItem = moveGridItemResult.conflictingGridItem

            val movingGridItem = moveGridItemResult.movingGridItem

            gridRepository.updateGridItem(gridItem = movingGridItem)

            when (val data = conflictingGridItem?.data) {
                is GridItemData.Folder -> {
                    addMovingGridItemIntoFolder(
                        data = data,
                        movingGridItem = movingGridItem,
                    )
                }

                is GridItemData.ApplicationInfo,
                is GridItemData.ShortcutConfig,
                is GridItemData.ShortcutInfo,
                is GridItemData.Widget,
                -> {
                    createNewFolder(
                        conflictingGridItem = conflictingGridItem,
                        movingGridItem = movingGridItem,
                    )
                }

                null -> Unit
            }
        }
    }

    private suspend fun addMovingGridItemIntoFolder(
        data: GridItemData.Folder,
        movingGridItem: GridItem,
    ) {
        val index = data.gridItems.maxOfOrNull { folderGridItem ->
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

            else -> return
        }

        gridRepository.updateGridItem(gridItem = movingGridItem.copy(data = newData))
    }

    @OptIn(ExperimentalUuidApi::class)
    private suspend fun createNewFolder(
        conflictingGridItem: GridItem,
        movingGridItem: GridItem,
    ) {
        val id = Uuid.random().toHexString()

        val conflictingData = when (val data = conflictingGridItem.data) {
            is GridItemData.ApplicationInfo -> {
                data.copy(
                    folderId = id,
                    index = 0,
                )
            }

            is GridItemData.ShortcutInfo -> {
                data.copy(
                    folderId = id,
                    index = 0,
                )
            }

            is GridItemData.ShortcutConfig -> {
                data.copy(
                    folderId = id,
                    index = 0,
                )
            }

            else -> {
                return
            }
        }

        val movingData = when (val data = movingGridItem.data) {
            is GridItemData.ApplicationInfo -> {
                data.copy(
                    folderId = id,
                    index = 1,
                )
            }

            is GridItemData.ShortcutInfo -> {
                data.copy(
                    folderId = id,
                    index = 1,
                )
            }

            is GridItemData.ShortcutConfig -> {
                data.copy(
                    folderId = id,
                    index = 1,
                )
            }

            else -> {
                return
            }
        }

        val newConflictingGridItem = conflictingGridItem.copy(data = conflictingData)

        val newMovingGridItem = movingGridItem.copy(data = movingData)

        gridRepository.updateGridItem(gridItem = newConflictingGridItem)

        gridRepository.updateGridItem(gridItem = newMovingGridItem)

        val folderGridItems = listOf(
            newConflictingGridItem,
            newMovingGridItem,
        )

        gridRepository.insertGridItem(
            gridItem = conflictingGridItem.copy(
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
