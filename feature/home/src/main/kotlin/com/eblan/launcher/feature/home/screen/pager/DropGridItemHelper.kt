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
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.LauncherApps.PinItemRequest
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.IntentSenderRequest
import com.eblan.launcher.domain.common.IconKeyGenerator
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
    context: Context,
    drag: Drag,
    gridItemSource: GridItemSource?,
    isDragging: Boolean,
    moveGridItemResult: MoveGridItemResult?,
    lockMovement: Boolean,
    isVisibleOverlay: Boolean,
    onResetGridAfterDeleteGridItem: (GridItem) -> Unit,
    onDragCancelAfterMove: () -> Unit,
    onDragEndAfterMove: (MoveGridItemResult) -> Unit,
    onDragEndAfterMoveFolder: () -> Unit,
    onLaunchShortcutConfigIntent: (Intent) -> Unit,
    onLaunchShortcutConfigIntentSenderRequest: (IntentSenderRequest) -> Unit,
    onLaunchWidgetIntent: (Intent) -> Unit,
    onUpdateAppWidgetId: (Int) -> Unit,
    onUpdateIsDragging: (Boolean) -> Unit,
    onUpdateWidgetGridItem: (GridItem) -> Unit,
    onUpdateIsVisibleOverlay: (Boolean) -> Unit,
) {
    if (drag == Drag.None || drag == Drag.Start || drag == Drag.Dragging || gridItemSource == null) {
        return
    }

    fun cancelWithToast() {
        onUpdateIsVisibleOverlay(false)

        onUpdateIsDragging(false)

        onDragCancelAfterMove()

        Toast.makeText(
            context,
            "Please wait for the white box indicator",
            Toast.LENGTH_LONG,
        ).show()
    }

    val isLongPress = isVisibleOverlay && !isDragging

    val isMoveGridItemResultFailed =
        drag == Drag.Cancel || moveGridItemResult == null || !moveGridItemResult.isSuccess

    when (gridItemSource) {
        is GridItemSource.Existing -> {
            if (isLongPress) {
                onUpdateIsVisibleOverlay(false)

                return
            }

            if (isVisibleOverlay && isMoveGridItemResultFailed) return cancelWithToast()

            if (lockMovement) return cancelWithToast()

            if (isVisibleOverlay && moveGridItemResult != null) {
                onDragEndAfterMove(moveGridItemResult)

                onUpdateIsDragging(false)
            }
        }

        is GridItemSource.New -> {
            if (isVisibleOverlay && isDragging && isMoveGridItemResultFailed) return cancelWithToast()

            if (lockMovement) return cancelWithToast()

            if (isVisibleOverlay && isDragging && moveGridItemResult != null) {
                val movingGridItem = moveGridItemResult.movingGridItem

                when (val data = movingGridItem.data) {
                    is GridItemData.Widget -> {
                        onDragEndWidget(
                            androidAppWidgetHostWrapper = androidAppWidgetHostWrapper,
                            androidAppWidgetManagerWrapper = androidAppWidgetManagerWrapper,
                            data = data,
                            gridItem = movingGridItem,
                            onLaunchWidgetIntent = onLaunchWidgetIntent,
                            onUpdateAppWidgetId = onUpdateAppWidgetId,
                            onUpdateWidgetGridItem = onUpdateWidgetGridItem,
                            onUpdateIsVisibleOverlay = onUpdateIsVisibleOverlay,
                            onUpdateIsDragging = onUpdateIsDragging,
                        )
                    }

                    is GridItemData.ShortcutConfig -> {
                        onDragEndShortcutConfig(
                            androidLauncherAppsWrapper = androidLauncherAppsWrapper,
                            androidUserManagerWrapper = androidUserManagerWrapper,
                            data = data,
                            gridItem = movingGridItem,
                            onResetGridAfterDeleteGridItem = onResetGridAfterDeleteGridItem,
                            onLaunchShortcutConfigIntent = onLaunchShortcutConfigIntent,
                            onLaunchShortcutConfigIntentSenderRequest = onLaunchShortcutConfigIntentSenderRequest,
                            onUpdateIsVisibleOverlay = onUpdateIsVisibleOverlay,
                            onUpdateIsDragging = onUpdateIsDragging,
                        )
                    }

                    is GridItemData.ApplicationInfo,
                    is GridItemData.Folder,
                    is GridItemData.ShortcutInfo,
                    -> {
                        onDragEndAfterMove(moveGridItemResult)

                        onUpdateIsDragging(false)
                    }
                }
            }
        }

        is GridItemSource.Pin -> {
            if (isVisibleOverlay && isDragging && isMoveGridItemResultFailed) return cancelWithToast()

            if (lockMovement) return cancelWithToast()

            if (isVisibleOverlay && isDragging && moveGridItemResult != null) {
                val movingGridItem = moveGridItemResult.movingGridItem

                when (val data = movingGridItem.data) {
                    is GridItemData.ShortcutInfo -> onDragEndPinShortcut(
                        gridItem = movingGridItem,
                        moveGridItemResult = moveGridItemResult,
                        pinItemRequest = gridItemSource.pinItemRequest,
                        onDeleteGridItem = onResetGridAfterDeleteGridItem,
                        onDragEndAfterMove = onDragEndAfterMove,
                        onUpdateIsVisibleOverlay = onUpdateIsVisibleOverlay,
                        onUpdateIsDragging = onUpdateIsDragging,
                    )

                    is GridItemData.Widget -> onDragEndWidget(
                        androidAppWidgetHostWrapper = androidAppWidgetHostWrapper,
                        androidAppWidgetManagerWrapper = androidAppWidgetManagerWrapper,
                        data = data,
                        gridItem = movingGridItem,
                        onLaunchWidgetIntent = onLaunchWidgetIntent,
                        onUpdateAppWidgetId = onUpdateAppWidgetId,
                        onUpdateWidgetGridItem = onUpdateWidgetGridItem,
                        onUpdateIsVisibleOverlay = onUpdateIsVisibleOverlay,
                        onUpdateIsDragging = onUpdateIsDragging,
                    )

                    else -> error("Expected ShortcutInfo or Widget")
                }
            }
        }

        is GridItemSource.Folder -> {
            if (isLongPress) {
                onUpdateIsVisibleOverlay(false)

                return
            }

            if (lockMovement) return cancelWithToast()

            if (isVisibleOverlay) {
                onDragEndAfterMoveFolder()

                onUpdateIsDragging(false)
            }
        }
    }
}

