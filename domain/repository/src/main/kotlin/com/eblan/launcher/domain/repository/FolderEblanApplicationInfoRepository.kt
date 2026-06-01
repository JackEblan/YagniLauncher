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
package com.eblan.launcher.domain.repository

import com.eblan.launcher.domain.model.FolderEblanApplicationInfo
import com.eblan.launcher.domain.model.FolderEblanApplicationInfoWrapper
import kotlinx.coroutines.flow.Flow

interface FolderEblanApplicationInfoRepository {
    val folderEblanApplicationInfoWrappersFlow: Flow<List<FolderEblanApplicationInfoWrapper>>

    val folderEblanApplicationInfoWrappersWithFolderIdFlow: Flow<List<FolderEblanApplicationInfoWrapper>>

    suspend fun getFolderEblanApplicationInfoWrapper(id: String): FolderEblanApplicationInfoWrapper?

    suspend fun getFolderEblanApplicationInfoWrappers(): List<FolderEblanApplicationInfoWrapper>

    suspend fun getFolderEblanApplicationInfoWrappersWithFolderId(): List<FolderEblanApplicationInfoWrapper>

    suspend fun upsertFolderEblanApplicationInfos(folderEblanApplicationInfos: List<FolderEblanApplicationInfo>)

    suspend fun updateFolderEblanApplicationInfo(folderEblanApplicationInfo: FolderEblanApplicationInfo)

    suspend fun deleteFolderEblanApplicationInfo(folderEblanApplicationInfo: FolderEblanApplicationInfo)

    suspend fun deleteFolderEblanApplicationInfos(folderEblanApplicationInfos: List<FolderEblanApplicationInfo>)

    suspend fun insertFolderEblanApplicationInfo(folderEblanApplicationInfo: FolderEblanApplicationInfo)

    suspend fun updateFolderEblanApplicationInfos(folderEblanApplicationInfos: List<FolderEblanApplicationInfo>)

    suspend fun insertFolderEblanApplicationInfos(folderEblanApplicationInfos: List<FolderEblanApplicationInfo>)

    suspend fun upsertFolderEblanApplicationInfo(folderEblanApplicationInfo: FolderEblanApplicationInfo)
}
