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

import com.eblan.launcher.libs
import kotlinx.benchmark.gradle.BenchmarksExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.invoke
import org.jetbrains.kotlin.allopen.gradle.AllOpenExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

class JvmBenchmarkLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = libs.plugins.kotlin.jvm.get().pluginId)
            apply(plugin = libs.plugins.kotlinx.benchmark.get().pluginId)
            apply(plugin = libs.plugins.kotlin.allopen.get().pluginId)

            extensions.configure<SourceSetContainer> {
                create("benchmarks")
            }

            extensions.configure<KotlinJvmProjectExtension> {
                sourceSets {
                    getByName("benchmarks") {
                        dependsOn(getByName("main"))
                    }
                }
            }

            extensions.configure<BenchmarksExtension> {
                targets {
                    register("benchmarks")
                }
            }

            extensions.configure<AllOpenExtension> {
                annotation("org.openjdk.jmh.annotations.State")
            }

            dependencies {
                add("benchmarksImplementation", libs.kotlinx.benchmark.runtime)
            }
        }
    }
}