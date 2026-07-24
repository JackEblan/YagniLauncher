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
import com.eblan.launcher.domain.common.IconKeyGenerator
import com.eblan.launcher.domain.framework.FileManager
import com.eblan.launcher.domain.framework.JaroWinklerSimilarityWrapper
import com.eblan.launcher.domain.framework.LauncherAppsWrapper
import com.eblan.launcher.domain.framework.TransliteratorWrapper
import com.eblan.launcher.domain.model.AppDrawerType
import com.eblan.launcher.domain.model.EblanApplicationInfo
import com.eblan.launcher.domain.model.EblanApplicationInfoOrder
import com.eblan.launcher.domain.model.EblanApplicationInfoWithIconPackInfo
import com.eblan.launcher.domain.model.EblanUserPageKey
import com.eblan.launcher.domain.model.EblanUserType
import com.eblan.launcher.domain.model.GetEblanApplicationInfosByLabelAndTag
import com.eblan.launcher.domain.repository.EblanApplicationInfoRepository
import com.eblan.launcher.domain.repository.UserDataRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import java.io.File
import javax.inject.Inject

class GetEblanApplicationInfosByLabelAndTagUseCase @Inject constructor(
    private val eblanApplicationInfoRepository: EblanApplicationInfoRepository,
    private val launcherAppsWrapper: LauncherAppsWrapper,
    private val userDataRepository: UserDataRepository,
    private val fileManager: FileManager,
    private val iconKeyGenerator: IconKeyGenerator,
    private val jaroWinklerSimilarityWrapper: JaroWinklerSimilarityWrapper,
    private val transliteratorWrapper: TransliteratorWrapper,
    @param:Dispatcher(EblanDispatchers.Default) private val defaultDispatcher: CoroutineDispatcher,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(
        labelFlow: Flow<String>,
        eblanApplicationInfoTagIdFlow: Flow<Long?>,
    ): Flow<GetEblanApplicationInfosByLabelAndTag> = combine(
        eblanApplicationInfoTagIdFlow,
        labelFlow,
        userDataRepository.userDataFlow,
        eblanApplicationInfoRepository.eblanApplicationInfosFlow,
    ) { tagId, label, userData, eblanApplicationInfos ->
        val iconPacksDirectory = fileManager.getFilesDirectory(
            FileManager.ICON_PACKS_DIR,
        )

        val iconPackInfoPackageName = userData.generalSettings.iconPackInfoPackageName

        val iconPackDirectory = File(
            iconPacksDirectory,
            iconPackInfoPackageName,
        )

        val eblanApplicationInfoWithIconPackInfosByLabel = filterEblanApplicationInfos(
            iconPackDirectory = iconPackDirectory,
            label = label,
            fuzzySearch = userData.appDrawerSettings.fuzzySearch,
            eblanApplicationInfos = if (tagId != null) {
                eblanApplicationInfoRepository.getEblanApplicationInfosByTagId(id = tagId)
            } else if (userData.appDrawerSettings.excludeTaggedApps) {
                eblanApplicationInfoRepository.getEblanApplicationInfosWithoutTag()
            } else {
                eblanApplicationInfos
            },
        )

        updateEblanApplicationInfoIndexes(
            eblanApplicationInfoOrder = userData.appDrawerSettings.eblanApplicationInfoOrder,
            eblanApplicationInfos = eblanApplicationInfoWithIconPackInfosByLabel,
        )

        when (userData.appDrawerSettings.appDrawerType) {
            AppDrawerType.Vertical, AppDrawerType.List -> {
                getVerticalOrListEblanApplicationInfosByLabel(eblanApplicationInfos = eblanApplicationInfoWithIconPackInfosByLabel)
            }

            AppDrawerType.Horizontal -> {
                getHorizontalEblanApplicationInfosByLabel(
                    horizontalAppDrawerColumns = userData.appDrawerSettings.horizontalAppDrawerColumns,
                    horizontalAppDrawerRows = userData.appDrawerSettings.horizontalAppDrawerRows,
                    eblanApplicationInfosByLabel = eblanApplicationInfoWithIconPackInfosByLabel,
                )
            }
        }
    }.flowOn(defaultDispatcher)

    private fun getVerticalOrListEblanApplicationInfosByLabel(eblanApplicationInfos: MutableList<EblanApplicationInfoWithIconPackInfo>): GetEblanApplicationInfosByLabelAndTag {
        val groupedEblanApplicationInfos = eblanApplicationInfos.groupBy {
            EblanUserPageKey(
                eblanUser = launcherAppsWrapper.getUser(serialNumber = it.eblanApplicationInfo.serialNumber),
                page = 0,
            )
        }.toSortedMap(nullsLast(compareBy { it.eblanUser.serialNumber }))

        val privateEblanUserPageKey = groupedEblanApplicationInfos.keys.firstOrNull {
            it.eblanUser.eblanUserType == EblanUserType.Private
        }

        return GetEblanApplicationInfosByLabelAndTag(
            eblanApplicationInfoWithIconPackInfos = groupedEblanApplicationInfos.filterKeys { it != privateEblanUserPageKey },
            privateEblanUser = privateEblanUserPageKey?.eblanUser,
            privateEblanApplicationInfoWithIconPackInfos = groupedEblanApplicationInfos[privateEblanUserPageKey].orEmpty(),
        )
    }

    private fun getHorizontalEblanApplicationInfosByLabel(
        horizontalAppDrawerColumns: Int,
        horizontalAppDrawerRows: Int,
        eblanApplicationInfosByLabel: MutableList<EblanApplicationInfoWithIconPackInfo>,
    ): GetEblanApplicationInfosByLabelAndTag {
        val pageSize = horizontalAppDrawerColumns * horizontalAppDrawerRows

        val groupedEblanApplicationInfos = eblanApplicationInfosByLabel.groupBy {
            launcherAppsWrapper.getUser(serialNumber = it.eblanApplicationInfo.serialNumber)
        }.toSortedMap(nullsLast(compareBy { it.serialNumber }))
            .flatMap { (eblanUser, eblanApplicationInfos) ->
                eblanApplicationInfos.chunked(pageSize).mapIndexed { index, eblanApplicationInfos ->
                    EblanUserPageKey(
                        eblanUser = eblanUser,
                        page = index,
                    ) to eblanApplicationInfos
                }
            }.toMap()

        return GetEblanApplicationInfosByLabelAndTag(
            eblanApplicationInfoWithIconPackInfos = groupedEblanApplicationInfos,
            privateEblanUser = null,
            privateEblanApplicationInfoWithIconPackInfos = emptyList(),
        )
    }

    private fun updateEblanApplicationInfoIndexes(
        eblanApplicationInfoOrder: EblanApplicationInfoOrder,
        eblanApplicationInfos: MutableList<EblanApplicationInfoWithIconPackInfo>,
    ) {
        if (eblanApplicationInfoOrder != EblanApplicationInfoOrder.Index) return

        val indexedEblanApplicationInfos =
            eblanApplicationInfos.filter { it.eblanApplicationInfo.index >= 0 }

        indexedEblanApplicationInfos.forEach {
            val fromIndex = eblanApplicationInfos.indexOf(it)

            if (fromIndex > -1) {
                eblanApplicationInfos.removeAt(fromIndex)

                val toIndex = it.eblanApplicationInfo.index.coerceAtMost(eblanApplicationInfos.size)

                eblanApplicationInfos.add(toIndex, it)
            }
        }
    }

    private suspend fun filterEblanApplicationInfos(
        iconPackDirectory: File,
        label: String,
        fuzzySearch: Boolean,
        eblanApplicationInfos: List<EblanApplicationInfo>,
    ): MutableList<EblanApplicationInfoWithIconPackInfo> {
        val normalizedText = transliteratorWrapper.normalize(label)

        val fastFilterEblanApplicationInfos = eblanApplicationInfos.filter {
            it.label.startsWith(normalizedText) ||
                it.label.contains(normalizedText)
        }

        val newEblanApplicationInfos = if (fastFilterEblanApplicationInfos.isNotEmpty()) {
            fastFilterEblanApplicationInfos
        } else if (!fuzzySearch) {
            emptyList()
        } else {
            eblanApplicationInfos
                .map {
                    it to jaroWinklerSimilarityWrapper.apply(
                        left = normalizedText,
                        right = it.label,
                    )
                }
                .filter { (_, score) -> score >= FUZZY_MATCH_THRESHOLD }
                .sortedByDescending { (_, score) -> score }
                .map { (application, _) -> application }
        }

        return newEblanApplicationInfos
            .map { application ->
                val iconPackInfoFilePath = File(
                    iconPackDirectory,
                    iconKeyGenerator.getHashedName(application.componentName),
                )

                EblanApplicationInfoWithIconPackInfo(
                    eblanApplicationInfo = application,
                    iconPackInfoFilePath = iconPackInfoFilePath
                        .takeIf(File::exists)
                        ?.absolutePath,
                )
            }
            .sortedBy { it.eblanApplicationInfo.label.lowercase() }
            .toMutableList()
    }
}

private const val FUZZY_MATCH_THRESHOLD = 0.85
