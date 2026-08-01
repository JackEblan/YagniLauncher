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
import com.eblan.launcher.data.room.migration.Migration3To4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class Migration3To4Test {
    private val testDatabase = "migration-test"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        EblanDatabase::class.java,
    )

    @Test
    @Throws(IOException::class)
    fun migrate3To4_eblanApplicationInfoEntity() {
        helper.createDatabase(testDatabase, 3).use { db ->
            db.execSQL(
                """
                INSERT INTO `EblanApplicationInfoEntity` 
                (packageName, serialNumber, componentName, icon, label)
                VALUES ('com.example.app', 0, NULL, NULL, 'Test App')
                """.trimIndent(),
            )
        }

        helper.runMigrationsAndValidate(testDatabase, 4, true, Migration3To4()).use { db ->
            db.query("SELECT * FROM `EblanApplicationInfoEntity`").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(
                    "com.example.app",
                    cursor.getString(cursor.getColumnIndexOrThrow("packageName")),
                )
                assertEquals(0, cursor.getInt(cursor.getColumnIndexOrThrow("serialNumber")))
                assertEquals(
                    "",
                    cursor.getString(cursor.getColumnIndexOrThrow("componentName")),
                ) // NULL → ""
                assertEquals("Test App", cursor.getString(cursor.getColumnIndexOrThrow("label")))
            }
        }
    }

    @Test
    @Throws(IOException::class)
    fun migrate3To4_eblanAppWidgetProviderInfoEntity() {
        helper.createDatabase(testDatabase, 3).use { db ->
            db.execSQL(
                """
                INSERT INTO `EblanAppWidgetProviderInfoEntity` (
                    className, componentName, configure, packageName,
                    targetCellWidth, targetCellHeight, minWidth, minHeight,
                    resizeMode, minResizeWidth, minResizeHeight,
                    maxResizeWidth, maxResizeHeight, preview, label, icon
                ) VALUES (
                    'com.example.widget.OldWidget', 'com.example.app/com.example.widget.OldWidget', NULL, 'com.example.app',
                    2, 2, 110, 110, 3, 110, 110, 400, 400, NULL, 'Clock Widget', NULL
                )
                """.trimIndent(),
            )
        }

        helper.runMigrationsAndValidate(testDatabase, 4, true, Migration3To4()).use { db ->
            db.query("SELECT componentName, serialNumber, packageName, label FROM `EblanAppWidgetProviderInfoEntity`")
                .use { cursor ->
                    assertTrue(cursor.moveToFirst())
                    assertEquals(
                        "com.example.app/com.example.widget.OldWidget",
                        cursor.getString(cursor.getColumnIndexOrThrow("componentName")),
                    )
                    assertEquals(
                        0,
                        cursor.getInt(cursor.getColumnIndexOrThrow("serialNumber")),
                    ) // default we set
                    assertEquals(
                        "Clock Widget",
                        cursor.getString(cursor.getColumnIndexOrThrow("label")),
                    )
                }
        }
    }

    @Test
    @Throws(IOException::class)
    fun migrate3To4_applicationInfoGridItemEntity() {
        helper.createDatabase(testDatabase, 3).use { db ->
            db.execSQL(
                """
                INSERT INTO `ApplicationInfoGridItemEntity` (
                    id, folderId, page, startColumn, startRow, columnSpan, rowSpan, associate,
                    componentName, packageName, icon, label, override, serialNumber,
                    iconSize, textColor, textSize, showLabel, singleLineLabel,
                    horizontalAlignment, verticalArrangement
                ) VALUES (
                    'app1', NULL, 0, 0, 0, 1, 1, 'assoc1',
                    NULL, 'com.example.app', NULL, 'Legacy App', 0, 0,
                    48, '#FFFFFF', 14, 1, 1, 'CENTER', 'MIDDLE'
                )
                """.trimIndent(),
            )
        }

        helper.runMigrationsAndValidate(testDatabase, 4, true, Migration3To4()).use { db ->
            db.query("SELECT componentName FROM `ApplicationInfoGridItemEntity` WHERE id = 'app1'")
                .use { cursor ->
                    assertTrue(cursor.moveToFirst())
                    assertEquals(
                        "",
                        cursor.getString(cursor.getColumnIndexOrThrow("componentName")),
                    ) // NULL → "" safely migrated
                }
        }
    }

    @Test
    @Throws(IOException::class)
    fun migrate3To4_widgetGridItemEntity() {
        helper.createDatabase(testDatabase, 3).use { db ->
            db.execSQL(
                """
                INSERT INTO `WidgetGridItemEntity` (
                    id, folderId, page, startColumn, startRow, columnSpan, rowSpan, associate,
                    appWidgetId, packageName, className, componentName, configure,
                    minWidth, minHeight, resizeMode, minResizeWidth, minResizeHeight,
                    maxResizeWidth, maxResizeHeight, targetCellHeight, targetCellWidth,
                    preview, label, icon, override, serialNumber,
                    iconSize, textColor, textSize, showLabel, singleLineLabel,
                    horizontalAlignment, verticalArrangement
                ) VALUES (
                    'widget1', NULL, 0, 1, 1, 2, 2, 'assoc2',
                    201, 'com.example.app', 'com.example.widget.OldWidget', 'com.example.app/com.example.widget.OldWidget', NULL,
                    110, 110, 3, 110, 110, 400, 400, 2, 2,
                    NULL, 'Weather', NULL, 0, 1,
                    56, '#000000', 12, 1, 0, 'LEFT', 'TOP'
                )
                """.trimIndent(),
            )
        }

        helper.runMigrationsAndValidate(testDatabase, 4, true, Migration3To4()).use { db ->
            db.query("SELECT componentName FROM `WidgetGridItemEntity` WHERE id = 'widget1'")
                .use { cursor ->
                    assertTrue(cursor.moveToFirst())
                    assertEquals(
                        "com.example.app/com.example.widget.OldWidget",
                        cursor.getString(cursor.getColumnIndexOrThrow("componentName")),
                    )
                    // className column should no longer exist
                    assertEquals(-1, cursor.getColumnIndex("className"))
                }
        }
    }

    @Test
    @Throws(IOException::class)
    fun migrate3To4_newTablesCreated() {
        helper.createDatabase(testDatabase, 3).use { db ->
            // Insert minimal data to ensure migration runs
            db.execSQL(
                """
                INSERT INTO `EblanApplicationInfoEntity` 
                (packageName, serialNumber, componentName, icon, label)
                VALUES ('com.example.app', 0, NULL, NULL, 'Test App')
                """.trimIndent(),
            )
        }

        helper.runMigrationsAndValidate(testDatabase, 4, true, Migration3To4()).use { db ->
            // EblanShortcutConfigEntity table should exist
            db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='EblanShortcutConfigEntity'")
                .use { cursor ->
                    assertEquals(1, cursor.count)
                }

            // ShortcutConfigGridItemEntity table should exist
            db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='ShortcutConfigGridItemEntity'")
                .use { cursor ->
                    assertEquals(1, cursor.count)
                }
        }
    }
}
