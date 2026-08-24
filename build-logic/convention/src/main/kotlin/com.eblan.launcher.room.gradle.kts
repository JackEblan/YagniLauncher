plugins {
    alias(libs.plugins.room)
    alias(libs.plugins.ksp)
}

ksp {
    arg("room.generateKotlin", "true")
}

room {
    // The schemas directory contains a schema file for each version of the Room database.
    // This is required to enable Room auto migrations.
    // See https://developer.android.com/reference/kotlin/androidx/room/AutoMigration.
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    add("implementation", libs.room.runtime)
    add("implementation", libs.room.ktx)
    add("ksp", libs.room.compiler)
}