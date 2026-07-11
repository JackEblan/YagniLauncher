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
package com.eblan.launcher.domain.usecase.application

import com.eblan.launcher.domain.common.Dispatcher
import com.eblan.launcher.domain.common.EblanDispatchers
import com.eblan.launcher.domain.model.EblanApplicationInfo
import com.eblan.launcher.domain.repository.EblanApplicationInfoRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject

class UpdateEblanApplicationInfosIndexesUseCase @Inject constructor(
    private val eblanApplicationInfoRepository: EblanApplicationInfoRepository,
    @param:Dispatcher(EblanDispatchers.DEFAULT) private val defaultDispatcher: CoroutineDispatcher,
) {
    suspend operator fun invoke(eblanApplicationInfos: List<EblanApplicationInfo>) {
        withContext(defaultDispatcher) {
            val eblanApplicationInfoIndexes =
                eblanApplicationInfos.mapIndexed { index, eblanApplicationInfo ->
                    eblanApplicationInfo.copy(index = index)
                }

            eblanApplicationInfoRepository.updateEblanApplicationInfos(eblanApplicationInfos = eblanApplicationInfoIndexes)
        }
    }
}
