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
import com.eblan.launcher.data.room.migration.Migration14To15
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class Migration14To15Test {

    private val testDatabase = "migration-test"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        EblanDatabase::class.java,
    )

    @Test
    @Throws(IOException::class)
    fun migrate14To15() {
        helper.createDatabase(testDatabase, 14).use { db ->

            // ---------------------------
            // ShortcutInfoGridItemEntity
            // ---------------------------
            db.execSQL(
                """
                INSERT INTO ShortcutInfoGridItemEntity (
                    id, page, startColumn, startRow, columnSpan, rowSpan,
                    associate, shortcutId, packageName, shortLabel, longLabel,
                    icon, `override`, serialNumber, isEnabled,
                    eblanApplicationInfoIcon, customIcon, customShortLabel,

                    iconSize, textColor, textSize, showLabel, singleLineLabel,
                    horizontalAlignment, verticalArrangement,
                    customTextColor, customBackgroundColor, padding, cornerRadius,

                    doubleTap_eblanActionType, doubleTap_serialNumber, doubleTap_componentName,
                    swipeUp_eblanActionType, swipeUp_serialNumber, swipeUp_componentName,
                    swipeDown_eblanActionType, swipeDown_serialNumber, swipeDown_componentName
                ) VALUES (
                    'info_id_1', 0, 0, 0, 1, 1,
                    0, 'shortcut_1', 'pkg', 'short', 'long',
                    NULL, 0, 1, 1,
                    NULL, NULL, NULL,

                    48, 0, 12, 1, 1,
                    1, 1,
                    0, 0, 0, 0,

                    0, 1, '',
                    0, 2, '',
                    0, 3, ''
                )
                """.trimIndent(),
            )

            // ---------------------------
            // ShortcutConfigGridItemEntity
            // ---------------------------
            db.execSQL(
                """
                INSERT INTO ShortcutConfigGridItemEntity (
                    id, page, startColumn, startRow, columnSpan, rowSpan,
                    associate, componentName, packageName,
                    activityIcon, activityLabel,
                    applicationIcon, applicationLabel,
                    `override`, serialNumber,
                    shortcutIntentName, shortcutIntentIcon, shortcutIntentUri,
                    customIcon, customLabel,

                    iconSize, textColor, textSize, showLabel, singleLineLabel,
                    horizontalAlignment, verticalArrangement,
                    customTextColor, customBackgroundColor, padding, cornerRadius,

                    doubleTap_eblanActionType, doubleTap_serialNumber, doubleTap_componentName,
                    swipeUp_eblanActionType, swipeUp_serialNumber, swipeUp_componentName,
                    swipeDown_eblanActionType, swipeDown_serialNumber, swipeDown_componentName
                ) VALUES (
                    'config_id_1', 0, 0, 0, 1, 1,
                    0, 'component_1', 'pkg',
                    NULL, 'activity_label',
                    NULL, 'app_label',
                    0, 1,
                    NULL, NULL, NULL,
                    NULL, NULL,

                    48, 0, 12, 1, 1,
                    1, 1,
                    0, 0, 0, 0,

                    0, 1, '',
                    0, 2, '',
                    0, 3, ''
                )
                """.trimIndent(),
            )
        }

        helper.runMigrationsAndValidate(
            testDatabase,
            15,
            true,
            Migration14To15(),
        ).use { db ->

            // ---------------------------
            // Verify ShortcutInfo survives
            // ---------------------------
            db.query("SELECT * FROM ShortcutInfoGridItemEntity WHERE id = 'info_id_1'")
                .use { cursor ->
                    assertTrue(cursor.moveToFirst())
                    assertEquals(
                        "pkg",
                        cursor.getString(cursor.getColumnIndexOrThrow("packageName")),
                    )
                    assertEquals(
                        "shortcut_1",
                        cursor.getString(cursor.getColumnIndexOrThrow("shortcutId")),
                    )
                }

            db.query("SELECT `index`, folderId FROM ShortcutInfoGridItemEntity WHERE id = 'info_id_1'")
                .use { cursor ->
                    assertTrue(cursor.moveToFirst())
                    assertEquals(0, cursor.getInt(cursor.getColumnIndexOrThrow("index")))
                    assertTrue(cursor.isNull(cursor.getColumnIndexOrThrow("folderId")))
                }

            // ---------------------------
            // Verify ShortcutConfig survives
            // ---------------------------
            db.query("SELECT * FROM ShortcutConfigGridItemEntity WHERE id = 'config_id_1'")
                .use { cursor ->
                    assertTrue(cursor.moveToFirst())
                    assertEquals(
                        "pkg",
                        cursor.getString(cursor.getColumnIndexOrThrow("packageName")),
                    )
                    assertEquals(
                        "component_1",
                        cursor.getString(cursor.getColumnIndexOrThrow("componentName")),
                    )
                }

            db.query("SELECT `index`, folderId FROM ShortcutConfigGridItemEntity WHERE id = 'config_id_1'")
                .use { cursor ->
                    assertTrue(cursor.moveToFirst())
                    assertEquals(0, cursor.getInt(cursor.getColumnIndexOrThrow("index")))
                    assertTrue(cursor.isNull(cursor.getColumnIndexOrThrow("folderId")))
                }
        }
    }
}
