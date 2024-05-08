package com.jetbrains.snakecharm.framework

import org.yaml.snakeyaml.LoaderOptions
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.CustomClassLoaderConstructor
import org.yaml.snakeyaml.introspector.BeanAccess

class SmkFrameworkDeprecationProvider {

    init {
        val input = javaClass.getResourceAsStream(DEPRECATION_DATA_LOCATION)
        val yaml = Yaml(CustomClassLoaderConstructor(SmkDeprecationData::class.java.classLoader, LoaderOptions()))
        yaml.setBeanAccess(BeanAccess.FIELD) // it must be set to be able to set vals
        val deprecationData = yaml.loadAs(input, SmkDeprecationData::class.java)
    }

    companion object {
        const val DEPRECATION_DATA_LOCATION = "/DeprecationData.yaml"
    }
}


data class SmkDeprecationKeywordData(
    val name: String = "",
    val type: String = "",
    val parent: List<String> = emptyList(),
    val advice: String = "",
)

data class SmkDeprecationVersionData(
    val version: String = "",
    val deprecated: List<SmkDeprecationKeywordData> = emptyList(),
    val removed: List<SmkDeprecationKeywordData> = emptyList(),
    val introduced: List<SmkDeprecationKeywordData> = emptyList(),
)

data class SmkDeprecationData(
    val changelog: List<SmkDeprecationVersionData> = emptyList(),
)