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

import com.eblan.launcher.domain.common.FakeIconKeyGenerator
import com.eblan.launcher.domain.framework.FakeFileManager
import com.eblan.launcher.domain.model.AppDrawerSettings
import com.eblan.launcher.domain.model.AppDrawerType
import com.eblan.launcher.domain.model.ApplicationInfoGridItem
import com.eblan.launcher.domain.model.Associate
import com.eblan.launcher.domain.model.BackgroundColor
import com.eblan.launcher.domain.model.EblanAction
import com.eblan.launcher.domain.model.EblanActionType
import com.eblan.launcher.domain.model.EblanApplicationInfoOrder
import com.eblan.launcher.domain.model.ExperimentalSettings
import com.eblan.launcher.domain.model.GeneralSettings
import com.eblan.launcher.domain.model.GestureSettings
import com.eblan.launcher.domain.model.GridItemSettings
import com.eblan.launcher.domain.model.GridItems
import com.eblan.launcher.domain.model.HomeSettings
import com.eblan.launcher.domain.model.HorizontalAlignment
import com.eblan.launcher.domain.model.TextColor
import com.eblan.launcher.domain.model.Theme
import com.eblan.launcher.domain.model.UserData
import com.eblan.launcher.domain.model.VerticalArrangement
import com.eblan.launcher.domain.model.WidgetGridItem
import com.eblan.launcher.domain.repository.FakeGridRepository
import com.eblan.launcher.domain.repository.FakeUserDataRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNull
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

@OptIn(ExperimentalCoroutinesApi::class)
class MoveGridItemUseCaseTest {
    @TempDir
    lateinit var tempDirectory: Path

    private val dispatcher = StandardTestDispatcher()

    @Test
    fun `moves grid item when there are no conflicts`() = runTest(dispatcher) {
        val applicationInfoGridItem = getApplicationInfoGridItem()

        val gridItems = GridItems(
            applicationInfoGridItems = listOf(applicationInfoGridItem),
            widgetGridItems = emptyList(),
            shortcutInfoGridItems = emptyList(),
            shortcutConfigGridItems = emptyList(),
            folderGridItems = emptyList(),
        )

        val gridRepository = FakeGridRepository(
            initialGridItems = gridItems,
        )

        val getGridItemsUseCase = GetGridItemsUseCase(
            userDataRepository = FakeUserDataRepository(
                initialUserData = getUserData(),
            ),
            fileManager = FakeFileManager(
                rootDirectory = tempDirectory.toFile(),
            ),
            iconKeyGenerator = FakeIconKeyGenerator(),
            gridRepository = gridRepository,
            ioDispatcher = dispatcher,
        )

        val moveGridItemUseCase = MoveGridItemUseCase(
            gridRepository = gridRepository,
            getGridItemsUseCase = getGridItemsUseCase,
            defaultDispatcher = dispatcher,
        )

        val movingGridItem = getGridItemsUseCase()
            .single()
            .copy(
                startColumn = 2,
                startRow = 1,
            )

        val moveGridItemResult = moveGridItemUseCase(
            movingGridItem = movingGridItem,
            x = 250,
            y = 150,
            columns = 5,
            rows = 4,
            gridWidth = 500,
            gridHeight = 400,
        )

        assertTrue(moveGridItemResult.isSuccess)
        assertEquals(movingGridItem, moveGridItemResult.movingGridItem)
        assertNull(moveGridItemResult.conflictingGridItem)

        val persistedGridItem = gridRepository
            .getGridItems()
            .applicationInfoGridItems
            .single()

        assertEquals(2, persistedGridItem.startColumn)
        assertEquals(1, persistedGridItem.startRow)
    }

    @Test
    fun `returns conflicting grid item when moving onto normal item at center`() = runTest(dispatcher) {
        val movingItem = getApplicationInfoGridItem(
            id = "moving",
        )

        val conflictingItem = getApplicationInfoGridItem(
            id = "conflicting",
            startColumn = 2,
            startRow = 1,
        )

        val gridRepository = FakeGridRepository(
            initialGridItems = GridItems(
                applicationInfoGridItems = listOf(
                    movingItem,
                    conflictingItem,
                ),
                widgetGridItems = emptyList(),
                shortcutInfoGridItems = emptyList(),
                shortcutConfigGridItems = emptyList(),
                folderGridItems = emptyList(),
            ),
        )

        val getGridItemsUseCase = getGridItemsUseCase(
            gridRepository = gridRepository,
        )

        val moveGridItemUseCase = MoveGridItemUseCase(
            gridRepository = gridRepository,
            getGridItemsUseCase = getGridItemsUseCase,
            defaultDispatcher = dispatcher,
        )

        val movedItem = getGridItemsUseCase()
            .first { it.id == movingItem.id }
            .copy(
                startColumn = 2,
                startRow = 1,
            )

        val result = moveGridItemUseCase(
            movingGridItem = movedItem,
            x = 250,
            y = 150,
            columns = 5,
            rows = 4,
            gridWidth = 500,
            gridHeight = 400,
        )

        assertTrue(result.isSuccess)
        assertEquals(movedItem, result.movingGridItem)
        assertEquals(
            conflictingItem.id,
            result.conflictingGridItem?.id,
        )
    }

