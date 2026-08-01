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

import android.database.sqlite.SQLiteConstraintException
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.eblan.launcher.data.room.migration.Migration17To18
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class Migration17To18Test {
    private val testDatabase = "migration-test"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        EblanDatabase::class.java,
    )

    private fun insertFolder(db: SupportSQLiteDatabase, id: String, label: String) {
        db.execSQL(
            """
            INSERT INTO FolderGridItemEntity (
                id, page, startColumn, startRow, columnSpan, rowSpan,
                associate, label, `override`, icon,
                iconSize, textColor, textSize, showLabel, singleLineLabel,
                horizontalAlignment, verticalArrangement,
                customTextColor, customBackgroundColor, padding, cornerRadius,
                doubleTap_eblanActionType, doubleTap_serialNumber, doubleTap_componentName,
                swipeUp_eblanActionType, swipeUp_serialNumber, swipeUp_componentName,
                swipeDown_eblanActionType, swipeDown_serialNumber, swipeDown_componentName,
                `index`
            ) VALUES (
                '$id', 0, 0, 0, 1, 1,
                0, '$label', 0, NULL,
                48, 0, 12, 1, 1,
                1, 1, 0, 0, 0, 0,
                0, 1, '', 0, 2, '', 0, 3, '',
                0
            )
            """.trimIndent(),
        )
    }

    @Test
    @Throws(IOException::class)
    fun migrate17To18_folderGridItemEntity() {
        helper.createDatabase(testDatabase, 17).use { db ->
            insertFolder(db, "folder_id_1", "Parent Folder")
        }

        helper.runMigrationsAndValidate(testDatabase, 18, true, Migration17To18()).use { db ->
            db.setForeignKeyConstraintsEnabled(true)

            // fields survived, folderId defaults null
            db.query("SELECT * FROM FolderGridItemEntity WHERE id = 'folder_id_1'").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("folder_id_1", cursor.getString(cursor.getColumnIndexOrThrow("id")))
                assertEquals(0, cursor.getInt(cursor.getColumnIndexOrThrow("page")))
                assertEquals(0, cursor.getInt(cursor.getColumnIndexOrThrow("startColumn")))
                assertEquals(0, cursor.getInt(cursor.getColumnIndexOrThrow("startRow")))
                assertEquals(1, cursor.getInt(cursor.getColumnIndexOrThrow("columnSpan")))
                assertEquals(1, cursor.getInt(cursor.getColumnIndexOrThrow("rowSpan")))
                assertEquals(0, cursor.getInt(cursor.getColumnIndexOrThrow("associate")))
                assertEquals(
                    "Parent Folder",
                    cursor.getString(cursor.getColumnIndexOrThrow("label")),
                )
                assertEquals(0, cursor.getInt(cursor.getColumnIndexOrThrow("override")))
                assertTrue(cursor.isNull(cursor.getColumnIndexOrThrow("icon")))
                assertEquals(48, cursor.getInt(cursor.getColumnIndexOrThrow("iconSize")))
                assertEquals(0, cursor.getInt(cursor.getColumnIndexOrThrow("textColor")))
                assertEquals(12, cursor.getInt(cursor.getColumnIndexOrThrow("textSize")))
                assertEquals(1, cursor.getInt(cursor.getColumnIndexOrThrow("showLabel")))
                assertEquals(1, cursor.getInt(cursor.getColumnIndexOrThrow("singleLineLabel")))
                assertEquals(1, cursor.getInt(cursor.getColumnIndexOrThrow("horizontalAlignment")))
                assertEquals(1, cursor.getInt(cursor.getColumnIndexOrThrow("verticalArrangement")))
                assertEquals(0, cursor.getInt(cursor.getColumnIndexOrThrow("customTextColor")))
                assertEquals(
                    0,
                    cursor.getInt(cursor.getColumnIndexOrThrow("customBackgroundColor")),
                )
                assertEquals(0, cursor.getInt(cursor.getColumnIndexOrThrow("padding")))
                assertEquals(0, cursor.getInt(cursor.getColumnIndexOrThrow("cornerRadius")))
                assertEquals(
                    0,
                    cursor.getInt(cursor.getColumnIndexOrThrow("doubleTap_eblanActionType")),
                )
                assertEquals(
                    1L,
                    cursor.getLong(cursor.getColumnIndexOrThrow("doubleTap_serialNumber")),
                )
                assertEquals(
                    "",
                    cursor.getString(cursor.getColumnIndexOrThrow("doubleTap_componentName")),
                )
                assertEquals(
                    0,
                    cursor.getInt(cursor.getColumnIndexOrThrow("swipeUp_eblanActionType")),
                )
                assertEquals(
                    2L,
                    cursor.getLong(cursor.getColumnIndexOrThrow("swipeUp_serialNumber")),
                )
                assertEquals(
                    "",
                    cursor.getString(cursor.getColumnIndexOrThrow("swipeUp_componentName")),
                )
                assertEquals(
                    0,
                    cursor.getInt(cursor.getColumnIndexOrThrow("swipeDown_eblanActionType")),
                )
                assertEquals(
                    3L,
                    cursor.getLong(cursor.getColumnIndexOrThrow("swipeDown_serialNumber")),
                )
                assertEquals(
                    "",
                    cursor.getString(cursor.getColumnIndexOrThrow("swipeDown_componentName")),
                )
                assertEquals(0, cursor.getInt(cursor.getColumnIndexOrThrow("index")))
                assertTrue(cursor.isNull(cursor.getColumnIndexOrThrow("folderId")))
            }

            // valid self-reference accepted
            insertFolder(db, "folder_id_2", "Nested Folder")
            db.execSQL("UPDATE FolderGridItemEntity SET folderId = 'folder_id_1' WHERE id = 'folder_id_2'")
            db.query("SELECT folderId FROM FolderGridItemEntity WHERE id = 'folder_id_2'")
                .use { cursor ->
                    assertTrue(cursor.moveToFirst())
                    assertEquals(
                        "folder_id_1",
                        cursor.getString(cursor.getColumnIndexOrThrow("folderId")),
                    )
                }

            // invalid self-reference rejected
            try {
                db.execSQL("UPDATE FolderGridItemEntity SET folderId = 'does_not_exist' WHERE id = 'folder_id_2'")
                fail("Expected SQLiteConstraintException for invalid folderId")
            } catch (_: SQLiteConstraintException) {
            }

            // cascade delete removes child
            db.execSQL("DELETE FROM FolderGridItemEntity WHERE id = 'folder_id_1'")
            db.query("SELECT * FROM FolderGridItemEntity WHERE id = 'folder_id_2'").use { cursor ->
                assertFalse(cursor.moveToFirst())
            }
        }
    }

    @Test
    @Throws(IOException::class)
    fun migrate17To18_applicationInfoGridItemEntity() {
        helper.createDatabase(testDatabase, 17).use { db ->
            insertFolder(db, "folder_id_1", "Parent Folder")

            db.execSQL(
                """
                INSERT INTO ApplicationInfoGridItemEntity (
                    id, page, startColumn, startRow, columnSpan, rowSpan,
                    associate, componentName, packageName, icon, label,
                    `override`, serialNumber, customIcon, customLabel,
                    iconSize, textColor, textSize, showLabel, singleLineLabel,
                    horizontalAlignment, verticalArrangement,
                    customTextColor, customBackgroundColor, padding, cornerRadius,
                    doubleTap_eblanActionType, doubleTap_serialNumber, doubleTap_componentName,
                    swipeUp_eblanActionType, swipeUp_serialNumber, swipeUp_componentName,
                    swipeDown_eblanActionType, swipeDown_serialNumber, swipeDown_componentName,
                    `index`
                ) VALUES (
                    'app_id_1', 0, 0, 0, 1, 1,
                    0, 'com.example/.MainActivity', 'com.example', NULL, 'Example App',
                    0, 1, NULL, NULL,
                    48, 0, 12, 1, 1,
                    1, 1, 0, 0, 0, 0,
                    0, 1, '', 0, 2, '', 0, 3, '',
                    0
                )
                """.trimIndent(),
            )
        }

        helper.runMigrationsAndValidate(testDatabase, 18, true, Migration17To18()).use { db ->
            db.setForeignKeyConstraintsEnabled(true)

            db.query("SELECT * FROM ApplicationInfoGridItemEntity WHERE id = 'app_id_1'")
                .use { cursor ->
                    assertTrue(cursor.moveToFirst())
                    assertEquals("app_id_1", cursor.getString(cursor.getColumnIndexOrThrow("id")))
                    assertEquals(0, cursor.getInt(cursor.getColumnIndexOrThrow("page")))
                    assertEquals(0, cursor.getInt(cursor.getColumnIndexOrThrow("startColumn")))
                    assertEquals(0, cursor.getInt(cursor.getColumnIndexOrThrow("startRow")))
                    assertEquals(1, cursor.getInt(cursor.getColumnIndexOrThrow("columnSpan")))
                    assertEquals(1, cursor.getInt(cursor.getColumnIndexOrThrow("rowSpan")))
                    assertEquals(0, cursor.getInt(cursor.getColumnIndexOrThrow("associate")))
                    assertEquals(
                        "com.example/.MainActivity",
                        cursor.getString(cursor.getColumnIndexOrThrow("componentName")),
                    )
                    assertEquals(
                        "com.example",
                        cursor.getString(cursor.getColumnIndexOrThrow("packageName")),
                    )
                    assertTrue(cursor.isNull(cursor.getColumnIndexOrThrow("icon")))
                    assertEquals(
                        "Example App",
                        cursor.getString(cursor.getColumnIndexOrThrow("label")),
                    )
                    assertEquals(0, cursor.getInt(cursor.getColumnIndexOrThrow("override")))
                    assertEquals(1L, cursor.getLong(cursor.getColumnIndexOrThrow("serialNumber")))
                    assertTrue(cursor.isNull(cursor.getColumnIndexOrThrow("customIcon")))
                    assertTrue(cursor.isNull(cursor.getColumnIndexOrThrow("customLabel")))
                    assertEquals(48, cursor.getInt(cursor.getColumnIndexOrThrow("iconSize")))
                    assertEquals(0, cursor.getInt(cursor.getColumnIndexOrThrow("textColor")))
                    assertEquals(12, cursor.getInt(cursor.getColumnIndexOrThrow("textSize")))
                    assertEquals(1, cursor.getInt(cursor.getColumnIndexOrThrow("showLabel")))
                    assertEquals(1, cursor.getInt(cursor.getColumnIndexOrThrow("singleLineLabel")))
                    assertEquals(
                        1,
                        cursor.getInt(cursor.getColumnIndexOrThrow("horizontalAlignment")),
                    )
                    assertEquals(
                        1,
                        cursor.getInt(cursor.getColumnIndexOrThrow("verticalArrangement")),
                    )
                    assertEquals(0, cursor.getInt(cursor.getColumnIndexOrThrow("customTextColor")))
                    assertEquals(
                        0,
                        cursor.getInt(cursor.getColumnIndexOrThrow("customBackgroundColor")),
                    )
                    assertEquals(0, cursor.getInt(cursor.getColumnIndexOrThrow("padding")))
                    assertEquals(0, cursor.getInt(cursor.getColumnIndexOrThrow("cornerRadius")))
                    assertEquals(
                        0,
                        cursor.getInt(cursor.getColumnIndexOrThrow("doubleTap_eblanActionType")),
                    )
                    assertEquals(
                        1L,
                        cursor.getLong(cursor.getColumnIndexOrThrow("doubleTap_serialNumber")),
                    )
                    assertEquals(
                        "",
                        cursor.getString(cursor.getColumnIndexOrThrow("doubleTap_componentName")),
                    )
                    assertEquals(
                        0,
                        cursor.getInt(cursor.getColumnIndexOrThrow("swipeUp_eblanActionType")),
                    )
                    assertEquals(
                        2L,
                        cursor.getLong(cursor.getColumnIndexOrThrow("swipeUp_serialNumber")),
                    )
                    assertEquals(
                        "",
                        cursor.getString(cursor.getColumnIndexOrThrow("swipeUp_componentName")),
                    )
                    assertEquals(
                        0,
                        cursor.getInt(cursor.getColumnIndexOrThrow("swipeDown_eblanActionType")),
                    )
                    assertEquals(
                        3L,
                        cursor.getLong(cursor.getColumnIndexOrThrow("swipeDown_serialNumber")),
                    )
                    assertEquals(
                        "",
                        cursor.getString(cursor.getColumnIndexOrThrow("swipeDown_componentName")),
                    )
                    assertEquals(0, cursor.getInt(cursor.getColumnIndexOrThrow("index")))
                    assertTrue(cursor.isNull(cursor.getColumnIndexOrThrow("folderId")))
                }

            // valid FK accepted
            db.execSQL("UPDATE ApplicationInfoGridItemEntity SET folderId = 'folder_id_1' WHERE id = 'app_id_1'")
            db.query("SELECT folderId FROM ApplicationInfoGridItemEntity WHERE id = 'app_id_1'")
                .use { cursor ->
                    assertTrue(cursor.moveToFirst())
                    assertEquals(
                        "folder_id_1",
                        cursor.getString(cursor.getColumnIndexOrThrow("folderId")),
                    )
                }

            // invalid FK rejected
            try {
                db.execSQL(
                    "UPDATE ApplicationInfoGridItemEntity SET folderId = 'does_not_exist' WHERE id = 'app_id_1'",
                )
                fail("Expected SQLiteConstraintException for invalid folderId")
            } catch (_: SQLiteConstraintException) {
            }

            // cascade delete removes child
            db.execSQL("DELETE FROM FolderGridItemEntity WHERE id = 'folder_id_1'")
            db.query("SELECT * FROM ApplicationInfoGridItemEntity WHERE id = 'app_id_1'")
                .use { cursor ->
                    assertFalse(cursor.moveToFirst())
                }
        }
    }

    @Test
    @Throws(IOException::class)
    fun migrate17To18_shortcutInfoGridItemEntity() {
        helper.createDatabase(testDatabase, 17).use { db ->
            insertFolder(db, "folder_id_1", "Parent Folder")

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
                    swipeDown_eblanActionType, swipeDown_serialNumber, swipeDown_componentName,
                    `index`
                ) VALUES (
                    'info_id_1', 0, 0, 0, 1, 1,
                    0, 'shortcut_1', 'pkg', 'short', 'long',
                    NULL, 0, 1, 1,
                    NULL, NULL, NULL,
                    48, 0, 12, 1, 1,
                    1, 1, 0, 0, 0, 0,
                    0, 1, '', 0, 2, '', 0, 3, '',
                    0
                )
                """.trimIndent(),
            )
        }

        helper.runMigrationsAndValidate(testDatabase, 18, true, Migration17To18()).use { db ->
            db.setForeignKeyConstraintsEnabled(true)

            db.query("SELECT * FROM ShortcutInfoGridItemEntity WHERE id = 'info_id_1'")
                .use { cursor ->
                    assertTrue(cursor.moveToFirst())
                    assertEquals("info_id_1", cursor.getString(cursor.getColumnIndexOrThrow("id")))
                    assertEquals(0, cursor.getInt(cursor.getColumnIndexOrThrow("page")))
                    assertEquals(0, cursor.getInt(cursor.getColumnIndexOrThrow("startColumn")))
                    assertEquals(0, cursor.getInt(cursor.getColumnIndexOrThrow("startRow")))
                    assertEquals(1, cursor.getInt(cursor.getColumnIndexOrThrow("columnSpan")))
                    assertEquals(1, cursor.getInt(cursor.getColumnIndexOrThrow("rowSpan")))
                    assertEquals(0, cursor.getInt(cursor.getColumnIndexOrThrow("associate")))
                    assertEquals(
                        "shortcut_1",
                        cursor.getString(cursor.getColumnIndexOrThrow("shortcutId")),
                    )
                    assertEquals(
                        "pkg",
                        cursor.getString(cursor.getColumnIndexOrThrow("packageName")),
                    )
                    assertEquals(
                        "short",
                        cursor.getString(cursor.getColumnIndexOrThrow("shortLabel")),
                    )
                    assertEquals(
                        "long",
                        cursor.getString(cursor.getColumnIndexOrThrow("longLabel")),
                    )
                    assertTrue(cursor.isNull(cursor.getColumnIndexOrThrow("icon")))
                    assertEquals(0, cursor.getInt(cursor.getColumnIndexOrThrow("override")))
                    assertEquals(1L, cursor.getLong(cursor.getColumnIndexOrThrow("serialNumber")))
                    assertEquals(1, cursor.getInt(cursor.getColumnIndexOrThrow("isEnabled")))
                    assertTrue(cursor.isNull(cursor.getColumnIndexOrThrow("eblanApplicationInfoIcon")))
                    assertTrue(cursor.isNull(cursor.getColumnIndexOrThrow("customIcon")))
                    assertTrue(cursor.isNull(cursor.getColumnIndexOrThrow("customShortLabel")))
                    assertEquals(48, cursor.getInt(cursor.getColumnIndexOrThrow("iconSize")))
                    assertEquals(0, cursor.getInt(cursor.getColumnIndexOrThrow("textColor")))
                    assertEquals(12, cursor.getInt(cursor.getColumnIndexOrThrow("textSize")))
                    assertEquals(1, cursor.getInt(cursor.getColumnIndexOrThrow("showLabel")))
                    assertEquals(1, cursor.getInt(cursor.getColumnIndexOrThrow("singleLineLabel")))
                    assertEquals(
                        1,
                        cursor.getInt(cursor.getColumnIndexOrThrow("horizontalAlignment")),
                    )
                    assertEquals(
                        1,
                        cursor.getInt(cursor.getColumnIndexOrThrow("verticalArrangement")),
                    )
                    assertEquals(0, cursor.getInt(cursor.getColumnIndexOrThrow("customTextColor")))
                    assertEquals(
                        0,
                        cursor.getInt(cursor.getColumnIndexOrThrow("customBackgroundColor")),
                    )
                    assertEquals(0, cursor.getInt(cursor.getColumnIndexOrThrow("padding")))
                    assertEquals(0, cursor.getInt(cursor.getColumnIndexOrThrow("cornerRadius")))
                    assertEquals(
                        0,
                        cursor.getInt(cursor.getColumnIndexOrThrow("doubleTap_eblanActionType")),
                    )
                    assertEquals(
                        1L,
                        cursor.getLong(cursor.getColumnIndexOrThrow("doubleTap_serialNumber")),
                    )
                    assertEquals(
                        "",
                        cursor.getString(cursor.getColumnIndexOrThrow("doubleTap_componentName")),
                    )
                    assertEquals(
                        0,
                        cursor.getInt(cursor.getColumnIndexOrThrow("swipeUp_eblanActionType")),
                    )
                    assertEquals(
                        2L,
                        cursor.getLong(cursor.getColumnIndexOrThrow("swipeUp_serialNumber")),
                    )
                    assertEquals(
                        "",
                        cursor.getString(cursor.getColumnIndexOrThrow("swipeUp_componentName")),
                    )
                    assertEquals(
                        0,
                        cursor.getInt(cursor.getColumnIndexOrThrow("swipeDown_eblanActionType")),
                    )
                    assertEquals(
                        3L,
                        cursor.getLong(cursor.getColumnIndexOrThrow("swipeDown_serialNumber")),
                    )
                    assertEquals(
                        "",
                        cursor.getString(cursor.getColumnIndexOrThrow("swipeDown_componentName")),
                    )
                    assertEquals(0, cursor.getInt(cursor.getColumnIndexOrThrow("index")))
                    assertTrue(cursor.isNull(cursor.getColumnIndexOrThrow("folderId")))
                }

            // valid FK accepted
            db.execSQL("UPDATE ShortcutInfoGridItemEntity SET folderId = 'folder_id_1' WHERE id = 'info_id_1'")
            db.query("SELECT folderId FROM ShortcutInfoGridItemEntity WHERE id = 'info_id_1'")
                .use { cursor ->
                    assertTrue(cursor.moveToFirst())
                    assertEquals(
                        "folder_id_1",
                        cursor.getString(cursor.getColumnIndexOrThrow("folderId")),
                    )
                }

            // invalid FK rejected
            try {
                db.execSQL(
                    "UPDATE ShortcutInfoGridItemEntity SET folderId = 'does_not_exist' WHERE id = 'info_id_1'",
                )
                fail("Expected SQLiteConstraintException for invalid folderId")
            } catch (_: SQLiteConstraintException) {
            }

            // cascade delete removes child
            db.execSQL("DELETE FROM FolderGridItemEntity WHERE id = 'folder_id_1'")
            db.query("SELECT * FROM ShortcutInfoGridItemEntity WHERE id = 'info_id_1'")
                .use { cursor ->
                    assertFalse(cursor.moveToFirst())
                }
        }
    }

    @Test
    @Throws(IOException::class)
    fun migrate17To18_shortcutConfigGridItemEntity() {
        helper.createDatabase(testDatabase, 17).use { db ->
            insertFolder(db, "folder_id_1", "Parent Folder")

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
                    swipeDown_eblanActionType, swipeDown_serialNumber, swipeDown_componentName,
                    `index`
                ) VALUES (
                    'config_id_1', 0, 0, 0, 1, 1,
                    0, 'component_1', 'pkg',
                    NULL, 'activity_label',
                    NULL, 'app_label',
                    0, 1,
                    NULL, NULL, NULL,
                    NULL, NULL,
                    48, 0, 12, 1, 1,
                    1, 1, 0, 0, 0, 0,
                    0, 1, '', 0, 2, '', 0, 3, '',
                    0
                )
                """.trimIndent(),
            )
        }

        helper.runMigrationsAndValidate(testDatabase, 18, true, Migration17To18()).use { db ->
            db.setForeignKeyConstraintsEnabled(true)

            db.query("SELECT * FROM ShortcutConfigGridItemEntity WHERE id = 'config_id_1'")
                .use { cursor ->
                    assertTrue(cursor.moveToFirst())
                    assertEquals(
                        "config_id_1",
                        cursor.getString(cursor.getColumnIndexOrThrow("id")),
                    )
                    assertEquals(0, cursor.getInt(cursor.getColumnIndexOrThrow("page")))
                    assertEquals(0, cursor.getInt(cursor.getColumnIndexOrThrow("startColumn")))
                    assertEquals(0, cursor.getInt(cursor.getColumnIndexOrThrow("startRow")))
                    assertEquals(1, cursor.getInt(cursor.getColumnIndexOrThrow("columnSpan")))
                    assertEquals(1, cursor.getInt(cursor.getColumnIndexOrThrow("rowSpan")))
                    assertEquals(0, cursor.getInt(cursor.getColumnIndexOrThrow("associate")))
                    assertEquals(
                        "component_1",
                        cursor.getString(cursor.getColumnIndexOrThrow("componentName")),
                    )
                    assertEquals(
                        "pkg",
                        cursor.getString(cursor.getColumnIndexOrThrow("packageName")),
                    )
                    assertTrue(cursor.isNull(cursor.getColumnIndexOrThrow("activityIcon")))
                    assertEquals(
                        "activity_label",
                        cursor.getString(cursor.getColumnIndexOrThrow("activityLabel")),
                    )
                    assertTrue(cursor.isNull(cursor.getColumnIndexOrThrow("applicationIcon")))
                    assertEquals(
                        "app_label",
                        cursor.getString(cursor.getColumnIndexOrThrow("applicationLabel")),
                    )
                    assertEquals(0, cursor.getInt(cursor.getColumnIndexOrThrow("override")))
                    assertEquals(1L, cursor.getLong(cursor.getColumnIndexOrThrow("serialNumber")))
                    assertTrue(cursor.isNull(cursor.getColumnIndexOrThrow("shortcutIntentName")))
                    assertTrue(cursor.isNull(cursor.getColumnIndexOrThrow("shortcutIntentIcon")))
                    assertTrue(cursor.isNull(cursor.getColumnIndexOrThrow("shortcutIntentUri")))
                    assertTrue(cursor.isNull(cursor.getColumnIndexOrThrow("customIcon")))
                    assertTrue(cursor.isNull(cursor.getColumnIndexOrThrow("customLabel")))
                    assertEquals(48, cursor.getInt(cursor.getColumnIndexOrThrow("iconSize")))
                    assertEquals(0, cursor.getInt(cursor.getColumnIndexOrThrow("textColor")))
                    assertEquals(12, cursor.getInt(cursor.getColumnIndexOrThrow("textSize")))
                    assertEquals(1, cursor.getInt(cursor.getColumnIndexOrThrow("showLabel")))
                    assertEquals(1, cursor.getInt(cursor.getColumnIndexOrThrow("singleLineLabel")))
                    assertEquals(
                        1,
                        cursor.getInt(cursor.getColumnIndexOrThrow("horizontalAlignment")),
                    )
                    assertEquals(
                        1,
                        cursor.getInt(cursor.getColumnIndexOrThrow("verticalArrangement")),
                    )
                    assertEquals(0, cursor.getInt(cursor.getColumnIndexOrThrow("customTextColor")))
                    assertEquals(
                        0,
                        cursor.getInt(cursor.getColumnIndexOrThrow("customBackgroundColor")),
                    )
                    assertEquals(0, cursor.getInt(cursor.getColumnIndexOrThrow("padding")))
                    assertEquals(0, cursor.getInt(cursor.getColumnIndexOrThrow("cornerRadius")))
                    assertEquals(
                        0,
                        cursor.getInt(cursor.getColumnIndexOrThrow("doubleTap_eblanActionType")),
                    )
                    assertEquals(
                        1L,
                        cursor.getLong(cursor.getColumnIndexOrThrow("doubleTap_serialNumber")),
                    )
                    assertEquals(
                        "",
                        cursor.getString(cursor.getColumnIndexOrThrow("doubleTap_componentName")),
                    )
                    assertEquals(
                        0,
                        cursor.getInt(cursor.getColumnIndexOrThrow("swipeUp_eblanActionType")),
                    )
                    assertEquals(
                        2L,
                        cursor.getLong(cursor.getColumnIndexOrThrow("swipeUp_serialNumber")),
                    )
                    assertEquals(
                        "",
                        cursor.getString(cursor.getColumnIndexOrThrow("swipeUp_componentName")),
                    )
                    assertEquals(
                        0,
                        cursor.getInt(cursor.getColumnIndexOrThrow("swipeDown_eblanActionType")),
                    )
                    assertEquals(
                        3L,
                        cursor.getLong(cursor.getColumnIndexOrThrow("swipeDown_serialNumber")),
                    )
                    assertEquals(
                        "",
                        cursor.getString(cursor.getColumnIndexOrThrow("swipeDown_componentName")),
                    )
                    assertEquals(0, cursor.getInt(cursor.getColumnIndexOrThrow("index")))
                    assertTrue(cursor.isNull(cursor.getColumnIndexOrThrow("folderId")))
                }

            // valid FK accepted
            db.execSQL("UPDATE ShortcutConfigGridItemEntity SET folderId = 'folder_id_1' WHERE id = 'config_id_1'")
            db.query("SELECT folderId FROM ShortcutConfigGridItemEntity WHERE id = 'config_id_1'")
                .use { cursor ->
                    assertTrue(cursor.moveToFirst())
                    assertEquals(
                        "folder_id_1",
                        cursor.getString(cursor.getColumnIndexOrThrow("folderId")),
                    )
                }

            // invalid FK rejected
            try {
                db.execSQL(
                    "UPDATE ShortcutConfigGridItemEntity SET folderId = 'does_not_exist' WHERE id = 'config_id_1'",
                )
                fail("Expected SQLiteConstraintException for invalid folderId")
            } catch (_: SQLiteConstraintException) {
            }

            // cascade delete removes child
            db.execSQL("DELETE FROM FolderGridItemEntity WHERE id = 'folder_id_1'")
            db.query("SELECT * FROM ShortcutConfigGridItemEntity WHERE id = 'config_id_1'")
                .use { cursor ->
                    assertFalse(cursor.moveToFirst())
                }
        }
    }
}
