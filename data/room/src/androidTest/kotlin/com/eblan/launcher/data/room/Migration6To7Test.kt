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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class Migration6To7Test {

    private val testDatabase = "migration-test"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        EblanDatabase::class.java,
    )

    @Test
    @Throws(IOException::class)
    fun migrate6To7() {
        // Create database at version 6
        helper.createDatabase(testDatabase, 6).use { db ->
            db.execSQL(
                """
                INSERT INTO EblanApplicationInfoEntity (
                    componentName, serialNumber, packageName, icon, label,
                    customIcon, customLabel
                ) VALUES (
                    'com.example/.Main', 1001, 'com.example', 'ic_launcher', 'Example',
                    'custom_ic', 'My Example App'
                )
                """.trimIndent(),
            )

            db.execSQL(
                """
                INSERT INTO EblanAppWidgetProviderInfoEntity (
                    componentName, serialNumber, packageName,
                    targetCellWidth, targetCellHeight, minWidth, minHeight,
                    resizeMode, minResizeWidth, minResizeHeight,
                    maxResizeWidth, maxResizeHeight, label
                ) VALUES (
                    'com.example/.widget', 0, 'com.example',
                    4, 2, 180, 110, 15, 180, 110, 400, 300, 'Clock'
                )
                """.trimIndent(),
            )

            db.execSQL(
                """
                INSERT INTO EblanShortcutConfigEntity (
                    componentName, packageName, serialNumber,
                    activityIcon, activityLabel, applicationIcon, applicationLabel
                ) VALUES (
                    'com.example/.ShortcutActivity', 'com.example', 1002,
                    'shortcut_ic', 'Open Settings', 'app_ic', 'Example'
                )
                """.trimIndent(),
            )

            db.execSQL(
                """
                INSERT INTO EblanShortcutInfoEntity (
                    shortcutId, serialNumber, packageName,
                    shortLabel, longLabel, icon,
                    shortcutQueryFlag, isEnabled
                ) VALUES (
                    'pin-settings', 1003, 'com.example',
                    'Settings', 'Open device settings', 'ic_settings',
                    'pinned', 1
                )
                """.trimIndent(),
            )
        }

        // Run migration → validate version 7
        helper.runMigrationsAndValidate(testDatabase, 7, true).use { db ->
            // EblanApplicationInfoEntity
            db.query(
                """
                SELECT componentName, serialNumber, packageName, icon, label,
                       customIcon, customLabel, isHidden, lastUpdateTime
                FROM EblanApplicationInfoEntity
                """.trimIndent(),
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())

                assertEquals(
                    "com.example/.Main",
                    cursor.getString(cursor.getColumnIndexOrThrow("componentName")),
                )
                assertEquals(1001L, cursor.getLong(cursor.getColumnIndexOrThrow("serialNumber")))
                assertEquals(
                    "com.example",
                    cursor.getString(cursor.getColumnIndexOrThrow("packageName")),
                )
                assertEquals("ic_launcher", cursor.getString(cursor.getColumnIndexOrThrow("icon")))
                assertEquals("Example", cursor.getString(cursor.getColumnIndexOrThrow("label")))
                assertEquals(
                    "custom_ic",
                    cursor.getString(cursor.getColumnIndexOrThrow("customIcon")),
                )
                assertEquals(
                    "My Example App",
                    cursor.getString(cursor.getColumnIndexOrThrow("customLabel")),
                )
                assertEquals(0, cursor.getInt(cursor.getColumnIndexOrThrow("isHidden")))
                assertEquals(0L, cursor.getLong(cursor.getColumnIndexOrThrow("lastUpdateTime")))
            }

            // EblanAppWidgetProviderInfoEntity
            db.query(
                """
                SELECT componentName, serialNumber, packageName,
                       targetCellWidth, targetCellHeight, minWidth, minHeight,
                       resizeMode, minResizeWidth, minResizeHeight,
                       maxResizeWidth, maxResizeHeight, label, lastUpdateTime
                FROM EblanAppWidgetProviderInfoEntity
                """.trimIndent(),
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())

                assertEquals(
                    "com.example/.widget",
                    cursor.getString(cursor.getColumnIndexOrThrow("componentName")),
                )
                assertEquals(0L, cursor.getLong(cursor.getColumnIndexOrThrow("serialNumber")))
                assertEquals(
                    "com.example",
                    cursor.getString(cursor.getColumnIndexOrThrow("packageName")),
                )
                assertEquals(4, cursor.getInt(cursor.getColumnIndexOrThrow("targetCellWidth")))
                assertEquals(2, cursor.getInt(cursor.getColumnIndexOrThrow("targetCellHeight")))
                assertEquals(180, cursor.getInt(cursor.getColumnIndexOrThrow("minWidth")))
                assertEquals(110, cursor.getInt(cursor.getColumnIndexOrThrow("minHeight")))
                assertEquals(15, cursor.getInt(cursor.getColumnIndexOrThrow("resizeMode")))
                assertEquals(180, cursor.getInt(cursor.getColumnIndexOrThrow("minResizeWidth")))
                assertEquals(110, cursor.getInt(cursor.getColumnIndexOrThrow("minResizeHeight")))
                assertEquals(400, cursor.getInt(cursor.getColumnIndexOrThrow("maxResizeWidth")))
                assertEquals(300, cursor.getInt(cursor.getColumnIndexOrThrow("maxResizeHeight")))
                assertEquals("Clock", cursor.getString(cursor.getColumnIndexOrThrow("label")))
                assertEquals(0L, cursor.getLong(cursor.getColumnIndexOrThrow("lastUpdateTime")))
            }

            // EblanShortcutConfigEntity
            db.query(
                """
                SELECT componentName, packageName, serialNumber,
                       activityIcon, activityLabel, applicationIcon, applicationLabel,
                       lastUpdateTime
                FROM EblanShortcutConfigEntity
                """.trimIndent(),
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())

                assertEquals(
                    "com.example/.ShortcutActivity",
                    cursor.getString(cursor.getColumnIndexOrThrow("componentName")),
                )
                assertEquals(
                    "com.example",
                    cursor.getString(cursor.getColumnIndexOrThrow("packageName")),
                )
                assertEquals(1002L, cursor.getLong(cursor.getColumnIndexOrThrow("serialNumber")))
                assertEquals(
                    "shortcut_ic",
                    cursor.getString(cursor.getColumnIndexOrThrow("activityIcon")),
                )
                assertEquals(
                    "Open Settings",
                    cursor.getString(cursor.getColumnIndexOrThrow("activityLabel")),
                )
                assertEquals(
                    "app_ic",
                    cursor.getString(cursor.getColumnIndexOrThrow("applicationIcon")),
                )
                assertEquals(
                    "Example",
                    cursor.getString(cursor.getColumnIndexOrThrow("applicationLabel")),
                )
                assertEquals(0L, cursor.getLong(cursor.getColumnIndexOrThrow("lastUpdateTime")))
            }

            // EblanShortcutInfoEntity
            db.query(
                """
                SELECT shortcutId, serialNumber, packageName,
                       shortLabel, longLabel, icon,
                       shortcutQueryFlag, isEnabled, lastUpdateTime
                FROM EblanShortcutInfoEntity
                """.trimIndent(),
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())

                assertEquals(
                    "pin-settings",
                    cursor.getString(cursor.getColumnIndexOrThrow("shortcutId")),
                )
                assertEquals(1003L, cursor.getLong(cursor.getColumnIndexOrThrow("serialNumber")))
                assertEquals(
                    "com.example",
                    cursor.getString(cursor.getColumnIndexOrThrow("packageName")),
                )
                assertEquals(
                    "Settings",
                    cursor.getString(cursor.getColumnIndexOrThrow("shortLabel")),
                )
                assertEquals(
                    "Open device settings",
                    cursor.getString(cursor.getColumnIndexOrThrow("longLabel")),
                )
                assertEquals("ic_settings", cursor.getString(cursor.getColumnIndexOrThrow("icon")))
                assertEquals(
                    "pinned",
                    cursor.getString(cursor.getColumnIndexOrThrow("shortcutQueryFlag")),
                )
                assertEquals(1, cursor.getInt(cursor.getColumnIndexOrThrow("isEnabled")))
                assertEquals(0L, cursor.getLong(cursor.getColumnIndexOrThrow("lastUpdateTime")))
            }
        }
    }
}
