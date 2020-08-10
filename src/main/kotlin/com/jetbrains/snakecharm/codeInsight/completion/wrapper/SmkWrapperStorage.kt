package com.jetbrains.snakecharm.codeInsight.completion.wrapper

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil

@State(name = "SmkWrapperStorage", storages = [Storage("smk-wrapper-storage.xml")])
class SmkWrapperStorage : PersistentStateComponent<SmkWrapperStorage> {
    companion object {
        fun getInstance(): SmkWrapperStorage =
                ApplicationManager.getApplication().getComponent(SmkWrapperStorage::class.java)
    }

    data class Wrapper(
        val name: String,
        val path: String,
        val firstTag: String,
        val args: Map<String, List<String>>,
        val description: String,
        val author: String
    )

    var wrapperStorage: List<Wrapper> = emptyList()

    override fun getState(): SmkWrapperStorage? {
        return this
    }

    override fun loadState(state: SmkWrapperStorage) {
        XmlSerializerUtil.copyBean(state, this)
    }
}
