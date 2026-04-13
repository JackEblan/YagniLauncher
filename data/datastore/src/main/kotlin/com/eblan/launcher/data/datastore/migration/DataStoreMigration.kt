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
package com.eblan.launcher.data.datastore.migration

import androidx.datastore.core.DataMigration
import com.eblan.launcher.data.datastore.proto.UserDataProto
import com.eblan.launcher.data.datastore.proto.copy

internal class DataStoreMigration : DataMigration<UserDataProto> {
    override suspend fun shouldMigrate(currentData: UserDataProto): Boolean = currentData.homeSettingsProto.dockPageCount == 0 ||
        currentData.appDrawerSettingsProto.horizontalAppDrawerColumns == 0 ||
        currentData.appDrawerSettingsProto.horizontalAppDrawerRows == 0

    override suspend fun migrate(currentData: UserDataProto): UserDataProto = currentData.copy {
        homeSettingsProto = homeSettingsProto.toBuilder()
            .setDockPageCount(1)
            .build()

        appDrawerSettingsProto = appDrawerSettingsProto.toBuilder()
            .setHorizontalAppDrawerColumns(5)
            .setHorizontalAppDrawerRows(5)
            .build()
    }

    override suspend fun cleanUp() {}
}