internal fun handleAppWidgetLauncherResult(
    androidAppWidgetManagerWrapper: AndroidAppWidgetManagerWrapper,
    moveGridItemResult: MoveGridItemResult?,
    result: ActivityResult,
    onDeleteAppWidgetId: () -> Unit,
    onUpdateWidgetGridItem: (GridItem) -> Unit,
) {
    val movingGridItem = requireNotNull(moveGridItemResult?.movingGridItem)

    val data = (movingGridItem.data as? GridItemData.Widget)
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

        onUpdateWidgetGridItem(movingGridItem.copy(data = newData))
    } else {
        onDeleteAppWidgetId()
    }
}

internal fun handleConfigureLauncherResultEffect(
    moveGridItemResult: MoveGridItemResult?,
    resultCode: Int?,
    updatedGridItem: GridItem?,
    onDeleteGridItem: (GridItem) -> Unit,
    onDragEndAfterMove: (MoveGridItemResult) -> Unit,
    onResetConfigureResultCode: () -> Unit,
) {
    if (resultCode == null) {
        return
    }

    requireNotNull(moveGridItemResult)

    requireNotNull(updatedGridItem)

    check(updatedGridItem.data is GridItemData.Widget)

    if (resultCode == Activity.RESULT_OK) {
        onDragEndAfterMove(moveGridItemResult.copy(movingGridItem = updatedGridItem))
    } else {
        onDeleteGridItem(updatedGridItem)
    }

    onResetConfigureResultCode()
}

