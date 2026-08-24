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
package com.eblan.launcher.feature.home.component

import androidx.compose.animation.core.animateIntAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.ParentDataModifier
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import com.eblan.launcher.domain.model.GridItem
import com.eblan.launcher.domain.usecase.grid.FOLDER_PREVIEW_COLUMNS
import com.eblan.launcher.domain.usecase.grid.FOLDER_PREVIEW_ROWS

@Composable
internal fun PreviewFolderGridLayout(
    modifier: Modifier = Modifier,
    gridItems: List<GridItem>?,
    previewColumns: Int = FOLDER_PREVIEW_COLUMNS,
    previewRows: Int = FOLDER_PREVIEW_ROWS,
    content: @Composable (GridItem) -> Unit,
) {
    Layout(
        modifier = modifier,
        content = {
            gridItems?.forEach { gridItem ->
                key(gridItem.id) {
                    content(gridItem)
                }
            }
        },
    ) { measurables, constraints ->
        val previewCellSize = minOf(
            constraints.maxWidth,
            constraints.maxHeight,
        ) / maxOf(
            previewColumns,
            previewRows,
        )

        val previewGridWidth = previewCellSize * previewColumns

        val previewGridHeight = previewCellSize * previewRows

        val previewOffsetX = (constraints.maxWidth - previewGridWidth) / 2

        val previewOffsetY = (constraints.maxHeight - previewGridHeight) / 2

        val placeables = measurables.mapIndexed { index, measurable ->
            val x = previewOffsetX +
                (index % previewColumns) * previewCellSize

            val y = previewOffsetY +
                (index / previewColumns) * previewCellSize

            measurable.measure(
                Constraints.fixed(
                    width = previewCellSize,
                    height = previewCellSize,
                ),
            ) to IntOffset(x = x, y = y)
        }

        layout(
            width = constraints.maxWidth,
            height = constraints.maxHeight,
        ) {
            placeables.forEach { (placeable, intOffset) ->
                placeable.placeRelative(
                    x = intOffset.x,
                    y = intOffset.y,
                )
            }
        }
    }
}

@Composable
internal fun FolderGridLayout(
    modifier: Modifier = Modifier,
    gridItems: List<GridItem>?,
    columns: Int,
    rows: Int,
    width: Int,
    height: Int,
    animate: Boolean,
    content: @Composable BoxScope.(GridItem) -> Unit,
) {
    Layout(
        modifier = modifier,
        content = {
            gridItems?.forEachIndexed { index, gridItem ->
                key(gridItem.id) {
                    FolderGridLayoutContent(
                        index = index,
                        columns = columns,
                        cellWidth = width / columns,
                        cellHeight = height / rows,
                        gridItem = gridItem,
                        animate = animate,
                        content = content,
                    )
                }
            }
        },
    ) { measurables, constraints ->
        val placeables = measurables.map { measurable ->
            val parentData =
                measurable.parentData as FolderGridItemParentData

            measurable.measure(
                Constraints.fixed(
                    width = parentData.width,
                    height = parentData.height,
                ),
            ) to parentData
        }

        layout(
            width = constraints.maxWidth,
            height = constraints.maxHeight,
        ) {
            placeables.forEach { (placeable, parentData) ->
                placeable.placeRelativeWithLayer(
                    x = parentData.x,
                    y = parentData.y,
                )
            }
        }
    }
}

@Composable
private fun FolderGridLayoutContent(
    modifier: Modifier = Modifier,
    index: Int,
    columns: Int,
    cellWidth: Int,
    cellHeight: Int,
    gridItem: GridItem,
    animate: Boolean,
    content: @Composable (BoxScope.(GridItem) -> Unit),
) {
    val x = (index % columns) * cellWidth
    val y = (index / columns) * cellHeight

    val animatedX by animateIntAsState(
        targetValue = x,
        label = "x",
    )

    val animatedY by animateIntAsState(
        targetValue = y,
        label = "y",
    )

    Box(
        modifier = modifier.folderGridItem(
            x = if (animate) animatedX else x,
            y = if (animate) animatedY else y,
            width = cellWidth,
            height = cellHeight,
        ),
    ) {
        content(gridItem)
    }
}

private fun Modifier.folderGridItem(
    x: Int,
    y: Int,
    width: Int,
    height: Int,
) = then(
    object : ParentDataModifier {
        override fun Density.modifyParentData(parentData: Any?) = FolderGridItemParentData(
            x = x,
            y = y,
            width = width,
            height = height,
        )
    },
)

private data class FolderGridItemParentData(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
)
