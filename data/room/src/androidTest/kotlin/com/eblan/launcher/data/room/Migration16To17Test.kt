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
import com.eblan.launcher.data.room.migration.Migration16To17
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class Migration16To17Test {
    private val testDatabase = "migration-test"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        EblanDatabase::class.java,
    )

    @Test
    @Throws(IOException::class)
    fun migrate16To17() {
        helper.createDatabase(testDatabase, 16).close()

        helper.runMigrationsAndValidate(
            testDatabase,
            17,
            true,
            Migration16To17(),
        ).use { db ->
            db.query(
                """
            SELECT name
            FROM sqlite_master
            WHERE type = 'index'
            AND name = 'index_ApplicationInfoGridItemEntity_folderId'
                """.trimIndent(),
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
            }

            db.query(
                """
            SELECT name
            FROM sqlite_master
            WHERE type = 'index'
            AND name = 'index_ShortcutInfoGridItemEntity_folderId'
                """.trimIndent(),
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
            }

            db.query(
                """
            SELECT name
            FROM sqlite_master
            WHERE type = 'index'
            AND name = 'index_ShortcutConfigGridItemEntity_folderId'
                """.trimIndent(),
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
            }

            db.query(
                """
            SELECT name
            FROM sqlite_master
            WHERE type = 'index'
            AND name = 'index_FolderGridItemEntity_folderId'
                """.trimIndent(),
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
            }
        }
    }
}