internal fun handleDeleteAppWidgetId(
    appWidgetId: Int,
    deleteAppWidgetId: Boolean,
    moveGridItemResult: MoveGridItemResult?,
    onResetGridAfterDeleteGridItem: (GridItem) -> Unit,
    onResetAppWidgetId: () -> Unit,
) {
    if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID ||
        !deleteAppWidgetId
    ) {
        return
    }

    requireNotNull(moveGridItemResult)

    val movingGridItem = moveGridItemResult.movingGridItem

    check(movingGridItem.data is GridItemData.Widget)

    onResetGridAfterDeleteGridItem(movingGridItem)

    onResetAppWidgetId()
}

internal fun handleBoundWidgetEffect(
    activity: Activity?,
    androidAppWidgetHostWrapper: AndroidAppWidgetHostWrapper,
    gridItemSource: GridItemSource?,
    moveGridItemResult: MoveGridItemResult?,
    updatedWidgetGridItem: GridItem?,
    onDeleteGridItem: (GridItem) -> Unit,
    onDragEndAfterMove: (MoveGridItemResult) -> Unit,
) {
    if (updatedWidgetGridItem == null) return

    requireNotNull(gridItemSource)

    requireNotNull(moveGridItemResult)

    val data = (updatedWidgetGridItem.data as? GridItemData.Widget)
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
                onDeleteGridItem = onDeleteGridItem,
                onDragEndAfterMove = onDragEndAfterMove,
            )
        }

        is GridItemSource.Pin -> {
            bindPinWidget(
                appWidgetId = data.appWidgetId,
                moveGridItemResult = moveGridItemResult,
                pinItemRequest = gridItemSource.pinItemRequest,
                updatedWidgetGridItem = updatedWidgetGridItem,
                onDeleteGridItem = onDeleteGridItem,
                onDragEndAfterMove = onDragEndAfterMove,
            )
        }

        else -> Unit
    }
}

