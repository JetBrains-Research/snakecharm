package com.jetbrains.snakecharm.codeInsight.wrapper

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil

@State(name = "WrapperStorage", storages = [Storage("wrapper.xml")])
class WrapperStorage : PersistentStateComponent<WrapperStorage> {

    var wrapperStringStorage: MutableList<String> = mutableListOf()

    data class Wrapper(
            val repositoryTag: String,
            val pathToWrapperDirectory: String,
            val environmentFileContent: String,
            val metaFileContent: String,
            val testSnakefileContent: String
    ) {
        fun toList(): List<String> = listOf(
                repositoryTag,
                pathToWrapperDirectory,
                environmentFileContent,
                metaFileContent,
                testSnakefileContent
        )
    }

    companion object {
        fun getInstance(): WrapperStorage = ApplicationManager.getApplication().getComponent(WrapperStorage::class.java)
    }

    fun addWrapper(wrapper: Wrapper) = wrapperStringStorage.addAll(wrapper.toList())

    fun getWrapperList(): List<Wrapper> {
        val wrappers = mutableListOf<Wrapper>()
        // storing everything as list of strings because that was the only way to get it to serialize
        val wrapperSize = 5
        for (i in 0 until wrapperStringStorage.size step wrapperSize) {
            wrappers.add(Wrapper(
                    wrapperStringStorage[i],
                    wrapperStringStorage[i + 1],
                    wrapperStringStorage[i + 2],
                    wrapperStringStorage[i + 3],
                    wrapperStringStorage[i + 4]
            ))
        }
        return wrappers
    }

    override fun getState(): WrapperStorage? {
        return this
    }

    override fun loadState(state: WrapperStorage) {
        XmlSerializerUtil.copyBean(state, this)
    }
}
