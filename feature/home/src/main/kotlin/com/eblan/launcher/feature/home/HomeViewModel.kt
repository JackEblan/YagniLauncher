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
package com.eblan.launcher.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eblan.launcher.domain.common.IconKeyGenerator
import com.eblan.launcher.domain.framework.AppWidgetHostWrapper
import com.eblan.launcher.domain.framework.FileManager
import com.eblan.launcher.domain.framework.LauncherAppsWrapper
import com.eblan.launcher.domain.framework.PackageManagerWrapper
import com.eblan.launcher.domain.model.AppDrawerSettings
import com.eblan.launcher.domain.model.ApplicationInfoGridItem
import com.eblan.launcher.domain.model.Associate
import com.eblan.launcher.domain.model.EblanApplicationInfo
import com.eblan.launcher.domain.model.EditPageData
import com.eblan.launcher.domain.model.GetEblanApplicationInfosByLabel
import com.eblan.launcher.domain.model.GridItem
import com.eblan.launcher.domain.model.GridItemData
import com.eblan.launcher.domain.model.GridItemData.ShortcutInfo
import com.eblan.launcher.domain.model.LauncherAppsEvent
import com.eblan.launcher.domain.model.MoveGridItemResult
import com.eblan.launcher.domain.model.PageItem
import com.eblan.launcher.domain.model.PinItemRequestType
import com.eblan.launcher.domain.repository.ApplicationInfoGridItemRepository
import com.eblan.launcher.domain.repository.EblanAppWidgetProviderInfoRepository
import com.eblan.launcher.domain.repository.EblanApplicationInfoTagRepository
import com.eblan.launcher.domain.repository.GridCacheRepository
import com.eblan.launcher.domain.repository.GridRepository
import com.eblan.launcher.domain.repository.UserDataRepository
import com.eblan.launcher.domain.usecase.GetHomeDataUseCase
import com.eblan.launcher.domain.usecase.application.GetEblanAppWidgetProviderInfosByLabelUseCase
import com.eblan.launcher.domain.usecase.application.GetEblanApplicationInfosByLabelUseCase
import com.eblan.launcher.domain.usecase.application.GetEblanShortcutConfigsByLabelUseCase
import com.eblan.launcher.domain.usecase.application.GetEblanShortcutInfosUseCase
import com.eblan.launcher.domain.usecase.application.UpdateEblanApplicationInfosIndexesUseCase
import com.eblan.launcher.domain.usecase.grid.GetFolderGridItemsByIdUseCase
import com.eblan.launcher.domain.usecase.grid.MoveFolderGridItemOutsideFolderUseCase
import com.eblan.launcher.domain.usecase.grid.MoveFolderGridItemUseCase
import com.eblan.launcher.domain.usecase.grid.MoveGridItemUseCase
import com.eblan.launcher.domain.usecase.grid.ResizeGridItemUseCase
import com.eblan.launcher.domain.usecase.grid.UpdateGridItemsAfterMoveUseCase
import com.eblan.launcher.domain.usecase.grid.UpdateGridItemsAfterResizeUseCase
import com.eblan.launcher.domain.usecase.iconpack.GetIconPackFilePathsUseCase
import com.eblan.launcher.domain.usecase.launcherapps.AddPackageUseCase
import com.eblan.launcher.domain.usecase.launcherapps.ChangePackageUseCase
import com.eblan.launcher.domain.usecase.launcherapps.ChangeShortcutsUseCase
import com.eblan.launcher.domain.usecase.launcherapps.RemovePackageUseCase
import com.eblan.launcher.domain.usecase.launcherapps.SyncDataUseCase
import com.eblan.launcher.domain.usecase.page.CachePageItemsUseCase
import com.eblan.launcher.domain.usecase.page.UpdatePageItemsUseCase
import com.eblan.launcher.domain.usecase.pin.GetPinGridItemUseCase
import com.eblan.launcher.feature.home.model.GridItemSource
import com.eblan.launcher.feature.home.model.HomeUiState
import com.eblan.launcher.feature.home.model.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
internal class HomeViewModel @Inject constructor(
    getHomeDataUseCase: GetHomeDataUseCase,
    private val gridCacheRepository: GridCacheRepository,
    private val moveGridItemUseCase: MoveGridItemUseCase,
    private val resizeGridItemUseCase: ResizeGridItemUseCase,
    private val cachePageItemsUseCase: CachePageItemsUseCase,
    private val updatePageItemsUseCase: UpdatePageItemsUseCase,
    private val appWidgetHostWrapper: AppWidgetHostWrapper,
    private val updateGridItemsAfterResizeUseCase: UpdateGridItemsAfterResizeUseCase,
    private val updateGridItemsAfterMoveUseCase: UpdateGridItemsAfterMoveUseCase,
    private val getPinGridItemUseCase: GetPinGridItemUseCase,
    private val fileManager: FileManager,
    private val packageManagerWrapper: PackageManagerWrapper,
    getEblanShortcutInfosUseCase: GetEblanShortcutInfosUseCase,
    eblanAppWidgetProviderInfoRepository: EblanAppWidgetProviderInfoRepository,
    getIconPackFilePathsUseCase: GetIconPackFilePathsUseCase,
    getEblanApplicationInfosByLabelUseCase: GetEblanApplicationInfosByLabelUseCase,
    getEblanAppWidgetProviderInfosByLabelUseCase: GetEblanAppWidgetProviderInfosByLabelUseCase,
    getEblanShortcutConfigsByLabelUseCase: GetEblanShortcutConfigsByLabelUseCase,
    private val gridRepository: GridRepository,
    eblanApplicationInfoTagRepository: EblanApplicationInfoTagRepository,
    private val syncDataUseCase: SyncDataUseCase,
    private val launcherAppsWrapper: LauncherAppsWrapper,
    private val addPackageUseCase: AddPackageUseCase,
    private val removePackageUseCase: RemovePackageUseCase,
    private val changePackageUseCase: ChangePackageUseCase,
    private val changeShortcutsUseCase: ChangeShortcutsUseCase,
    private val userDataRepository: UserDataRepository,
    private val updateEblanApplicationInfosIndexesUseCase: UpdateEblanApplicationInfosIndexesUseCase,
    getFolderGridItemsByIdUseCase: GetFolderGridItemsByIdUseCase,
    private val moveFolderGridItemUseCase: MoveFolderGridItemUseCase,
    private val moveFolderGridItemOutsideFolderUseCase: MoveFolderGridItemOutsideFolderUseCase,
    private val applicationInfoGridItemRepository: ApplicationInfoGridItemRepository,
    private val iconKeyGenerator: IconKeyGenerator,
) : ViewModel() {
    private val _isCache = MutableStateFlow(false)

    val homeUiState = getHomeDataUseCase(isCacheFlow = _isCache).map(HomeUiState::Success).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState.Loading,
    )

    private val _screen = MutableStateFlow(Screen.Pager)

    val screen = _screen.asStateFlow()

    private val _moveGridItemResult = MutableStateFlow<MoveGridItemResult?>(null)

    val movedGridItemResult = _moveGridItemResult.asStateFlow()

    private val defaultDelay = 500L

    private val moveDelay = 100L

    private val _editPageData = MutableStateFlow<EditPageData?>(null)

    val editPageData = _editPageData.asStateFlow()

    private var moveGridItemJob: Job? = null

    private val _pinGridItem = MutableStateFlow<GridItem?>(null)

    val pinGridItem = _pinGridItem.asStateFlow()

    val eblanShortcutInfosGroup = getEblanShortcutInfosUseCase().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyMap(),
    )

    val eblanAppWidgetProviderInfosGroup =
        eblanAppWidgetProviderInfoRepository.eblanAppWidgetProviderInfos.map { eblanAppWidgetProviderInfos ->
            eblanAppWidgetProviderInfos.groupBy { eblanAppWidgetProviderInfo ->
                eblanAppWidgetProviderInfo.packageName
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyMap(),
        )

    val iconPackFilePaths = getIconPackFilePathsUseCase().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyMap(),
    )

    private val _eblanApplicationInfoLabel = MutableStateFlow("")

    private val _eblanApplicationInfoTagId = MutableStateFlow<Long?>(null)

    val getEblanApplicationInfosByLabel = getEblanApplicationInfosByLabelUseCase(
        labelFlow = _eblanApplicationInfoLabel,
        eblanApplicationInfoTagIdFlow = _eblanApplicationInfoTagId,
    ).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = GetEblanApplicationInfosByLabel(
            eblanApplicationInfos = emptyMap(),
            privateEblanUser = null,
            privateEblanApplicationInfos = emptyList(),
        ),
    )

    private val _eblanAppWidgetProviderInfoLabel = MutableStateFlow("")

    val eblanAppWidgetProviderInfos =
        getEblanAppWidgetProviderInfosByLabelUseCase(labelFlow = _eblanAppWidgetProviderInfoLabel).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyMap(),
        )

    private val _eblanShortcutConfigLabel = MutableStateFlow("")

    val eblanShortcutConfigs =
        getEblanShortcutConfigsByLabelUseCase(labelFlow = _eblanShortcutConfigLabel).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyMap(),
        )

    val eblanApplicationInfoTags =
        eblanApplicationInfoTagRepository.eblanApplicationInfoTags.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    private var syncDataJob: Job? = null

    private var launcherAppsEventJob: Job? = null

    private val _folderGridItemId = MutableStateFlow<String?>(null)

    val folderGridItem = getFolderGridItemsByIdUseCase(
        isCacheFlow = _isCache,
        idFlow = _folderGridItemId,
    ).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = null,
    )

    private val _resizeGridItem = MutableStateFlow<GridItem?>(null)

    val resizeGridItem = _resizeGridItem.asStateFlow()

    private val _gridItemSource = MutableStateFlow<GridItemSource?>(null)

    val gridItemSource = _gridItemSource.asStateFlow()

    private val _isVisibleOverlay = MutableStateFlow(false)

    val isVisibleOverlay = _isVisibleOverlay.asStateFlow()

    fun moveGridItem(
        movingGridItem: GridItem,
        x: Int,
        y: Int,
        columns: Int,
        rows: Int,
        gridWidth: Int,
        gridHeight: Int,
    ) {
        moveGridItemJob?.cancel()

        moveGridItemJob = viewModelScope.launch {
            delay(moveDelay)

            _moveGridItemResult.update {
                moveGridItemUseCase(
                    movingGridItem = movingGridItem,
                    x = x,
                    y = y,
                    columns = columns,
                    rows = rows,
                    gridWidth = gridWidth,
                    gridHeight = gridHeight,
                )
            }
        }
    }

    fun resizeGridItem(
        resizingGridItem: GridItem,
        columns: Int,
        rows: Int,
    ) {
        moveGridItemJob?.cancel()

        moveGridItemJob = viewModelScope.launch {
            delay(moveDelay)

            _resizeGridItem.update {
                resizeGridItemUseCase(
                    resizingGridItem = resizingGridItem,
                    columns = columns,
                    rows = rows,
                )
            }
        }
    }

    fun showGridCache(gridItems: List<GridItem>) {
        viewModelScope.launch {
            gridCacheRepository.insertGridItems(gridItems = gridItems)

            _isCache.update {
                true
            }
        }
    }

    fun showPageCache(
        gridItems: List<GridItem>,
        associate: Associate,
    ) {
        viewModelScope.launch {
            _screen.update {
                Screen.Loading
            }

            _editPageData.update {
                cachePageItemsUseCase(
                    gridItems = gridItems,
                    associate = associate,
                )
            }

            delay(defaultDelay)

            _screen.update {
                Screen.EditPage
            }
        }
    }

    fun saveEditPage(
        id: Int,
        pageItems: List<PageItem>,
        pageItemsToDelete: List<PageItem>,
        associate: Associate,
    ) {
        viewModelScope.launch {
            _screen.update {
                Screen.Loading
            }

            updatePageItemsUseCase(
                id = id,
                pageItems = pageItems,
                pageItemsToDelete = pageItemsToDelete,
                associate = associate,
            )

            delay(defaultDelay)

            _screen.update {
                Screen.Pager
            }
        }
    }

    fun updateScreen(screen: Screen) {
        _screen.update {
            screen
        }
    }

    fun resetGridCacheAfterResize(resizingGridItem: GridItem) {
        viewModelScope.launch {
            moveGridItemJob?.cancelAndJoin()

            _moveGridItemResult.update {
                null
            }

            _resizeGridItem.update {
                null
            }

            updateGridItemsAfterResizeUseCase(resizingGridItem = resizingGridItem)

            _isCache.update {
                false
            }

            _gridItemSource.update {
                null
            }
        }
    }

    fun resetGridCacheAfterMove(moveGridItemResult: MoveGridItemResult) {
        viewModelScope.launch {
            moveGridItemJob?.cancelAndJoin()

            _moveGridItemResult.update {
                null
            }

            updateGridItemsAfterMoveUseCase(moveGridItemResult = moveGridItemResult)

            _isCache.update {
                false
            }

            _isVisibleOverlay.update {
                false
            }

            _gridItemSource.update {
                null
            }
        }
    }

    fun resetGridCacheAfterMoveWidgetGridItem(moveGridItemResult: MoveGridItemResult) {
        viewModelScope.launch {
            moveGridItemJob?.cancelAndJoin()

            _moveGridItemResult.update {
                null
            }

            gridCacheRepository.updateGridItemData(
                id = moveGridItemResult.movingGridItem.id,
                data = moveGridItemResult.movingGridItem.data,
            )

            updateGridItemsAfterMoveUseCase(moveGridItemResult = moveGridItemResult)

            _isCache.update {
                false
            }

            _gridItemSource.update {
                null
            }
        }
    }

    fun cancelGridCache() {
        viewModelScope.launch {
            moveGridItemJob?.cancelAndJoin()

            _moveGridItemResult.update {
                null
            }

            _isCache.update {
                false
            }

            _isVisibleOverlay.update {
                false
            }

            _gridItemSource.update {
                null
            }
        }
    }

    fun updateShortcutConfigGridItemDataCache(
        byteArray: ByteArray?,
        moveGridItemResult: MoveGridItemResult,
        gridItem: GridItem,
        data: GridItemData.ShortcutConfig,
    ) {
        viewModelScope.launch {
            val shortcutIntentIcon = byteArray?.let { currentByteArray ->
                fileManager.updateAndGetFilePath(
                    fileManager.getFilesDirectory(FileManager.SHORTCUT_INTENT_ICONS_DIR),
                    gridItem.id,
                    currentByteArray,
                )
            }

            gridCacheRepository.updateGridItemData(
                id = gridItem.id,
                data = data.copy(shortcutIntentIcon = shortcutIntentIcon),
            )

            resetGridCacheAfterMove(moveGridItemResult = moveGridItemResult)
        }
    }

    fun resetGridCacheAfterDeleteGridItemCache(gridItem: GridItem) {
        viewModelScope.launch {
            moveGridItemJob?.cancelAndJoin()

            _moveGridItemResult.update {
                null
            }

            gridCacheRepository.deleteGridItemById(id = gridItem.id)

            gridRepository.updateGridItems(gridItems = gridCacheRepository.gridItemsCache.first())

            _isCache.update {
                false
            }

            _gridItemSource.update {
                null
            }
        }
    }

    fun resetGridCacheAfterDeleteWidgetGridItemCache(
        gridItem: GridItem,
        appWidgetId: Int,
    ) {
        viewModelScope.launch {
            moveGridItemJob?.cancelAndJoin()

            _moveGridItemResult.update {
                null
            }

            appWidgetHostWrapper.deleteAppWidgetId(appWidgetId = appWidgetId)

            gridCacheRepository.deleteGridItemById(id = gridItem.id)

            gridRepository.updateGridItems(gridItems = gridCacheRepository.gridItemsCache.first())

            _isCache.update {
                false
            }

            _gridItemSource.update {
                null
            }
        }
    }

    fun getEblanApplicationInfosByLabel(label: String) {
        _eblanApplicationInfoLabel.update {
            label
        }
    }

    fun getEblanAppWidgetProviderInfosByLabel(label: String) {
        _eblanAppWidgetProviderInfoLabel.update {
            label
        }
    }

    fun getEblanShortcutConfigsByLabel(label: String) {
        _eblanShortcutConfigLabel.update {
            label
        }
    }

    fun deleteGridItem(gridItem: GridItem) {
        viewModelScope.launch {
            gridRepository.deleteGridItem(gridItem = gridItem)
        }
    }

    fun getPinGridItem(pinItemRequestType: PinItemRequestType) {
        viewModelScope.launch {
            _pinGridItem.update {
                getPinGridItemUseCase(pinItemRequestType = pinItemRequestType)
            }
        }
    }

    fun resetPinGridItem() {
        _pinGridItem.update {
            null
        }
    }

    fun updateShortcutConfigIntoShortcutInfoGridItem(
        moveGridItemResult: MoveGridItemResult,
        pinItemRequestType: PinItemRequestType.ShortcutInfo,
    ) {
        viewModelScope.launch {
            gridCacheRepository.deleteGridItemById(id = moveGridItemResult.movingGridItem.id)

            val eblanApplicationInfoIcon =
                packageManagerWrapper.getComponentName(packageName = pinItemRequestType.packageName)
                    ?.let { componentName ->
                        val directory = fileManager.getFilesDirectory(FileManager.ICONS_DIR)

                        val file = File(
                            directory,
                            iconKeyGenerator.getActivityIconKey(
                                serialNumber = pinItemRequestType.serialNumber,
                                componentName = componentName,
                            ),
                        )

                        file.absolutePath
                    }

            val data = ShortcutInfo(
                shortcutId = pinItemRequestType.shortcutId,
                packageName = pinItemRequestType.packageName,
                serialNumber = pinItemRequestType.serialNumber,
                shortLabel = pinItemRequestType.shortLabel,
                longLabel = pinItemRequestType.longLabel,
                icon = pinItemRequestType.icon,
                isEnabled = pinItemRequestType.isEnabled,
                eblanApplicationInfoIcon = eblanApplicationInfoIcon,
                customIcon = null,
                customShortLabel = null,
            )

            gridCacheRepository.insertGridItem(
                gridItem = moveGridItemResult.movingGridItem.copy(
                    data = data,
                ),
            )

            resetGridCacheAfterMove(moveGridItemResult = moveGridItemResult)
        }
    }

    fun getEblanApplicationInfosByTagId(id: Long?) {
        _eblanApplicationInfoTagId.update {
            id
        }
    }

    fun startSyncData() {
        syncDataJob = viewModelScope.launch {
            syncDataUseCase()
        }

        launcherAppsEventJob = viewModelScope.launch {
            launcherAppsWrapper.launcherAppsEvent.collect { launcherAppsEvent ->
                when (launcherAppsEvent) {
                    is LauncherAppsEvent.PackageAdded -> {
                        addPackageUseCase(
                            serialNumber = launcherAppsEvent.serialNumber,
                            packageName = launcherAppsEvent.packageName,
                        )
                    }

                    is LauncherAppsEvent.PackageChanged -> {
                        changePackageUseCase(
                            serialNumber = launcherAppsEvent.serialNumber,
                            packageName = launcherAppsEvent.packageName,
                        )
                    }

                    is LauncherAppsEvent.PackageRemoved -> {
                        removePackageUseCase(
                            serialNumber = launcherAppsEvent.serialNumber,
                            packageName = launcherAppsEvent.packageName,
                        )
                    }

                    is LauncherAppsEvent.ShortcutsChanged -> {
                        changeShortcutsUseCase(
                            serialNumber = launcherAppsEvent.serialNumber,
                            packageName = launcherAppsEvent.packageName,
                            launcherAppsShortcutInfos = launcherAppsEvent.launcherAppsShortcutInfos,
                        )
                    }
                }
            }
        }
    }

    fun stopSyncData() {
        syncDataJob?.cancel()

        launcherAppsEventJob?.cancel()

        syncDataJob = null

        launcherAppsEventJob = null
    }

    fun updateAppDrawerSettings(appDrawerSettings: AppDrawerSettings) {
        viewModelScope.launch {
            userDataRepository.updateAppDrawerSettings(appDrawerSettings = appDrawerSettings)
        }
    }

    fun updateEblanApplicationInfos(eblanApplicationInfos: List<EblanApplicationInfo>) {
        viewModelScope.launch {
            updateEblanApplicationInfosIndexesUseCase(eblanApplicationInfos = eblanApplicationInfos)
        }
    }

    fun updateFolderGridItemId(id: String?) {
        _folderGridItemId.update {
            id
        }
    }

    fun moveFolderGridItem(
        folderGridItem: GridItem,
        applicationInfoGridItems: List<ApplicationInfoGridItem>,
        movingApplicationInfoGridItem: ApplicationInfoGridItem,
        dragX: Int,
        dragY: Int,
        columns: Int,
        rows: Int,
        gridWidth: Int,
        gridHeight: Int,
        currentPage: Int,
    ) {
        moveGridItemJob?.cancel()

        moveGridItemJob = viewModelScope.launch {
            delay(moveDelay)

            moveFolderGridItemUseCase(
                folderGridItem = folderGridItem,
                applicationInfoGridItems = applicationInfoGridItems,
                movingApplicationInfoGridItem = movingApplicationInfoGridItem,
                dragX = dragX,
                dragY = dragY,
                columns = columns,
                rows = rows,
                gridWidth = gridWidth,
                gridHeight = gridHeight,
                currentPage = currentPage,
            )
        }
    }

    fun resetGridCacheAfterMoveFolder(moveGridItemResult: MoveGridItemResult?) {
        viewModelScope.launch {
            moveGridItemJob?.cancelAndJoin()

            moveGridItemResult?.movingGridItem?.id?.let { id ->
                gridCacheRepository.deleteGridItemById(id = id)

                _moveGridItemResult.update {
                    null
                }
            }

            gridRepository.updateGridItems(gridItems = gridCacheRepository.gridItemsCache.first())

            _isCache.update {
                false
            }

            _isVisibleOverlay.update {
                false
            }

            _gridItemSource.update {
                null
            }
        }
    }

    fun moveFolderGridItemOutsideFolder(
        folderGridItem: GridItem,
        movingApplicationInfoGridItem: ApplicationInfoGridItem,
        applicationInfoGridItems: List<ApplicationInfoGridItem>,
    ) {
        viewModelScope.launch {
            moveGridItemJob?.cancelAndJoin()

            moveFolderGridItemOutsideFolderUseCase(
                folderGridItem = folderGridItem,
                movingApplicationInfoGridItem = movingApplicationInfoGridItem,
                applicationInfoGridItems = applicationInfoGridItems,
            )

            _folderGridItemId.update {
                null
            }
        }
    }

    fun deleteApplicationInfoGridItem(applicationInfoGridItem: ApplicationInfoGridItem) {
        viewModelScope.launch {
            applicationInfoGridItemRepository.deleteApplicationInfoGridItem(applicationInfoGridItem = applicationInfoGridItem)
        }
    }

    fun updateGridItemSource(gridItemSource: GridItemSource) {
        _gridItemSource.update {
            gridItemSource
        }
    }

    fun updateIsVisibleOverlay(isVisibleOverlay: Boolean) {
        _isVisibleOverlay.update {
            isVisibleOverlay
        }
    }
}
