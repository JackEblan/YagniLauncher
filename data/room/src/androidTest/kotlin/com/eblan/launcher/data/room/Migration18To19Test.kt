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
import com.eblan.launcher.data.room.migration.Migration18To19
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class Migration18To19Test {
    private val testDatabase = "migration-test"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        EblanDatabase::class.java,
    )

    @Test
    @Throws(IOException::class)
    fun migrate18To19_eblanAppWidgetProviderInfoEntity() {
        helper.createDatabase(testDatabase, 18).use { db ->
            db.execSQL(
                """
                INSERT INTO EblanAppWidgetProviderInfoEntity (
                    componentName, serialNumber, configure, packageName,
                    targetCellWidth, targetCellHeight, minWidth, minHeight,
                    resizeMode, minResizeWidth, minResizeHeight,
                    maxResizeWidth, maxResizeHeight, preview,
                    applicationLabel, applicationIcon, lastUpdateTime,
                    label, description
                ) VALUES (
                    'com.example.app/.Widget', 1001, 'com.example.app/.Configure', 'com.example.app',
                    2, 3, 100, 150,
                    1, 100, 150,
                    400, 450, 'preview.png',
                    'Example Widget', 'icon.png', 999,
                    'Widget Label', 'Widget Description'
                )
                """.trimIndent(),
            )
        }

        helper.runMigrationsAndValidate(
            testDatabase,
            19,
            true,
            Migration18To19(),
        ).use { db ->
            db.query("SELECT * FROM EblanAppWidgetProviderInfoEntity WHERE componentName = 'com.example.app/.Widget'")
                .use { cursor ->
                    assertTrue(cursor.moveToFirst())

                    assertEquals(
                        "com.example.app/.Widget",
                        cursor.getString(cursor.getColumnIndexOrThrow("componentName")),
                    )

                    assertEquals(
                        1001L,
                        cursor.getLong(cursor.getColumnIndexOrThrow("serialNumber")),
                    )

                    assertEquals(
                        "com.example.app/.Configure",
                        cursor.getString(cursor.getColumnIndexOrThrow("configure")),
                    )

                    assertEquals(
                        "com.example.app",
                        cursor.getString(cursor.getColumnIndexOrThrow("packageName")),
                    )

                    assertEquals(
                        2,
                        cursor.getInt(cursor.getColumnIndexOrThrow("targetCellWidth")),
                    )

                    assertEquals(
                        3,
                        cursor.getInt(cursor.getColumnIndexOrThrow("targetCellHeight")),
                    )

                    assertEquals(
                        100,
                        cursor.getInt(cursor.getColumnIndexOrThrow("minWidth")),
                    )

                    assertEquals(
                        150,
                        cursor.getInt(cursor.getColumnIndexOrThrow("minHeight")),
                    )

                    assertEquals(
                        1,
                        cursor.getInt(cursor.getColumnIndexOrThrow("resizeMode")),
                    )

                    assertEquals(
                        100,
                        cursor.getInt(cursor.getColumnIndexOrThrow("minResizeWidth")),
                    )

                    assertEquals(
                        150,
                        cursor.getInt(cursor.getColumnIndexOrThrow("minResizeHeight")),
                    )

                    assertEquals(
                        400,
                        cursor.getInt(cursor.getColumnIndexOrThrow("maxResizeWidth")),
                    )

                    assertEquals(
                        450,
                        cursor.getInt(cursor.getColumnIndexOrThrow("maxResizeHeight")),
                    )

                    assertEquals(
                        "preview.png",
                        cursor.getString(cursor.getColumnIndexOrThrow("preview")),
                    )

                    assertEquals(
                        "Example Widget",
                        cursor.getString(cursor.getColumnIndexOrThrow("applicationLabel")),
                    )

                    assertEquals(
                        "icon.png",
                        cursor.getString(cursor.getColumnIndexOrThrow("applicationIcon")),
                    )

                    assertEquals(
                        999L,
                        cursor.getLong(cursor.getColumnIndexOrThrow("lastUpdateTime")),
                    )

                    assertEquals(
                        "Widget Label",
                        cursor.getString(cursor.getColumnIndexOrThrow("label")),
                    )

                    assertEquals(
                        "Widget Description",
                        cursor.getString(cursor.getColumnIndexOrThrow("description")),
                    )
                }

            db.execSQL(
                "UPDATE EblanAppWidgetProviderInfoEntity SET applicationLabel = NULL WHERE componentName = 'com.example.app/.Widget'",
            )

            db.query("SELECT * FROM EblanAppWidgetProviderInfoEntity WHERE componentName = 'com.example.app/.Widget'")
                .use { cursor ->
                    assertTrue(cursor.moveToFirst())
                    assertNull(cursor.getString(cursor.getColumnIndexOrThrow("applicationLabel")))
                }
        }
    }

    @Test
    @Throws(IOException::class)
    fun migrate18To19_widgetGridItemEntity() {
        helper.createDatabase(testDatabase, 18).use { db ->
            db.execSQL(
                """
                INSERT INTO WidgetGridItemEntity (
                    id, page, startColumn, startRow, columnSpan, rowSpan,
                    associate, appWidgetId, packageName, componentName, configure,
                    minWidth, minHeight, resizeMode, minResizeWidth, minResizeHeight,
                    maxResizeWidth, maxResizeHeight, targetCellHeight, targetCellWidth,
                    preview, label, icon, override, serialNumber,
                    iconSize, textColor, textSize, showLabel, singleLineLabel,
                    horizontalAlignment, verticalArrangement, customTextColor,
                    customBackgroundColor, padding, cornerRadius
                ) VALUES (
                    'grid_item_1', 0, 1, 2, 3, 4,
                    'GRID', 5, 'com.example.app', 'com.example.app/.Widget', 'com.example.app/.Configure',
                    100, 150, 1, 100, 150,
                    400, 450, 3, 2,
                    'preview.png', 'Test Label', 'icon.png', 1, 777,
                    24, 'SYSTEM', 12, 1, 0,
                    'START', 'TOP', 111,
                    222, 8, 16
                )
                """.trimIndent(),
            )
        }

        helper.runMigrationsAndValidate(
            testDatabase,
            19,
            true,
            Migration18To19(),
        ).use { db ->
            db.query("SELECT * FROM WidgetGridItemEntity WHERE id = 'grid_item_1'").use { cursor ->
                assertTrue(cursor.moveToFirst())

                assertEquals(
                    "grid_item_1",
                    cursor.getString(cursor.getColumnIndexOrThrow("id")),
                )

                assertEquals(
                    0,
                    cursor.getInt(cursor.getColumnIndexOrThrow("page")),
                )

                assertEquals(
                    1,
                    cursor.getInt(cursor.getColumnIndexOrThrow("startColumn")),
                )

                assertEquals(
                    2,
                    cursor.getInt(cursor.getColumnIndexOrThrow("startRow")),
                )

                assertEquals(
                    3,
                    cursor.getInt(cursor.getColumnIndexOrThrow("columnSpan")),
                )

                assertEquals(
                    4,
                    cursor.getInt(cursor.getColumnIndexOrThrow("rowSpan")),
                )

                assertEquals(
                    "GRID",
                    cursor.getString(cursor.getColumnIndexOrThrow("associate")),
                )

                assertEquals(
                    5,
                    cursor.getInt(cursor.getColumnIndexOrThrow("appWidgetId")),
                )

                assertEquals(
                    "com.example.app",
                    cursor.getString(cursor.getColumnIndexOrThrow("packageName")),
                )

                assertEquals(
                    "com.example.app/.Widget",
                    cursor.getString(cursor.getColumnIndexOrThrow("componentName")),
                )

                assertEquals(
                    "com.example.app/.Configure",
                    cursor.getString(cursor.getColumnIndexOrThrow("configure")),
                )

                assertEquals(
                    100,
                    cursor.getInt(cursor.getColumnIndexOrThrow("minWidth")),
                )

                assertEquals(
                    150,
                    cursor.getInt(cursor.getColumnIndexOrThrow("minHeight")),
                )

                assertEquals(
                    1,
                    cursor.getInt(cursor.getColumnIndexOrThrow("resizeMode")),
                )

                assertEquals(
                    100,
                    cursor.getInt(cursor.getColumnIndexOrThrow("minResizeWidth")),
                )

                assertEquals(
                    150,
                    cursor.getInt(cursor.getColumnIndexOrThrow("minResizeHeight")),
                )

                assertEquals(
                    400,
                    cursor.getInt(cursor.getColumnIndexOrThrow("maxResizeWidth")),
                )

                assertEquals(
                    450,
                    cursor.getInt(cursor.getColumnIndexOrThrow("maxResizeHeight")),
                )

                assertEquals(
                    3,
                    cursor.getInt(cursor.getColumnIndexOrThrow("targetCellHeight")),
                )

                assertEquals(
                    2,
                    cursor.getInt(cursor.getColumnIndexOrThrow("targetCellWidth")),
                )

                assertEquals(
                    "preview.png",
                    cursor.getString(cursor.getColumnIndexOrThrow("preview")),
                )

                assertEquals(
                    "Test Label",
                    cursor.getString(cursor.getColumnIndexOrThrow("label")),
                )

                assertEquals(
                    "icon.png",
                    cursor.getString(cursor.getColumnIndexOrThrow("icon")),
                )

                assertEquals(
                    1,
                    cursor.getInt(cursor.getColumnIndexOrThrow("override")),
                )

                assertEquals(
                    777L,
                    cursor.getLong(cursor.getColumnIndexOrThrow("serialNumber")),
                )

                assertEquals(
                    24,
                    cursor.getInt(cursor.getColumnIndexOrThrow("iconSize")),
                )

                assertEquals(
                    "SYSTEM",
                    cursor.getString(cursor.getColumnIndexOrThrow("textColor")),
                )

                assertEquals(
                    12,
                    cursor.getInt(cursor.getColumnIndexOrThrow("textSize")),
                )

                assertEquals(
                    1,
                    cursor.getInt(cursor.getColumnIndexOrThrow("showLabel")),
                )

                assertEquals(
                    0,
                    cursor.getInt(cursor.getColumnIndexOrThrow("singleLineLabel")),
                )

                assertEquals(
                    "START",
                    cursor.getString(cursor.getColumnIndexOrThrow("horizontalAlignment")),
                )

                assertEquals(
                    "TOP",
                    cursor.getString(cursor.getColumnIndexOrThrow("verticalArrangement")),
                )

                assertEquals(
                    111,
                    cursor.getInt(cursor.getColumnIndexOrThrow("customTextColor")),
                )

                assertEquals(
                    222,
                    cursor.getInt(cursor.getColumnIndexOrThrow("customBackgroundColor")),
                )

                assertEquals(
                    8,
                    cursor.getInt(cursor.getColumnIndexOrThrow("padding")),
                )

                assertEquals(
                    16,
                    cursor.getInt(cursor.getColumnIndexOrThrow("cornerRadius")),
                )
            }

            db.execSQL(
                "UPDATE WidgetGridItemEntity SET label = NULL WHERE id = 'grid_item_1'",
            )

            db.query("SELECT * FROM WidgetGridItemEntity WHERE id = 'grid_item_1'").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertNull(cursor.getString(cursor.getColumnIndexOrThrow("label")))
            }
        }
    }
}
