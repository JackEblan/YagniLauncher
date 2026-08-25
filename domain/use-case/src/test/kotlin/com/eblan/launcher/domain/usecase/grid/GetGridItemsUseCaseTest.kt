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
import com.eblan.launcher.domain.model.FolderGridItem
import com.eblan.launcher.domain.model.GeneralSettings
import com.eblan.launcher.domain.model.GestureSettings
import com.eblan.launcher.domain.model.GridItemData
import com.eblan.launcher.domain.model.GridItems
import com.eblan.launcher.domain.model.ShortcutConfigGridItem
import com.eblan.launcher.domain.model.ShortcutInfoGridItem
import com.eblan.launcher.domain.model.Theme
import com.eblan.launcher.domain.model.UserData
import com.eblan.launcher.domain.model.WidgetGridItem
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
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

@OptIn(ExperimentalCoroutinesApi::class)
class GetGridItemsUseCaseTest {
    @TempDir
    lateinit var tempDirectory: Path

    private val dispatcher = StandardTestDispatcher()

    @Test
    fun `returns empty list when there are no grid items`() = runTest(dispatcher) {
        val getGridItemsUseCase = getGridItemsUseCase(gridItems = emptyGridItems())

        val result = getGridItemsUseCase()

        assertTrue(result.isEmpty())
    }

    @Test
    fun `returns all grid item types`() = runTest(dispatcher) {
        val getGridItemsUseCase = getGridItemsUseCase(
            gridItems = GridItems(
                applicationInfoGridItems = listOf(
                    getApplicationInfoGridItem("application"),
                ),
                widgetGridItems = listOf(
                    getWidgetGridItem("widget"),
                ),
                shortcutInfoGridItems = listOf(
                    getShortcutInfoGridItem("shortcut-info"),
                ),
                shortcutConfigGridItems = listOf(
                    getShortcutConfigGridItem("shortcut-config"),
                ),
                folderGridItems = listOf(
                    getFolderGridItem("folder"),
                ),
            ),
        )

        val gridItems = getGridItemsUseCase()

        assertTrue(gridItems[0].data is GridItemData.ApplicationInfo)
        assertTrue(gridItems[1].data is GridItemData.Widget)
        assertTrue(gridItems[2].data is GridItemData.ShortcutInfo)
        assertTrue(gridItems[3].data is GridItemData.ShortcutConfig)
        assertTrue(gridItems[4].data is GridItemData.Folder)
    }

    @Test
    fun `preserves multiple items from every grid item type`() = runTest(dispatcher) {
        val applicationInfoGridItems = listOf(
            getApplicationInfoGridItem("application-1"),
            getApplicationInfoGridItem("application-2"),
        )

        val widgetGridItems = listOf(
            getWidgetGridItem("widget-1"),
            getWidgetGridItem("widget-2"),
            getWidgetGridItem("widget-3"),
        )

        val shortcutInfoGridItems = listOf(
            getShortcutInfoGridItem("shortcut-info-1"),
            getShortcutInfoGridItem("shortcut-info-2"),
        )

        val shortcutConfigGridItems = listOf(
            getShortcutConfigGridItem("shortcut-config-1"),
        )

        val folderGridItems = listOf(
            getFolderGridItem("folder-1"),
            getFolderGridItem("folder-2"),
        )

        val getGridItemsUseCase = getGridItemsUseCase(
            gridItems = GridItems(
                applicationInfoGridItems = applicationInfoGridItems,
                widgetGridItems = widgetGridItems,
                shortcutInfoGridItems = shortcutInfoGridItems,
                shortcutConfigGridItems = shortcutConfigGridItems,
                folderGridItems = folderGridItems,
            ),
        )

        val gridItems = getGridItemsUseCase()

        assertEquals(
            applicationInfoGridItems.size,
            gridItems.count { it.data is GridItemData.ApplicationInfo },
        )
        assertEquals(
            widgetGridItems.size,
            gridItems.count { it.data is GridItemData.Widget },
        )
        assertEquals(
            shortcutInfoGridItems.size,
            gridItems.count { it.data is GridItemData.ShortcutInfo },
        )
        assertEquals(
            shortcutConfigGridItems.size,
            gridItems.count { it.data is GridItemData.ShortcutConfig },
        )
        assertEquals(
            folderGridItems.size,
            gridItems.count { it.data is GridItemData.Folder },
        )
    }

    @Test
    fun `application item does not have icon pack file path when icon pack file does not exist`() = runTest(dispatcher) {
        val iconPackPackageName = "com.example.iconpack"

        val getGridItemsUseCase = getGridItemsUseCase(
            gridItems = GridItems(
                applicationInfoGridItems = listOf(
                    getApplicationInfoGridItem("application"),
                ),
                widgetGridItems = emptyList(),
                shortcutInfoGridItems = emptyList(),
                shortcutConfigGridItems = emptyList(),
                folderGridItems = emptyList(),
            ),
            iconPackInfoPackageName = iconPackPackageName,
        )

        val data = getGridItemsUseCase().single().data as GridItemData.ApplicationInfo

        assertNull(data.iconPackInfoFilePath)
    }

