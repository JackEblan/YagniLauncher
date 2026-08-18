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
package com.eblan.launcher.feature.editgriditem

import com.eblan.launcher.domain.model.GridItem
import com.eblan.launcher.domain.model.GridItemData

internal fun getGridItem(gridItem: GridItem, customIcon: String?): GridItem = when (val data = gridItem.data) {
    is GridItemData.ApplicationInfo -> gridItem.copy(data = data.copy(customIcon = customIcon))

    is GridItemData.Folder -> gridItem.copy(data = data.copy(icon = customIcon))

    is GridItemData.ShortcutConfig -> gridItem.copy(data = data.copy(customIcon = customIcon))

    is GridItemData.ShortcutInfo ->
        gridItem.copy(data = data.copy(customIcon = customIcon))

    else -> gridItem
}