    @Test
    fun `ignores grid item from different page`() = runTest(dispatcher) {
        val movingItem = getApplicationInfoGridItem(
            id = "moving",
            page = 0,
        )

        val otherPageItem = getApplicationInfoGridItem(
            id = "other-page",
            page = 1,
            startColumn = 2,
            startRow = 1,
        )

        val gridRepository = FakeGridRepository(
            initialGridItems = GridItems(
                applicationInfoGridItems = listOf(
                    movingItem,
                    otherPageItem,
                ),
                widgetGridItems = emptyList(),
                shortcutInfoGridItems = emptyList(),
                shortcutConfigGridItems = emptyList(),
                folderGridItems = emptyList(),
            ),
        )

        val getGridItemsUseCase = getGridItemsUseCase(
            gridRepository = gridRepository,
        )

        val moveGridItemUseCase = MoveGridItemUseCase(
            gridRepository = gridRepository,
            getGridItemsUseCase = getGridItemsUseCase,
            defaultDispatcher = dispatcher,
        )

        val movedItem = getGridItemsUseCase()
            .first { it.id == movingItem.id }
            .copy(
                startColumn = 2,
                startRow = 1,
            )

        val result = moveGridItemUseCase(
            movingGridItem = movedItem,
            x = 250,
            y = 150,
            columns = 5,
            rows = 4,
            gridWidth = 500,
            gridHeight = 400,
        )

        assertTrue(result.isSuccess)
        assertNull(result.conflictingGridItem)

        val persistedGridItems = gridRepository
            .getGridItems()
            .applicationInfoGridItems

        assertEquals(
            2,
            persistedGridItems
                .first { it.id == movingItem.id }
                .startColumn,
        )

        assertEquals(
            1,
            persistedGridItems
                .first { it.id == movingItem.id }
                .startRow,
        )

        assertEquals(
            2,
            persistedGridItems
                .first { it.id == otherPageItem.id }
                .startColumn,
        )

        assertEquals(
            1,
            persistedGridItems
                .first { it.id == otherPageItem.id }
                .startRow,
        )
    }

    @Test
    fun `ignores grid item with different associate`() = runTest(dispatcher) {
        val movingItem = getApplicationInfoGridItem(
            id = "moving",
            associate = Associate.Grid,
        )

        val dockItem = getApplicationInfoGridItem(
            id = "dock",
            startColumn = 2,
            startRow = 1,
            associate = Associate.Dock,
        )

        val gridRepository = FakeGridRepository(
            initialGridItems = GridItems(
                applicationInfoGridItems = listOf(
                    movingItem,
                    dockItem,
                ),
                widgetGridItems = emptyList(),
                shortcutInfoGridItems = emptyList(),
                shortcutConfigGridItems = emptyList(),
                folderGridItems = emptyList(),
            ),
        )

        val getGridItemsUseCase = getGridItemsUseCase(
            gridRepository = gridRepository,
        )

        val moveGridItemUseCase = MoveGridItemUseCase(
            gridRepository = gridRepository,
            getGridItemsUseCase = getGridItemsUseCase,
            defaultDispatcher = dispatcher,
        )

        val movedItem = getGridItemsUseCase()
            .first { it.id == movingItem.id }
            .copy(
                startColumn = 2,
                startRow = 1,
            )

        val result = moveGridItemUseCase(
            movingGridItem = movedItem,
            x = 250,
            y = 150,
            columns = 5,
            rows = 4,
            gridWidth = 500,
            gridHeight = 400,
        )

        assertTrue(result.isSuccess)
        assertNull(result.conflictingGridItem)

        val persistedGridItems = gridRepository
            .getGridItems()
            .applicationInfoGridItems

        val persistedMovingItem = persistedGridItems
            .first { it.id == movingItem.id }

        val persistedDockItem = persistedGridItems
            .first { it.id == dockItem.id }

        assertEquals(2, persistedMovingItem.startColumn)
        assertEquals(1, persistedMovingItem.startRow)

        assertEquals(2, persistedDockItem.startColumn)
        assertEquals(1, persistedDockItem.startRow)
    }

