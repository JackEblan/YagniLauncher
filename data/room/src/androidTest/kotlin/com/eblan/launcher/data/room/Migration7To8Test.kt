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
import com.eblan.launcher.data.room.migration.Migration7To8
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class Migration7To8Test {

    private val testDatabase = "migration-test"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        EblanDatabase::class.java,
    )

    @Test
    @Throws(IOException::class)
    fun migrate7To8() {
        helper.createDatabase(testDatabase, 7).use { db ->

            // ----------------------------
            // ApplicationInfoGridItemEntity
            // ----------------------------
            db.execSQL(
                """
                INSERT INTO ApplicationInfoGridItemEntity (
                    id, folderId, page, startColumn, startRow,
                    columnSpan, rowSpan, associate,
                    componentName, packageName,
                    icon, label, override, serialNumber,
                    customIcon, customLabel,
                    iconSize, textColor, textSize,
                    showLabel, singleLineLabel,
                    horizontalAlignment, verticalArrangement
                ) VALUES (
                    'app_1', NULL, 0, 0, 0,
                    1, 1, 'APP',
                    'com.example.app/.MainActivity', 'com.example.app',
                    'icon', 'Example App', 0, 1002,
                    NULL, NULL,
                    0, '#FF000000', 14,
                    1, 1,
                    'CENTER', 'CENTER'
                )
                """.trimIndent(),
            )

            // ----------------------------
            // ShortcutInfoGridItemEntity
            // ----------------------------
            db.execSQL(
                """
                INSERT INTO ShortcutInfoGridItemEntity (
                    id, folderId, page, startColumn, startRow,
                    columnSpan, rowSpan, associate,
                    shortcutId, packageName,
                    shortLabel, longLabel,
                    icon, override, serialNumber, isEnabled,
                    eblanApplicationInfoIcon, customIcon, customShortLabel,
                    iconSize, textColor, textSize,
                    showLabel, singleLineLabel,
                    horizontalAlignment, verticalArrangement
                ) VALUES (
                    'shortcut_1', NULL, 0, 1, 0,
                    1, 1, 'UNSPECIFIED',
                    'shortcut_id_1', 'com.example',
                    'Example', 'Example Shortcut',
                    NULL, 0, 1, 1,
                    NULL, NULL, NULL,
                    0, '#FF000000', 14,
                    1, 1,
                    'CENTER', 'CENTER'
                )
                """.trimIndent(),
            )

            // ----------------------------
            // FolderGridItemEntity
            // ----------------------------
            db.execSQL(
                """
                INSERT INTO FolderGridItemEntity (
                    id, folderId, page, startColumn, startRow,
                    columnSpan, rowSpan, associate,
                    label, override, pageCount, icon,
                    iconSize, textColor, textSize,
                    showLabel, singleLineLabel,
                    horizontalAlignment, verticalArrangement
                ) VALUES (
                    'folder_1', NULL, 0, 0, 0,
                    2, 2, 'FOLDER',
                    'My Folder', 0, 4, NULL,
                    0, '#FF000000', 14,
                    1, 1,
                    'CENTER', 'CENTER'
                )
                """.trimIndent(),
            )

            // ----------------------------
            // ShortcutConfigGridItemEntity
            // ----------------------------
            db.execSQL(
                """
                INSERT INTO ShortcutConfigGridItemEntity (
                    id, folderId, page, startColumn, startRow,
                    columnSpan, rowSpan, associate,
                    componentName, packageName,
                    activityIcon, activityLabel,
                    applicationIcon, applicationLabel,
                    override, serialNumber,
                    shortcutIntentName, shortcutIntentIcon, shortcutIntentUri,
                    customIcon, customLabel,
                    iconSize, textColor, textSize,
                    showLabel, singleLineLabel,
                    horizontalAlignment, verticalArrangement
                ) VALUES (
                    'config_1', NULL, 0, 2, 1,
                    1, 1, 'APP',
                    'com.whatsapp/.MainActivity', 'com.whatsapp',
                    NULL, NULL,
                    'ic_whatsapp', 'WhatsApp',
                    0, 1001,
                    NULL, NULL, NULL,
                    NULL, NULL,
                    0, '#FF000000', 14,
                    1, 1,
                    'CENTER', 'CENTER'
                )
                """.trimIndent(),
            )

            // ----------------------------
            // WidgetGridItemEntity
            // ----------------------------
            db.execSQL(
                """
                INSERT INTO WidgetGridItemEntity (
                    id, folderId, page, startColumn, startRow,
                    columnSpan, rowSpan, associate,
                    appWidgetId, packageName, componentName,
                    configure, minWidth, minHeight,
                    resizeMode, minResizeWidth, minResizeHeight,
                    maxResizeWidth, maxResizeHeight,
                    targetCellHeight, targetCellWidth,
                    preview, label, icon,
                    override, serialNumber,
                    iconSize, textColor, textSize,
                    showLabel, singleLineLabel,
                    horizontalAlignment, verticalArrangement
                ) VALUES (
                    'widget_1', NULL, 1, 0, 1,
                    2, 2, 'WIDGET',
                    1234, 'com.google.android.deskclock',
                    'com.google.android.deskclock.widget.AnalogClockWidgetProvider',
                    NULL, 110, 110,
                    15, 80, 80,
                    400, 400,
                    2, 2,
                    NULL, 'Clock', 'icon',
                    0, 2001,
                    0, '#FF000000', 12,
                    1, 0,
                    'CENTER', 'CENTER'
                )
                """.trimIndent(),
            )
        }

        helper.runMigrationsAndValidate(
            testDatabase,
            8,
            true,
            Migration7To8(),
        ).use { db ->

            // ============================
            // ApplicationInfoGridItemEntity
            // ============================
            db.query("SELECT * FROM ApplicationInfoGridItemEntity WHERE id = 'app_1'")
                .use { cursor ->
                    assertTrue(cursor.moveToFirst())
                    assertEquals(
                        "com.example.app",
                        cursor.getString(cursor.getColumnIndexOrThrow("packageName")),
                    )
                    assertEquals(
                        1002L,
                        cursor.getLong(cursor.getColumnIndexOrThrow("serialNumber")),
                    )
                    assertEquals(0, cursor.getInt(0))
                    assertEquals(0, cursor.getInt(1))
                }

            // ============================
            // ShortcutInfoGridItemEntity
            // ============================
            db.query("SELECT * FROM ShortcutInfoGridItemEntity WHERE id = 'shortcut_1'")
                .use { cursor ->
                    assertTrue(cursor.moveToFirst())
                    assertEquals(
                        "com.example",
                        cursor.getString(cursor.getColumnIndexOrThrow("packageName")),
                    )
                    assertEquals(0, cursor.getInt(0))
                }

            // ============================
            // FolderGridItemEntity
            // ============================
            db.query("SELECT * FROM FolderGridItemEntity WHERE id = 'folder_1'")
                .use { cursor ->
                    assertTrue(cursor.moveToFirst())
                    assertEquals(
                        "My Folder",
                        cursor.getString(cursor.getColumnIndexOrThrow("label")),
                    )
                    assertEquals(0, cursor.getInt(0))
                }

            // ============================
            // ShortcutConfigGridItemEntity
            // ============================
            db.query("SELECT * FROM ShortcutConfigGridItemEntity WHERE id = 'config_1'")
                .use { cursor ->
                    assertTrue(cursor.moveToFirst())
                    assertEquals(
                        "com.whatsapp",
                        cursor.getString(cursor.getColumnIndexOrThrow("packageName")),
                    )
                }

            db.query("SELECT cornerRadius FROM ShortcutConfigGridItemEntity WHERE id = 'config_1'")
                .use { cursor ->
                    assertTrue(cursor.moveToFirst())
                    assertEquals(0, cursor.getInt(0))
                }

            // ============================
            // WidgetGridItemEntity
            // ============================
            db.query("SELECT * FROM WidgetGridItemEntity WHERE id = 'widget_1'")
                .use { cursor ->
                    assertTrue(cursor.moveToFirst())
                    assertEquals("Clock", cursor.getString(cursor.getColumnIndexOrThrow("label")))
                    assertEquals(0, cursor.getInt(0))
                }
        }
    }
}
