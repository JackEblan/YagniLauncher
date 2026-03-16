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
package com.eblan.launcher.feature.home.screen.pager

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.LauncherApps.PinItemRequest
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.os.Process
import androidx.activity.result.ActivityResult
import androidx.activity.result.IntentSenderRequest
import com.eblan.launcher.domain.framework.FileManager
import com.eblan.launcher.domain.model.GridItem
import com.eblan.launcher.domain.model.GridItemData
import com.eblan.launcher.domain.model.MoveGridItemResult
import com.eblan.launcher.domain.model.PinItemRequestType
import com.eblan.launcher.feature.home.model.Drag
import com.eblan.launcher.feature.home.model.GridItemSource
import com.eblan.launcher.framework.imageserializer.AndroidImageSerializer
import com.eblan.launcher.framework.launcherapps.AndroidLauncherAppsWrapper
import com.eblan.launcher.framework.usermanager.AndroidUserManagerWrapper
import com.eblan.launcher.framework.widgetmanager.AndroidAppWidgetHostWrapper
import com.eblan.launcher.framework.widgetmanager.AndroidAppWidgetManagerWrapper
import java.io.File

internal suspend fun handleDropGridItem(
    androidAppWidgetHostWrapper: AndroidAppWidgetHostWrapper,
    androidAppWidgetManagerWrapper: AndroidAppWidgetManagerWrapper,
    androidLauncherAppsWrapper: AndroidLauncherAppsWrapper,
    androidUserManagerWrapper: AndroidUserManagerWrapper,
    drag: Drag,
    gridItemSource: GridItemSource?,
    isDragging: Boolean,
    isLongPress: Boolean,
    moveGridItemResult: MoveGridItemResult?,
    lockMovement: Boolean,
    onDeleteGridItemCache: (GridItem) -> Unit,
    onDragCancelAfterMove: () -> Unit,
    onDragEndAfterMove: (MoveGridItemResult) -> Unit,
    onDragEndAfterMoveFolder: () -> Unit,
    onLaunchShortcutConfigIntent: (Intent) -> Unit,
    onLaunchShortcutConfigIntentSenderRequest: (IntentSenderRequest) -> Unit,
    onLaunchWidgetIntent: (Intent) -> Unit,
    onToast: () -> Unit,
    onUpdateAppWidgetId: (Int) -> Unit,
    onUpdateIsDragging: (Boolean) -> Unit,
    onUpdateIsLongPress: (Boolean) -> Unit,
    onUpdateWidgetGridItem: (GridItem) -> Unit,
) {
    if (drag == Drag.None || drag == Drag.Start || drag == Drag.Dragging || gridItemSource == null) {
        return
    }

    when (gridItemSource) {
        is GridItemSource.Existing -> {
            if (lockMovement) {
                onUpdateIsLongPress(false)

                onUpdateIsDragging(false)

                onDragCancelAfterMove()

                onToast()
            } else if (isLongPress && !isDragging) {
                onUpdateIsLongPress(false)
            } else if (isLongPress && (drag == Drag.Cancel || moveGridItemResult == null || !moveGridItemResult.isSuccess)) {
                onUpdateIsLongPress(false)

                onUpdateIsDragging(false)

                onDragCancelAfterMove()

                onToast()
            } else if (isLongPress && moveGridItemResult != null) {
                onUpdateIsLongPress(false)

                onUpdateIsDragging(false)

                onDragEndAfterMove(moveGridItemResult)
            }
        }

        is GridItemSource.New -> {
            if (lockMovement) {
                onUpdateIsLongPress(false)

                onUpdateIsDragging(false)

                onDragCancelAfterMove()

                onToast()
            } else if (isLongPress && isDragging && (drag == Drag.Cancel || moveGridItemResult == null || !moveGridItemResult.isSuccess)) {
                onUpdateIsLongPress(false)

                onUpdateIsDragging(false)

                onDragCancelAfterMove()

                onToast()
            } else if (isLongPress && isDragging && moveGridItemResult != null) {
                onUpdateIsLongPress(false)

                onUpdateIsDragging(false)

                when (val data = gridItemSource.gridItem.data) {
                    is GridItemData.Widget -> {
                        onDragEndWidget(
                            androidAppWidgetHostWrapper = androidAppWidgetHostWrapper,
                            androidAppWidgetManagerWrapper = androidAppWidgetManagerWrapper,
                            data = data,
                            gridItem = gridItemSource.gridItem,
                            onLaunchWidgetIntent = onLaunchWidgetIntent,
                            onUpdateAppWidgetId = onUpdateAppWidgetId,
                            onUpdateWidgetGridItem = onUpdateWidgetGridItem,
                        )
                    }

                    is GridItemData.ShortcutConfig -> {
                        onDragEndShortcutConfig(
                            androidLauncherAppsWrapper = androidLauncherAppsWrapper,
                            androidUserManagerWrapper = androidUserManagerWrapper,
                            data = data,
                            gridItem = gridItemSource.gridItem,
                            onDeleteGridItemCache = onDeleteGridItemCache,
                            onLaunchShortcutConfigIntent = onLaunchShortcutConfigIntent,
                            onLaunchShortcutConfigIntentSenderRequest = onLaunchShortcutConfigIntentSenderRequest,
                        )
                    }

                    is GridItemData.ApplicationInfo,
                    is GridItemData.Folder,
                    is GridItemData.ShortcutInfo,
                    -> {
                        onDragEndAfterMove(moveGridItemResult)
                    }
                }
            }
        }

        is GridItemSource.Pin -> {
            if (lockMovement) {
                onUpdateIsLongPress(false)

                onUpdateIsDragging(false)

                onDragCancelAfterMove()

                onToast()
            } else if (isDragging && isLongPress && (drag == Drag.Cancel || moveGridItemResult == null || !moveGridItemResult.isSuccess)) {
                onUpdateIsLongPress(false)

                onUpdateIsDragging(false)

                onDragCancelAfterMove()

                onToast()
            } else if (isDragging && isLongPress && moveGridItemResult != null) {
                onUpdateIsLongPress(false)

                onUpdateIsDragging(false)

                when (val data = gridItemSource.gridItem.data) {
                    is GridItemData.ShortcutInfo -> {
                        onDragEndPinShortcut(
                            gridItem = gridItemSource.gridItem,
                            moveGridItemResult = moveGridItemResult,
                            pinItemRequest = gridItemSource.pinItemRequest,
                            onDeleteGridItemCache = onDeleteGridItemCache,
                            onDragEndAfterMove = onDragEndAfterMove,
                        )
                    }

                    is GridItemData.Widget -> {
                        onDragEndWidget(
                            androidAppWidgetHostWrapper = androidAppWidgetHostWrapper,
                            androidAppWidgetManagerWrapper = androidAppWidgetManagerWrapper,
                            data = data,
                            gridItem = gridItemSource.gridItem,
                            onLaunchWidgetIntent = onLaunchWidgetIntent,
                            onUpdateAppWidgetId = onUpdateAppWidgetId,
                            onUpdateWidgetGridItem = onUpdateWidgetGridItem,
                        )
                    }

                    else -> error("Expected GridItemData.ShortcutInfo or GridItemData.Widget")
                }
            }
        }

        is GridItemSource.Folder -> {
            if (lockMovement) {
                onUpdateIsLongPress(false)

                onUpdateIsDragging(false)

                onDragCancelAfterMove()

                onToast()
            } else if (isLongPress && !isDragging) {
                onUpdateIsLongPress(false)
            } else if (isLongPress && drag == Drag.Cancel) {
                onUpdateIsLongPress(false)

                onUpdateIsDragging(false)

                onDragCancelAfterMove()

                onToast()
            } else if (isLongPress) {
                onUpdateIsLongPress(false)

                onUpdateIsDragging(false)

                onDragEndAfterMoveFolder()
            }
        }
    }
}

