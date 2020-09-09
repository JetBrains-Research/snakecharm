package com.jetbrains.snakecharm.codeInsight.completion.wrapper

//import com.intellij.util.xmlb.XmlSerializerUtil
import kotlinx.serialization.Serializable

// TODO: serialization doesn't work properly, see Cleanup XML serialization for wrappers #325
// TODO: need to re-implement it, e.g. don't store copy of bundled wrappers
// in xml, store only wrappers for custom location

//@State(name = "smk-wrapper-storage.xml")
//class SmkWrapperStorage : PersistentStateComponent<SmkWrapperStorage> {
class SmkWrapperStorage  {
    @Serializable
    data class Wrapper(
        val path: String = "",
        val args: Map<String, List<String>> = emptyMap(),
        val description: String = ""
    )

    var version = ""
    var wrappers: List<Wrapper> = emptyList()

//    override fun getState(): SmkWrapperStorage? {
//        return this
//    }
//
//    override fun loadState(state: SmkWrapperStorage) {
//        XmlSerializerUtil.copyBean(state, this)
//    }
}
