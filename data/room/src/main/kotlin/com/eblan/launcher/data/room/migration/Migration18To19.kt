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

class Migration18To19 : Migration(18, 19) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `EblanAppWidgetProviderInfoEntity_new` (
                `componentName` TEXT NOT NULL PRIMARY KEY,
                `serialNumber` INTEGER NOT NULL,
                `configure` TEXT,
                `packageName` TEXT NOT NULL,
                `targetCellWidth` INTEGER NOT NULL,
                `targetCellHeight` INTEGER NOT NULL,
                `minWidth` INTEGER NOT NULL,
                `minHeight` INTEGER NOT NULL,
                `resizeMode` INTEGER NOT NULL,
                `minResizeWidth` INTEGER NOT NULL,
                `minResizeHeight` INTEGER NOT NULL,
                `maxResizeWidth` INTEGER NOT NULL,
                `maxResizeHeight` INTEGER NOT NULL,
                `preview` TEXT,
                `applicationLabel` TEXT,
                `applicationIcon` TEXT,
                `lastUpdateTime` INTEGER NOT NULL DEFAULT 0,
                `label` TEXT,
                `description` TEXT
            )
            """.trimIndent(),
        )

        db.execSQL(
            """
            INSERT INTO `EblanAppWidgetProviderInfoEntity_new` (
                `componentName`, `serialNumber`, `configure`, `packageName`,
                `targetCellWidth`, `targetCellHeight`, `minWidth`, `minHeight`,
                `resizeMode`, `minResizeWidth`, `minResizeHeight`,
                `maxResizeWidth`, `maxResizeHeight`, `preview`,
                `applicationLabel`, `applicationIcon`, `lastUpdateTime`,
                `label`, `description`
            )
            SELECT
                `componentName`, `serialNumber`, `configure`, `packageName`,
                `targetCellWidth`, `targetCellHeight`, `minWidth`, `minHeight`,
                `resizeMode`, `minResizeWidth`, `minResizeHeight`,
                `maxResizeWidth`, `maxResizeHeight`, `preview`,
                `applicationLabel`, `applicationIcon`, `lastUpdateTime`,
                `label`, `description`
            FROM `EblanAppWidgetProviderInfoEntity`
            """.trimIndent(),
        )

        db.execSQL("DROP TABLE `EblanAppWidgetProviderInfoEntity`")
        db.execSQL("ALTER TABLE `EblanAppWidgetProviderInfoEntity_new` RENAME TO `EblanAppWidgetProviderInfoEntity`")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `WidgetGridItemEntity_new` (
                `id` TEXT NOT NULL PRIMARY KEY,
                `page` INTEGER NOT NULL,
                `startColumn` INTEGER NOT NULL,
                `startRow` INTEGER NOT NULL,
                `columnSpan` INTEGER NOT NULL,
                `rowSpan` INTEGER NOT NULL,
                `associate` TEXT NOT NULL,
                `appWidgetId` INTEGER NOT NULL,
                `packageName` TEXT NOT NULL,
                `componentName` TEXT NOT NULL,
                `configure` TEXT,
                `minWidth` INTEGER NOT NULL,
                `minHeight` INTEGER NOT NULL,
                `resizeMode` INTEGER NOT NULL,
                `minResizeWidth` INTEGER NOT NULL,
                `minResizeHeight` INTEGER NOT NULL,
                `maxResizeWidth` INTEGER NOT NULL,
                `maxResizeHeight` INTEGER NOT NULL,
                `targetCellHeight` INTEGER NOT NULL,
                `targetCellWidth` INTEGER NOT NULL,
                `preview` TEXT,
                `label` TEXT,
                `icon` TEXT,
                `override` INTEGER NOT NULL,
                `serialNumber` INTEGER NOT NULL,
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
                `cornerRadius` INTEGER NOT NULL
            )
            """.trimIndent(),
        )

        db.execSQL(
            """
            INSERT INTO `WidgetGridItemEntity_new` (
                `id`, `page`, `startColumn`, `startRow`, `columnSpan`, `rowSpan`,
                `associate`, `appWidgetId`, `packageName`, `componentName`, `configure`,
                `minWidth`, `minHeight`, `resizeMode`, `minResizeWidth`, `minResizeHeight`,
                `maxResizeWidth`, `maxResizeHeight`, `targetCellHeight`, `targetCellWidth`,
                `preview`, `label`, `icon`, `override`, `serialNumber`,
                `iconSize`, `textColor`, `textSize`, `showLabel`, `singleLineLabel`,
                `horizontalAlignment`, `verticalArrangement`, `customTextColor`,
                `customBackgroundColor`, `padding`, `cornerRadius`
            )
            SELECT
                `id`, `page`, `startColumn`, `startRow`, `columnSpan`, `rowSpan`,
                `associate`, `appWidgetId`, `packageName`, `componentName`, `configure`,
                `minWidth`, `minHeight`, `resizeMode`, `minResizeWidth`, `minResizeHeight`,
                `maxResizeWidth`, `maxResizeHeight`, `targetCellHeight`, `targetCellWidth`,
                `preview`, `label`, `icon`, `override`, `serialNumber`,
                `iconSize`, `textColor`, `textSize`, `showLabel`, `singleLineLabel`,
                `horizontalAlignment`, `verticalArrangement`, `customTextColor`,
                `customBackgroundColor`, `padding`, `cornerRadius`
            FROM `WidgetGridItemEntity`
            """.trimIndent(),
        )

        db.execSQL("DROP TABLE `WidgetGridItemEntity`")
        db.execSQL("ALTER TABLE `WidgetGridItemEntity_new` RENAME TO `WidgetGridItemEntity`")
    }
}
