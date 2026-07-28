package com.eblan.launcher.domain.model

sealed interface SettingsItem {
    data class Column(
        val title: String,
        val subtitle: String,
        val onClick: () -> Unit,
    ) : SettingsItem

    data class Switch(
        val checked: Boolean,
        val title: String,
        val subtitle: String,
        val onClick: () -> Unit,
        val onCheckedChange: (Boolean) -> Unit,
    ) : SettingsItem
}
