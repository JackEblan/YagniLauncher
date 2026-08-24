import com.eblan.launcher.configureAndroid
import com.eblan.launcher.configureCompose
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.compose)
}

android {
    configureAndroid()
    configureCompose(this)
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
}