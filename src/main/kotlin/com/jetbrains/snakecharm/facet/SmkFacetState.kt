package com.jetbrains.snakecharm.facet

data class SmkFacetState(
    val useBundledWrappersInfo: Boolean = true,
    val wrappersCustomSourcesFolder: String = "" // use system independent separators
)