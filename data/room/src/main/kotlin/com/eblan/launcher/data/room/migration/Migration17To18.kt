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
package com.eblan.launcher.data.room.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class Migration17To18 : Migration(17, 18) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // ---------------------------------------------------------
        // ApplicationInfoGridItemEntity
        // ---------------------------------------------------------
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `ApplicationInfoGridItemEntity_new` (
                `id` TEXT NOT NULL,
                `page` INTEGER NOT NULL,
                `startColumn` INTEGER NOT NULL,
                `startRow` INTEGER NOT NULL,
                `columnSpan` INTEGER NOT NULL,
                `rowSpan` INTEGER NOT NULL,
                `associate` TEXT NOT NULL,
                `componentName` TEXT NOT NULL,
                `packageName` TEXT NOT NULL,
                `icon` TEXT,
                `label` TEXT NOT NULL,
                `override` INTEGER NOT NULL,
                `serialNumber` INTEGER NOT NULL,
                `customIcon` TEXT,
                `customLabel` TEXT,
                `iconSize` INTEGER NOT NULL,
                `textColor` TEXT NOT NULL,
                `textSize` INTEGER NOT NULL,
                `showLabel` INTEGER NOT NULL,
                `singleLineLabel` INTEGER NOT NULL,
                `horizontalAlignment` TEXT NOT NULL,
                `verticalArrangement` TEXT NOT NULL,
                `customTextColor` INTEGER NOT NULL,
                `customBackgroundColor` INTEGER NOT NULL,
                `padding` INTEGER NOT NULL,
                `cornerRadius` INTEGER NOT NULL,
                `doubleTap_eblanActionType` TEXT NOT NULL,
                `doubleTap_serialNumber` INTEGER NOT NULL,
                `doubleTap_componentName` TEXT NOT NULL,
                `swipeUp_eblanActionType` TEXT NOT NULL,
                `swipeUp_serialNumber` INTEGER NOT NULL,
                `swipeUp_componentName` TEXT NOT NULL,
                `swipeDown_eblanActionType` TEXT NOT NULL,
                `swipeDown_serialNumber` INTEGER NOT NULL,
                `swipeDown_componentName` TEXT NOT NULL,
                `index` INTEGER NOT NULL,
                `folderId` TEXT,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`folderId`) REFERENCES `FolderGridItemEntity`(`id`) ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            INSERT INTO `ApplicationInfoGridItemEntity_new` (
                id, page, startColumn, startRow, columnSpan, rowSpan, associate,
                componentName, packageName, icon, label, override, serialNumber,
                customIcon, customLabel,
                iconSize, textColor, textSize, showLabel, singleLineLabel,
                horizontalAlignment, verticalArrangement, customTextColor,
                customBackgroundColor, padding, cornerRadius,
                doubleTap_eblanActionType, doubleTap_serialNumber, doubleTap_componentName,
                swipeUp_eblanActionType, swipeUp_serialNumber, swipeUp_componentName,
                swipeDown_eblanActionType, swipeDown_serialNumber, swipeDown_componentName,
                `index`, folderId
            )
            SELECT
                id, page, startColumn, startRow, columnSpan, rowSpan, associate,
                componentName, packageName, icon, label, override, serialNumber,
                customIcon, customLabel,
                iconSize, textColor, textSize, showLabel, singleLineLabel,
                horizontalAlignment, verticalArrangement, customTextColor,
                customBackgroundColor, padding, cornerRadius,
                doubleTap_eblanActionType, doubleTap_serialNumber, doubleTap_componentName,
                swipeUp_eblanActionType, swipeUp_serialNumber, swipeUp_componentName,
                swipeDown_eblanActionType, swipeDown_serialNumber, swipeDown_componentName,
                `index`, NULL
            FROM `ApplicationInfoGridItemEntity`
            """.trimIndent(),
        )
        db.execSQL("DROP TABLE `ApplicationInfoGridItemEntity`")
        db.execSQL("ALTER TABLE `ApplicationInfoGridItemEntity_new` RENAME TO `ApplicationInfoGridItemEntity`")
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_ApplicationInfoGridItemEntity_folderId` ON `ApplicationInfoGridItemEntity` (`folderId`)",
        )

        // ---------------------------------------------------------
        // ShortcutInfoGridItemEntity
        // ---------------------------------------------------------
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `ShortcutInfoGridItemEntity_new` (
                `id` TEXT NOT NULL,
                `page` INTEGER NOT NULL,
                `startColumn` INTEGER NOT NULL,
                `startRow` INTEGER NOT NULL,
                `columnSpan` INTEGER NOT NULL,
                `rowSpan` INTEGER NOT NULL,
                `associate` TEXT NOT NULL,
                `shortcutId` TEXT NOT NULL,
                `packageName` TEXT NOT NULL,
                `shortLabel` TEXT NOT NULL,
                `longLabel` TEXT NOT NULL,
                `icon` TEXT,
                `override` INTEGER NOT NULL,
                `serialNumber` INTEGER NOT NULL,
                `isEnabled` INTEGER NOT NULL,
                `eblanApplicationInfoIcon` TEXT,
                `customIcon` TEXT,
                `customShortLabel` TEXT,
                `iconSize` INTEGER NOT NULL,
                `textColor` TEXT NOT NULL,
                `textSize` INTEGER NOT NULL,
                `showLabel` INTEGER NOT NULL,
                `singleLineLabel` INTEGER NOT NULL,
                `horizontalAlignment` TEXT NOT NULL,
                `verticalArrangement` TEXT NOT NULL,
                `customTextColor` INTEGER NOT NULL,
                `customBackgroundColor` INTEGER NOT NULL,
                `padding` INTEGER NOT NULL,
                `cornerRadius` INTEGER NOT NULL,
                `doubleTap_eblanActionType` TEXT NOT NULL,
                `doubleTap_serialNumber` INTEGER NOT NULL,
                `doubleTap_componentName` TEXT NOT NULL,
                `swipeUp_eblanActionType` TEXT NOT NULL,
                `swipeUp_serialNumber` INTEGER NOT NULL,
                `swipeUp_componentName` TEXT NOT NULL,
                `swipeDown_eblanActionType` TEXT NOT NULL,
                `swipeDown_serialNumber` INTEGER NOT NULL,
                `swipeDown_componentName` TEXT NOT NULL,
                `index` INTEGER NOT NULL,
                `folderId` TEXT,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`folderId`) REFERENCES `FolderGridItemEntity`(`id`) ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            INSERT INTO `ShortcutInfoGridItemEntity_new` (
                id, page, startColumn, startRow, columnSpan, rowSpan, associate,
                shortcutId, packageName, shortLabel, longLabel, icon, override,
                serialNumber, isEnabled, eblanApplicationInfoIcon, customIcon, customShortLabel,
                iconSize, textColor, textSize, showLabel, singleLineLabel,
                horizontalAlignment, verticalArrangement, customTextColor,
                customBackgroundColor, padding, cornerRadius,
                doubleTap_eblanActionType, doubleTap_serialNumber, doubleTap_componentName,
                swipeUp_eblanActionType, swipeUp_serialNumber, swipeUp_componentName,
                swipeDown_eblanActionType, swipeDown_serialNumber, swipeDown_componentName,
                `index`, folderId
            )
            SELECT
                id, page, startColumn, startRow, columnSpan, rowSpan, associate,
                shortcutId, packageName, shortLabel, longLabel, icon, override,
                serialNumber, isEnabled, eblanApplicationInfoIcon, customIcon, customShortLabel,
                iconSize, textColor, textSize, showLabel, singleLineLabel,
                horizontalAlignment, verticalArrangement, customTextColor,
                customBackgroundColor, padding, cornerRadius,
                doubleTap_eblanActionType, doubleTap_serialNumber, doubleTap_componentName,
                swipeUp_eblanActionType, swipeUp_serialNumber, swipeUp_componentName,
                swipeDown_eblanActionType, swipeDown_serialNumber, swipeDown_componentName,
                `index`, NULL
            FROM `ShortcutInfoGridItemEntity`
            """.trimIndent(),
        )
        db.execSQL("DROP TABLE `ShortcutInfoGridItemEntity`")
        db.execSQL("ALTER TABLE `ShortcutInfoGridItemEntity_new` RENAME TO `ShortcutInfoGridItemEntity`")
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_ShortcutInfoGridItemEntity_folderId` ON `ShortcutInfoGridItemEntity` (`folderId`)",
        )

        // ---------------------------------------------------------
        // ShortcutConfigGridItemEntity
        // ---------------------------------------------------------
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `ShortcutConfigGridItemEntity_new` (
                `id` TEXT NOT NULL,
                `page` INTEGER NOT NULL,
                `startColumn` INTEGER NOT NULL,
                `startRow` INTEGER NOT NULL,
                `columnSpan` INTEGER NOT NULL,
                `rowSpan` INTEGER NOT NULL,
                `associate` TEXT NOT NULL,
                `componentName` TEXT NOT NULL,
                `packageName` TEXT NOT NULL,
                `activityIcon` TEXT,
                `activityLabel` TEXT,
                `applicationIcon` TEXT,
                `applicationLabel` TEXT,
                `override` INTEGER NOT NULL,
                `serialNumber` INTEGER NOT NULL,
                `shortcutIntentName` TEXT,
                `shortcutIntentIcon` TEXT,
                `shortcutIntentUri` TEXT,
                `customIcon` TEXT,
                `customLabel` TEXT,
                `iconSize` INTEGER NOT NULL,
                `textColor` TEXT NOT NULL,
                `textSize` INTEGER NOT NULL,
                `showLabel` INTEGER NOT NULL,
                `singleLineLabel` INTEGER NOT NULL,
                `horizontalAlignment` TEXT NOT NULL,
                `verticalArrangement` TEXT NOT NULL,
                `customTextColor` INTEGER NOT NULL,
                `customBackgroundColor` INTEGER NOT NULL,
                `padding` INTEGER NOT NULL,
                `cornerRadius` INTEGER NOT NULL,
                `doubleTap_eblanActionType` TEXT NOT NULL,
                `doubleTap_serialNumber` INTEGER NOT NULL,
                `doubleTap_componentName` TEXT NOT NULL,
                `swipeUp_eblanActionType` TEXT NOT NULL,
                `swipeUp_serialNumber` INTEGER NOT NULL,
                `swipeUp_componentName` TEXT NOT NULL,
                `swipeDown_eblanActionType` TEXT NOT NULL,
                `swipeDown_serialNumber` INTEGER NOT NULL,
                `swipeDown_componentName` TEXT NOT NULL,
                `index` INTEGER NOT NULL,
                `folderId` TEXT,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`folderId`) REFERENCES `FolderGridItemEntity`(`id`) ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            INSERT INTO `ShortcutConfigGridItemEntity_new` (
                id, page, startColumn, startRow, columnSpan, rowSpan, associate,
                componentName, packageName, activityIcon, activityLabel, applicationIcon,
                applicationLabel, override, serialNumber, shortcutIntentName,
                shortcutIntentIcon, shortcutIntentUri, customIcon, customLabel,
                iconSize, textColor, textSize, showLabel, singleLineLabel,
                horizontalAlignment, verticalArrangement, customTextColor,
                customBackgroundColor, padding, cornerRadius,
                doubleTap_eblanActionType, doubleTap_serialNumber, doubleTap_componentName,
                swipeUp_eblanActionType, swipeUp_serialNumber, swipeUp_componentName,
                swipeDown_eblanActionType, swipeDown_serialNumber, swipeDown_componentName,
                `index`, folderId
            )
            SELECT
                id, page, startColumn, startRow, columnSpan, rowSpan, associate,
                componentName, packageName, activityIcon, activityLabel, applicationIcon,
                applicationLabel, override, serialNumber, shortcutIntentName,
                shortcutIntentIcon, shortcutIntentUri, customIcon, customLabel,
                iconSize, textColor, textSize, showLabel, singleLineLabel,
                horizontalAlignment, verticalArrangement, customTextColor,
                customBackgroundColor, padding, cornerRadius,
                doubleTap_eblanActionType, doubleTap_serialNumber, doubleTap_componentName,
                swipeUp_eblanActionType, swipeUp_serialNumber, swipeUp_componentName,
                swipeDown_eblanActionType, swipeDown_serialNumber, swipeDown_componentName,
                `index`, NULL
            FROM `ShortcutConfigGridItemEntity`
            """.trimIndent(),
        )
        db.execSQL("DROP TABLE `ShortcutConfigGridItemEntity`")
        db.execSQL("ALTER TABLE `ShortcutConfigGridItemEntity_new` RENAME TO `ShortcutConfigGridItemEntity`")
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_ShortcutConfigGridItemEntity_folderId` ON `ShortcutConfigGridItemEntity` (`folderId`)",
        )

        // ---------------------------------------------------------
        // FolderGridItemEntity (self-referencing FK on folderId)
        // ---------------------------------------------------------
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `FolderGridItemEntity_new` (
                `id` TEXT NOT NULL,
                `page` INTEGER NOT NULL,
                `startColumn` INTEGER NOT NULL,
                `startRow` INTEGER NOT NULL,
                `columnSpan` INTEGER NOT NULL,
                `rowSpan` INTEGER NOT NULL,
                `associate` TEXT NOT NULL,
                `label` TEXT NOT NULL,
                `override` INTEGER NOT NULL,
                `icon` TEXT,
                `iconSize` INTEGER NOT NULL,
                `textColor` TEXT NOT NULL,
                `textSize` INTEGER NOT NULL,
                `showLabel` INTEGER NOT NULL,
                `singleLineLabel` INTEGER NOT NULL,
                `horizontalAlignment` TEXT NOT NULL,
                `verticalArrangement` TEXT NOT NULL,
                `customTextColor` INTEGER NOT NULL,
                `customBackgroundColor` INTEGER NOT NULL,
                `padding` INTEGER NOT NULL,
                `cornerRadius` INTEGER NOT NULL,
                `doubleTap_eblanActionType` TEXT NOT NULL,
                `doubleTap_serialNumber` INTEGER NOT NULL,
                `doubleTap_componentName` TEXT NOT NULL,
                `swipeUp_eblanActionType` TEXT NOT NULL,
                `swipeUp_serialNumber` INTEGER NOT NULL,
                `swipeUp_componentName` TEXT NOT NULL,
                `swipeDown_eblanActionType` TEXT NOT NULL,
                `swipeDown_serialNumber` INTEGER NOT NULL,
                `swipeDown_componentName` TEXT NOT NULL,
                `index` INTEGER NOT NULL,
                `folderId` TEXT,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`folderId`) REFERENCES `FolderGridItemEntity`(`id`) ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            INSERT INTO `FolderGridItemEntity_new` (
                id, page, startColumn, startRow, columnSpan, rowSpan, associate,
                label, override, icon,
                iconSize, textColor, textSize, showLabel, singleLineLabel,
                horizontalAlignment, verticalArrangement, customTextColor,
                customBackgroundColor, padding, cornerRadius,
                doubleTap_eblanActionType, doubleTap_serialNumber, doubleTap_componentName,
                swipeUp_eblanActionType, swipeUp_serialNumber, swipeUp_componentName,
                swipeDown_eblanActionType, swipeDown_serialNumber, swipeDown_componentName,
                `index`, folderId
            )
            SELECT
                id, page, startColumn, startRow, columnSpan, rowSpan, associate,
                label, override, icon,
                iconSize, textColor, textSize, showLabel, singleLineLabel,
                horizontalAlignment, verticalArrangement, customTextColor,
                customBackgroundColor, padding, cornerRadius,
                doubleTap_eblanActionType, doubleTap_serialNumber, doubleTap_componentName,
                swipeUp_eblanActionType, swipeUp_serialNumber, swipeUp_componentName,
                swipeDown_eblanActionType, swipeDown_serialNumber, swipeDown_componentName,
                `index`, NULL
            FROM `FolderGridItemEntity`
            """.trimIndent(),
        )
        db.execSQL("DROP TABLE `FolderGridItemEntity`")
        db.execSQL("ALTER TABLE `FolderGridItemEntity_new` RENAME TO `FolderGridItemEntity`")
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_FolderGridItemEntity_folderId` ON `FolderGridItemEntity` (`folderId`)",
        )
    }
}
