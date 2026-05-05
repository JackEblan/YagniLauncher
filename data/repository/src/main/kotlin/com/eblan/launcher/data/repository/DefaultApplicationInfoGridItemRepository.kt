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
import com.eblan.launcher.data.repository.mapper.asGridItem
import com.eblan.launcher.data.repository.mapper.asModel
import com.eblan.launcher.data.room.dao.ApplicationInfoGridItemDao
import com.eblan.launcher.domain.model.ApplicationInfoGridItem
import com.eblan.launcher.domain.model.GridItem
import com.eblan.launcher.domain.model.PartialApplicationInfoGridItem
import com.eblan.launcher.domain.repository.ApplicationInfoGridItemRepository
import kotlinx.coroutines.flow.map
import javax.inject.Inject

internal class DefaultApplicationInfoGridItemRepository @Inject constructor(private val applicationInfoGridItemDao: ApplicationInfoGridItemDao) : ApplicationInfoGridItemRepository {
    override val gridItemsFlow =
        applicationInfoGridItemDao.getApplicationInfoGridItemEntitiesFlow().map { entities ->
            entities.filter { entity ->
                entity.folderId == null
            }.map { entity ->
                entity.asGridItem()
            }
        }

    override val gridItemsWithFolderIdFlow =
        applicationInfoGridItemDao.getApplicationInfoGridItemEntitiesFlow().map { entities ->
            entities.map { entity ->
                entity.asGridItem()
            }
        }

    override suspend fun getGridItems(): List<GridItem> = applicationInfoGridItemDao.getApplicationInfoGridItemEntities().filter { entity ->
        entity.folderId == null
    }.map { entity ->
        entity.asGridItem()
    }

    override suspend fun getGridItemsWithFolderId(): List<GridItem> = applicationInfoGridItemDao.getApplicationInfoGridItemEntities().map { entity ->
        entity.asGridItem()
    }

    override suspend fun getApplicationInfoGridItems(): List<ApplicationInfoGridItem> = applicationInfoGridItemDao.getApplicationInfoGridItemEntities().map { entity ->
        entity.asModel()
    }

    override suspend fun upsertApplicationInfoGridItems(applicationInfoGridItems: List<ApplicationInfoGridItem>) {
        val entities = applicationInfoGridItems.map { applicationInfoGridItem ->
            applicationInfoGridItem.asEntity()
        }

        applicationInfoGridItemDao.upsertApplicationInfoGridItemEntities(entities = entities)
    }

    override suspend fun updateApplicationInfoGridItem(applicationInfoGridItem: ApplicationInfoGridItem) {
        applicationInfoGridItemDao.updateApplicationInfoGridItemEntity(
            applicationInfoGridItem.asEntity(),
        )
    }

    override suspend fun deleteApplicationInfoGridItems(applicationInfoGridItems: List<ApplicationInfoGridItem>) {
        val entities = applicationInfoGridItems.map { applicationInfoGridItem ->
            applicationInfoGridItem.asEntity()
        }

        applicationInfoGridItemDao.deleteApplicationInfoGridItemEntities(entities = entities)
    }

    override suspend fun deleteApplicationInfoGridItem(applicationInfoGridItem: ApplicationInfoGridItem) {
        applicationInfoGridItemDao.deleteApplicationInfoGridItemEntity(entity = applicationInfoGridItem.asEntity())
    }

    override suspend fun getApplicationInfoGridItemsByPackageName(
        serialNumber: Long,
        packageName: String,
    ): List<ApplicationInfoGridItem> = applicationInfoGridItemDao.getApplicationInfoGridItemEntitiesByPackageName(
        serialNumber = serialNumber,
        packageName = packageName,
    ).map { entity ->
        entity.asModel()
    }

    override suspend fun deleteApplicationInfoGridItem(
        serialNumber: Long,
        packageName: String,
    ) {
        applicationInfoGridItemDao.deleteApplicationInfoGridItemEntity(
            serialNumber = serialNumber,
            packageName = packageName,
        )
    }

    override suspend fun updatePartialApplicationInfoGridItems(partialApplicationInfoGridItems: List<PartialApplicationInfoGridItem>) {
        applicationInfoGridItemDao.updatePartialApplicationInfoGridItems(
            partialApplicationInfoGridItems = partialApplicationInfoGridItems,
        )
    }

    override suspend fun insertApplicationInfoGridItems(applicationInfoGridItems: List<ApplicationInfoGridItem>) {
        val entities = applicationInfoGridItems.map { applicationInfoGridItem ->
            applicationInfoGridItem.asEntity()
        }

        applicationInfoGridItemDao.insertApplicationInfoGridItemEntities(entities = entities)
    }

    override suspend fun insertApplicationInfoGridItem(applicationInfoGridItem: ApplicationInfoGridItem) {
        applicationInfoGridItemDao.insertApplicationInfoGridItemEntity(entity = applicationInfoGridItem.asEntity())
    }

    override suspend fun updateApplicationInfoGridItems(applicationInfoGridItems: List<ApplicationInfoGridItem>) {
        val entities = applicationInfoGridItems.map { applicationInfoGridItem ->
            applicationInfoGridItem.asEntity()
        }

        applicationInfoGridItemDao.updateApplicationInfoGridItemEntities(entities = entities)
    }
}
