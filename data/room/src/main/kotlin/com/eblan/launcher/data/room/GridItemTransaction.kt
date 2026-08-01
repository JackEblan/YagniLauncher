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
package com.eblan.launcher.data.room

import com.eblan.launcher.data.room.entity.ApplicationInfoGridItemEntity
import com.eblan.launcher.data.room.entity.FolderGridItemEntity
import com.eblan.launcher.data.room.entity.ShortcutConfigGridItemEntity
import com.eblan.launcher.data.room.entity.ShortcutInfoGridItemEntity
import com.eblan.launcher.data.room.entity.WidgetGridItemEntity

interface GridItemTransaction {
    suspend fun upsertGridItemEntitiesTransaction(
        applicationInfoGridItemEntities: List<ApplicationInfoGridItemEntity>,
        widgetGridItemEntities: List<WidgetGridItemEntity>,
        shortcutInfoGridItemEntities: List<ShortcutInfoGridItemEntity>,
        folderGridItemEntities: List<FolderGridItemEntity>,
        shortcutConfigGridItemEntities: List<ShortcutConfigGridItemEntity>,
    )

    suspend fun deleteGridItemEntitiesTransaction(
        applicationInfoGridItemEntities: List<ApplicationInfoGridItemEntity>,
        widgetGridItemEntities: List<WidgetGridItemEntity>,
        shortcutInfoGridItemEntities: List<ShortcutInfoGridItemEntity>,
        folderGridItemEntities: List<FolderGridItemEntity>,
        shortcutConfigGridItemEntities: List<ShortcutConfigGridItemEntity>,
    )
}
