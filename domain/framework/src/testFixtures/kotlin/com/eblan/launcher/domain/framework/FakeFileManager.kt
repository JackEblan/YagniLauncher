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
package com.eblan.launcher.domain.framework

import java.io.File

class FakeFileManager(private val rootDirectory: File) : FileManager {
    override suspend fun getFilesDirectory(name: String): File = File(rootDirectory, name).apply {
        mkdirs()
    }

    override suspend fun updateAndGetFilePath(
        directory: File,
        name: String,
        byteArray: ByteArray,
    ): String? {
        val file = File(directory, name)

        file.parentFile?.mkdirs()
        file.writeBytes(byteArray)

        return file.absolutePath
    }
}
