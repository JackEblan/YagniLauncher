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
package com.eblan.launcher.domain.usecase.launcherapps

import com.eblan.launcher.domain.common.dispatcher.Dispatcher
import com.eblan.launcher.domain.common.dispatcher.EblanDispatchers
import com.eblan.launcher.domain.framework.FileManager
import com.eblan.launcher.domain.framework.LauncherAppsWrapper
import com.eblan.launcher.domain.framework.PackageManagerWrapper
import com.eblan.launcher.domain.model.DeleteEblanShortcutInfo
import com.eblan.launcher.domain.model.EblanShortcutInfo
import com.eblan.launcher.domain.model.LauncherAppsShortcutInfo
import com.eblan.launcher.domain.repository.EblanShortcutInfoRepository
import com.eblan.launcher.domain.repository.ShortcutInfoGridItemRepository
import com.eblan.launcher.domain.repository.UserDataRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

class ChangeShortcutsUseCase @Inject constructor(
    private val eblanShortcutInfoRepository: EblanShortcutInfoRepository,
    private val launcherAppsWrapper: LauncherAppsWrapper,
    private val userDataRepository: UserDataRepository,
    private val shortcutInfoGridItemRepository: ShortcutInfoGridItemRepository,
    private val fileManager: FileManager,
    private val packageManagerWrapper: PackageManagerWrapper,
    @param:Dispatcher(EblanDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
) {
    suspend operator fun invoke(
        serialNumber: Long,
        packageName: String,
        launcherAppsShortcutInfos: List<LauncherAppsShortcutInfo>?,
    ) {
        if (!launcherAppsWrapper.hasShortcutHostPermission) {
            return
        }

        withContext(ioDispatcher) {
            if (!userDataRepository.userData.first().experimentalSettings.syncData ||
                launcherAppsShortcutInfos === null
            ) {
                return@withContext
            }

            val oldEblanShortcutInfos = eblanShortcutInfoRepository.getEblanShortcutInfos(
                serialNumber = serialNumber,
                packageName = packageName,
            )

            val newEblanShortcutInfos = launcherAppsShortcutInfos.map { launcherAppsShortcutInfo ->
                ensureActive()

                launcherAppsShortcutInfo.toEblanShortcutInfo()
            }

            if (oldEblanShortcutInfos.toSet() != newEblanShortcutInfos.toSet()) {
                val newDeleteEblanShortcutInfos = newEblanShortcutInfos.map { eblanShortcutInfo ->
                    eblanShortcutInfo.toDeleteEblanShortcutInfo()
                }.toSet()

                val oldDeleteEblanShortcutInfos = oldEblanShortcutInfos.map { eblanShortcutInfo ->
                    eblanShortcutInfo.toDeleteEblanShortcutInfo()
                }.filter { deleteEblanShortcutInfo ->
                    deleteEblanShortcutInfo !in newDeleteEblanShortcutInfos
                }

                eblanShortcutInfoRepository.upsertEblanShortcutInfos(
                    eblanShortcutInfos = newEblanShortcutInfos,
                )

                eblanShortcutInfoRepository.deleteEblanShortcutInfos(
                    deleteEblanShortcutInfos = oldDeleteEblanShortcutInfos,
                )

                deleteEblanShortInfoIcons(oldDeleteEblanShortcutInfos = oldDeleteEblanShortcutInfos)

                updateShortcutInfoGridItems(
                    eblanShortcutInfos = eblanShortcutInfoRepository.getEblanShortcutInfos(),
                    shortcutInfoGridItemRepository = shortcutInfoGridItemRepository,
                    fileManager = fileManager,
                    packageManagerWrapper = packageManagerWrapper,
                )
            }
        }
    }
}
