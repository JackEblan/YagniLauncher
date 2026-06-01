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
package com.eblan.launcher.data.repository.mapper

import com.eblan.launcher.data.room.entity.ApplicationInfoGridItemEntity
import com.eblan.launcher.domain.common.IconKeyGenerator
import com.eblan.launcher.domain.framework.FileManager
import com.eblan.launcher.domain.model.ApplicationInfoGridItem
import com.eblan.launcher.domain.model.GridItem
import com.eblan.launcher.domain.model.GridItemData
import com.eblan.launcher.domain.model.UserData
import java.io.File

internal suspend fun ApplicationInfoGridItemEntity.asGridItem(
    fileManager: FileManager,
    iconKeyGenerator: IconKeyGenerator,
    userData: UserData,
): GridItem {
    val iconPacksDirectory = fileManager.getFilesDirectory(
        FileManager.ICON_PACKS_DIR,
    )

    val iconPackInfoPackageName = userData.generalSettings.iconPackInfoPackageName

    val iconPackDirectory = File(
        iconPacksDirectory,
        iconPackInfoPackageName,
    )

    val iconPackInfoFilePath = File(
        iconPackDirectory,
        iconKeyGenerator.getHashedName(name = componentName),
    )

    return GridItem(
        id = id,
        page = page,
        startColumn = startColumn,
        startRow = startRow,
        columnSpan = columnSpan,
        rowSpan = rowSpan,
        data = GridItemData.ApplicationInfo(
            serialNumber = serialNumber,
            componentName = componentName,
            packageName = packageName,
            icon = icon,
            label = label,
            customIcon = customIcon,
            customLabel = customLabel,
            index = index,
            folderId = folderId,
            iconPackInfoFilePath = if (iconPackInfoFilePath.exists()) {
                iconPackInfoFilePath.absolutePath
            } else {
                null
            },
        ),
        associate = associate,
        override = override,
        gridItemSettings = gridItemSettings,
        doubleTap = doubleTap,
        swipeUp = swipeUp,
        swipeDown = swipeDown,
    )
}

internal fun ApplicationInfoGridItemEntity.asModel(): ApplicationInfoGridItem = ApplicationInfoGridItem(
    id = id,
    page = page,
    startColumn = startColumn,
    startRow = startRow,
    columnSpan = columnSpan,
    rowSpan = rowSpan,
    associate = associate,
    componentName = componentName,
    packageName = packageName,
    icon = icon,
    label = label,
    override = override,
    serialNumber = serialNumber,
    customIcon = customIcon,
    customLabel = customLabel,
    gridItemSettings = gridItemSettings,
    doubleTap = doubleTap,
    swipeUp = swipeUp,
    swipeDown = swipeDown,
    index = index,
    folderId = folderId,
)

internal fun ApplicationInfoGridItem.asEntity(): ApplicationInfoGridItemEntity = ApplicationInfoGridItemEntity(
    id = id,
    serialNumber = serialNumber,
    page = page,
    startColumn = startColumn,
    startRow = startRow,
    columnSpan = columnSpan,
    rowSpan = rowSpan,
    associate = associate,
    componentName = componentName,
    packageName = packageName,
    icon = icon,
    label = label,
    customIcon = customIcon,
    customLabel = customLabel,
    override = override,
    gridItemSettings = gridItemSettings,
    doubleTap = doubleTap,
    swipeUp = swipeUp,
    swipeDown = swipeDown,
    index = index,
    folderId = folderId,
)

internal fun GridItem.asApplicationInfoGridItem(data: GridItemData.ApplicationInfo): ApplicationInfoGridItem = ApplicationInfoGridItem(
    id = id,
    page = page,
    startColumn = startColumn,
    startRow = startRow,
    columnSpan = columnSpan,
    rowSpan = rowSpan,
    associate = associate,
    componentName = data.componentName,
    packageName = data.packageName,
    icon = data.icon,
    label = data.label,
    override = override,
    serialNumber = data.serialNumber,
    customIcon = data.customIcon,
    customLabel = data.customLabel,
    gridItemSettings = gridItemSettings,
    doubleTap = doubleTap,
    swipeUp = swipeUp,
    swipeDown = swipeDown,
    index = data.index,
    folderId = data.folderId,
)