    @Test
    fun `ignores grid item inside folder`() = runTest(dispatcher) {
        val movingItem = getApplicationInfoGridItem(
            id = "moving",
        )

        val folderChild = getApplicationInfoGridItem(
            id = "folder-child",
            startColumn = 2,
            startRow = 1,
            folderId = "folder",
        )

        val gridRepository = FakeGridRepository(
            initialGridItems = GridItems(
                applicationInfoGridItems = listOf(
                    movingItem,
                    folderChild,
                ),
                widgetGridItems = emptyList(),
                shortcutInfoGridItems = emptyList(),
                shortcutConfigGridItems = emptyList(),
                folderGridItems = emptyList(),
            ),
        )

        val getGridItemsUseCase = getGridItemsUseCase(
            gridRepository = gridRepository,
        )

        val moveGridItemUseCase = MoveGridItemUseCase(
            gridRepository = gridRepository,
            getGridItemsUseCase = getGridItemsUseCase,
            defaultDispatcher = dispatcher,
        )

        val movedItem = getGridItemsUseCase()
            .first { it.id == movingItem.id }
            .copy(
                startColumn = 2,
                startRow = 1,
            )

        val result = moveGridItemUseCase(
            movingGridItem = movedItem,
            x = 250,
            y = 150,
            columns = 5,
            rows = 4,
            gridWidth = 500,
            gridHeight = 400,
        )

        assertTrue(result.isSuccess)
        assertNull(result.conflictingGridItem)

        val persistedGridItems = gridRepository
            .getGridItems()
            .applicationInfoGridItems

        val persistedMovingItem = persistedGridItems
            .first { it.id == movingItem.id }

        val persistedFolderChild = persistedGridItems
            .first { it.id == folderChild.id }

        assertEquals(2, persistedMovingItem.startColumn)
        assertEquals(1, persistedMovingItem.startRow)

        assertEquals(2, persistedFolderChild.startColumn)
        assertEquals(1, persistedFolderChild.startRow)
    }

    @Test
    fun `fails when moving grid item onto widget at center`() = runTest(dispatcher) {
        val movingItem = getApplicationInfoGridItem(
            id = "moving",
        )

        val widgetItem = getWidgetGridItem()

        val gridRepository = FakeGridRepository(
            initialGridItems = GridItems(
                applicationInfoGridItems = listOf(movingItem),
                widgetGridItems = listOf(widgetItem),
                shortcutInfoGridItems = emptyList(),
                shortcutConfigGridItems = emptyList(),
                folderGridItems = emptyList(),
            ),
        )

        val getGridItemsUseCase = getGridItemsUseCase(
            gridRepository = gridRepository,
        )

        val moveGridItemUseCase = MoveGridItemUseCase(
            gridRepository = gridRepository,
            getGridItemsUseCase = getGridItemsUseCase,
            defaultDispatcher = dispatcher,
        )

        val movedItem = getGridItemsUseCase()
            .first { it.id == movingItem.id }
            .copy(
                startColumn = 2,
                startRow = 1,
            )

        val result = moveGridItemUseCase(
            movingGridItem = movedItem,
            x = 250,
            y = 150,
            columns = 5,
            rows = 4,
            gridWidth = 500,
            gridHeight = 400,
        )

        assertFalse(result.isSuccess)
        assertEquals(movedItem, result.movingGridItem)
        assertNull(result.conflictingGridItem)

        val persistedMovingItem = gridRepository
            .getGridItems()
            .applicationInfoGridItems
            .single()

        assertEquals(0, persistedMovingItem.startColumn)
        assertEquals(0, persistedMovingItem.startRow)
    }

    private fun getApplicationInfoGridItem(
        id: String = "app",
        page: Int = 0,
        startColumn: Int = 0,
        startRow: Int = 0,
        associate: Associate = Associate.Grid,
        folderId: String? = null,
    ) = ApplicationInfoGridItem(
        id = id,
        page = page,
        startColumn = startColumn,
        startRow = startRow,
        columnSpan = 1,
        rowSpan = 1,
        associate = associate,
        componentName = "com.example/.MainActivity",
        packageName = "com.example",
        icon = null,
        label = "Example",
        override = false,
        serialNumber = 0L,
        customIcon = null,
        customLabel = null,
        gridItemSettings = getGridItemSettings(),
        doubleTap = getEblanAction(),
        swipeUp = getEblanAction(),
        swipeDown = getEblanAction(),
        index = 0,
        folderId = folderId,
    )

