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
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class Migration4To5Test {
    private val testDatabase = "migration-test"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        EblanDatabase::class.java,
    )

    @Test
    @Throws(IOException::class)
    fun migrate4To5() {
        // Create database at version 4
        helper.createDatabase(
            testDatabase,
            4,
        ).use { db ->
            // EblanApplicationInfoEntity
            db.execSQL(
                """
                INSERT INTO `EblanApplicationInfoEntity`
                (packageName, serialNumber, componentName, icon, label) 
                VALUES ('com.example.app', 1, 'com.example.app/.MainActivity', '/path/icon.png', 'Original App')
                """.trimIndent(),
            )

            // EblanAppWidgetProviderInfoEntity
            db.execSQL(
                """
                INSERT INTO `EblanAppWidgetProviderInfoEntity` (
                    componentName, serialNumber, packageName,
                    targetCellWidth, targetCellHeight, minWidth, minHeight,
                    resizeMode, minResizeWidth, minResizeHeight,
                    maxResizeWidth, maxResizeHeight, label
                ) VALUES 
                ('com.example.clock', 100, 'com.example.app', 4, 2, 4, 2, 1, 2, 2, 8, 8, 'Clock')
                """.trimIndent(),
            )

            // ApplicationInfoGridItemEntity
            db.execSQL(
                """
                INSERT INTO `ApplicationInfoGridItemEntity` (id, page, startColumn, startRow, columnSpan, rowSpan,
                    associate, componentName, packageName, override, serialNumber,
                    iconSize, textColor, textSize, showLabel, singleLineLabel,
                    horizontalAlignment, verticalArrangement, label)
                VALUES 
                ('item1', 0, 0, 0, 1, 1, 'none', 'com.app/.Main', 'com.app', 0, 100, 48, 
                 '#FFFFFF', 14, 1, 0, 'center', 'top', 'Browser')
                """.trimIndent(),
            )

            // WidgetGridItemEntity
            db.execSQL(
                """
                INSERT INTO `WidgetGridItemEntity` (
                id, folderId, page, startColumn, startRow, columnSpan, rowSpan,
                associate, appWidgetId, packageName, componentName, configure,
                minWidth, minHeight,
                resizeMode, minResizeWidth, minResizeHeight, maxResizeWidth, maxResizeHeight,
                targetCellWidth, targetCellHeight,
                preview, label, icon,
                override, serialNumber,
                iconSize, textColor, textSize, showLabel, singleLineLabel,
                horizontalAlignment, verticalArrangement
                ) VALUES 
                ('widget_1', NULL, 0, 0, 0, 4, 2,
                'none', 101, 'com.example', 'com.example.Clock', NULL,
                 200, 100,
                1, 2, 2, 8, 8,           
                4, 2,
                NULL, 'Clock', NULL,
                0, 500,
                48, '#FFFFFF', 14, 1, 0,
                'center', 'top')
                """.trimIndent(),
            )

            // FolderGridItemEntity
            db.execSQL(
                """
                INSERT INTO `FolderGridItemEntity` (
                    id, page, startColumn, startRow, columnSpan, rowSpan,
                    associate, label, override, pageCount,
                    iconSize, textColor, textSize, showLabel, singleLineLabel,
                    horizontalAlignment, verticalArrangement
                ) VALUES (
                    'folder_001', 0, 0, 0, 2, 2,
                    'folder', 'Work Apps', 0, 3,
                    64, '#FFFFFF', 16, 1, 0,
                    'center', 'top'
                )
                """.trimIndent(),
            )

            // ShortcutConfigGridItemEntity
            db.execSQL(
                """
                INSERT INTO `ShortcutConfigGridItemEntity` (
                    id, page, startColumn, startRow, columnSpan, rowSpan,
                    associate, componentName, packageName, activityLabel,
                    override, serialNumber, iconSize, textColor, textSize,
                    showLabel, singleLineLabel, horizontalAlignment, verticalArrangement
                ) VALUES (
                    'item_001', 0, 1, 2, 1, 1,
                    'app', 'com.browser/.Main', 'com.browser', 'Browser',
                    0, 300, 56, '#FFFFFF', 14, 1, 0, 'center', 'bottom'
                )
                """.trimIndent(),
            )
        }

        // Run migration and validate version 5
        helper.runMigrationsAndValidate(
            testDatabase,
            5,
            true,
        ).use { db ->
            // EblanApplicationInfoEntity
            db.query(
                """
            SELECT componentName, serialNumber, packageName, label, customIcon, customLabel
            FROM `EblanApplicationInfoEntity`
            ORDER BY serialNumber
                """.trimIndent(),
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())

                // Row 1
                assertEquals(
                    "com.example.app/.MainActivity",
                    cursor.getString(cursor.getColumnIndexOrThrow("componentName")),
                )
                assertEquals(1, cursor.getInt(cursor.getColumnIndexOrThrow("serialNumber")))
                assertEquals(
                    "com.example.app",
                    cursor.getString(cursor.getColumnIndexOrThrow("packageName")),
                )
                assertEquals(
                    "Original App",
                    cursor.getString(cursor.getColumnIndexOrThrow("label")),
                )
                assertNull(cursor.getString(cursor.getColumnIndexOrThrow("customIcon")))
                assertNull(cursor.getString(cursor.getColumnIndexOrThrow("customLabel")))
            }

            // EblanAppWidgetProviderInfoEntity
            db.query("SELECT componentName, label, serialNumber FROM `EblanAppWidgetProviderInfoEntity` ORDER BY serialNumber")
                .use { cursor ->
                    assertTrue(cursor.moveToFirst())

                    // Row 1
                    assertEquals(
                        "com.example.clock",
                        cursor.getString(cursor.getColumnIndexOrThrow("componentName")),
                    )
                    assertEquals("Clock", cursor.getString(cursor.getColumnIndexOrThrow("label")))
                }

            // ApplicationInfoGridItemEntity
            db.query("SELECT id, label, customIcon, customLabel FROM `ApplicationInfoGridItemEntity` ORDER BY serialNumber")
                .use { cursor ->
                    assertTrue(cursor.moveToFirst())
                    assertEquals("item1", cursor.getString(cursor.getColumnIndexOrThrow("id")))
                    assertEquals("Browser", cursor.getString(cursor.getColumnIndexOrThrow("label")))
                    assertNull(cursor.getString(cursor.getColumnIndexOrThrow("customIcon")))
                    assertNull(cursor.getString(cursor.getColumnIndexOrThrow("customLabel")))
                }

            // WidgetGridItemEntity
            db.query("SELECT id, label FROM `WidgetGridItemEntity` ORDER BY serialNumber")
                .use { cursor ->
                    assertTrue(cursor.moveToFirst())
                    assertEquals("Clock", cursor.getString(cursor.getColumnIndexOrThrow("label")))
                }

            // FolderGridItemEntity
            db.query("SELECT label, pageCount, icon, iconSize FROM `FolderGridItemEntity`")
                .use { cursor ->
                    assertTrue(cursor.moveToFirst())
                    assertEquals(
                        "Work Apps",
                        cursor.getString(cursor.getColumnIndexOrThrow("label")),
                    )
                    assertEquals(3, cursor.getInt(cursor.getColumnIndexOrThrow("pageCount")))
                    assertNull(cursor.getString(cursor.getColumnIndexOrThrow("icon")))
                    assertEquals(64, cursor.getInt(cursor.getColumnIndexOrThrow("iconSize")))
                }

            // ShortcutConfigGridItemEntity
            db.query("SELECT activityLabel, iconSize, customIcon, customLabel FROM `ShortcutConfigGridItemEntity`")
                .use { cursor ->
                    assertTrue(cursor.moveToFirst())
                    assertEquals(
                        "Browser",
                        cursor.getString(cursor.getColumnIndexOrThrow("activityLabel")),
                    )
                    assertEquals(56, cursor.getInt(cursor.getColumnIndexOrThrow("iconSize")))
                    assertNull(cursor.getString(cursor.getColumnIndexOrThrow("customIcon")))
                    assertNull(cursor.getString(cursor.getColumnIndexOrThrow("customLabel")))
                }
        }
    }
}
