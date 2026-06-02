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

import com.eblan.launcher.data.room.entity.EblanApplicationInfoEntity
import com.eblan.launcher.domain.common.IconKeyGenerator
import com.eblan.launcher.domain.framework.FileManager
import com.eblan.launcher.domain.model.EblanApplicationInfo
import com.eblan.launcher.domain.model.UserData
import java.io.File

fun EblanApplicationInfo.asEntity(): EblanApplicationInfoEntity = EblanApplicationInfoEntity(
    componentName = componentName,
    serialNumber = serialNumber,
    packageName = packageName,
    icon = icon,
    label = label,
    customIcon = customIcon,
    customLabel = customLabel,
    isHidden = isHidden,
    lastUpdateTime = lastUpdateTime,
    index = index,
    flags = flags,
)

suspend fun EblanApplicationInfoEntity.asModel(
    fileManager: FileManager,
    iconKeyGenerator: IconKeyGenerator,
    userData: UserData,
): EblanApplicationInfo {
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

    return EblanApplicationInfo(
        componentName = componentName,
        serialNumber = serialNumber,
        packageName = packageName,
        icon = icon,
        label = label,
        customIcon = customIcon,
        customLabel = customLabel,
        isHidden = isHidden,
        lastUpdateTime = lastUpdateTime,
        index = index,
        flags = flags,
        iconPackInfoFilePath = if (iconPackInfoFilePath.exists()) {
            iconPackInfoFilePath.absolutePath
        } else {
            null
        },
    )
}
