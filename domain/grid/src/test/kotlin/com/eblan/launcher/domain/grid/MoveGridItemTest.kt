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

import com.eblan.launcher.domain.model.ResolveDirection
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MoveGridItemTest {
    @Test
    fun `returns true when there are no conflicts`() {
        val moving = gridItem(
            id = "moving",
            startColumn = 0,
            startRow = 0,
            columnSpan = 1,
            rowSpan = 1,
        )

        val other = gridItem(
            id = "other",
            startColumn = 2,
            startRow = 0,
            columnSpan = 1,
            rowSpan = 1,
        )

        val gridItems = mutableListOf(moving, other)

        val result = runBlocking {
            resolveConflicts(
                gridItems = gridItems,
                resolveDirection = ResolveDirection.Right,
                movingGridItem = moving,
                columns = 5,
                rows = 4,
            )
        }

        assertTrue(result)
        assertEquals(
            listOf(moving, other),
            gridItems,
        )
    }

    @Test
    fun `moves conflicting item to the right`() {
        val moving = gridItem(
            id = "moving",
            startColumn = 0,
            startRow = 0,
            columnSpan = 1,
            rowSpan = 1,
        )

        val conflicting = gridItem(
            id = "conflicting",
            startColumn = 0,
            startRow = 0,
            columnSpan = 1,
            rowSpan = 1,
        )

        val gridItems = mutableListOf(moving, conflicting)

        val result = runBlocking {
            resolveConflicts(
                gridItems = gridItems,
                resolveDirection = ResolveDirection.Right,
                movingGridItem = moving,
                columns = 5,
                rows = 4,
            )
        }

        assertTrue(result)

        assertEquals(
            conflicting.copy(
                startColumn = 1,
                startRow = 0,
            ),
            gridItems[1],
        )
    }

    @Test
    fun `moves conflicting item to the left`() {
        val moving = gridItem(
            id = "moving",
            startColumn = 2,
            startRow = 0,
            columnSpan = 1,
            rowSpan = 1,
        )

        val conflicting = gridItem(
            id = "conflicting",
            startColumn = 2,
            startRow = 0,
            columnSpan = 1,
            rowSpan = 1,
        )

        val gridItems = mutableListOf(moving, conflicting)

        val result = runBlocking {
            resolveConflicts(
                gridItems = gridItems,
                resolveDirection = ResolveDirection.Left,
                movingGridItem = moving,
                columns = 5,
                rows = 4,
            )
        }

        assertTrue(result)

        assertEquals(
            conflicting.copy(
                startColumn = 1,
                startRow = 0,
            ),
            gridItems[1],
        )
    }

    @Test
    fun `returns false when conflicting item cannot move right`() {
        val moving = gridItem(
            id = "moving",
            startColumn = 4,
            startRow = 3,
            columnSpan = 1,
            rowSpan = 1,
        )

        val conflicting = gridItem(
            id = "conflicting",
            startColumn = 4,
            startRow = 3,
            columnSpan = 1,
            rowSpan = 1,
        )

        val gridItems = mutableListOf(moving, conflicting)

        val result = runBlocking {
            resolveConflicts(
                gridItems = gridItems,
                resolveDirection = ResolveDirection.Right,
                movingGridItem = moving,
                columns = 5,
                rows = 4,
            )
        }

        assertFalse(result)
    }

    @Test
    fun `returns false when conflicting item cannot move left`() {
        val moving = gridItem(
            id = "moving",
            startColumn = 0,
            startRow = 0,
            columnSpan = 1,
            rowSpan = 1,
        )

        val conflicting = gridItem(
            id = "conflicting",
            startColumn = 0,
            startRow = 0,
            columnSpan = 1,
            rowSpan = 1,
        )

        val gridItems = mutableListOf(moving, conflicting)

        val result = runBlocking {
            resolveConflicts(
                gridItems = gridItems,
                resolveDirection = ResolveDirection.Left,
                movingGridItem = moving,
                columns = 5,
                rows = 4,
            )
        }

        assertFalse(result)
    }

    @Test
    fun `moves conflicting item to next row when moving right exceeds column bounds`() {
        val moving = gridItem(
            id = "moving",
            startColumn = 4,
            startRow = 0,
            columnSpan = 1,
            rowSpan = 1,
        )

        val conflicting = gridItem(
            id = "conflicting",
            startColumn = 4,
            startRow = 0,
            columnSpan = 1,
            rowSpan = 1,
        )

        val gridItems = mutableListOf(moving, conflicting)

        val result = runBlocking {
            resolveConflicts(
                gridItems = gridItems,
                resolveDirection = ResolveDirection.Right,
                movingGridItem = moving,
                columns = 5,
                rows = 4,
            )
        }

        assertTrue(result)

        assertEquals(
            conflicting.copy(
                startColumn = 0,
                startRow = 1,
            ),
            gridItems[1],
        )
    }

    @Test
    fun `moves conflicting item to previous row when moving left exceeds column bounds`() {
        val moving = gridItem(
            id = "moving",
            startColumn = 0,
            startRow = 1,
            columnSpan = 1,
            rowSpan = 1,
        )

        val conflicting = gridItem(
            id = "conflicting",
            startColumn = 0,
            startRow = 1,
            columnSpan = 1,
            rowSpan = 1,
        )

        val gridItems = mutableListOf(moving, conflicting)

        val result = runBlocking {
            resolveConflicts(
                gridItems = gridItems,
                resolveDirection = ResolveDirection.Left,
                movingGridItem = moving,
                columns = 5,
                rows = 4,
            )
        }

        assertTrue(result)

        assertEquals(
            conflicting.copy(
                startColumn = 4,
                startRow = 0,
            ),
            gridItems[1],
        )
    }
}
