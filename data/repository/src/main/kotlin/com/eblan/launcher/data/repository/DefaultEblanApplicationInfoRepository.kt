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
import com.eblan.launcher.data.repository.mapper.asModel
import com.eblan.launcher.data.room.dao.EblanApplicationInfoDao
import com.eblan.launcher.data.room.entity.EblanApplicationInfoTagEntity
import com.eblan.launcher.domain.common.Dispatcher
import com.eblan.launcher.domain.common.EblanDispatchers
import com.eblan.launcher.domain.model.DeleteEblanApplicationInfo
import com.eblan.launcher.domain.model.EblanApplicationInfo
import com.eblan.launcher.domain.model.EblanApplicationInfoTag
import com.eblan.launcher.domain.model.SyncEblanApplicationInfo
import com.eblan.launcher.domain.repository.EblanApplicationInfoRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

internal class DefaultEblanApplicationInfoRepository @Inject constructor(
    private val eblanApplicationInfoDao: EblanApplicationInfoDao,
    @param:Dispatcher(EblanDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
) : EblanApplicationInfoRepository {
    override val eblanApplicationInfos =
        eblanApplicationInfoDao.getEblanApplicationInfoEntities().map { entities ->
            entities.map { entity ->
                entity.asModel()
            }
        }

    override suspend fun getEblanApplicationInfos(): List<EblanApplicationInfo> = eblanApplicationInfoDao.getEblanApplicationInfoEntityList()
        .map { eblanApplicationInfoEntity ->
            eblanApplicationInfoEntity.asModel()
        }

    override suspend fun upsertEblanApplicationInfo(eblanApplicationInfo: EblanApplicationInfo) {
        eblanApplicationInfoDao.upsertEblanApplicationInfoEntity(entity = eblanApplicationInfo.asEntity())
    }

    override suspend fun updateEblanApplicationInfos(eblanApplicationInfos: List<EblanApplicationInfo>) {
        val entities = eblanApplicationInfos.map { eblanApplicationInfo ->
            eblanApplicationInfo.asEntity()
        }

        eblanApplicationInfoDao.updateEblanApplicationInfoEntities(entities = entities)
    }

    override suspend fun deleteEblanApplicationInfoByPackageName(
        serialNumber: Long,
        packageName: String,
    ) {
        eblanApplicationInfoDao.deleteEblanApplicationInfoEntityByPackageName(
            serialNumber = serialNumber,
            packageName = packageName,
        )
    }

    override suspend fun deleteEblanApplicationInfos(eblanApplicationInfos: List<EblanApplicationInfo>) {
        val entities = eblanApplicationInfos.map { eblanApplicationInfo ->
            eblanApplicationInfo.asEntity()
        }

        eblanApplicationInfoDao.deleteEblanApplicationInfoEntities(entities = entities)
    }

    override suspend fun upsertSyncEblanApplicationInfos(syncEblanApplicationInfos: List<SyncEblanApplicationInfo>) {
        eblanApplicationInfoDao.upsertSyncEblanApplicationInfoEntities(syncEblanApplicationInfos = syncEblanApplicationInfos)
    }

    override suspend fun deleteSyncEblanApplicationInfos(deleteEblanApplicationInfos: List<DeleteEblanApplicationInfo>) {
        eblanApplicationInfoDao.deleteSyncEblanApplicationInfoEntities(deleteEblanApplicationInfos = deleteEblanApplicationInfos)
    }

    override suspend fun updateEblanApplicationInfo(eblanApplicationInfo: EblanApplicationInfo) {
        eblanApplicationInfoDao.updateEblanApplicationInfoEntity(entity = eblanApplicationInfo.asEntity())
    }

    override suspend fun resetEblanApplicationInfoCustomIcon(eblanApplicationInfo: EblanApplicationInfo) {
        withContext(ioDispatcher) {
            eblanApplicationInfo.customIcon?.let { customIcon ->
                val customIconFile = File(customIcon)

                if (customIconFile.exists()) {
                    customIconFile.delete()
                }
            }

            updateEblanApplicationInfo(eblanApplicationInfo = eblanApplicationInfo.copy(customIcon = null))
        }
    }

    override suspend fun getEblanApplicationInfoByComponentName(
        serialNumber: Long,
        componentName: String,
    ): EblanApplicationInfo? = eblanApplicationInfoDao.getEblanApplicationInfoEntityByComponentName(
        serialNumber = serialNumber,
        componentName = componentName,
    )?.asModel()

    override suspend fun getEblanApplicationInfosByPackageName(
        serialNumber: Long,
        packageName: String,
    ): List<EblanApplicationInfo> = eblanApplicationInfoDao.getEblanApplicationInfoEntitiesByPackageName(
        serialNumber = serialNumber,
        packageName = packageName,
    ).map { entity ->
        entity.asModel()
    }

    override fun getEblanApplicationInfosByTagId(tagIds: List<Long>): Flow<List<EblanApplicationInfo>> = eblanApplicationInfoDao.getEblanApplicationInfoEntitiesByTagId(tagIds = tagIds)
        .map { entities ->
            entities.map { entity ->
                entity.asModel()
            }
        }

    override fun getEblanApplicationInfoTags(
        serialNumber: Long,
        componentName: String,
    ): Flow<List<EblanApplicationInfoTag>> = eblanApplicationInfoDao.getEblanApplicationInfoTagEntities(
        serialNumber = serialNumber,
        componentName = componentName,
    ).map { entities ->
        entities.map { entity ->
            entity.asModel()
        }
    }

    private fun EblanApplicationInfoTagEntity.asModel(): EblanApplicationInfoTag = EblanApplicationInfoTag(
        id = id,
        name = name,
    )
}
