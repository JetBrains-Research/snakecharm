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
        val name: String = "default_name",
        val path: String = "default_path",
        val firstTag: String = "0.64.0",
        val args: String = "",
        val description: String = "default_description",
        val author: String = "default_author"
    )

    private var wrapperStorage: List<Wrapper> = emptyList()

    fun getStorage() : List<Wrapper> {
        return wrapperStorage
    }

    fun setStorage(storage: List<Wrapper>) {
        wrapperStorage = storage
    }

    override fun getState(): SmkWrapperStorage? {
        return this
    }

    override fun loadState(state: SmkWrapperStorage) {
        XmlSerializerUtil.copyBean(state, this)
    }
}
