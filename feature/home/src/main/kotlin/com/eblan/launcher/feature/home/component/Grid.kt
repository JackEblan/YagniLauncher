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
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ParentDataModifier
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import com.eblan.launcher.domain.model.EblanApplicationInfoWithIconPackInfo
import com.eblan.launcher.domain.model.GridItem

@Composable
internal fun GridLayout(
    modifier: Modifier = Modifier,
    columns: Int,
    gridItems: List<GridItem>?,
    rows: Int,
    content: @Composable BoxScope.(GridItem) -> Unit,
) {
    SubcomposeLayout(modifier = modifier) { constraints ->
        val cellWidth = constraints.maxWidth / columns

        val cellHeight = constraints.maxHeight / rows

        layout(width = constraints.maxWidth, height = constraints.maxHeight) {
            gridItems?.forEach { gridItem ->
                subcompose(gridItem.id) {
                    GridLayoutContent(
                        gridItem = gridItem,
                        cellWidth = cellWidth,
                        cellHeight = cellHeight,
                        content = content,
                    )
                }.forEach { measurable ->
                    val parentData = measurable.parentData as GridItemParentData

                    measurable.measure(
                        Constraints.fixed(
                            width = parentData.width,
                            height = parentData.height,
                        ),
                    ).placeRelative(
                        x = parentData.x,
                        y = parentData.y,
                    )
                }
            }
        }
    }
}

@Composable
internal fun HorizontalAppDrawerGridLayout(
    modifier: Modifier = Modifier,
    columns: Int,
    eblanApplicationInfoWithIconPackInfos: List<EblanApplicationInfoWithIconPackInfo>?,
    rows: Int,
    content: @Composable BoxScope.(EblanApplicationInfoWithIconPackInfo) -> Unit,
) {
    SubcomposeLayout(modifier = modifier) { constraints ->
        val cellWidth = constraints.maxWidth / columns

        val cellHeight = constraints.maxHeight / rows

        layout(constraints.maxWidth, constraints.maxHeight) {
            eblanApplicationInfoWithIconPackInfos?.forEachIndexed { index, eblanApplicationInfoWithIconPackInfo ->
                val row = index / columns

                val column = index % columns

                subcompose(
                    eblanApplicationInfoWithIconPackInfo.eblanApplicationInfo.serialNumber to
                        eblanApplicationInfoWithIconPackInfo.eblanApplicationInfo.componentName,
                ) {
                    Box(
                        modifier = Modifier.gridItem(
                            width = cellWidth,
                            height = cellHeight,
                            x = column * cellWidth,
                            y = row * cellHeight,
                        ),
                    ) {
                        content(eblanApplicationInfoWithIconPackInfo)
                    }
                }.forEach { measurable ->
                    measurable.measure(
                        Constraints.fixed(
                            width = cellWidth,
                            height = cellHeight,
                        ),
                    ).placeRelative(
                        x = column * cellWidth,
                        y = row * cellHeight,
                    )
                }
            }
        }
    }
}

@Composable
private fun GridLayoutContent(
    gridItem: GridItem,
    cellWidth: Int,
    cellHeight: Int,
    content: @Composable (BoxScope.(GridItem) -> Unit),
) {
    val width by animateIntAsState(
        targetValue = gridItem.columnSpan * cellWidth,
        label = "width",
    )

    val height by animateIntAsState(
        targetValue = gridItem.rowSpan * cellHeight,
        label = "height",
    )

    val x by animateIntAsState(
        targetValue = gridItem.startColumn * cellWidth,
        label = "x",
    )

    val y by animateIntAsState(
        targetValue = gridItem.startRow * cellHeight,
        label = "y",
    )

    Box(
        modifier = Modifier.gridItem(
            width = width,
            height = height,
            x = x,
            y = y,
        ),
        content = {
            content(gridItem)
        },
    )
}

private data class GridItemParentData(
    val width: Int,
    val height: Int,
    val x: Int,
    val y: Int,
)

private fun Modifier.gridItem(
    width: Int,
    height: Int,
    x: Int,
    y: Int,
): Modifier = then(
    object : ParentDataModifier {
        override fun Density.modifyParentData(parentData: Any?): Any = GridItemParentData(
            width = width,
            height = height,
            x = x,
            y = y,
        )
    },
)

