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

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.eblan.launcher.data.room.migration.Migration15To16
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class Migration15To16Test {

    private val testDatabase = "migration-test"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        EblanDatabase::class.java,
    )

    @Test
    @Throws(IOException::class)
    fun migrate15To16() {
        helper.createDatabase(testDatabase, 15).use { db ->
            db.execSQL(
                """
                INSERT INTO FolderGridItemEntity (
                    id,
                    page,
                    startColumn,
                    startRow,
                    columnSpan,
                    rowSpan,
                    associate,
                    label,
                    `override`,
                    icon,
                    iconSize,
                    textColor,
                    textSize,
                    showLabel,
                    singleLineLabel,
                    horizontalAlignment,
                    verticalArrangement,
                    customTextColor,
                    customBackgroundColor,
                    padding,
                    cornerRadius,
                    doubleTap_eblanActionType,
                    doubleTap_serialNumber,
                    doubleTap_componentName,
                    swipeUp_eblanActionType,
                    swipeUp_serialNumber,
                    swipeUp_componentName,
                    swipeDown_eblanActionType,
                    swipeDown_serialNumber,
                    swipeDown_componentName
                ) VALUES (
                    'folder_1',
                    0,
                    0,
                    0,
                    1,
                    1,
                    0,
                    'Folder',
                    0,
                    NULL,
                    48,
                    0,
                    12,
                    1,
                    1,
                    1,
                    1,
                    0,
                    0,
                    0,
                    0,
                    0,
                    1,
                    '',
                    0,
                    2,
                    '',
                    0,
                    3,
                    ''
                )
                """.trimIndent(),
            )
        }

        helper.runMigrationsAndValidate(
            testDatabase,
            16,
            true,
            Migration15To16(),
        ).use { db ->
            db.query(
                "SELECT * FROM FolderGridItemEntity WHERE id = 'folder_1'",
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())

                assertEquals(
                    "folder_1",
                    cursor.getString(cursor.getColumnIndexOrThrow("id")),
                )
                assertEquals(
                    0,
                    cursor.getInt(cursor.getColumnIndexOrThrow("page")),
                )
                assertEquals(
                    0,
                    cursor.getInt(cursor.getColumnIndexOrThrow("startColumn")),
                )
                assertEquals(
                    0,
                    cursor.getInt(cursor.getColumnIndexOrThrow("startRow")),
                )
                assertEquals(
                    1,
                    cursor.getInt(cursor.getColumnIndexOrThrow("columnSpan")),
                )
                assertEquals(
                    1,
                    cursor.getInt(cursor.getColumnIndexOrThrow("rowSpan")),
                )
                assertEquals(
                    0,
                    cursor.getInt(cursor.getColumnIndexOrThrow("associate")),
                )
                assertEquals(
                    "Folder",
                    cursor.getString(cursor.getColumnIndexOrThrow("label")),
                )
                assertEquals(
                    0,
                    cursor.getInt(cursor.getColumnIndexOrThrow("override")),
                )
                assertTrue(
                    cursor.isNull(cursor.getColumnIndexOrThrow("icon")),
                )
                assertEquals(
                    48,
                    cursor.getInt(cursor.getColumnIndexOrThrow("iconSize")),
                )

                assertEquals(
                    0,
                    cursor.getInt(cursor.getColumnIndexOrThrow("index")),
                )

                assertTrue(
                    cursor.isNull(cursor.getColumnIndexOrThrow("folderId")),
                )
            }
        }
    }
}