    @Test
    fun `application item uses icon pack file from current user data`() = runTest(dispatcher) {
        val iconPackPackageName = "com.example.iconpack"
        val application = getApplicationInfoGridItem("application")

        val iconPackDirectory = tempDirectory
            .resolve("iconpacks")
            .resolve(iconPackPackageName)

        iconPackDirectory.toFile().mkdirs()

        val iconKeyGenerator = FakeIconKeyGenerator()

        val iconFile = iconPackDirectory.resolve(
            iconKeyGenerator.getHashedName(application.componentName),
        )

        iconFile.toFile().writeBytes(byteArrayOf(1, 2, 3))

        val getGridItemsUseCase = getGridItemsUseCase(
            gridItems = GridItems(
                applicationInfoGridItems = listOf(application),
                widgetGridItems = emptyList(),
                shortcutInfoGridItems = emptyList(),
                shortcutConfigGridItems = emptyList(),
                folderGridItems = emptyList(),
            ),
            iconPackInfoPackageName = iconPackPackageName,
            iconKeyGenerator = iconKeyGenerator,
        )

        val data = getGridItemsUseCase().single().data as GridItemData.ApplicationInfo

        assertEquals(
            iconFile.toFile().absolutePath,
            data.iconPackInfoFilePath,
        )
    }

    private fun getGridItemsUseCase(
        gridItems: GridItems,
        iconPackInfoPackageName: String = "",
        iconKeyGenerator: FakeIconKeyGenerator = FakeIconKeyGenerator(),
    ): GetGridItemsUseCase = GetGridItemsUseCase(
        userDataRepository = FakeUserDataRepository(
            initialUserData = getUserData(
                iconPackInfoPackageName = iconPackInfoPackageName,
            ),
        ),
        fileManager = FakeFileManager(
            rootDirectory = tempDirectory.toFile(),
        ),
        iconKeyGenerator = iconKeyGenerator,
        gridRepository = FakeGridRepository(
            initialGridItems = gridItems,
        ),
        ioDispatcher = dispatcher,
    )

    private fun emptyGridItems() = GridItems(
        applicationInfoGridItems = emptyList(),
        widgetGridItems = emptyList(),
        shortcutInfoGridItems = emptyList(),
        shortcutConfigGridItems = emptyList(),
        folderGridItems = emptyList(),
    )

    private fun getApplicationInfoGridItem(
        id: String,
    ) = ApplicationInfoGridItem(
        id = id,
        page = 0,
        startColumn = 0,
        startRow = 0,
        columnSpan = 1,
        rowSpan = 1,
        associate = Associate.Grid,
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
        folderId = null,
    )

    private fun getWidgetGridItem(
        id: String,
    ) = WidgetGridItem(
        id = id,
        page = 0,
        startColumn = 0,
        startRow = 0,
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

    private fun getShortcutInfoGridItem(
        id: String,
    ) = ShortcutInfoGridItem(
        id = id,
        page = 0,
        startColumn = 0,
        startRow = 0,
        columnSpan = 1,
        rowSpan = 1,
        associate = Associate.Grid,
        shortcutId = "shortcut",
        packageName = "com.example",
        shortLabel = "Shortcut",
        longLabel = "Example Shortcut",
        icon = null,
        override = false,
        serialNumber = 0L,
        isEnabled = true,
        eblanApplicationInfoIcon = null,
        customIcon = null,
        customShortLabel = null,
        gridItemSettings = getGridItemSettings(),
        doubleTap = getEblanAction(),
        swipeUp = getEblanAction(),
        swipeDown = getEblanAction(),
        index = 0,
        folderId = null,
    )

    private fun getShortcutConfigGridItem(
        id: String,
    ) = ShortcutConfigGridItem(
        id = id,
        page = 0,
        startColumn = 0,
        startRow = 0,
        columnSpan = 1,
        rowSpan = 1,
        associate = Associate.Grid,
        componentName = "com.example/.MainActivity",
        packageName = "com.example",
        activityIcon = null,
        activityLabel = "Example",
        applicationIcon = null,
        applicationLabel = "Example",
        override = false,
        serialNumber = 0L,
        shortcutIntentName = null,
        shortcutIntentIcon = null,
        shortcutIntentUri = null,
        customIcon = null,
        customLabel = null,
        gridItemSettings = getGridItemSettings(),
        doubleTap = getEblanAction(),
        swipeUp = getEblanAction(),
        swipeDown = getEblanAction(),
        index = 0,
        folderId = null,
    )

    private fun getFolderGridItem(
        id: String,
    ) = FolderGridItem(
        id = id,
        page = 0,
        startColumn = 0,
        startRow = 0,
        columnSpan = 1,
        rowSpan = 1,
        associate = Associate.Grid,
        label = "Folder",
        override = false,
        icon = null,
        gridItemSettings = getGridItemSettings(),
        doubleTap = getEblanAction(),
        swipeUp = getEblanAction(),
        swipeDown = getEblanAction(),
        index = 0,
        folderId = null,
    )

    private fun getUserData(
        iconPackInfoPackageName: String,
    ) = UserData(
        homeSettings = getHomeSettings(),
        appDrawerSettings = getAppDrawerSettings(),
        generalSettings = GeneralSettings(
            theme = Theme.System,
            dynamicTheme = false,
            iconPackInfoPackageName = iconPackInfoPackageName,
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
}