internal fun handleAppWidgetLauncherResult(
    androidAppWidgetManagerWrapper: AndroidAppWidgetManagerWrapper,
    gridItemSource: GridItemSource?,
    result: ActivityResult,
    onDeleteAppWidgetId: () -> Unit,
    onUpdateWidgetGridItem: (GridItem) -> Unit,
) {
    if (gridItemSource == null) return

    val data = (gridItemSource.gridItem.data as? GridItemData.Widget)
        ?: error("Expected GridItemData.Widget")

    if (result.resultCode == Activity.RESULT_OK) {
        val appWidgetId = result.data?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1) ?: -1

        val options = Bundle().apply {
            putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, data.minWidth)
            putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, data.minHeight)
            putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, data.minWidth)
            putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, data.minHeight)
        }

        androidAppWidgetManagerWrapper.updateAppWidgetOptions(
            appWidgetId = appWidgetId,
            options = options,
        )

        val newData = data.copy(appWidgetId = appWidgetId)

        onUpdateWidgetGridItem(gridItemSource.gridItem.copy(data = newData))
    } else {
        onDeleteAppWidgetId()
    }
}

internal fun handleConfigureLauncherResultEffect(
    moveGridItemResult: MoveGridItemResult?,
    resultCode: Int?,
    updatedGridItem: GridItem?,
    onDeleteWidgetGridItemCache: (
        gridItem: GridItem,
        appWidgetId: Int,
    ) -> Unit,
    onDragEndAfterMoveWidgetGridItem: (MoveGridItemResult) -> Unit,
    onResetConfigureResultCode: () -> Unit,
) {
    if (resultCode == null || moveGridItemResult == null || updatedGridItem == null) {
        return
    }

    val data =
        (updatedGridItem.data as? GridItemData.Widget) ?: error("Expected GridItemData.Widget")

    if (resultCode == Activity.RESULT_OK) {
        onDragEndAfterMoveWidgetGridItem(moveGridItemResult.copy(movingGridItem = updatedGridItem))
    } else {
        onDeleteWidgetGridItemCache(updatedGridItem, data.appWidgetId)
    }

    onResetConfigureResultCode()
}

