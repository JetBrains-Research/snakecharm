package com.jetbrains.snakecharm.framework

import com.jetbrains.snakecharm.lang.SmkVersion
import org.yaml.snakeyaml.LoaderOptions
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.CustomClassLoaderConstructor
import org.yaml.snakeyaml.introspector.BeanAccess
import java.io.InputStream
import java.util.*

class SmkFrameworkDeprecationProvider {

    private lateinit var topLevelCorrection: Map<String, TreeMap<SmkVersion, SmkKeywordUpdateData>>
    private lateinit var functionCorrection: Map<String, TreeMap<SmkVersion, SmkKeywordUpdateData>>
    private lateinit var subsectionCorrection: Map<Pair<String, String?>, TreeMap<SmkVersion, SmkKeywordUpdateData>>

    private lateinit var subsectionIntroduction: Map<Pair<String, String?>, SmkVersion>
    private lateinit var topLevelIntroduction: Map<String, SmkVersion>

    init {
        val input = javaClass.getResourceAsStream(DEPRECATION_DATA_LOCATION)
        initiate(input)
    }
    fun overrideInputFile(input: InputStream) {
        initiate(input)
    }
    private fun initiate(inputStream: InputStream?) {
        val yaml = Yaml(CustomClassLoaderConstructor(SmkDeprecationData::class.java.classLoader, LoaderOptions()))
        yaml.setBeanAccess(BeanAccess.FIELD) // it must be set to be able to set vals
        val deprecationData = yaml.loadAs(inputStream, SmkDeprecationData::class.java)
        val topLevelData = emptyMap<String, TreeMap<SmkVersion, SmkKeywordUpdateData>>().toMutableMap()
        val functionData = emptyMap<String, TreeMap<SmkVersion, SmkKeywordUpdateData>>().toMutableMap()
        val subsectionData = emptyMap<Pair<String, String?>, TreeMap<SmkVersion, SmkKeywordUpdateData>>().toMutableMap()

        val subsectionIntroductionsMap = emptyMap<Pair<String, String?>, SmkVersion>().toMutableMap()
        val topLevelIntroductionsMap = emptyMap<String, SmkVersion>().toMutableMap()
        for (data in deprecationData.changelog) {
            val version = SmkVersion(data.version)

            val mapGetter = { src: SmkDeprecationKeywordData, newValue: SmkKeywordUpdateData ->
                when (src.type) {
                    TOP_LEVEL_KEYWORD_TYPE -> {
                        topLevelData.getOrPut(src.name) { TreeMap() }[version] = newValue
                    }
                    FUNCTION_KEYWORD_TYPE -> {
                        functionData.getOrPut(src.name) { TreeMap() }[version] = newValue
                    }
                    SUBSECTION_KEYWORD_TYPE -> {
                        if (src.parent.isEmpty()) {
                            subsectionData.getOrPut(src.name to null) { TreeMap() }[version] = newValue
                        }
                        else {
                            for (parent in src.parent) {
                                subsectionData.getOrPut(src.name to parent) { TreeMap() }[version] = newValue
                            }
                        }
                    }
                    else -> throw IllegalArgumentException("Unknown keyword type: ${src.type}")
                }
            }


            putKeywordDataIntoMap(data.deprecated, UpdateType.DEPRECATED, mapGetter)
            putKeywordDataIntoMap(data.removed, UpdateType.REMOVED, mapGetter)

            for (introduction in data.introduced) {
                when (introduction.type) {
                    TOP_LEVEL_KEYWORD_TYPE -> topLevelIntroductionsMap[introduction.name] = version
                    SUBSECTION_KEYWORD_TYPE -> {
                        if (introduction.parent.isEmpty()) {
                            subsectionIntroductionsMap[introduction.name to null] = version
                        }
                        for (parent in introduction.parent) {
                            subsectionIntroductionsMap[introduction.name to parent] = version
                        }
                    }
                    else -> throw IllegalArgumentException("Type ${introduction.type} introduction is not supported")
                }
            }
        }

        topLevelCorrection = topLevelData
        functionCorrection = functionData
        subsectionCorrection = subsectionData

        subsectionIntroduction = subsectionIntroductionsMap
        topLevelIntroduction = topLevelIntroductionsMap

    }

    /**
     * @param name name of keyword to check
     * @param version version in which keyword status should be checked
     * @return Pair of latest deprecation/removal update that was made to the top level keyword as of provided version,
     *           and advice if any was assigned to the change
     */
    fun getTopLevelCorrection(name: String, version: SmkVersion): Pair<UpdateType, Pair<String?, SmkVersion>>? {
        return getKeywordCorrection(topLevelCorrection[name], version)
    }

    /**
     * @param name name of keyword to check
     * @param version version in which keyword status should be checked
     * @param parent section in which keyword was used
     * @return Pair of latest deprecation/removal update that was made to the subsection keyword as of provided version,
     *           and advice if any was assigned to the change
     */
    fun getSubsectionCorrection(name: String, version: SmkVersion, parent: String): Pair<UpdateType, Pair<String?, SmkVersion>>? {
        return getKeywordCorrection(subsectionCorrection[name to parent], version) ?:
                getKeywordCorrection(subsectionCorrection[name to null], version)
    }

    /**
     * @param name name of keyword to check
     * @param version version in which keyword status should be checked
     * @return Pair of latest deprecation/removal update that was made to the function keyword as of provided version,
     *           and advice if any was assigned to the change
     */
    fun getFunctionCorrection(name: String, version: SmkVersion): Pair<UpdateType, Pair<String?, SmkVersion>>? {
        return getKeywordCorrection(functionCorrection[name], version)
    }

    fun getTopLevelIntroductionVersion(name: String): SmkVersion? {
        return topLevelIntroduction[name]
    }

    fun getSubSectionIntroductionVersion(name: String, parent: String): SmkVersion? {
        return subsectionIntroduction[name to parent] ?: subsectionIntroduction[name to null]
    }

    private fun getKeywordCorrection(
        keywords: TreeMap<SmkVersion, SmkKeywordUpdateData>?,
        version: SmkVersion
    ): Pair<UpdateType, Pair<String?, SmkVersion>>? {
        val entry = keywords?.floorEntry(version) ?: return null
        return entry.value.type to (entry.value.advice to entry.key)
    }


    private fun putKeywordDataIntoMap(
        keywords: List<SmkDeprecationKeywordData>,
        updateType: UpdateType,
        updateMap: (SmkDeprecationKeywordData, SmkKeywordUpdateData) -> Unit
    ) {
        for (keyword: SmkDeprecationKeywordData in keywords) {
            updateMap(keyword, SmkKeywordUpdateData(updateType, keyword.advice.ifEmpty { null }))
        }
    }

    companion object {
        const val DEPRECATION_DATA_LOCATION = "/DeprecationData.yaml"
        const val TOP_LEVEL_KEYWORD_TYPE = "top-level"
        const val SUBSECTION_KEYWORD_TYPE = "subsection"
        const val FUNCTION_KEYWORD_TYPE = "function"
    }
}

enum class UpdateType {
    REMOVED, DEPRECATED
}

data class SmkKeywordUpdateData(
    val type: UpdateType,
    val advice: String?,
)


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