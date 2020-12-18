package com.jetbrains.snakecharm.codeInsight.completion.wrapper

import kotlinx.serialization.Serializable
import java.util.*

class SmkWrapperStorage  {
    var version = ""
        private set

    var wrappers: List<WrapperInfo> = emptyList()
        private set

    fun initFrom(
        version: String,
        wrappers: List<WrapperInfo>
    ) {
        this.version = version
        this.wrappers = Collections.unmodifiableList(wrappers)
    }

    @Serializable
    data class WrapperInfo(
        val path: String = "",
        val args: Map<String, List<String>> = emptyMap(),
        val description: String = ""
    )
}
