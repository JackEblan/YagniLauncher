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
package com.eblan.launcher.ui.edit

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.eblan.launcher.designsystem.icon.EblanLauncherIcons
import com.eblan.launcher.domain.model.PackageManagerIconPackInfo
import com.eblan.launcher.ui.settings.SettingsColumn

@Composable
fun CustomIcon(
    modifier: Modifier = Modifier,
    customIcon: String?,
    packageManagerIconPackInfos: List<PackageManagerIconPackInfo>,
    onUpdateIconPackInfoPackageName: (
        packageName: String,
        label: String?,
    ) -> Unit,
    onUpdateUri: (String) -> Unit,
    onResetCustomIcon: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    val context = LocalContext.current

    val pickMedia =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) {
                val flag = Intent.FLAG_GRANT_READ_URI_PERMISSION

                context.contentResolver.takePersistableUriPermission(uri, flag)

                onUpdateUri(uri.toString())
            }
        }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(15.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (customIcon != null) {
                AsyncImage(
                    modifier = Modifier.size(40.dp),
                    model = customIcon,
                    contentDescription = null,
                )
            } else {
                Icon(
                    modifier = Modifier.size(40.dp),
                    imageVector = EblanLauncherIcons.BrokenImage,
                    contentDescription = null,
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Custom Icon")

                Spacer(modifier = Modifier.height(10.dp))

                Text(text = customIcon ?: "None")
            }

            IconButton(
                onClick = {
                    expanded = !expanded
                },
            ) {
                Icon(
                    imageVector = if (expanded) {
                        EblanLauncherIcons.ArrowDropUp
                    } else {
                        EblanLauncherIcons.ArrowDropDown
                    },
                    contentDescription = null,
                )
            }
        }

        if (expanded) {
            HorizontalDivider(modifier = Modifier.fillMaxWidth())

            SettingsColumn(
                title = "Gallery",
                subtitle = "Pick icons from your gallery",
                onClick = {
                    pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
            )

            packageManagerIconPackInfos.forEach { packageManagerIconPackInfo ->
                HorizontalDivider(modifier = Modifier.fillMaxWidth())

                IconPackItem(
                    icon = packageManagerIconPackInfo.icon,
                    label = packageManagerIconPackInfo.label,
                    packageName = packageManagerIconPackInfo.packageName,
                    onClick = {
                        onUpdateIconPackInfoPackageName(
                            packageManagerIconPackInfo.packageName,
                            packageManagerIconPackInfo.label,
                        )
                    },
                )
            }

            if (customIcon != null) {
                HorizontalDivider(modifier = Modifier.fillMaxWidth())

                SettingsColumn(
                    title = "Reset to defaults",
                    subtitle = "Delete custom icon",
                    onClick = onResetCustomIcon,
                )
            }
        }
    }
}

@Composable
private fun IconPackItem(
    modifier: Modifier = Modifier,
    icon: ByteArray?,
    label: String?,
    packageName: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = modifier
            .clickable(onClick = onClick)
            .fillMaxWidth()
            .padding(15.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            modifier = Modifier.size(40.dp),
            model = icon,
            contentDescription = null,
        )

        Spacer(modifier = Modifier.width(20.dp))

        Column {
            Text(
                text = label.toString(),
                style = MaterialTheme.typography.titleMedium,
            )

            Spacer(modifier = Modifier.height(5.dp))

            Text(
                text = packageName,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