internal fun handleDeleteAppWidgetId(
    appWidgetId: Int,
    deleteAppWidgetId: Boolean,
    gridItemSource: GridItemSource?,
    onDeleteWidgetGridItemCache: (
        gridItem: GridItem,
        appWidgetId: Int,
    ) -> Unit,
    onResetAppWidgetId: () -> Unit,
) {
    if (gridItemSource == null || appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID || !deleteAppWidgetId) {
        return
    }

    check(gridItemSource.gridItem.data is GridItemData.Widget)

    onDeleteWidgetGridItemCache(gridItemSource.gridItem, appWidgetId)

    onResetAppWidgetId()
}

internal fun handleBoundWidgetEffect(
    activity: Activity?,
    androidAppWidgetHostWrapper: AndroidAppWidgetHostWrapper,
    gridItemSource: GridItemSource?,
    moveGridItemResult: MoveGridItemResult?,
    updatedWidgetGridItem: GridItem?,
    onDeleteGridItemCache: (GridItem) -> Unit,
    onDeleteWidgetGridItemCache: (
        gridItem: GridItem,
        appWidgetId: Int,
    ) -> Unit,
    onDragEndAfterMoveWidgetGridItem: (MoveGridItemResult) -> Unit,
) {
    if (gridItemSource == null || moveGridItemResult == null) return

    val data = (updatedWidgetGridItem?.data as? GridItemData.Widget)
        ?: error("Expected GridItemData.Widget")

    when (gridItemSource) {
        is GridItemSource.New -> {
            startAppWidgetConfigureActivityForResult(
                activity = activity,
                androidAppWidgetHostWrapper = androidAppWidgetHostWrapper,
                appWidgetId = data.appWidgetId,
                configure = data.configure,
                moveGridItemResult = moveGridItemResult,
                updatedWidgetGridItem = updatedWidgetGridItem,
                onDeleteWidgetGridItemCache = onDeleteWidgetGridItemCache,
                onDragEndAfterMoveWidgetGridItem = onDragEndAfterMoveWidgetGridItem,
            )
        }

        is GridItemSource.Pin -> {
            bindPinWidget(
                appWidgetId = data.appWidgetId,
                moveGridItemResult = moveGridItemResult,
                pinItemRequest = gridItemSource.pinItemRequest,
                updatedWidgetGridItem = updatedWidgetGridItem,
                onDeleteGridItemCache = onDeleteGridItemCache,
                onDragEndAfterMove = onDragEndAfterMoveWidgetGridItem,
            )
        }

        else -> Unit
    }
}

@Suppress("DEPRECATION")
internal suspend fun handleShortcutConfigLauncherResult(
    androidImageSerializer: AndroidImageSerializer,
    gridItemSource: GridItemSource?,
    moveGridItemResult: MoveGridItemResult?,
    result: ActivityResult,
    onDeleteGridItemCache: (GridItem) -> Unit,
    onUpdateShortcutConfigGridItemDataCache: (
        byteArray: ByteArray?,
        moveGridItemResult: MoveGridItemResult,
        gridItem: GridItem,
        data: GridItemData.ShortcutConfig,
    ) -> Unit,
) {
    if (gridItemSource == null || moveGridItemResult == null) return

    if (result.resultCode == Activity.RESULT_CANCELED) {
        onDeleteGridItemCache(gridItemSource.gridItem)

        return
    }

    val name = result.data?.getStringExtra(Intent.EXTRA_SHORTCUT_NAME)

    val icon = result.data?.let { intent ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(
                Intent.EXTRA_SHORTCUT_ICON,
                Bitmap::class.java,
            )
        } else {
            intent.getParcelableExtra(Intent.EXTRA_SHORTCUT_ICON)
        }
    }?.let { bitmap ->
        androidImageSerializer.createByteArray(bitmap = bitmap)
    }

    val shortcutIntentUri = result.data?.let { intent ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(
                Intent.EXTRA_SHORTCUT_INTENT,
                Intent::class.java,
            )
        } else {
            intent.getParcelableExtra(Intent.EXTRA_SHORTCUT_INTENT)
        }
    }?.toUri(Intent.URI_INTENT_SCHEME)

    val data = (gridItemSource.gridItem.data as? GridItemData.ShortcutConfig)
        ?: error("Expected GridItemData.ShortcutConfig")

    onUpdateShortcutConfigGridItemDataCache(
        icon,
        moveGridItemResult,
        gridItemSource.gridItem,
        data.copy(
            shortcutIntentName = name,
            shortcutIntentUri = shortcutIntentUri,
        ),
    )
}

