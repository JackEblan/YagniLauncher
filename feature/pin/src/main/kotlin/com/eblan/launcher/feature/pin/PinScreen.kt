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
package com.eblan.launcher.feature.pin

import android.appwidget.AppWidgetManager
import android.content.ClipData
import android.content.pm.LauncherApps.PinItemRequest
import android.os.Build
import android.view.View
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.draganddrop.dragAndDropSource
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropTransferData
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.eblan.launcher.domain.framework.FileManager
import com.eblan.launcher.domain.model.GridItem
import com.eblan.launcher.ui.local.LocalAppWidgetHost
import com.eblan.launcher.ui.local.LocalAppWidgetManager
import com.eblan.launcher.ui.local.LocalFileManager
import com.eblan.launcher.ui.local.LocalIconKeyGenerator
import com.eblan.launcher.ui.local.LocalImageSerializer
import com.eblan.launcher.ui.local.LocalLauncherApps
import com.eblan.launcher.ui.local.LocalPinItemRequest
import com.eblan.launcher.ui.local.LocalUserManager
import kotlinx.coroutines.launch
import java.io.File

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun PinScreen(
    modifier: Modifier = Modifier,
    pinItemRequest: PinItemRequest,
    viewModel: PinScreenViewModel = hiltViewModel(),
    onDragStart: () -> Unit,
    onFinish: () -> Unit,
) {
    val gridItem by viewModel.gridItem.collectAsStateWithLifecycle()

    val isBoundWidget by viewModel.isBoundWidget.collectAsStateWithLifecycle()

    val isFinished by viewModel.isFinished.collectAsStateWithLifecycle()

    when (pinItemRequest.requestType) {
        PinItemRequest.REQUEST_TYPE_APPWIDGET -> {
            PinWidgetScreen(
                modifier = modifier,
                gridItem = gridItem,
                isBoundWidget = isBoundWidget,
                isFinished = isFinished,
                pinItemRequest = pinItemRequest,
                onAddPinWidgetToHomeScreen = viewModel::addPinWidgetToHomeScreen,
                onDeleteGridItemCache = viewModel::deleteGridItemCache,
                onDragStart = onDragStart,
                onFinish = onFinish,
                onUpdateGridItemCache = viewModel::updateGridItemDataCache,
                onUpdateGridItems = viewModel::updateGridItems,
            )
        }

        PinItemRequest.REQUEST_TYPE_SHORTCUT -> {
            PinShortcutScreen(
                modifier = modifier,
                gridItem = gridItem,
                isFinished = isFinished,
                pinItemRequest = pinItemRequest,
                onAddPinShortcutToHomeScreen = viewModel::addPinShortcutToHomeScreen,
                onDeleteShortcutGridItem = viewModel::deleteGridItemCache,
                onDragStart = onDragStart,
                onFinish = onFinish,
                onUpdateGridItems = viewModel::updateGridItems,
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun PinShortcutScreen(
    modifier: Modifier = Modifier,
    gridItem: GridItem?,
    isFinished: Boolean,
    pinItemRequest: PinItemRequest,
    onAddPinShortcutToHomeScreen: (
        serialNumber: Long,
        id: String,
        packageName: String,
        shortLabel: String,
        longLabel: String,
        isEnabled: Boolean,
        icon: String?,
    ) -> Unit,
    onDeleteShortcutGridItem: (GridItem) -> Unit,
    onDragStart: () -> Unit,
    onFinish: () -> Unit,
    onUpdateGridItems: () -> Unit,
) {
    val pinItemRequestWrapper = LocalPinItemRequest.current

    val androidLauncherAppsWrapper = LocalLauncherApps.current

    val imageSerializer = LocalImageSerializer.current

    val shortcutInfo = pinItemRequest.shortcutInfo

    val androidUserManagerWrapper = LocalUserManager.current

    val fileManager = LocalFileManager.current

    val context = LocalContext.current

    val iconKeyGenerator = LocalIconKeyGenerator.current

    val scope = rememberCoroutineScope()

    if (shortcutInfo != null) {
        val icon = remember {
            androidLauncherAppsWrapper.getShortcutBadgedIconDrawable(
                shortcutInfo = shortcutInfo,
                density = 0,
            )
        }

        LaunchedEffect(key1 = gridItem) {
            if (gridItem == null) return@LaunchedEffect

            if (pinItemRequest.isValid && pinItemRequest.accept()) {
                Toast.makeText(
                    context,
                    """
                ${gridItem.page}
                ${gridItem.startRow}
                ${gridItem.startColumn}
                    """.trimIndent(),
                    Toast.LENGTH_LONG,
                ).show()

                onUpdateGridItems()
            } else {
                onDeleteShortcutGridItem(gridItem)
            }
        }

        LaunchedEffect(key1 = isFinished) {
            if (isFinished) {
                onFinish()
            }
        }

        Scaffold(containerColor = Color.Transparent) { paddingValues ->
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .padding(paddingValues),
            ) {
                PinBottomSheet(
                    icon = icon,
                    label = shortcutInfo.shortLabel.toString(),
                    onAdd = {
                        scope.launch {
                            val serialNumber =
                                androidUserManagerWrapper.getSerialNumberForUser(userHandle = shortcutInfo.userHandle)

                            val icon = androidLauncherAppsWrapper.getShortcutBadgedIconDrawable(
                                shortcutInfo = shortcutInfo,
                                density = 0,
                            )?.let { drawable ->
                                val directory =
                                    fileManager.getFilesDirectory(FileManager.SHORTCUTS_DIR)

                                val file = File(
                                    directory,
                                    iconKeyGenerator.getShortcutIconKey(
                                        serialNumber = serialNumber,
                                        packageName = shortcutInfo.`package`,
                                        id = shortcutInfo.id,
                                    ),
                                )

                                imageSerializer.createDrawablePath(drawable = drawable, file = file)

                                file.absolutePath
                            }

                            onAddPinShortcutToHomeScreen(
                                serialNumber,
                                shortcutInfo.id,
                                shortcutInfo.`package`,
                                shortcutInfo.shortLabel.toString(),
                                shortcutInfo.longLabel.toString(),
                                shortcutInfo.isEnabled,
                                icon,
                            )
                        }
                    },
                    onFinish = onFinish,
                    onLongPress = {
                        pinItemRequestWrapper.updatePinItemRequest(
                            pinItemRequest = pinItemRequest,
                        )

                        onDragStart()

                        onFinish()
                    },
                )
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun PinWidgetScreen(
    modifier: Modifier = Modifier,
    gridItem: GridItem?,
    isBoundWidget: Boolean,
    isFinished: Boolean,
    pinItemRequest: PinItemRequest,
    onAddPinWidgetToHomeScreen: (
        serialNumber: Long,
        componentName: String,
        configure: String?,
        packageName: String,
        targetCellHeight: Int,
        targetCellWidth: Int,
        minWidth: Int,
        minHeight: Int,
        resizeMode: Int,
        minResizeWidth: Int,
        minResizeHeight: Int,
        maxResizeWidth: Int,
        maxResizeHeight: Int,
        rootWidth: Int,
        rootHeight: Int,
        preview: String?,
    ) -> Unit,
    onDeleteGridItemCache: (GridItem) -> Unit,
    onDragStart: () -> Unit,
    onFinish: () -> Unit,
    onUpdateGridItemCache: (GridItem) -> Unit,
    onUpdateGridItems: () -> Unit,
) {
    val pinItemRequestWrapper = LocalPinItemRequest.current

    val appWidgetHostWrapper = LocalAppWidgetHost.current

    val appWidgetManager = LocalAppWidgetManager.current

    val userManager = LocalUserManager.current

    val context = LocalContext.current

    val fileManager = LocalFileManager.current

    val iconKeyGenerator = LocalIconKeyGenerator.current

    val paddingValues = WindowInsets.safeDrawing.asPaddingValues()

    val appWidgetProviderInfo = pinItemRequest.getAppWidgetProviderInfo(context)

    var appWidgetId by remember { mutableIntStateOf(AppWidgetManager.INVALID_APPWIDGET_ID) }

    var deleteAppWidgetId by remember { mutableStateOf(false) }

    if (appWidgetProviderInfo != null) {
        val icon = remember {
            appWidgetProviderInfo.loadPreviewImage(context, 0)
        }

        val appWidgetLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult(),
        ) { result ->
            handleAppWidgetLauncherResult(
                gridItem = gridItem,
                result = result,
                onDeleteAppWidgetId = {
                    deleteAppWidgetId = true
                },
                onUpdateGridItemCache = onUpdateGridItemCache,
            )
        }

        LaunchedEffect(key1 = gridItem) {
            handleGridItem(
                appWidgetHostWrapper = appWidgetHostWrapper,
                appWidgetManager = appWidgetManager,
                gridItem = gridItem,
                userHandle = appWidgetProviderInfo.profile,
                onAddedToHomeScreenToast = { message ->
                    Toast.makeText(
                        context,
                        message,
                        Toast.LENGTH_LONG,
                    ).show()
                },
                onLaunch = appWidgetLauncher::launch,
                onUpdateAppWidgetId = { newAppWidgetId ->
                    appWidgetId = newAppWidgetId
                },
                onUpdateGridItemCache = onUpdateGridItemCache,
            )
        }

        LaunchedEffect(key1 = deleteAppWidgetId) {
            handleDeleteAppWidgetId(
                appWidgetId = appWidgetId,
                deleteAppWidgetId = deleteAppWidgetId,
                gridItem = gridItem,
                onDeleteGridItem = onDeleteGridItemCache,
            )
        }

        LaunchedEffect(key1 = isBoundWidget) {
            handleIsBoundWidget(
                appWidgetId = appWidgetId,
                gridItem = gridItem,
                isBoundWidget = isBoundWidget,
                pinItemRequest = pinItemRequest,
                onDeleteGridItem = onDeleteGridItemCache,
                onUpdateGridItems = onUpdateGridItems,
            )
        }

        LaunchedEffect(key1 = isFinished) {
            if (isFinished) {
                onFinish()
            }
        }

        BoxWithConstraints(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            PinBottomSheet(
                icon = icon,
                label = appWidgetProviderInfo.loadLabel(context.packageManager),
                onAdd = {
                    val componentName = appWidgetProviderInfo.provider.flattenToString()

                    val directory =
                        fileManager.getFilesDirectory(FileManager.WIDGETS_DIR)

                    val file = File(
                        directory,
                        iconKeyGenerator.getHashedName(name = componentName),
                    )

                    val preview = file.absolutePath

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        onAddPinWidgetToHomeScreen(
                            userManager.getSerialNumberForUser(userHandle = appWidgetProviderInfo.profile),
                            componentName,
                            appWidgetProviderInfo.configure.flattenToString(),
                            appWidgetProviderInfo.provider.packageName,
                            appWidgetProviderInfo.targetCellHeight,
                            appWidgetProviderInfo.targetCellWidth,
                            appWidgetProviderInfo.minWidth,
                            appWidgetProviderInfo.minHeight,
                            appWidgetProviderInfo.resizeMode,
                            appWidgetProviderInfo.minResizeWidth,
                            appWidgetProviderInfo.minResizeHeight,
                            appWidgetProviderInfo.maxResizeWidth,
                            appWidgetProviderInfo.maxResizeHeight,
                            constraints.maxWidth,
                            constraints.maxHeight,
                            preview,
                        )
                    } else {
                        onAddPinWidgetToHomeScreen(
                            userManager.getSerialNumberForUser(userHandle = appWidgetProviderInfo.profile),
                            appWidgetProviderInfo.provider.flattenToString(),
                            appWidgetProviderInfo.configure.flattenToString(),
                            appWidgetProviderInfo.provider.packageName,
                            0,
                            0,
                            appWidgetProviderInfo.minWidth,
                            appWidgetProviderInfo.minHeight,
                            appWidgetProviderInfo.resizeMode,
                            appWidgetProviderInfo.minResizeWidth,
                            appWidgetProviderInfo.minResizeHeight,
                            0,
                            0,
                            constraints.maxWidth,
                            constraints.maxHeight,
                            preview,
                        )
                    }
                },
                onFinish = onFinish,
                onLongPress = {
                    pinItemRequestWrapper.updatePinItemRequest(
                        pinItemRequest = pinItemRequest,
                    )

                    onDragStart()

                    onFinish()
                },
            )
        }
    }
}

@Suppress("DEPRECATION")
@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun PinBottomSheet(
    modifier: Modifier = Modifier,
    icon: Any?,
    label: String,
    onAdd: suspend () -> Unit,
    onFinish: () -> Unit,
    onLongPress: () -> Unit,
) {
    var showBottomSheet by remember { mutableStateOf(true) }

    val sheetState = rememberModalBottomSheetState()

    val scope = rememberCoroutineScope()

    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                showBottomSheet = false

                onFinish()
            },
            sheetState = sheetState,
        ) {
            Column(
                modifier = modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(text = label, style = MaterialTheme.typography.bodyLarge)

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = "Touch and hold the widget to move it around the home screen",
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(10.dp))

                AsyncImage(
                    modifier = Modifier
                        .dragAndDropSource(
                            block = {
                                detectTapGestures(
                                    onLongPress = {
                                        startTransfer(
                                            DragAndDropTransferData(
                                                clipData = ClipData.newPlainText(
                                                    "PinItemRequest",
                                                    "PinItemRequest",
                                                ),
                                                flags = View.DRAG_FLAG_GLOBAL,
                                            ),
                                        )

                                        onLongPress()
                                    },
                                )
                            },
                        )
                        .size(100.dp),
                    model = icon,
                    contentDescription = null,
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    Button(
                        onClick = {
                            scope.launch { sheetState.hide() }.invokeOnCompletion {
                                if (!sheetState.isVisible) {
                                    showBottomSheet = false

                                    onFinish()
                                }
                            }
                        },
                    ) {
                        Text(text = "Cancel")
                    }

                    Button(
                        onClick = {
                            scope.launch {
                                onAdd()
                            }
                        },
                    ) {
                        Text(text = "Add")
                    }
                }
            }
        }
    }
}
