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
package com.eblan.launcher.domain.grid

import com.eblan.launcher.domain.model.Associate
import com.eblan.launcher.domain.model.EblanAction
import com.eblan.launcher.domain.model.EblanActionType
import com.eblan.launcher.domain.model.GridItem
import com.eblan.launcher.domain.model.GridItemData
import com.eblan.launcher.domain.model.GridItemSettings
import com.eblan.launcher.domain.model.HorizontalAlignment
import com.eblan.launcher.domain.model.TextColor
import com.eblan.launcher.domain.model.VerticalArrangement

fun gridItem(
    id: String = "Test",
    page: Int = 0,
    startColumn: Int,
    startRow: Int,
    columnSpan: Int,
    rowSpan: Int,
) = GridItem(
    id = id,
    page = page,
    startColumn = startColumn,
    startRow = startRow,
    columnSpan = columnSpan,
    rowSpan = rowSpan,
    data = GridItemData.Folder(
        label = "Test",
        icon = null,
        index = 0,
        folderId = null,
    ),
    associate = Associate.Grid,
    override = false,
    gridItemSettings = GridItemSettings(
        iconSize = 0,
        textColor = TextColor.System,
        textSize = 0,
        showLabel = true,
        singleLineLabel = true,
        horizontalAlignment = HorizontalAlignment.CenterHorizontally,
        verticalArrangement = VerticalArrangement.Center,
        customTextColor = 0,
        customBackgroundColor = 0,
        padding = 0,
        cornerRadius = 0,
    ),
    doubleTap = EblanAction(
        eblanActionType = EblanActionType.None,
        serialNumber = 0,
        componentName = "",
    ),
    swipeUp = EblanAction(
        eblanActionType = EblanActionType.None,
        serialNumber = 0,
        componentName = "",
    ),
    swipeDown = EblanAction(
        eblanActionType = EblanActionType.None,
        serialNumber = 0,
        componentName = "",
    ),
)