@Suppress("DEPRECATION")
internal suspend fun handleShortcutConfigIntentSenderLauncherResult(
    androidImageSerializer: AndroidImageSerializer,
    androidLauncherAppsWrapper: AndroidLauncherAppsWrapper,
    androidUserManagerWrapper: AndroidUserManagerWrapper,
    fileManager: FileManager,
    gridItemSource: GridItemSource?,
    moveGridItemResult: MoveGridItemResult?,
    result: ActivityResult,
    onDeleteGridItemCache: (GridItem) -> Unit,
    onUpdateShortcutConfigIntoShortcutInfoGridItem: (
        moveGridItemResult: MoveGridItemResult,
        pinItemRequestType: PinItemRequestType.ShortcutInfo,
    ) -> Unit,
) {
    if (gridItemSource == null || moveGridItemResult == null) return

    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || result.resultCode == Activity.RESULT_CANCELED) {
        onDeleteGridItemCache(gridItemSource.gridItem)

        return
    }

    val pinItemRequest = result.data?.let { intent ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(
                LauncherApps.EXTRA_PIN_ITEM_REQUEST,
                PinItemRequest::class.java,
            )
        } else {
            intent.getParcelableExtra(LauncherApps.EXTRA_PIN_ITEM_REQUEST)
        }
    }

    val shortcutInfo = pinItemRequest?.shortcutInfo

    if (pinItemRequest != null && shortcutInfo != null && pinItemRequest.isValid && pinItemRequest.accept()) {
        val icon = androidLauncherAppsWrapper.getShortcutIconDrawable(
            shortcutInfo = shortcutInfo,
            density = 0,
        )?.let { drawable ->
            val directory = fileManager.getFilesDirectory(FileManager.SHORTCUTS_DIR)

            val file = File(
                directory,
                fileManager.getHashedFileName(name = shortcutInfo.id),
            )

            androidImageSerializer.createDrawablePath(drawable = drawable, file = file)

            file.absolutePath
        }

        val pinItemRequestType = PinItemRequestType.ShortcutInfo(
            serialNumber = androidUserManagerWrapper.getSerialNumberForUser(userHandle = shortcutInfo.userHandle),
            shortcutId = shortcutInfo.id,
            packageName = shortcutInfo.`package`,
            shortLabel = shortcutInfo.shortLabel.toString(),
            longLabel = shortcutInfo.longLabel.toString(),
            isEnabled = shortcutInfo.isEnabled,
            disabledMessage = shortcutInfo.disabledMessage?.toString(),
            icon = icon,
        )

        onUpdateShortcutConfigIntoShortcutInfoGridItem(
            moveGridItemResult,
            pinItemRequestType,
        )
    } else {
        onDeleteGridItemCache(gridItemSource.gridItem)
    }
}

private fun onDragEndWidget(
    androidAppWidgetHostWrapper: AndroidAppWidgetHostWrapper,
    androidAppWidgetManagerWrapper: AndroidAppWidgetManagerWrapper,
    data: GridItemData.Widget,
    gridItem: GridItem,
    onLaunchWidgetIntent: (Intent) -> Unit,
    onUpdateAppWidgetId: (Int) -> Unit,
    onUpdateWidgetGridItem: (GridItem) -> Unit,
) {
    val appWidgetId = androidAppWidgetHostWrapper.allocateAppWidgetId()

    onUpdateAppWidgetId(appWidgetId)

    val provider = ComponentName.unflattenFromString(data.componentName)

    val bindAppWidgetIdIfAllowed = androidAppWidgetManagerWrapper.bindAppWidgetIdIfAllowed(
        appWidgetId = appWidgetId,
        provider = provider,
    )

    if (bindAppWidgetIdIfAllowed) {
        val options = Bundle().apply {
            putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, data.minWidth)
            putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, data.minHeight)
            putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, data.minWidth)
            putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, data.minHeight)
        }

        androidAppWidgetManagerWrapper.updateAppWidgetOptions(
            appWidgetId = appWidgetId,
            options = options,
        )

        val newData = data.copy(appWidgetId = appWidgetId)

        onUpdateWidgetGridItem(gridItem.copy(data = newData))
    } else {
        val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_BIND).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)

            putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, provider)
        }

        onLaunchWidgetIntent(intent)
    }
}

