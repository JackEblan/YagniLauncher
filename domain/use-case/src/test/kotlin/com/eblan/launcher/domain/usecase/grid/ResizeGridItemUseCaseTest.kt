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
import com.eblan.launcher.domain.model.ApplicationInfoGridItem
import com.eblan.launcher.domain.model.Associate
import com.eblan.launcher.domain.model.ExperimentalSettings
import com.eblan.launcher.domain.model.GeneralSettings
import com.eblan.launcher.domain.model.GestureSettings
import com.eblan.launcher.domain.model.GridItems
import com.eblan.launcher.domain.model.Theme
import com.eblan.launcher.domain.model.UserData
import com.eblan.launcher.domain.model.getAppDrawerSettings
import com.eblan.launcher.domain.model.getEblanAction
import com.eblan.launcher.domain.model.getGridItemSettings
import com.eblan.launcher.domain.model.getHomeSettings
import com.eblan.launcher.domain.repository.FakeGridRepository
import com.eblan.launcher.domain.repository.FakeUserDataRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

@OptIn(ExperimentalCoroutinesApi::class)
class ResizeGridItemUseCaseTest {
    @TempDir
    lateinit var tempDirectory: Path

    private val dispatcher = StandardTestDispatcher()

    @Test
    fun `resizes grid item when there are no conflicts`() = runTest(dispatcher) {
        val applicationInfoGridItem = getApplicationInfoGridItem()

        val gridRepository = FakeGridRepository(
            initialGridItems = getGridItems(
                applicationInfoGridItems = listOf(applicationInfoGridItem),
            ),
        )

        val getGridItemsUseCase = getGridItemsUseCase(
            gridRepository = gridRepository,
        )

        val resizeGridItemUseCase = ResizeGridItemUseCase(
            gridRepository = gridRepository,
            getGridItemsUseCase = getGridItemsUseCase,
            defaultDispatcher = dispatcher,
        )

        val resizingGridItem = getGridItemsUseCase()
            .first { it.id == applicationInfoGridItem.id }
            .copy(
                columnSpan = 2,
                rowSpan = 2,
            )

        val resizedGridItem = resizeGridItemUseCase(
            resizingGridItem = resizingGridItem,
            columns = 5,
            rows = 4,
        )

        assertEquals(2, resizedGridItem.columnSpan)
        assertEquals(2, resizedGridItem.rowSpan)
    }

    @Test
    fun `resizes grid item and resolves conflict`() = runTest(dispatcher) {
        val applicationInfoGridItem = getApplicationInfoGridItem(
            id = "resizing",
            columnSpan = 1,
            rowSpan = 1,
        )

        val conflictingApplicationInfoGridItem = getApplicationInfoGridItem(
            id = "conflicting",
            startColumn = 1,
            startRow = 0,
        )

        val gridRepository = FakeGridRepository(
            initialGridItems = getGridItems(
                applicationInfoGridItems = listOf(
                    applicationInfoGridItem,
                    conflictingApplicationInfoGridItem,
                ),
            ),
        )

        val getGridItemsUseCase = getGridItemsUseCase(
            gridRepository = gridRepository,
        )

        val resizeGridItemUseCase = ResizeGridItemUseCase(
            gridRepository = gridRepository,
            getGridItemsUseCase = getGridItemsUseCase,
            defaultDispatcher = dispatcher,
        )

        val resizingGridItem = getGridItemsUseCase()
            .first { it.id == applicationInfoGridItem.id }
            .copy(
                columnSpan = 2,
                rowSpan = 1,
            )

        val resizedGridItem = resizeGridItemUseCase(
            resizingGridItem = resizingGridItem,
            columns = 5,
            rows = 4,
        )

        assertEquals(0, resizedGridItem.startColumn)
        assertEquals(0, resizedGridItem.startRow)
        assertEquals(2, resizedGridItem.columnSpan)
        assertEquals(1, resizedGridItem.rowSpan)

        val resolvedGridItem = getGridItemsUseCase()
            .first { it.id == conflictingApplicationInfoGridItem.id }

        assertEquals(2, resolvedGridItem.startColumn)
        assertEquals(0, resolvedGridItem.startRow)
    }

