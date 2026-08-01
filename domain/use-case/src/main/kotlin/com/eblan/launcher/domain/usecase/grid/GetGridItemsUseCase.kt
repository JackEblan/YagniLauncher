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
import com.eblan.launcher.domain.repository.GridRepository
import com.eblan.launcher.domain.repository.UserDataRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject

class GetGridItemsUseCase @Inject constructor(
    private val userDataRepository: UserDataRepository,
    private val fileManager: FileManager,
    private val iconKeyGenerator: IconKeyGenerator,
    private val gridRepository: GridRepository,
    @param:Dispatcher(EblanDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
) {
    suspend operator fun invoke(): List<GridItem> = withContext(ioDispatcher) {
        val userData = userDataRepository.userDataFlow.first()

        val gridItems = gridRepository.getGridItems()

        buildList {
            addAll(
                gridItems.applicationInfoGridItems.map {
                    it.asGridItem(
                        fileManager = fileManager,
                        iconKeyGenerator = iconKeyGenerator,
                        iconPackInfoPackageName = userData.generalSettings.iconPackInfoPackageName,
                    )
                },
            )
            addAll(
                gridItems.widgetGridItems.map {
                    it.asGridItem()
                },
            )
            addAll(
                gridItems.shortcutInfoGridItems.map {
                    it.asGridItem()
                },
            )
            addAll(
                gridItems.shortcutConfigGridItems.map {
                    it.asGridItem()
                },
            )
            addAll(
                gridItems.folderGridItems.map {
                    it.asEmptyFolderGridItem()
                },
            )
        }
    }
}
