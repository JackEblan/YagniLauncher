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
import com.eblan.launcher.domain.model.Associate
import com.eblan.launcher.domain.model.GridItem
import com.eblan.launcher.domain.repository.GridCacheRepository
import com.eblan.launcher.domain.repository.GridRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject

class UpdateGridItemsAfterResizeUseCase @Inject constructor(
    private val gridCacheRepository: GridCacheRepository,
    private val gridRepository: GridRepository,
    @param:Dispatcher(EblanDispatchers.Default) private val defaultDispatcher: CoroutineDispatcher,
) {
    suspend operator fun invoke(resizingGridItem: GridItem) {
        withContext(defaultDispatcher) {
            val gridItems = gridCacheRepository.gridItemsCache.first()

            when (resizingGridItem.associate) {
                Associate.Grid -> {
                    gridRepository.updateGridItems(
                        gridItems = gridItems.filter { gridItem ->
                            gridItem.page == resizingGridItem.page
                        },
                    )
                }

                Associate.Dock -> {
                    gridRepository.updateGridItems(gridItems = gridItems)
                }
            }
        }
    }
}
