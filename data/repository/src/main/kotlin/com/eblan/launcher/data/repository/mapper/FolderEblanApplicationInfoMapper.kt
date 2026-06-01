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

import com.eblan.launcher.data.room.entity.FolderEblanApplicationInfoEntity
import com.eblan.launcher.data.room.entity.FolderEblanApplicationInfoWrapperEntity
import com.eblan.launcher.domain.model.FolderEblanApplicationInfo
import com.eblan.launcher.domain.model.FolderEblanApplicationInfoWrapper

internal fun FolderEblanApplicationInfoWrapperEntity.asFolderEblanApplicationInfoWrapper(): FolderEblanApplicationInfoWrapper = FolderEblanApplicationInfoWrapper(
    folderEblanApplicationInfo = folderEblanApplicationInfoEntity.asModel(),
    eblanApplicationInfos = eblanApplicationInfoEntities.map { it.asModel() },
    folderEblanApplicationInfos = folderEblanApplicationInfoEntities.map { it.asModel() },
)

internal fun FolderEblanApplicationInfoEntity.asModel(): FolderEblanApplicationInfo = FolderEblanApplicationInfo(
    id = id,
    icon = icon,
    label = label,
    customIcon = customIcon,
    customLabel = customLabel,
    index = index,
    folderId = folderId,
)

internal fun FolderEblanApplicationInfo.asEntity(): FolderEblanApplicationInfoEntity = FolderEblanApplicationInfoEntity(
    id = id,
    icon = icon,
    label = label,
    customIcon = customIcon,
    customLabel = customLabel,
    index = index,
    folderId = folderId,
)
