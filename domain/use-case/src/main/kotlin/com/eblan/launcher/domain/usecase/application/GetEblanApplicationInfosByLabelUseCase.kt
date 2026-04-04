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
import com.eblan.launcher.domain.framework.LauncherAppsWrapper
import com.eblan.launcher.domain.model.EblanApplicationInfo
import com.eblan.launcher.domain.model.EblanApplicationInfoOrder
import com.eblan.launcher.domain.model.EblanUserType
import com.eblan.launcher.domain.model.GetEblanApplicationInfosByLabel
import com.eblan.launcher.domain.repository.EblanApplicationInfoRepository
import com.eblan.launcher.domain.repository.UserDataRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

class GetEblanApplicationInfosByLabelUseCase @Inject constructor(
    private val eblanApplicationInfoRepository: EblanApplicationInfoRepository,
    private val launcherAppsWrapper: LauncherAppsWrapper,
    private val userDataRepository: UserDataRepository,
    @param:Dispatcher(EblanDispatchers.Default) private val defaultDispatcher: CoroutineDispatcher,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(
        labelFlow: Flow<String>,
        eblanApplicationInfoTagIdFlow: Flow<Long?>,
    ): Flow<GetEblanApplicationInfosByLabel> {
        val eblanApplicationInfosFlow = eblanApplicationInfoTagIdFlow.flatMapLatest { id ->
            if (id != null) {
                eblanApplicationInfoRepository.getEblanApplicationInfosByTagId(id = id)
            } else {
                eblanApplicationInfoRepository.eblanApplicationInfos
            }
        }

        return combine(
            userDataRepository.userData,
            eblanApplicationInfosFlow,
            labelFlow,
        ) { userData, eblanApplicationInfos, label ->
            val eblanApplicationInfosByLabel =
                eblanApplicationInfos.filter { eblanApplicationInfo ->
                    !eblanApplicationInfo.isHidden && eblanApplicationInfo.label.contains(
                        label,
                        ignoreCase = true,
                    )
                }.sortedBy { it.label.lowercase() }.toMutableList()

            updateEblanApplicationInfoIndexes(
                eblanApplicationInfoOrder = userData.appDrawerSettings.eblanApplicationInfoOrder,
                eblanApplicationInfos = eblanApplicationInfosByLabel,
            )

            val groupedEblanApplicationInfos = eblanApplicationInfosByLabel.groupBy {
                launcherAppsWrapper.getUser(serialNumber = it.serialNumber)
            }.toSortedMap(nullsLast(compareBy { it.serialNumber }))

            val privateEblanUser = groupedEblanApplicationInfos.keys.firstOrNull {
                it.eblanUserType == EblanUserType.Private
            }

            GetEblanApplicationInfosByLabel(
                eblanApplicationInfos = groupedEblanApplicationInfos.filterKeys { eblanUser -> eblanUser != privateEblanUser },
                privateEblanUser = privateEblanUser,
                privateEblanApplicationInfos = groupedEblanApplicationInfos[privateEblanUser].orEmpty(),
            )
        }.flowOn(defaultDispatcher)
    }

    private fun updateEblanApplicationInfoIndexes(
        eblanApplicationInfoOrder: EblanApplicationInfoOrder,
        eblanApplicationInfos: MutableList<EblanApplicationInfo>,
    ) {
        if (eblanApplicationInfoOrder != EblanApplicationInfoOrder.Index) return

        val indexedEblanApplicationInfos = eblanApplicationInfos.filter { it.index >= 0 }

        indexedEblanApplicationInfos.forEach { eblanApplicationInfo ->
            val fromIndex = eblanApplicationInfos.indexOf(eblanApplicationInfo)

            if (fromIndex > -1) {
                eblanApplicationInfos.removeAt(fromIndex)

                val toIndex = eblanApplicationInfo.index.coerceAtMost(eblanApplicationInfos.size)

                eblanApplicationInfos.add(toIndex, eblanApplicationInfo)
            }
        }
    }
}
