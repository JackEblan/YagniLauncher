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
import com.eblan.launcher.domain.repository.FolderGridItemRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetFolderGridItemsUseCase @Inject constructor(
    private val folderGridItemRepository: FolderGridItemRepository,
    @param:Dispatcher(EblanDispatchers.Default) private val defaultDispatcher: CoroutineDispatcher,
) {
    operator fun invoke(): Flow<List<GridItem>> = folderGridItemRepository.folderGridItemWrappersFlow.map { folderGridItemWrappers ->
        folderGridItemWrappers.map { folderGridItemWrapper ->
            folderGridItemWrapper.asGridItem()
        }
    }.flowOn(defaultDispatcher)
}
