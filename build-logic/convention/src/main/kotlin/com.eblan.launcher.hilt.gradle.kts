plugins {
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

dependencies {
    add("implementation", libs.hilt.android)
    add("ksp", libs.hilt.compiler)
}