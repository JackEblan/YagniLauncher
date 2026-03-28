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

import com.eblan.launcher.domain.common.IconKeyGenerator
import com.eblan.launcher.domain.framework.FileManager
import com.eblan.launcher.domain.framework.IconPackManager
import com.eblan.launcher.domain.model.FastLauncherAppsActivityInfo
import com.eblan.launcher.domain.model.IconPackInfoComponent
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.io.File

internal suspend fun updateIconPackInfos(
    iconPackInfoPackageName: String,
    fileManager: FileManager,
    iconPackManager: IconPackManager,
    fastLauncherAppsActivityInfos: List<FastLauncherAppsActivityInfo>,
    iconKeyGenerator: IconKeyGenerator,
) {
    if (iconPackInfoPackageName.isEmpty()) return

    val iconPackInfoDirectory = File(
        fileManager.getFilesDirectory(name = FileManager.ICON_PACKS_DIR),
        iconPackInfoPackageName,
    ).apply { if (!exists()) mkdirs() }

    val appFilter = iconPackManager.getIconPackInfoComponents(packageName = iconPackInfoPackageName)

    val installedComponentHashCodes = buildSet {
        fastLauncherAppsActivityInfos.forEach { fastLauncherAppsActivityInfo ->
            currentCoroutineContext().ensureActive()

            val file = File(
                iconPackInfoDirectory,
                iconKeyGenerator.getHashedName(name = fastLauncherAppsActivityInfo.componentName),
            )

            cacheIconPackFile(
                iconPackManager = iconPackManager,
                appFilter = appFilter,
                iconPackInfoPackageName = iconPackInfoPackageName,
                file = file,
                componentName = fastLauncherAppsActivityInfo.componentName,
            )

            add(iconKeyGenerator.getHashedName(name = fastLauncherAppsActivityInfo.componentName))
        }
    }

    iconPackInfoDirectory.listFiles()
        ?.filter {
            currentCoroutineContext().ensureActive()

            it.isFile && it.name !in installedComponentHashCodes
        }
        ?.forEach {
            currentCoroutineContext().ensureActive()

            it.delete()
        }
}

internal suspend fun cacheIconPackFile(
    iconPackManager: IconPackManager,
    appFilter: List<IconPackInfoComponent>,
    iconPackInfoPackageName: String,
    file: File,
    componentName: String,
) {
    appFilter.find { iconPackInfoComponent ->
        currentCoroutineContext().ensureActive()

        componentName == iconPackInfoComponent.componentName.removePrefix("ComponentInfo{")
            .removeSuffix("}")
    }?.let { iconPackInfoComponent ->
        iconPackManager.createIconPackInfoPath(
            packageName = iconPackInfoPackageName,
            drawableName = iconPackInfoComponent.drawableName,
            file = file,
        )
    }
}
