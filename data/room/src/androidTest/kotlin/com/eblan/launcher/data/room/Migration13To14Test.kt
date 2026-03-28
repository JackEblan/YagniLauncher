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
import com.eblan.launcher.data.room.migration.Migration13To14
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class Migration13To14Test {

    private val testDatabase = "migration-test"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        EblanDatabase::class.java,
    )

    @Test
    @Throws(IOException::class)
    fun migrate13To14() {
        helper.createDatabase(testDatabase, 13).apply {
            execSQL(
                """
                INSERT INTO EblanApplicationInfoTagEntity (id, name)
                VALUES (1, 'Work')
                """.trimIndent(),
            )

            close()
        }

        helper.runMigrationsAndValidate(
            testDatabase,
            14,
            true,
            Migration13To14(),
        ).use { db ->

            // 1. Verify existing data still exists
            val cursor = db.query("SELECT * FROM EblanApplicationInfoTagEntity")

            assert(cursor.moveToFirst())
            assert(cursor.getLong(cursor.getColumnIndex("id")) == 1L)
            assert(cursor.getString(cursor.getColumnIndex("name")) == "Work")

            cursor.close()

            // 2. Verify duplicates are now allowed
            db.execSQL(
                """
                INSERT INTO EblanApplicationInfoTagEntity (name)
                VALUES ('Work')
                """.trimIndent(),
            )

            val duplicateCursor = db.query(
                "SELECT COUNT(*) FROM EblanApplicationInfoTagEntity WHERE name = 'Work'",
            )

            assert(duplicateCursor.moveToFirst())
            assert(duplicateCursor.getInt(0) == 2)

            duplicateCursor.close()
        }
    }
}