    @Test
    fun `does not resize grid item when conflict cannot be resolved`() = runTest(dispatcher) {
        val applicationInfoGridItem = getApplicationInfoGridItem(
            id = "resizing",
            columnSpan = 1,
            rowSpan = 1,
        )

        val conflictingApplicationInfoGridItems = listOf(
            getApplicationInfoGridItem(
                id = "conflicting-1",
                startColumn = 1,
                startRow = 0,
                columnSpan = 4,
                rowSpan = 1,
            ),
            getApplicationInfoGridItem(
                id = "conflicting-2",
                startColumn = 1,
                startRow = 1,
                columnSpan = 4,
                rowSpan = 1,
            ),
            getApplicationInfoGridItem(
                id = "conflicting-3",
                startColumn = 1,
                startRow = 2,
                columnSpan = 4,
                rowSpan = 1,
            ),
            getApplicationInfoGridItem(
                id = "conflicting-4",
                startColumn = 1,
                startRow = 3,
                columnSpan = 4,
                rowSpan = 1,
            ),
        )

        val gridRepository = FakeGridRepository(
            initialGridItems = getGridItems(
                applicationInfoGridItems = listOf(
                    applicationInfoGridItem,
                    *conflictingApplicationInfoGridItems.toTypedArray(),
                ),
            ),
        )

        val getGridItemsUseCase = getGridItemsUseCase(
            gridRepository = gridRepository,
        )

        val resizeGridItemUseCase = ResizeGridItemUseCase(
            gridRepository = gridRepository,
            getGridItemsUseCase = getGridItemsUseCase,
            defaultDispatcher = dispatcher,
        )

        val resizingGridItem = getGridItemsUseCase()
            .first { it.id == applicationInfoGridItem.id }
            .copy(
                columnSpan = 2,
                rowSpan = 1,
            )

        val resizedGridItem = resizeGridItemUseCase(
            resizingGridItem = resizingGridItem,
            columns = 5,
            rows = 4,
        )

        assertEquals(0, resizedGridItem.startColumn)
        assertEquals(0, resizedGridItem.startRow)
        assertEquals(1, resizedGridItem.columnSpan)
        assertEquals(1, resizedGridItem.rowSpan)
    }

    @Test
    fun `ignores conflicting grid item on different page`() = runTest(dispatcher) {
        val applicationInfoGridItem = getApplicationInfoGridItem(
            id = "resizing",
            page = 0,
        )

        val otherPageApplicationInfoGridItem = getApplicationInfoGridItem(
            id = "other-page",
            page = 1,
            startColumn = 1,
            startRow = 0,
        )

        val gridRepository = FakeGridRepository(
            initialGridItems = getGridItems(
                applicationInfoGridItems = listOf(
                    applicationInfoGridItem,
                    otherPageApplicationInfoGridItem,
                ),
            ),
        )

        val getGridItemsUseCase = getGridItemsUseCase(
            gridRepository = gridRepository,
        )

        val resizeGridItemUseCase = ResizeGridItemUseCase(
            gridRepository = gridRepository,
            getGridItemsUseCase = getGridItemsUseCase,
            defaultDispatcher = dispatcher,
        )

        val resizingGridItem = getGridItemsUseCase()
            .first { it.id == applicationInfoGridItem.id }
            .copy(
                columnSpan = 2,
                rowSpan = 1,
            )

        val resizedGridItem = resizeGridItemUseCase(
            resizingGridItem = resizingGridItem,
            columns = 5,
            rows = 4,
        )

        assertEquals(0, resizedGridItem.startColumn)
        assertEquals(0, resizedGridItem.startRow)
        assertEquals(2, resizedGridItem.columnSpan)
        assertEquals(1, resizedGridItem.rowSpan)
    }

