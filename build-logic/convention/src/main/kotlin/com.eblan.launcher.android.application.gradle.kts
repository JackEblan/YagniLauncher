import com.eblan.launcher.configureAndroid
import com.eblan.launcher.configureCompose
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose)
}

android {
    configureAndroid()
    configureCompose(this)

    defaultConfig {
        targetSdk = 36
    }

    packaging {
        jniLibs.keepDebugSymbols.add("**/*.so")
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
}