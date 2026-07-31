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
import com.eblan.launcher.domain.repository.ApplicationInfoGridItemRepository
import com.eblan.launcher.domain.repository.FolderGridItemRepository
import com.eblan.launcher.domain.repository.ShortcutConfigGridItemRepository
import com.eblan.launcher.domain.repository.ShortcutInfoGridItemRepository
import com.eblan.launcher.domain.repository.UserDataRepository
import com.eblan.launcher.domain.repository.WidgetGridItemRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject

class GetGridItemsUseCase @Inject constructor(
    private val userDataRepository: UserDataRepository,
    private val applicationInfoGridItemRepository: ApplicationInfoGridItemRepository,
    private val widgetGridItemRepository: WidgetGridItemRepository,
    private val shortcutInfoGridItemRepository: ShortcutInfoGridItemRepository,
    private val folderGridItemRepository: FolderGridItemRepository,
    private val shortcutConfigGridItemRepository: ShortcutConfigGridItemRepository,
    private val fileManager: FileManager,
    private val iconKeyGenerator: IconKeyGenerator,
    @param:Dispatcher(EblanDispatchers.Default) private val defaultDispatcher: CoroutineDispatcher,
) {
    suspend operator fun invoke(): List<GridItem> = withContext(defaultDispatcher) {
        val userData = userDataRepository.userDataFlow.first()

        val currentApplicationGridItems =
            applicationInfoGridItemRepository.getApplicationInfoGridItems().map {
                ensureActive()

                it.asGridItem(
                    fileManager = fileManager,
                    iconKeyGenerator = iconKeyGenerator,
                    iconPackInfoPackageName = userData.generalSettings.iconPackInfoPackageName,
                )
            }

        val currentWidgetGridItems = widgetGridItemRepository.getWidgetGridItems().map {
            ensureActive()

            it.asGridItem()
        }

        val currentShortcutInfoGridItems =
            shortcutInfoGridItemRepository.getShortcutInfoGridItems().map {
                ensureActive()

                it.asGridItem()
            }

        val currentShortcutConfigGridItems =
            shortcutConfigGridItemRepository.getShortcutConfigGridItems().map {
                ensureActive()

                it.asGridItem()
            }

        val currentFolderGridItems = folderGridItemRepository.getFolderGridItems().map {
            ensureActive()

            it.asEmptyFolderGridItem()
        }

        buildList {
            addAll(currentApplicationGridItems)
            addAll(currentWidgetGridItems)
            addAll(currentShortcutInfoGridItems)
            addAll(currentShortcutConfigGridItems)
            addAll(currentFolderGridItems)
        }
    }
}
