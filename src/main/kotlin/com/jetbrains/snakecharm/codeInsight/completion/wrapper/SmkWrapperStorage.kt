package com.jetbrains.snakecharm.codeInsight.completion.wrapper

import com.intellij.openapi.components.*
import com.intellij.util.xmlb.XmlSerializerUtil

@State(name = "SmkWrapperStorage", storages = [Storage("smk-wrapper-storage.xml")])
class SmkWrapperStorage : PersistentStateComponent<SmkWrapperStorage> {
    data class Wrapper(
        val name: String = "",
        val path: String = "",
        val firstTag: String = "",
        val args: Map<String, List<String>> = emptyMap(),
        val description: String = "",
        val author: String = ""
    )

    var wrapperStorage: List<Wrapper> = emptyList()

    override fun getState(): SmkWrapperStorage? {
        return this
    }

    override fun loadState(state: SmkWrapperStorage) {
        XmlSerializerUtil.copyBean(state, this)
    }
}
