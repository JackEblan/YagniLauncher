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
import com.eblan.launcher.data.room.migration.Migration12To13
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class Migration12To13Test {

    private val testDatabase = "migration-test"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        EblanDatabase::class.java,
    )

    @Test
    @Throws(IOException::class)
    fun migrate12To13() {
        // Create database at version 12
        helper.createDatabase(testDatabase, 12).use { db ->
            db.execSQL(
                """
        INSERT INTO EblanShortcutInfoEntity (
            shortcutId, 
            serialNumber, 
            packageName, 
            shortLabel, 
            longLabel, 
            icon, 
            shortcutQueryFlag, 
            isEnabled, 
            lastUpdateTime
        ) VALUES (
            'id_1', 
            101, 
            'com.example.app', 
            'Label', 
            'Long Label', 
            null, 
            'Pinned', 
            1, 
            123456789
        )
                """.trimIndent(),
            )
        }

        // Run migration → validate version 13
        helper.runMigrationsAndValidate(
            testDatabase,
            13,
            true,
            Migration12To13(),
        ).use { db ->
            db.query("SELECT * FROM EblanShortcutInfoEntity").use { cursor ->
                assertTrue(cursor.moveToFirst())

                assertEquals("id_1", cursor.getString(cursor.getColumnIndexOrThrow("shortcutId")))
                assertEquals(
                    "com.example.app",
                    cursor.getString(cursor.getColumnIndexOrThrow("packageName")),
                )
                assertEquals(101L, cursor.getLong(cursor.getColumnIndexOrThrow("serialNumber")))
                assertEquals(
                    123456789L,
                    cursor.getLong(cursor.getColumnIndexOrThrow("lastChangedTimestamp")),
                )
            }
        }
    }
}
