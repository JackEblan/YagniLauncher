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
package com.eblan.launcher.data.repository

import com.eblan.launcher.data.repository.mapper.asEntity
import com.eblan.launcher.data.repository.mapper.asFolderEblanApplicationInfoWrapper
import com.eblan.launcher.data.room.dao.FolderEblanApplicationInfoDao
import com.eblan.launcher.domain.model.FolderEblanApplicationInfo
import com.eblan.launcher.domain.model.FolderEblanApplicationInfoWrapper
import com.eblan.launcher.domain.repository.FolderEblanApplicationInfoRepository
import kotlinx.coroutines.flow.map
import javax.inject.Inject

internal class DefaultFolderEblanApplicationInfoRepository @Inject constructor(private val folderEblanApplicationInfoDao: FolderEblanApplicationInfoDao) : FolderEblanApplicationInfoRepository {
    override val folderEblanApplicationInfoWrappersFlow =
        folderEblanApplicationInfoDao.getFolderEblanApplicationInfoWrapperEntitiesFlow()
            .map { entities ->
                entities.filter { entity ->
                    entity.folderEblanApplicationInfoEntity.folderId == null
                }.map { entity ->
                    entity.asFolderEblanApplicationInfoWrapper()
                }
            }

    override val folderEblanApplicationInfoWrappersWithFolderIdFlow =
        folderEblanApplicationInfoDao.getFolderEblanApplicationInfoWrapperEntitiesFlow()
            .map { entities ->
                entities.map { entity ->
                    entity.asFolderEblanApplicationInfoWrapper()
                }
            }

    override suspend fun getFolderEblanApplicationInfoWrapper(id: String): FolderEblanApplicationInfoWrapper? = folderEblanApplicationInfoDao.getFolderEblanApplicationInfoWrapperEntity(id = id)
        ?.asFolderEblanApplicationInfoWrapper()

    override suspend fun getFolderEblanApplicationInfoWrappers(): List<FolderEblanApplicationInfoWrapper> = folderEblanApplicationInfoDao.getFolderEblanApplicationInfoWrapperEntities()
        .filter { entity ->
            entity.folderEblanApplicationInfoEntity.folderId == null
        }.map { entity ->
            entity.asFolderEblanApplicationInfoWrapper()
        }

    override suspend fun getFolderEblanApplicationInfoWrappersWithFolderId(): List<FolderEblanApplicationInfoWrapper> = folderEblanApplicationInfoDao.getFolderEblanApplicationInfoWrapperEntities().map { entity ->
        entity.asFolderEblanApplicationInfoWrapper()
    }

    override suspend fun upsertFolderEblanApplicationInfos(folderEblanApplicationInfos: List<FolderEblanApplicationInfo>) {
        val entities = folderEblanApplicationInfos.map { folderEblanApplicationInfo ->
            folderEblanApplicationInfo.asEntity()
        }

        folderEblanApplicationInfoDao.upsertFolderEblanApplicationInfoEntities(entities = entities)
    }

    override suspend fun updateFolderEblanApplicationInfo(folderEblanApplicationInfo: FolderEblanApplicationInfo) {
        folderEblanApplicationInfoDao.updateFolderEblanApplicationInfoEntity(entity = folderEblanApplicationInfo.asEntity())
    }

    override suspend fun deleteFolderEblanApplicationInfo(folderEblanApplicationInfo: FolderEblanApplicationInfo) {
        folderEblanApplicationInfoDao.deleteFolderEblanApplicationInfoEntity(entity = folderEblanApplicationInfo.asEntity())
    }

    override suspend fun deleteFolderEblanApplicationInfos(folderEblanApplicationInfos: List<FolderEblanApplicationInfo>) {
        val entities = folderEblanApplicationInfos.map { folderEblanApplicationInfo ->
            folderEblanApplicationInfo.asEntity()
        }

        folderEblanApplicationInfoDao.deleteFolderEblanApplicationInfoEntities(entities = entities)
    }

    override suspend fun insertFolderEblanApplicationInfo(folderEblanApplicationInfo: FolderEblanApplicationInfo) {
        folderEblanApplicationInfoDao.insertFolderEblanApplicationInfoEntity(entity = folderEblanApplicationInfo.asEntity())
    }

    override suspend fun updateFolderEblanApplicationInfos(folderEblanApplicationInfos: List<FolderEblanApplicationInfo>) {
        val entities = folderEblanApplicationInfos.map { folderEblanApplicationInfo ->
            folderEblanApplicationInfo.asEntity()
        }

        folderEblanApplicationInfoDao.updateFolderEblanApplicationInfoEntities(entities = entities)
    }

    override suspend fun insertFolderEblanApplicationInfos(folderEblanApplicationInfos: List<FolderEblanApplicationInfo>) {
        val entities = folderEblanApplicationInfos.map { folderEblanApplicationInfo ->
            folderEblanApplicationInfo.asEntity()
        }

        folderEblanApplicationInfoDao.insertFolderEblanApplicationInfoEntities(entities = entities)
    }

    override suspend fun upsertFolderEblanApplicationInfo(folderEblanApplicationInfo: FolderEblanApplicationInfo) {
        folderEblanApplicationInfoDao.upsertFolderEblanApplicationInfoEntity(entity = folderEblanApplicationInfo.asEntity())
    }
}