private fun onDragEndPinShortcut(
    gridItem: GridItem,
    moveGridItemResult: MoveGridItemResult,
    pinItemRequest: PinItemRequest?,
    onDeleteGridItemCache: (GridItem) -> Unit,
    onDragEndAfterMove: (MoveGridItemResult) -> Unit,
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && pinItemRequest != null && pinItemRequest.isValid && pinItemRequest.accept()) {
        onDragEndAfterMove(moveGridItemResult)
    } else {
        onDeleteGridItemCache(gridItem)
    }
}

private fun bindPinWidget(
    appWidgetId: Int,
    moveGridItemResult: MoveGridItemResult,
    pinItemRequest: PinItemRequest,
    updatedWidgetGridItem: GridItem,
    onDeleteGridItemCache: (GridItem) -> Unit,
    onDragEndAfterMove: (MoveGridItemResult) -> Unit,
) {
    val extras = Bundle().apply {
        putInt(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && pinItemRequest.isValid && pinItemRequest.accept(
            extras,
        )
    ) {
        onDragEndAfterMove(moveGridItemResult.copy(movingGridItem = updatedWidgetGridItem))
    } else {
        onDeleteGridItemCache(updatedWidgetGridItem)
    }
}

private suspend fun onDragEndShortcutConfig(
    androidLauncherAppsWrapper: AndroidLauncherAppsWrapper,
    androidUserManagerWrapper: AndroidUserManagerWrapper,
    data: GridItemData.ShortcutConfig,
    gridItem: GridItem,
    onDeleteGridItemCache: (GridItem) -> Unit,
    onLaunchShortcutConfigIntent: (Intent) -> Unit,
    onLaunchShortcutConfigIntentSenderRequest: (IntentSenderRequest) -> Unit,
) {
    val serialNumber =
        androidUserManagerWrapper.getSerialNumberForUser(userHandle = Process.myUserHandle())

    if (serialNumber == data.serialNumber) {
        val intent = Intent(Intent.ACTION_CREATE_SHORTCUT).setComponent(
            ComponentName.unflattenFromString(data.componentName),
        )

        try {
            onLaunchShortcutConfigIntent(intent)
        } catch (_: ActivityNotFoundException) {
            onDeleteGridItemCache(gridItem)
        }
    } else {
        val shortcutConfigIntent = androidLauncherAppsWrapper.getShortcutConfigIntent(
            serialNumber = data.serialNumber,
            packageName = data.packageName,
            componentName = data.componentName,
        )

        if (shortcutConfigIntent != null) {
            val intentSenderRequest = IntentSenderRequest.Builder(shortcutConfigIntent).build()

            onLaunchShortcutConfigIntentSenderRequest(intentSenderRequest)
        } else {
            onDeleteGridItemCache(gridItem)
        }
    }
}

private fun startAppWidgetConfigureActivityForResult(
    activity: Activity?,
    androidAppWidgetHostWrapper: AndroidAppWidgetHostWrapper,
    appWidgetId: Int,
    configure: String?,
    moveGridItemResult: MoveGridItemResult,
    updatedWidgetGridItem: GridItem,
    onDeleteWidgetGridItemCache: (
        gridItem: GridItem,
        appWidgetId: Int,
    ) -> Unit,
    onDragEndAfterMoveWidgetGridItem: (MoveGridItemResult) -> Unit,
) {
    val configureComponent = configure?.let(ComponentName::unflattenFromString)

    try {
        if (activity != null && configureComponent != null) {
            androidAppWidgetHostWrapper.startAppWidgetConfigureActivityForResult(
                activity,
                appWidgetId,
                0,
                AndroidAppWidgetHostWrapper.CONFIGURE_REQUEST_CODE,
                null,
            )
        } else {
            onDragEndAfterMoveWidgetGridItem(moveGridItemResult.copy(movingGridItem = updatedWidgetGridItem))
        }
    } catch (_: ActivityNotFoundException) {
        onDeleteWidgetGridItemCache(updatedWidgetGridItem, appWidgetId)
    } catch (_: SecurityException) {
        onDeleteWidgetGridItemCache(updatedWidgetGridItem, appWidgetId)
    }
}
