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

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ParentDataModifier
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.util.lerp
import com.eblan.launcher.domain.model.EblanApplicationInfo
import com.eblan.launcher.domain.model.GridItem
import com.eblan.launcher.feature.home.util.FOLDER_PREVIEW_COLUMNS
import com.eblan.launcher.feature.home.util.FOLDER_PREVIEW_ROWS
import kotlin.math.roundToInt

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
                    val width by animateIntAsState(targetValue = gridItem.columnSpan * cellWidth)

                    val height by animateIntAsState(targetValue = gridItem.rowSpan * cellHeight)

                    val x by animateIntAsState(targetValue = gridItem.startColumn * cellWidth)

                    val y by animateIntAsState(targetValue = gridItem.startRow * cellHeight)

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
internal fun FolderGridLayout(
    modifier: Modifier = Modifier,
    gridItems: List<GridItem>?,
    columns: Int,
    rows: Int,
    previewEnabled: Boolean = false,
    previewColumns: Int = FOLDER_PREVIEW_COLUMNS,
    previewRows: Int = FOLDER_PREVIEW_ROWS,
    progress: Float = 0f,
    content: @Composable BoxScope.(GridItem) -> Unit,
) {
    SubcomposeLayout(modifier = modifier) { constraints ->
        val containerWidth = constraints.maxWidth
        val containerHeight = constraints.maxHeight

        val endCellWidth = containerWidth / columns
        val endCellHeight = containerHeight / rows

        val previewItemCount = previewColumns * previewRows
        val previewCellSize = minOf(containerWidth, containerHeight) /
            maxOf(previewColumns, previewRows).toFloat()

        val previewOffsetX = (containerWidth - (previewCellSize * previewColumns)) / 2f
        val previewOffsetY = (containerHeight - (previewCellSize * previewRows)) / 2f

        layout(width = containerWidth, height = containerHeight) {
            gridItems?.forEachIndexed { index, gridItem ->
                subcompose(gridItem.id) {
                    val endX = (index % columns) * endCellWidth
                    val endY = (index / columns) * endCellHeight
                    val isPreview = index < previewItemCount

                    // 1. Determine Starting Points
                    // If preview is disabled, we start and end at the same grid position
                    val startX = if (previewEnabled && isPreview) {
                        previewOffsetX + (index % previewColumns) * previewCellSize
                    } else {
                        endX.toFloat()
                    }

                    val startY = if (previewEnabled && isPreview) {
                        previewOffsetY + (index / previewColumns) * previewCellSize
                    } else {
                        endY.toFloat()
                    }

                    val startSize = if (previewEnabled && isPreview) {
                        previewCellSize
                    } else {
                        endCellWidth.toFloat()
                    }

                    // 2. Calculate Targets based on progress
                    val targetX = lerp(startX, endX.toFloat(), progress)
                    val targetY = lerp(startY, endY.toFloat(), progress)
                    val targetWidth = lerp(startSize, endCellWidth.toFloat(), progress)
                    val targetHeight = lerp(startSize, endCellHeight.toFloat(), progress)
                    val targetAlpha = if (previewEnabled && !isPreview) progress else 1f

                    // 3. Select Animation Spec
                    // Use snap during opening (progress < 1) to avoid lag/shake.
                    // Use spring if preview is disabled OR if folder is fully open (for reordering).
                    val animationSpec = if (previewEnabled && progress < 1f) {
                        snap<Float>()
                    } else {
                        spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMediumLow,
                        )
                    }

                    val animatedX by animateFloatAsState(
                        targetX,
                        animationSpec,
                        label = "x",
                    )

                    val animatedY by animateFloatAsState(
                        targetY,
                        animationSpec,
                        label = "y",
                    )

                    val animatedWidth by animateFloatAsState(
                        targetWidth,
                        animationSpec,
                        label = "w",
                    )
                    val animatedHeight by animateFloatAsState(
                        targetHeight,
                        animationSpec,
                        label = "h",
                    )

                    val animatedAlpha by animateFloatAsState(
                        targetAlpha,
                        label = "a",
                    )

                    Box(
                        modifier = Modifier.folderGridItem(
                            animatedX.roundToInt(),
                            animatedY.roundToInt(),
                            animatedWidth.roundToInt(),
                            animatedHeight.roundToInt(),
                            animatedAlpha,
                        ),
                    ) {
                        content(gridItem)
                    }
                }.forEach { measurable ->
                    val parentData = measurable.parentData as FolderGridItemParentData

                    measurable.measure(
                        Constraints.fixed(
                            width = parentData.width,
                            height = parentData.height,
                        ),
                    ).placeRelativeWithLayer(
                        x = parentData.x,
                        y = parentData.y,
                    ) {
                        alpha = parentData.alpha
                    }
                }
            }
        }
    }
}

@Composable
internal fun HorizontalAppDrawerGridLayout(
    modifier: Modifier = Modifier,
    columns: Int,
    eblanApplicationInfos: List<EblanApplicationInfo>?,
    rows: Int,
    content: @Composable BoxScope.(EblanApplicationInfo) -> Unit,
) {
    SubcomposeLayout(modifier = modifier) { constraints ->
        val cellWidth = constraints.maxWidth / columns

        val cellHeight = constraints.maxHeight / rows

        layout(constraints.maxWidth, constraints.maxHeight) {
            eblanApplicationInfos?.forEachIndexed { index, eblanApplicationInfo ->
                val row = index / columns

                val column = index % columns

                subcompose(eblanApplicationInfo.serialNumber to eblanApplicationInfo.componentName) {
                    Box(
                        modifier = Modifier.gridItem(
                            width = cellWidth,
                            height = cellHeight,
                            x = column * cellWidth,
                            y = row * cellHeight,
                        ),
                    ) {
                        content(eblanApplicationInfo)
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

private data class GridItemParentData(
    val width: Int,
    val height: Int,
    val x: Int,
    val y: Int,
)

private data class FolderGridItemParentData(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
    val alpha: Float,
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

private fun Modifier.folderGridItem(
    x: Int,
    y: Int,
    width: Int,
    height: Int,
    alpha: Float,
) = then(
    object : ParentDataModifier {
        override fun Density.modifyParentData(parentData: Any?) = FolderGridItemParentData(
            x = x,
            y = y,
            width = width,
            height = height,
            alpha = alpha,
        )
    },
)
