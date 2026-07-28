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
package com.eblan.launcher.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.eblan.launcher.ui.model.SettingsItem

@Composable
fun SettingsSwitch(
    modifier: Modifier = Modifier,
    checked: Boolean,
    title: String,
    subtitle: String,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = modifier
            .clickable(
                onClick = {
                    onCheckedChange(!checked)
                },
            )
            .fillMaxWidth()
            .padding(15.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}

@Composable
fun SettingsSwitch(
    modifier: Modifier = Modifier,
    index: Int,
    size: Int,
    checked: Boolean,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    onCheckedChange: (Boolean) -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = settingsItemShape(
            index = index,
            size = size,
        ),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(15.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
            )
        }
    }
}

@Composable
fun SettingsColumn(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .fillMaxWidth()
            .padding(15.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
        )

        Spacer(modifier = Modifier.height(5.dp))

        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
fun SettingsColumn(
    modifier: Modifier = Modifier,
    index: Int,
    size: Int,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = settingsItemShape(
            index = index,
            size = size,
        ),
        onClick = onClick,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(15.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
            )

            Spacer(modifier = Modifier.height(5.dp))

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
fun SettingsItemContent(
    settingsItem: SettingsItem,
    index: Int,
    size: Int,
) {
    when (settingsItem) {
        is SettingsItem.Column -> {
            SettingsColumn(
                index = index,
                size = size,
                title = settingsItem.title,
                subtitle = settingsItem.subtitle,
                onClick = settingsItem.onClick,
            )
        }

        is SettingsItem.Switch -> {
            SettingsSwitch(
                index = index,
                size = size,
                checked = settingsItem.checked,
                title = settingsItem.title,
                subtitle = settingsItem.subtitle,
                onClick = settingsItem.onClick,
                onCheckedChange = settingsItem.onCheckedChange,
            )
        }

        is SettingsItem.CustomBackgroundColor,
        -> {
            CustomBackgroundColor(
                index = index,
                size = size,
                title = settingsItem.title,
                customBackgroundColor = settingsItem.customBackgroundColor,
                onClick = settingsItem.onClick,
            )
        }

        is SettingsItem.Row -> {
            SettingsRow(
                index = index,
                size = size,
                imageVector = settingsItem.imageVector,
                title = settingsItem.title,
                subtitle = settingsItem.subtitle,
                onClick = settingsItem.onClick,
            )
        }
    }
}

@Composable
fun SettingsCategoryText(
    modifier: Modifier = Modifier,
    text: String,
) {
    Text(
        modifier = modifier.padding(15.dp),
        text = text,
        style = MaterialTheme.typography.bodySmall,
    )
}

@Composable
private fun SettingsRow(
    modifier: Modifier = Modifier,
    index: Int,
    size: Int,
    imageVector: ImageVector,
    subtitle: String,
    title: String,
    onClick: () -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = settingsItemShape(
            index = index,
            size = size,
        ),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(15.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = imageVector,
                contentDescription = null,
            )

            Spacer(modifier = Modifier.width(20.dp))

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                )

                Spacer(modifier = Modifier.height(5.dp))

                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

fun settingsItemShape(
    index: Int,
    size: Int,
    radius: Dp = 16.dp,
): Shape = when {
    size == 1 -> RoundedCornerShape(size = radius)

    index == 0 -> RoundedCornerShape(
        topStart = radius,
        topEnd = radius,
        bottomStart = radius / 2,
        bottomEnd = radius / 2,
    )

    index == size - 1 -> RoundedCornerShape(
        topStart = radius / 2,
        topEnd = radius / 2,
        bottomStart = radius,
        bottomEnd = radius,
    )

    else -> RoundedCornerShape(size = radius / 2)
}
