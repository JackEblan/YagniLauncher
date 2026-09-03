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
package com.eblan.launcher.domain.model

fun getHomeSettings() = HomeSettings(
    columns = 5,
    rows = 5,
    pageCount = 1,
    infiniteScroll = false,
    dockColumns = 5,
    dockRows = 1,
    dockHeight = 1,
    initialPage = 0,
    wallpaperScroll = false,
    gridItemSettings = getGridItemSettings(),
    lockScreenOrientation = false,
    dockPageCount = 1,
    dockInfiniteScroll = false,
    dockInitialPage = 0,
    addNewAppsToHomeScreen = false,
    folderCellWidth = 1,
    folderCellHeight = 1,
    maxFolderColumns = 4,
    maxFolderRows = 4,
    showPageIndicator = true,
    dockCustomBackgroundColor = 0,
    dockTopStartCornerRadius = 0,
    dockTopEndCornerRadius = 0,
    dockBottomStartCornerRadius = 0,
    dockBottomEndCornerRadius = 0,
    dockTopCornerPadding = 0,
    dockStartCornerPadding = 0,
    dockBottomCornerPadding = 0,
    dockEndCornerPadding = 0,
)
