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
package com.eblan.launcher.benchmark.grid

import com.eblan.launcher.domain.grid.resolveConflicts
import com.eblan.launcher.domain.model.Associate
import com.eblan.launcher.domain.model.EblanAction
import com.eblan.launcher.domain.model.EblanActionType
import com.eblan.launcher.domain.model.GridItem
import com.eblan.launcher.domain.model.GridItemData
import com.eblan.launcher.domain.model.GridItemSettings
import com.eblan.launcher.domain.model.HorizontalAlignment
import com.eblan.launcher.domain.model.ResolveDirection
import com.eblan.launcher.domain.model.TextColor
import com.eblan.launcher.domain.model.VerticalArrangement
import kotlinx.coroutines.runBlocking
import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.BenchmarkMode
import org.openjdk.jmh.annotations.Fork
import org.openjdk.jmh.annotations.Level
import org.openjdk.jmh.annotations.Measurement
import org.openjdk.jmh.annotations.Mode
import org.openjdk.jmh.annotations.OutputTimeUnit
import org.openjdk.jmh.annotations.Param
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.Setup
import org.openjdk.jmh.annotations.State
import org.openjdk.jmh.annotations.Warmup
import java.util.concurrent.TimeUnit

@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(
    iterations = 2,
    time = 10,
)
@Measurement(
    iterations = 3,
    time = 10,
)
@Fork(1)
open class MoveGridItemBenchmark {
    @Benchmark
    fun resolveConflictsRight(state: MoveGridItemBenchmarkState): Boolean = runBlocking {
        val gridItems = state.gridItems
        val movingGridItem = state.movingGridItem

        resolveConflicts(
            gridItems = gridItems,
            resolveDirection = ResolveDirection.Right,
            movingGridItem = movingGridItem,
            columns = COLUMNS,
            rows = ROWS,
        )
    }

    companion object {
        const val COLUMNS = 5
        const val ROWS = 4
    }
}

@State(Scope.Thread)
open class MoveGridItemBenchmarkState {
    @Param(
        "100",
        "1000",
        "5000",
        "10000",
    )
    var itemCount: Int = 0

    lateinit var movingGridItem: GridItem

    lateinit var gridItems: MutableList<GridItem>
        private set

    private lateinit var masterGridItems: List<GridItem>

    @Setup(Level.Trial)
    fun setup() {
        masterGridItems = buildGridItems(itemCount)
    }

    /*
     * Runs before every single invocation and is NOT included in JMH's
     * measured time. This guarantees every invocation of resolveConflicts()
     * sees the same congested starting grid, instead of the previous
     * invocation's already-resolved (and therefore cheaper) output.
     */
    @Setup(Level.Invocation)
    fun resetFixture() {
        gridItems = masterGridItems.map { it.copy() }.toMutableList()
        movingGridItem = gridItems.first()
    }

    private fun buildGridItems(
        itemCount: Int,
    ): List<GridItem> = buildList(itemCount) {
        /*
         * The moving item overlaps item-1 at (0, 0).
         *
         * [M/1][2][3][4][5]
         * [ 6 ][7][8][9][10]
         * [11 ][12][13][14][15]
         * [16 ][17][18][19][20]
         *
         * With more than 20 items, positions intentionally repeat.
         * This creates a highly congested grid and stresses conflict
         * resolution with a large number of items.
         */

        add(
            createGridItem(
                id = "moving",
                column = 0,
                row = 0,
                index = 0,
            ),
        )

        for (index in 1 until itemCount) {
            val position = index - 1

            add(
                createGridItem(
                    id = "item-$index",
                    column = position % MoveGridItemBenchmark.COLUMNS,
                    row = (position / MoveGridItemBenchmark.COLUMNS) % MoveGridItemBenchmark.ROWS,
                    index = index,
                ),
            )
        }
    }

    private fun createGridItem(
        id: String,
        column: Int,
        row: Int,
        index: Int,
    ): GridItem = GridItem(
        id = id,
        page = 0,
        startColumn = column,
        startRow = row,
        columnSpan = 1,
        rowSpan = 1,
        data = GridItemData.ApplicationInfo(
            serialNumber = 0,
            componentName = "",
            packageName = "",
            icon = null,
            label = "Item $index",
            customIcon = null,
            customLabel = null,
            index = index,
            folderId = null,
            iconPackInfoFilePath = null,
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
}
