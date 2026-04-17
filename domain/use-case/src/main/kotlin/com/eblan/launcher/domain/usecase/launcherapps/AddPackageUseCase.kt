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

import com.eblan.launcher.domain.common.Dispatcher
import com.eblan.launcher.domain.common.EblanDispatchers
import com.eblan.launcher.domain.common.IconKeyGenerator
import com.eblan.launcher.domain.framework.AppWidgetManagerWrapper
import com.eblan.launcher.domain.framework.FileManager
import com.eblan.launcher.domain.framework.IconPackManager
import com.eblan.launcher.domain.framework.LauncherAppsWrapper
import com.eblan.launcher.domain.framework.PackageManagerWrapper
import com.eblan.launcher.domain.grid.findAvailableRegionByPage
import com.eblan.launcher.domain.model.ApplicationInfoGridItem
import com.eblan.launcher.domain.model.Associate
import com.eblan.launcher.domain.model.EblanAction
import com.eblan.launcher.domain.model.EblanActionType
import com.eblan.launcher.domain.model.EblanApplicationInfo
import com.eblan.launcher.domain.model.GridItem
import com.eblan.launcher.domain.model.GridItemData
import com.eblan.launcher.domain.model.HomeSettings
import com.eblan.launcher.domain.model.LauncherAppsActivityInfo
import com.eblan.launcher.domain.repository.ApplicationInfoGridItemRepository
import com.eblan.launcher.domain.repository.EblanAppWidgetProviderInfoRepository
import com.eblan.launcher.domain.repository.EblanApplicationInfoRepository
import com.eblan.launcher.domain.repository.EblanShortcutConfigRepository
import com.eblan.launcher.domain.repository.EblanShortcutInfoRepository
import com.eblan.launcher.domain.repository.GridRepository
import com.eblan.launcher.domain.repository.UserDataRepository
import com.eblan.launcher.domain.usecase.grid.GetFolderGridItemsUseCase
import com.eblan.launcher.domain.usecase.iconpack.cacheIconPackFile
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class AddPackageUseCase @Inject constructor(
    private val userDataRepository: UserDataRepository,
    private val packageManagerWrapper: PackageManagerWrapper,
    private val eblanApplicationInfoRepository: EblanApplicationInfoRepository,
    private val appWidgetManagerWrapper: AppWidgetManagerWrapper,
    private val eblanAppWidgetProviderInfoRepository: EblanAppWidgetProviderInfoRepository,
    private val eblanShortcutInfoRepository: EblanShortcutInfoRepository,
    private val launcherAppsWrapper: LauncherAppsWrapper,
    private val eblanShortcutConfigRepository: EblanShortcutConfigRepository,
    private val fileManager: FileManager,
    private val iconPackManager: IconPackManager,
    private val iconKeyGenerator: IconKeyGenerator,
    private val gridRepository: GridRepository,
    private val getFolderGridItemsUseCase: GetFolderGridItemsUseCase,
    private val applicationInfoGridItemRepository: ApplicationInfoGridItemRepository,
    @param:Dispatcher(EblanDispatchers.Default) private val defaultDispatcher: CoroutineDispatcher,
) {
    suspend operator fun invoke(
        serialNumber: Long,
        packageName: String,
    ) {
        withContext(defaultDispatcher) {
            val userData = userDataRepository.userData.first()

            if (!userData.experimentalSettings.syncData) return@withContext

            val launcherAppsActivityInfosByPackageName = launcherAppsWrapper.getActivityList(
                serialNumber = serialNumber,
                packageName = packageName,
            ).onEach { launcherAppsActivityInfo ->
                currentCoroutineContext().ensureActive()

                eblanApplicationInfoRepository.upsertEblanApplicationInfo(
                    eblanApplicationInfo = EblanApplicationInfo(
                        componentName = launcherAppsActivityInfo.componentName,
                        serialNumber = launcherAppsActivityInfo.serialNumber,
                        packageName = launcherAppsActivityInfo.packageName,
                        icon = launcherAppsActivityInfo.activityIcon,
                        label = launcherAppsActivityInfo.activityLabel,
                        customIcon = null,
                        customLabel = null,
                        isHidden = false,
                        lastUpdateTime = launcherAppsActivityInfo.lastUpdateTime,
                        index = -1,
                    ),
                )

                addNewApplicationToHomeScreen(
                    homeSettings = userData.homeSettings,
                    serialNumber = launcherAppsActivityInfo.serialNumber,
                    componentName = launcherAppsActivityInfo.componentName,
                    packageName = launcherAppsActivityInfo.packageName,
                    icon = launcherAppsActivityInfo.activityIcon,
                    label = launcherAppsActivityInfo.activityLabel,
                )
            }

            addEblanAppWidgetProviderInfos(
                serialNumber = serialNumber,
                packageName = packageName,
            )

            addEblanShortcutInfos(
                serialNumber = serialNumber,
                packageName = packageName,
            )

            addEblanShortcutConfigs(
                serialNumber = serialNumber,
                packageName = packageName,
            )

            addIconPackInfos(
                iconPackInfoPackageName = userData.generalSettings.iconPackInfoPackageName,
                launcherAppsActivityInfos = launcherAppsActivityInfosByPackageName,
            )
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    private suspend fun addNewApplicationToHomeScreen(
        homeSettings: HomeSettings,
        serialNumber: Long,
        componentName: String,
        packageName: String,
        icon: String?,
        label: String,
    ) {
        if (!homeSettings.addNewAppsToHomeScreen) return

        val gridItems = gridRepository.gridItems.first() + getFolderGridItemsUseCase().first()

        val alreadyOnHome = gridItems.any { item ->
            val data = item.data as? GridItemData.ApplicationInfo ?: return@any false

            data.serialNumber == serialNumber && data.componentName == componentName
        }

        if (alreadyOnHome) return

        val eblanAction = EblanAction(
            eblanActionType = EblanActionType.None,
            serialNumber = 0L,
            componentName = "",
        )

        val data = GridItemData.ApplicationInfo(
            serialNumber = serialNumber,
            componentName = componentName,
            packageName = packageName,
            icon = icon,
            label = label,
            customIcon = null,
            customLabel = null,
            index = -1,
            folderId = null,
        )

        val gridItem = GridItem(
            id = Uuid.random().toHexString(),
            page = homeSettings.initialPage,
            startColumn = 0,
            startRow = 0,
            columnSpan = 1,
            rowSpan = 1,
            data = data,
            associate = Associate.Grid,
            override = false,
            gridItemSettings = homeSettings.gridItemSettings,
            doubleTap = eblanAction,
            swipeUp = eblanAction,
            swipeDown = eblanAction,
        )

        val newGridItem = findAvailableRegionByPage(
            gridItems = gridItems,
            gridItem = gridItem,
            pageCount = homeSettings.pageCount,
            columns = homeSettings.columns,
            rows = homeSettings.rows,
        )

        if (newGridItem != null) {
            applicationInfoGridItemRepository.insertApplicationInfoGridItem(
                applicationInfoGridItem = ApplicationInfoGridItem(
                    id = newGridItem.id,
                    page = newGridItem.page,
                    startColumn = newGridItem.startColumn,
                    startRow = newGridItem.startRow,
                    columnSpan = newGridItem.columnSpan,
                    rowSpan = newGridItem.rowSpan,
                    associate = newGridItem.associate,
                    componentName = data.componentName,
                    packageName = data.packageName,
                    icon = data.icon,
                    label = data.label,
                    override = newGridItem.override,
                    serialNumber = data.serialNumber,
                    customIcon = data.customIcon,
                    customLabel = data.customLabel,
                    gridItemSettings = newGridItem.gridItemSettings,
                    doubleTap = newGridItem.doubleTap,
                    swipeUp = newGridItem.swipeUp,
                    swipeDown = newGridItem.swipeDown,
                    index = data.index,
                    folderId = data.folderId,
                ),
            )
        }
    }

    private suspend fun addEblanAppWidgetProviderInfos(
        serialNumber: Long,
        packageName: String,
    ) {
        val eblanAppWidgetProviderInfos = appWidgetManagerWrapper.getInstalledProviders()
            .filter { appWidgetManagerAppWidgetProviderInfo ->
                appWidgetManagerAppWidgetProviderInfo.serialNumber == serialNumber &&
                    appWidgetManagerAppWidgetProviderInfo.packageName == packageName
            }.map { appWidgetManagerAppWidgetProviderInfo ->
                currentCoroutineContext().ensureActive()

                appWidgetManagerAppWidgetProviderInfo.toEblanAppWidgetProviderInfo(
                    fileManager = fileManager,
                    packageManagerWrapper = packageManagerWrapper,
                    iconKeyGenerator = iconKeyGenerator,
                )
            }

        eblanAppWidgetProviderInfoRepository.upsertEblanAppWidgetProviderInfos(
            eblanAppWidgetProviderInfos = eblanAppWidgetProviderInfos,
        )
    }

    private suspend fun addEblanShortcutInfos(
        serialNumber: Long,
        packageName: String,
    ) {
        val eblanShortcutInfos =
            launcherAppsWrapper.getShortcutsByPackageName(
                serialNumber = serialNumber,
                packageName = packageName,
            )?.map { launcherAppsShortcutInfo ->
                currentCoroutineContext().ensureActive()

                launcherAppsShortcutInfo.toEblanShortcutInfo()
            }

        if (eblanShortcutInfos != null) {
            eblanShortcutInfoRepository.upsertEblanShortcutInfos(
                eblanShortcutInfos = eblanShortcutInfos,
            )
        }
    }

    private suspend fun addEblanShortcutConfigs(
        serialNumber: Long,
        packageName: String,
    ) {
        val eblanShortcutConfigs = launcherAppsWrapper.getShortcutConfigActivityList(
            serialNumber = serialNumber,
            packageName = packageName,
        ).map { shortcutConfigActivityInfo ->
            currentCoroutineContext().ensureActive()

            shortcutConfigActivityInfo.toEblanShortcutConfig(
                fileManager = fileManager,
                packageManagerWrapper = packageManagerWrapper,
                iconKeyGenerator = iconKeyGenerator,
            )
        }

        eblanShortcutConfigRepository.upsertEblanShortcutConfigs(
            eblanShortcutConfigs = eblanShortcutConfigs,
        )
    }

    private suspend fun addIconPackInfos(
        iconPackInfoPackageName: String,
        launcherAppsActivityInfos: List<LauncherAppsActivityInfo>,
    ) {
        if (iconPackInfoPackageName.isEmpty()) return

        val iconPackInfoDirectory = File(
            fileManager.getFilesDirectory(name = FileManager.ICON_PACKS_DIR),
            iconPackInfoPackageName,
        ).apply { if (!exists()) mkdirs() }

        val appFilter =
            iconPackManager.getIconPackInfoComponents(packageName = iconPackInfoPackageName)

        launcherAppsActivityInfos.forEach { launcherAppsActivityInfo ->
            currentCoroutineContext().ensureActive()

            val file = File(
                iconPackInfoDirectory,
                iconKeyGenerator.getHashedName(name = launcherAppsActivityInfo.componentName),
            )

            cacheIconPackFile(
                iconPackManager = iconPackManager,
                appFilter = appFilter,
                iconPackInfoPackageName = iconPackInfoPackageName,
                file = file,
                componentName = launcherAppsActivityInfo.componentName,
            )
        }
    }
}
