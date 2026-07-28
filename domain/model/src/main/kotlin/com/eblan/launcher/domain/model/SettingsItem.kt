package com.eblan.launcher.domain.model

sealed interface SettingsItem {
    data class Category(
        val title: String,
    ) : SettingsItem

    data class Column(
        val title: String,
        val subtitle: String,
        val onClick: () -> Unit,
    ) : SettingsItem

    data class Switch(
        val title: String,
        val subtitle: String,
        val checked: Boolean,
        val onClick: () -> Unit,
        val onCheckedChange: (Boolean) -> Unit,
    ) : SettingsItem

    data class CustomBackgroundColor(
        val title: String,
        val customBackgroundColor: Int,
        val onClick: () -> Unit,
    ) : SettingsItem
}
