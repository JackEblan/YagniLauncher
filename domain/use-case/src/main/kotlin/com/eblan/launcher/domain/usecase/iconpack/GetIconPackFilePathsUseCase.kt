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
package com.eblan.launcher.domain.usecase.iconpack

import com.eblan.launcher.domain.common.Dispatcher
import com.eblan.launcher.domain.common.EblanDispatchers
import com.eblan.launcher.domain.common.IconKeyGenerator
import com.eblan.launcher.domain.framework.FileManager
import com.eblan.launcher.domain.repository.EblanApplicationInfoRepository
import com.eblan.launcher.domain.repository.UserDataRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import java.io.File
import javax.inject.Inject

class GetIconPackFilePathsUseCase @Inject constructor(
    private val userDataRepository: UserDataRepository,
    private val eblanApplicationInfoRepository: EblanApplicationInfoRepository,
    private val fileManager: FileManager,
    private val iconKeyGenerator: IconKeyGenerator,
    @param:Dispatcher(EblanDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
) {
    operator fun invoke(): Flow<Map<String, String>> = combine(
        userDataRepository.userDataFlow,
        eblanApplicationInfoRepository.eblanApplicationInfosFlow,
    ) { userData, eblaApplicationInfos ->
        val iconPacksDirectory = fileManager.getFilesDirectory(
            FileManager.ICON_PACKS_DIR,
        )

        val iconPackInfoPackageName = userData.generalSettings.iconPackInfoPackageName

        if (iconPackInfoPackageName.isNotEmpty()) {
            val iconPackDirectory = File(
                iconPacksDirectory,
                iconPackInfoPackageName,
            )

            buildMap {
                eblaApplicationInfos.forEach { eblanApplicationInfo ->
                    val iconPackFile = File(
                        iconPackDirectory,
                        iconKeyGenerator.getHashedName(name = eblanApplicationInfo.componentName),
                    )

                    if (iconPackFile.exists()) {
                        put(eblanApplicationInfo.componentName, iconPackFile.absolutePath)
                    }
                }
            }
        } else {
            emptyMap()
        }
    }.flowOn(ioDispatcher)
}
