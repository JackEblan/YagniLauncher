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
package com.eblan.launcher.ui.dialog

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.eblan.launcher.designsystem.component.EblanDialogContainer
import com.eblan.launcher.designsystem.component.EblanRadioButton
import com.eblan.launcher.domain.model.EblanAction
import com.eblan.launcher.domain.model.EblanActionType
import com.eblan.launcher.domain.model.EblanApplicationInfo
import com.eblan.launcher.domain.model.EblanUser
import com.eblan.launcher.ui.local.LocalAccessibilityManager
import com.eblan.launcher.ui.settings.getEblanActionTypeSubtitle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EblanActionDialog(
    modifier: Modifier = Modifier,
    title: String,
    eblanAction: EblanAction,
    eblanApplicationInfos: Map<EblanUser, List<EblanApplicationInfo>>,
    onSelectEblanAction: (EblanAction) -> Unit,
    onDismissRequest: () -> Unit,
) {
    val context = LocalContext.current

    val accessibilityManager = LocalAccessibilityManager.current

    var selectedEblanAction by remember { mutableStateOf(eblanAction) }

    var showSelectApplicationDialog by remember { mutableStateOf(false) }

    val isAccessibilityServiceEnabled = remember {
        accessibilityManager.isAccessibilityServiceEnabled()
    }

    EblanDialogContainer(
        content = {
            Column(
                modifier = modifier
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth(),
            ) {
                Text(
                    modifier = Modifier.padding(10.dp),
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                )

                Spacer(modifier = Modifier.height(10.dp))

                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    onClick = {
                        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)

                        context.startActivity(intent)
                    },
                ) {
                    Text(
                        modifier = Modifier.padding(15.dp),
                        text = "Enable the Accessibility Services permission to use additional actions",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Column(
                    modifier = Modifier
                        .selectableGroup()
                        .fillMaxWidth(),
                ) {
                    EblanActionType.entries.forEach { eblanActionType ->
                        val enabled = when (eblanActionType) {
                            EblanActionType.OpenNotificationPanel,
                            EblanActionType.LockScreen,
                            EblanActionType.OpenQuickSettings,
                            EblanActionType.OpenRecents,
                            -> isAccessibilityServiceEnabled

                            else -> true
                        }

                        EblanRadioButton(
                            enabled = enabled,
                            selected = selectedEblanAction.eblanActionType == eblanActionType,
                            text = eblanActionType.getEblanActionTypeSubtitle(componentName = selectedEblanAction.componentName),
                            onClick = {
                                if (eblanActionType == EblanActionType.OpenApp) {
                                    showSelectApplicationDialog = true
                                } else {
                                    selectedEblanAction = EblanAction(
                                        eblanActionType = eblanActionType,
                                        serialNumber = 0L,
                                        componentName = "",
                                    )
                                }
                            },
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            end = 10.dp,
                            bottom = 10.dp,
                        ),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(
                        onClick = onDismissRequest,
                    ) {
                        Text("Cancel")
                    }

                    Spacer(modifier = Modifier.width(5.dp))

                    TextButton(
                        onClick = {
                            onSelectEblanAction(selectedEblanAction)
                        },
                    ) {
                        Text("Save")
                    }
                }
            }
        },
        onDismissRequest = onDismissRequest,
    )

    if (showSelectApplicationDialog) {
        SelectApplicationDialog(
            eblanApplicationInfos = eblanApplicationInfos,
            onDismissRequest = {
                showSelectApplicationDialog = false
            },
            onClick = { eblanApplicationInfo ->
                selectedEblanAction = EblanAction(
                    eblanActionType = EblanActionType.OpenApp,
                    serialNumber = eblanApplicationInfo.serialNumber,
                    componentName = eblanApplicationInfo.componentName,
                )

                showSelectApplicationDialog = false
            },
        )
    }
}
