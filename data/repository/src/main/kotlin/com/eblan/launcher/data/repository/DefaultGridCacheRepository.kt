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

import com.eblan.launcher.data.cache.GridCacheDataSource
import com.eblan.launcher.domain.model.GridItem
import com.eblan.launcher.domain.model.GridItemData
import com.eblan.launcher.domain.repository.GridCacheRepository
import javax.inject.Inject

internal class DefaultGridCacheRepository @Inject constructor(private val gridCacheDataSource: GridCacheDataSource) : GridCacheRepository {
    override val gridItemsCache = gridCacheDataSource.gridItemsCache

    override fun insertGridItems(gridItems: List<GridItem>) {
        gridCacheDataSource.insertGridItems(gridItems = gridItems)
    }

    override fun insertGridItem(gridItem: GridItem) {
        gridCacheDataSource.insertGridItem(gridItem = gridItem)
    }

    override fun deleteGridItemById(id: String) {
        gridCacheDataSource.deleteGridItemById(id = id)
    }

    override suspend fun updateGridItemData(id: String, data: GridItemData) {
        gridCacheDataSource.updateGridItemData(id = id, data = data)
    }

    override suspend fun upsertGridItems(gridItems: List<GridItem>) {
        gridCacheDataSource.upsertGridItems(gridItems = gridItems)
    }
}
