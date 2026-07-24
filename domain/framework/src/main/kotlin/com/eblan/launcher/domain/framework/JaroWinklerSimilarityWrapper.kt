package com.eblan.launcher.domain.framework

interface JaroWinklerSimilarityWrapper {
    suspend fun apply(
        left: CharSequence,
        right: CharSequence,
    )
}