    @Test
    fun `ignores conflicting grid item with different associate`() = runTest(dispatcher) {
        val applicationInfoGridItem = getApplicationInfoGridItem(
            id = "resizing",
            associate = Associate.Grid,
        )

        val dockApplicationInfoGridItem = getApplicationInfoGridItem(
            id = "dock",
            associate = Associate.Dock,
            startColumn = 1,
            startRow = 0,
        )

        val gridRepository = FakeGridRepository(
            initialGridItems = getGridItems(
                applicationInfoGridItems = listOf(
                    applicationInfoGridItem,
                    dockApplicationInfoGridItem,
                ),
            ),
        )

        val getGridItemsUseCase = getGridItemsUseCase(
            gridRepository = gridRepository,
        )

        val resizeGridItemUseCase = ResizeGridItemUseCase(
            gridRepository = gridRepository,
            getGridItemsUseCase = getGridItemsUseCase,
            defaultDispatcher = dispatcher,
        )

        val resizingGridItem = getGridItemsUseCase()
            .first { it.id == applicationInfoGridItem.id }
            .copy(
                columnSpan = 2,
                rowSpan = 1,
            )

        val resizedGridItem = resizeGridItemUseCase(
            resizingGridItem = resizingGridItem,
            columns = 5,
            rows = 4,
        )

        assertEquals(0, resizedGridItem.startColumn)
        assertEquals(0, resizedGridItem.startRow)
        assertEquals(2, resizedGridItem.columnSpan)
        assertEquals(1, resizedGridItem.rowSpan)
    }

    @Test
    fun `ignores conflicting non top level grid item`() = runTest(dispatcher) {
        val applicationInfoGridItem = getApplicationInfoGridItem(
            id = "resizing",
        )

        val folderApplicationInfoGridItem = getApplicationInfoGridItem(
            id = "folder-item",
            startColumn = 1,
            startRow = 0,
            folderId = "folder",
        )

        val gridRepository = FakeGridRepository(
            initialGridItems = getGridItems(
                applicationInfoGridItems = listOf(
                    applicationInfoGridItem,
                    folderApplicationInfoGridItem,
                ),
            ),
        )

        val getGridItemsUseCase = getGridItemsUseCase(
            gridRepository = gridRepository,
        )

        val resizeGridItemUseCase = ResizeGridItemUseCase(
            gridRepository = gridRepository,
            getGridItemsUseCase = getGridItemsUseCase,
            defaultDispatcher = dispatcher,
        )

        val resizingGridItem = getGridItemsUseCase()
            .first { it.id == applicationInfoGridItem.id }
            .copy(
                columnSpan = 2,
                rowSpan = 1,
            )

        val resizedGridItem = resizeGridItemUseCase(
            resizingGridItem = resizingGridItem,
            columns = 5,
            rows = 4,
        )

        assertEquals(0, resizedGridItem.startColumn)
        assertEquals(0, resizedGridItem.startRow)
        assertEquals(2, resizedGridItem.columnSpan)
        assertEquals(1, resizedGridItem.rowSpan)
    }

    private fun getGridItems(
        applicationInfoGridItems: List<ApplicationInfoGridItem> = emptyList(),
    ) = GridItems(
        applicationInfoGridItems = applicationInfoGridItems,
        widgetGridItems = emptyList(),
        shortcutInfoGridItems = emptyList(),
        shortcutConfigGridItems = emptyList(),
        folderGridItems = emptyList(),
    )

    private fun getApplicationInfoGridItem(
        id: String = "app",
        page: Int = 0,
        startColumn: Int = 0,
        startRow: Int = 0,
        columnSpan: Int = 1,
        rowSpan: Int = 1,
        associate: Associate = Associate.Grid,
        folderId: String? = null,
    ) = ApplicationInfoGridItem(
        id = id,
        page = page,
        startColumn = startColumn,
        startRow = startRow,
        columnSpan = columnSpan,
        rowSpan = rowSpan,
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