@Suppress("DEPRECATION")
internal suspend fun handleShortcutConfigLauncherResult(
    androidImageSerializer: AndroidImageSerializer,
    moveGridItemResult: MoveGridItemResult?,
    result: ActivityResult,
    fileManager: FileManager,
    onDeleteGridItem: (GridItem) -> Unit,
    onDragEndAfterMove: (MoveGridItemResult) -> Unit,
) {
    requireNotNull(moveGridItemResult)

    val movingGridItem = moveGridItemResult.movingGridItem

    if (result.resultCode == Activity.RESULT_CANCELED) {
        onDeleteGridItem(movingGridItem)

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

    val movingData = (movingGridItem.data as? GridItemData.ShortcutConfig)
        ?: error("Expected GridItemData.ShortcutConfig")

    val shortcutIntentIcon = icon?.let { currentByteArray ->
        fileManager.updateAndGetFilePath(
            fileManager.getFilesDirectory(FileManager.SHORTCUT_INTENT_ICONS_DIR),
            movingGridItem.id,
            currentByteArray,
        )
    }

    val newData = movingData.copy(
        shortcutIntentName = name,
        shortcutIntentIcon = shortcutIntentIcon,
        shortcutIntentUri = shortcutIntentUri,
    )

    val newMovingGridItem = moveGridItemResult.movingGridItem.copy(data = newData)

    onDragEndAfterMove(
        moveGridItemResult.copy(movingGridItem = newMovingGridItem),
    )
}

@Suppress("DEPRECATION")
internal suspend fun handleShortcutConfigIntentSenderLauncherResult(
    androidImageSerializer: AndroidImageSerializer,
    androidLauncherAppsWrapper: AndroidLauncherAppsWrapper,
    androidUserManagerWrapper: AndroidUserManagerWrapper,
    fileManager: FileManager,
    moveGridItemResult: MoveGridItemResult?,
    result: ActivityResult,
    iconKeyGenerator: IconKeyGenerator,
    onDeleteGridItem: (GridItem) -> Unit,
    onUpdateShortcutConfigIntoShortcutInfoGridItem: (
        moveGridItemResult: MoveGridItemResult,
        pinItemRequestType: PinItemRequestType.ShortcutInfo,
    ) -> Unit,
) {
    requireNotNull(moveGridItemResult)

    val movingGridItem = moveGridItemResult.movingGridItem

    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || result.resultCode == Activity.RESULT_CANCELED) {
        onDeleteGridItem(movingGridItem)

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
        val serialNumber =
            androidUserManagerWrapper.getSerialNumberForUser(userHandle = shortcutInfo.userHandle)

        val icon = androidLauncherAppsWrapper.getShortcutBadgedIconDrawable(
            shortcutInfo = shortcutInfo,
            density = 0,
        )?.let { drawable ->
            val directory = fileManager.getFilesDirectory(FileManager.SHORTCUTS_DIR)

            val file = File(
                directory,
                iconKeyGenerator.getShortcutIconKey(
                    serialNumber = serialNumber,
                    packageName = shortcutInfo.`package`,
                    id = shortcutInfo.id,
                ),
            )

            androidImageSerializer.createDrawablePath(drawable = drawable, file = file)

            file.absolutePath
        }

        val pinItemRequestType = PinItemRequestType.ShortcutInfo(
            serialNumber = serialNumber,
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
        onDeleteGridItem(movingGridItem)
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
    onUpdateIsVisibleOverlay: (Boolean) -> Unit,
    onUpdateIsDragging: (Boolean) -> Unit,
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

    onUpdateIsDragging(false)

    onUpdateIsVisibleOverlay(false)
}

private fun onDragEndPinShortcut(
    gridItem: GridItem,
    moveGridItemResult: MoveGridItemResult,
    pinItemRequest: PinItemRequest?,
    onDeleteGridItem: (GridItem) -> Unit,
    onDragEndAfterMove: (MoveGridItemResult) -> Unit,
    onUpdateIsVisibleOverlay: (Boolean) -> Unit,
    onUpdateIsDragging: (Boolean) -> Unit,
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && pinItemRequest != null && pinItemRequest.isValid && pinItemRequest.accept()) {
        onDragEndAfterMove(moveGridItemResult)
    } else {
        onDeleteGridItem(gridItem)
    }

    onUpdateIsDragging(false)

    onUpdateIsVisibleOverlay(false)
}

private fun bindPinWidget(
    appWidgetId: Int,
    moveGridItemResult: MoveGridItemResult,
    pinItemRequest: PinItemRequest,
    updatedWidgetGridItem: GridItem,
    onDeleteGridItem: (GridItem) -> Unit,
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
        onDeleteGridItem(updatedWidgetGridItem)
    }
}

private suspend fun onDragEndShortcutConfig(
    androidLauncherAppsWrapper: AndroidLauncherAppsWrapper,
    androidUserManagerWrapper: AndroidUserManagerWrapper,
    data: GridItemData.ShortcutConfig,
    gridItem: GridItem,
    onResetGridAfterDeleteGridItem: (GridItem) -> Unit,
    onLaunchShortcutConfigIntent: (Intent) -> Unit,
    onLaunchShortcutConfigIntentSenderRequest: (IntentSenderRequest) -> Unit,
    onUpdateIsVisibleOverlay: (Boolean) -> Unit,
    onUpdateIsDragging: (Boolean) -> Unit,
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
            onResetGridAfterDeleteGridItem(gridItem)
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
            onResetGridAfterDeleteGridItem(gridItem)
        }
    }

    onUpdateIsDragging(false)

    onUpdateIsVisibleOverlay(false)
}

private fun startAppWidgetConfigureActivityForResult(
    activity: Activity?,
    androidAppWidgetHostWrapper: AndroidAppWidgetHostWrapper,
    appWidgetId: Int,
    configure: String?,
    moveGridItemResult: MoveGridItemResult,
    updatedWidgetGridItem: GridItem,
    onDeleteGridItem: (GridItem) -> Unit,
    onDragEndAfterMove: (MoveGridItemResult) -> Unit,
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
            onDragEndAfterMove(moveGridItemResult.copy(movingGridItem = updatedWidgetGridItem))
        }
    } catch (_: ActivityNotFoundException) {
        onDeleteGridItem(updatedWidgetGridItem)
    } catch (_: SecurityException) {
        onDeleteGridItem(updatedWidgetGridItem)
    }
}
