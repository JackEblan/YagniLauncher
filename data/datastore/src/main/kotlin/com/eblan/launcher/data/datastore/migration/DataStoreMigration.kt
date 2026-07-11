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
import com.eblan.launcher.data.datastore.proto.home.GridItemLayoutTypeProto

internal class DataStoreMigration : DataMigration<UserDataProto> {
    override suspend fun shouldMigrate(currentData: UserDataProto): Boolean = currentData.homeSettingsProto.dockPageCount == 0 ||
        currentData.appDrawerSettingsProto.horizontalAppDrawerColumns == 0 ||
        currentData.appDrawerSettingsProto.horizontalAppDrawerRows == 0 ||
        currentData.homeSettingsProto.folderCellWidth == 0 ||
        currentData.homeSettingsProto.folderCellHeight == 0 ||
        currentData.homeSettingsProto.maxFolderColumns == 0 ||
        currentData.homeSettingsProto.maxFolderRows == 0 ||
        currentData.homeSettingsProto.gridItemSettingsProto.gridItemLayoutTypeProto == GridItemLayoutTypeProto.UNRECOGNIZED ||
        currentData.appDrawerSettingsProto.gridItemSettingsProto.gridItemLayoutTypeProto == GridItemLayoutTypeProto.UNRECOGNIZED

    override suspend fun migrate(currentData: UserDataProto): UserDataProto {
        var mutableData = currentData

        // Only migrate home settings if needed
        if (currentData.homeSettingsProto.dockPageCount == 0 ||
            currentData.homeSettingsProto.folderCellWidth == 0 ||
            currentData.homeSettingsProto.folderCellHeight == 0 ||
            currentData.homeSettingsProto.maxFolderColumns == 0 ||
            currentData.homeSettingsProto.maxFolderRows == 0 ||
            currentData.homeSettingsProto.gridItemSettingsProto.gridItemLayoutTypeProto == GridItemLayoutTypeProto.UNRECOGNIZED
        ) {
            val homeBuilder = currentData.homeSettingsProto.toBuilder()

            if (currentData.homeSettingsProto.dockPageCount == 0) {
                homeBuilder.setDockPageCount(1)
            }

            if (currentData.homeSettingsProto.folderCellWidth == 0) {
                homeBuilder.setFolderCellWidth(64)
            }

            if (currentData.homeSettingsProto.folderCellHeight == 0) {
                homeBuilder.setFolderCellHeight(96)
            }

            if (currentData.homeSettingsProto.maxFolderColumns == 0) {
                homeBuilder.setMaxFolderColumns(5)
            }

            if (currentData.homeSettingsProto.maxFolderRows == 0) {
                homeBuilder.setMaxFolderRows(4)
            }

            if (currentData.homeSettingsProto.gridItemSettingsProto.gridItemLayoutTypeProto == GridItemLayoutTypeProto.UNRECOGNIZED) {
                homeBuilder.setGridItemSettingsProto(
                    currentData.homeSettingsProto.gridItemSettingsProto.toBuilder()
                        .setGridItemLayoutTypeProto(GridItemLayoutTypeProto.TOP_ICON_BOTTOM_LABEL)
                        .build(),
                )
            }

            mutableData = mutableData.toBuilder()
                .setHomeSettingsProto(homeBuilder.build())
                .build()
        }

        // Only migrate app drawer settings if needed
        if (currentData.appDrawerSettingsProto.horizontalAppDrawerColumns == 0 ||
            currentData.appDrawerSettingsProto.horizontalAppDrawerRows == 0 ||
            currentData.appDrawerSettingsProto.gridItemSettingsProto.gridItemLayoutTypeProto == GridItemLayoutTypeProto.UNRECOGNIZED
        ) {
            val appDrawerBuilder = currentData.appDrawerSettingsProto.toBuilder()

            if (currentData.appDrawerSettingsProto.horizontalAppDrawerColumns == 0) {
                appDrawerBuilder.setHorizontalAppDrawerColumns(5)
            }

            if (currentData.appDrawerSettingsProto.horizontalAppDrawerRows == 0) {
                appDrawerBuilder.setHorizontalAppDrawerRows(5)
            }

            if (currentData.appDrawerSettingsProto.gridItemSettingsProto.gridItemLayoutTypeProto == GridItemLayoutTypeProto.UNRECOGNIZED) {
                appDrawerBuilder.setGridItemSettingsProto(
                    currentData.appDrawerSettingsProto.gridItemSettingsProto.toBuilder()
                        .setGridItemLayoutTypeProto(GridItemLayoutTypeProto.TOP_ICON_BOTTOM_LABEL)
                        .build(),
                )
            }

            mutableData = mutableData.toBuilder()
                .setAppDrawerSettingsProto(appDrawerBuilder.build())
                .build()
        }

        return mutableData
    }

    override suspend fun cleanUp() {}
}
