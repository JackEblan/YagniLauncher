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
package com.eblan.launcher.data.datastore

import androidx.datastore.core.DataStore
import com.eblan.launcher.data.datastore.mapper.toAppDrawerSettings
import com.eblan.launcher.data.datastore.mapper.toAppDrawerSettingsProto
import com.eblan.launcher.data.datastore.mapper.toExperimentalSettings
import com.eblan.launcher.data.datastore.mapper.toExperimentalSettingsProto
import com.eblan.launcher.data.datastore.mapper.toGeneralSettings
import com.eblan.launcher.data.datastore.mapper.toGeneralSettingsProto
import com.eblan.launcher.data.datastore.mapper.toGestureSettings
import com.eblan.launcher.data.datastore.mapper.toGestureSettingsProto
import com.eblan.launcher.data.datastore.mapper.toHomeSettings
import com.eblan.launcher.data.datastore.mapper.toHomeSettingsProto
import com.eblan.launcher.data.datastore.proto.UserDataProto
import com.eblan.launcher.data.datastore.proto.copy
import com.eblan.launcher.domain.model.AppDrawerSettings
import com.eblan.launcher.domain.model.ExperimentalSettings
import com.eblan.launcher.domain.model.GeneralSettings
import com.eblan.launcher.domain.model.GestureSettings
import com.eblan.launcher.domain.model.HomeSettings
import com.eblan.launcher.domain.model.UserData
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class UserDataStore @Inject constructor(private val dataStore: DataStore<UserDataProto>) {
    val userDataFlow = dataStore.data.map { userDataProto ->
        UserData(
            homeSettings = userDataProto.homeSettingsProto.toHomeSettings(),
            appDrawerSettings = userDataProto.appDrawerSettingsProto.toAppDrawerSettings(),
            gestureSettings = userDataProto.gestureSettingsProto.toGestureSettings(),
            generalSettings = userDataProto.generalSettingsProto.toGeneralSettings(),
            experimentalSettings = userDataProto.experimentalSettingsProto.toExperimentalSettings(),
        )
    }

    suspend fun updateGeneralSettings(generalSettings: GeneralSettings) {
        dataStore.updateData { userDataProto ->
            userDataProto.copy {
                generalSettingsProto = generalSettings.toGeneralSettingsProto()
            }
        }
    }

    suspend fun updateHomeSettings(homeSettings: HomeSettings) {
        dataStore.updateData { userDataProto ->
            userDataProto.copy {
                homeSettingsProto = homeSettings.toHomeSettingsProto()
            }
        }
    }

    suspend fun updateAppDrawerSettings(appDrawerSettings: AppDrawerSettings) {
        dataStore.updateData { userDataProto ->
            userDataProto.copy {
                appDrawerSettingsProto = appDrawerSettings.toAppDrawerSettingsProto()
            }
        }
    }

    suspend fun updateGestureSettings(gestureSettings: GestureSettings) {
        dataStore.updateData { userDataProto ->
            userDataProto.copy {
                gestureSettingsProto = gestureSettings.toGestureSettingsProto()
            }
        }
    }

    suspend fun updateExperimentalSettings(experimentalSettings: ExperimentalSettings) {
        dataStore.updateData { userDataProto ->
            userDataProto.copy {
                experimentalSettingsProto = experimentalSettings.toExperimentalSettingsProto()
            }
        }
    }
}