    private fun getWidgetGridItem() = WidgetGridItem(
        id = "widget",
        page = 0,
        startColumn = 2,
        startRow = 1,
        columnSpan = 1,
        rowSpan = 1,
        associate = Associate.Grid,
        appWidgetId = 1,
        packageName = "com.example",
        serialNumber = 0L,
        componentName = "com.example/.Widget",
        configure = null,
        minWidth = 100,
        minHeight = 100,
        resizeMode = 0,
        minResizeWidth = 100,
        minResizeHeight = 100,
        maxResizeWidth = 300,
        maxResizeHeight = 300,
        targetCellHeight = 1,
        targetCellWidth = 1,
        preview = null,
        label = "Example Widget",
        icon = null,
        override = false,
        gridItemSettings = getGridItemSettings(),
    )

    private fun getGridItemSettings() = GridItemSettings(
        iconSize = 48,
        textColor = TextColor.System,
        textSize = 14,
        showLabel = true,
        singleLineLabel = true,
        horizontalAlignment = HorizontalAlignment.CenterHorizontally,
        verticalArrangement = VerticalArrangement.Center,
        customTextColor = 0,
        customBackgroundColor = 0,
        padding = 0,
        cornerRadius = 0,
    )

    private fun getEblanAction() = EblanAction(
        eblanActionType = EblanActionType.None,
        serialNumber = 0L,
        componentName = "",
    )

    private fun getUserData() = UserData(
        homeSettings = getHomeSettings(),
        appDrawerSettings = getAppDrawerSettings(),
        generalSettings = GeneralSettings(
            theme = Theme.System,
            dynamicTheme = false,
            iconPackInfoPackageName = "",
        ),
        gestureSettings = GestureSettings(
            doubleTap = getEblanAction(),
            swipeUp = getEblanAction(),
            swipeDown = getEblanAction(),
        ),
        experimentalSettings = ExperimentalSettings(
            syncData = false,
            firstLaunch = false,
            lockMovement = false,
        ),
    )

    private fun getHomeSettings() = HomeSettings(
        columns = 5,
        rows = 5,
        pageCount = 1,
        infiniteScroll = false,
        dockColumns = 5,
        dockRows = 1,
        dockHeight = 1,
        initialPage = 0,
        wallpaperScroll = false,
        gridItemSettings = getGridItemSettings(),
        lockScreenOrientation = false,
        dockPageCount = 1,
        dockInfiniteScroll = false,
        dockInitialPage = 0,
        addNewAppsToHomeScreen = false,
        folderCellWidth = 1,
        folderCellHeight = 1,
        maxFolderColumns = 4,
        maxFolderRows = 4,
        showPageIndicator = true,
        dockCustomBackgroundColor = 0,
        dockPadding = 0,
        dockTopStartCornerRadius = 0,
        dockTopEndCornerRadius = 0,
        dockBottomStartCornerRadius = 0,
        dockBottomEndCornerRadius = 0,
    )

    private fun getAppDrawerSettings() = AppDrawerSettings(
        appDrawerColumns = 5,
        appDrawerRowsHeight = 5,
        gridItemSettings = getGridItemSettings(),
        eblanApplicationInfoOrder = EblanApplicationInfoOrder.Alphabetical,
        backgroundColor = BackgroundColor.System,
        customBackgroundColor = 0,
        appDrawerType = AppDrawerType.Vertical,
        horizontalAppDrawerColumns = 5,
        horizontalAppDrawerRows = 5,
        excludeTaggedApps = false,
        showKeyboard = false,
        fuzzySearch = false,
        blurBehind = false,
    )

    private fun getGridItemsUseCase(
        gridRepository: FakeGridRepository,
        iconKeyGenerator: FakeIconKeyGenerator = FakeIconKeyGenerator(),
    ): GetGridItemsUseCase = GetGridItemsUseCase(
        userDataRepository = FakeUserDataRepository(
            initialUserData = getUserData(),
        ),
        fileManager = FakeFileManager(
            rootDirectory = tempDirectory.toFile(),
        ),
        iconKeyGenerator = iconKeyGenerator,
        gridRepository = gridRepository,
        ioDispatcher = dispatcher,
    )
}
