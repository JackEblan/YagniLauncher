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
import com.eblan.launcher.domain.model.EblanApplicationInfoGroup
import com.eblan.launcher.domain.model.EblanShortcutConfig
import com.eblan.launcher.domain.model.EblanUser
import com.eblan.launcher.domain.repository.EblanShortcutConfigRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

class GetEblanShortcutConfigsByLabelUseCase @Inject constructor(
    private val eblanShortcutConfigRepository: EblanShortcutConfigRepository,
    private val launcherAppsWrapper: LauncherAppsWrapper,
    @param:Dispatcher(EblanDispatchers.Default) private val defaultDispatcher: CoroutineDispatcher,
) {
    operator fun invoke(labelFlow: Flow<String>): Flow<Map<EblanUser, Map<EblanApplicationInfoGroup, List<EblanShortcutConfig>>>> = combine(
        eblanShortcutConfigRepository.eblanShortcutConfigs,
        labelFlow,
    ) { eblanShortcutConfigs, label ->
        eblanShortcutConfigs.filter { eblanShortcutConfig ->
            eblanShortcutConfig.applicationLabel.toString()
                .contains(
                    other = label,
                    ignoreCase = true,
                )
        }.sortedWith(
            compareBy(
                { it.serialNumber },
                { it.applicationLabel?.lowercase() },
            ),
        ).groupBy { eblanShortcutConfig ->
            launcherAppsWrapper.getUser(serialNumber = eblanShortcutConfig.serialNumber)
        }.mapValues { entry ->
            entry.value.groupBy { eblanShortcutConfig ->
                EblanApplicationInfoGroup(
                    serialNumber = eblanShortcutConfig.serialNumber,
                    packageName = eblanShortcutConfig.packageName,
                    icon = eblanShortcutConfig.applicationIcon,
                    label = eblanShortcutConfig.applicationLabel,
                )
            }
        }
    }.flowOn(defaultDispatcher)
}
