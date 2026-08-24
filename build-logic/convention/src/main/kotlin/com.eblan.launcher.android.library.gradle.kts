import com.eblan.launcher.configureAndroid
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
}

android {
    configureAndroid()
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
}

androidComponents {
    beforeVariants {
        it.androidTest.enable = project.projectDir
            .resolve("src/androidTest")
            .exists()
    }
}