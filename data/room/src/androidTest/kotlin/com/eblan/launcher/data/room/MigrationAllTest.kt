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

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.eblan.launcher.data.room.migration.Migration12To13
import com.eblan.launcher.data.room.migration.Migration13To14
import com.eblan.launcher.data.room.migration.Migration14To15
import com.eblan.launcher.data.room.migration.Migration3To4
import com.eblan.launcher.data.room.migration.Migration7To8
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class MigrationAllTest {
    private val testDatabase = "migration-test"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        EblanDatabase::class.java,
    )

    @Test
    @Throws(IOException::class)
    fun migrateAll() {
        helper.createDatabase(testDatabase, 1).apply {
            close()
        }

        Room.databaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            EblanDatabase::class.java,
            testDatabase,
        ).addMigrations(
            Migration3To4(),
            Migration7To8(),
            Migration12To13(),
            Migration13To14(),
            Migration14To15(),
        ).fallbackToDestructiveMigrationFrom(
            dropAllTables = true,
            1,
            2,
            11,
        ).build().apply {
            openHelper.writableDatabase.close()
        }
    }
}
