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
import com.eblan.launcher.domain.common.IconKeyGenerator
import com.eblan.launcher.domain.framework.FileManager
import com.eblan.launcher.domain.model.GridItem
import com.eblan.launcher.domain.model.GridItemData
import com.eblan.launcher.domain.model.MoveGridItemResult
import com.eblan.launcher.domain.repository.FolderGridItemRepository
import com.eblan.launcher.domain.repository.GridRepository
import com.eblan.launcher.domain.repository.UserDataRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class UpdateGridItemsAfterMoveUseCase @Inject constructor(
    private val userDataRepository: UserDataRepository,
    private val gridRepository: GridRepository,
    private val folderGridItemRepository: FolderGridItemRepository,
    private val fileManager: FileManager,
    private val iconKeyGenerator: IconKeyGenerator,
    @param:Dispatcher(EblanDispatchers.Default) private val defaultDispatcher: CoroutineDispatcher,
) {
    suspend operator fun invoke(moveGridItemResult: MoveGridItemResult) {
        withContext(defaultDispatcher) {
            val conflictingGridItem = moveGridItemResult.conflictingGridItem

            val movingGridItem = moveGridItemResult.movingGridItem

            gridRepository.updateGridItem(gridItem = movingGridItem)

            if (conflictingGridItem != null) {
                when (conflictingGridItem.data) {
                    is GridItemData.Folder -> {
                        val userData = userDataRepository.userDataFlow.first()

                        val conflictingFolderGridItem =
                            folderGridItemRepository.getFolderGridItemWrapper(id = conflictingGridItem.id)
                                ?.asGridItem(
                                    folderGridItemRepository = folderGridItemRepository,
                                    maxFolderColumns = userData.homeSettings.maxFolderColumns,
                                    maxFolderRows = userData.homeSettings.maxFolderRows,
                                    fileManager = fileManager,
                                    iconKeyGenerator = iconKeyGenerator,
                                    iconPackInfoPackageName = userData.generalSettings.iconPackInfoPackageName,
                                )

                        addMovingGridItemIntoFolder(
                            conflictingFolderGridItem = conflictingFolderGridItem,
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
                }
            }
        }
    }

    private suspend fun addMovingGridItemIntoFolder(
        conflictingFolderGridItem: GridItem?,
        movingGridItem: GridItem,
    ) {
        val data = conflictingFolderGridItem?.data as? GridItemData.Folder ?: return

        val index = data.gridItems.maxOfOrNull {
            when (val folderData = it.data) {
                is GridItemData.ApplicationInfo -> folderData.index + 1
                is GridItemData.ShortcutConfig -> folderData.index + 1
                is GridItemData.ShortcutInfo -> folderData.index + 1
                is GridItemData.Folder -> folderData.index + 1
                else -> error("Unsupported addMovingGridItemIntoFolder")
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

            is GridItemData.Folder -> folderData.copy(
                index = index,
                folderId = data.id,
            )

            else -> error("Unsupported addMovingGridItemIntoFolder")
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

            is GridItemData.Folder -> {
                data.copy(
                    folderId = id,
                    index = 0,
                )
            }

            else -> error("Unsupported createNewFolder")
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

            is GridItemData.Folder -> {
                data.copy(
                    folderId = id,
                    index = 1,
                )
            }

            else -> error("Unsupported createNewFolder")
        }

        gridRepository.upsertGridItems(
            gridItems = listOf(
                conflictingGridItem.copy(data = conflictingData),
                movingGridItem.copy(data = movingData),
                conflictingGridItem.copy(
                    id = id,
                    data = GridItemData.Folder(
                        id = id,
                        label = "New Folder",
                        gridItems = emptyList(),
                        gridItemsByPage = emptyMap(),
                        icon = null,
                        columns = 1,
                        rows = 2,
                        maxIndex = 1,
                        index = -1,
                        folderId = null,
                    ),
                ),
            ),
        )
    }
}
