package com.jetbrains.snakecharm.framework

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.jetbrains.snakecharm.SnakemakePluginUtil
import com.jetbrains.snakecharm.SnakemakeTestUtil
import com.jetbrains.snakecharm.lang.SmkLanguageVersion
import org.yaml.snakeyaml.LoaderOptions
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.CustomClassLoaderConstructor
import org.yaml.snakeyaml.introspector.BeanAccess
import java.io.InputStream
import java.util.*
import kotlin.io.path.exists
import kotlin.io.path.inputStream

@Service
class SmkFrameworkDeprecationProvider {

    private lateinit var topLevelCorrection: Map<String, TreeMap<SmkLanguageVersion, SmkKeywordUpdateData>>
    private lateinit var functionCorrection: Map<String, TreeMap<SmkLanguageVersion, SmkKeywordUpdateData>>
    private lateinit var subsectionCorrection: Map<Pair<String, String?>, TreeMap<SmkLanguageVersion, SmkKeywordUpdateData>>

    private lateinit var subsectionIntroduction: Map<Pair<String, String?>, SmkLanguageVersion>
    private lateinit var topLevelIntroduction: Map<String, SmkLanguageVersion>

    private lateinit var defaultVersion: String

    init {
        val unitTestMode = ApplicationManager.getApplication().isUnitTestMode

        val snakemakeApiFile = if (!unitTestMode) {
            val pluginSandboxPath =
                SnakemakePluginUtil.getPluginSandboxPath(SmkFrameworkDeprecationProvider::class.java)
            pluginSandboxPath.resolve("extra/snakemake_api.yaml")
        } else {
            SnakemakeTestUtil.getTestDataPath().parent.resolve("snakemake_api.yaml")
        }

        requireNotNull(!snakemakeApiFile.exists()) {
            "Missing wrappers bundle in plugin bundle: '$snakemakeApiFile' doesn't exist"
        }
        initialize(snakemakeApiFile.inputStream())
    }

    fun overrideInputFile(input: InputStream) {
        initialize(input)
    }

    private fun initialize(inputStream: InputStream?) {
        val yaml = Yaml(CustomClassLoaderConstructor(SmkDeprecationData::class.java.classLoader, LoaderOptions()))
        yaml.setBeanAccess(BeanAccess.FIELD) // it must be set to be able to set vals
        val deprecationData = yaml.loadAs(inputStream, SmkDeprecationData::class.java)
        val topLevelData = emptyMap<String, TreeMap<SmkLanguageVersion, SmkKeywordUpdateData>>().toMutableMap()
        val functionData = emptyMap<String, TreeMap<SmkLanguageVersion, SmkKeywordUpdateData>>().toMutableMap()
        val subsectionData =
            emptyMap<Pair<String, String?>, TreeMap<SmkLanguageVersion, SmkKeywordUpdateData>>().toMutableMap()

        val subsectionIntroductionsMap = emptyMap<Pair<String, String?>, SmkLanguageVersion>().toMutableMap()
        val topLevelIntroductionsMap = emptyMap<String, SmkLanguageVersion>().toMutableMap()

        defaultVersion = deprecationData.defaultVersion
        for (data in deprecationData.changelog) {
            val version = SmkLanguageVersion(data.version)

            val mapGetter = { src: SmkDeprecationKeywordData, newValue: SmkKeywordUpdateData ->
                when (src.type) {
                    FUNCTION_KEYWORD_TYPE, TOP_LEVEL_KEYWORD_TYPE -> {
                        val dataToInsert = if (src.type == FUNCTION_KEYWORD_TYPE) functionData else topLevelData
                        dataToInsert.getOrPut(src.name) { TreeMap() }[version] = newValue
                    }

                    SUBSECTION_KEYWORD_TYPE -> {
                        if (src.parent.isEmpty()) {
                            subsectionData.getOrPut(src.name to null) { TreeMap() }[version] = newValue
                        } else {
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

    fun getDefaultVersion() = defaultVersion

    /**
     * @param name name of keyword to check
     * @param version version in which keyword status should be checked
     * @return Pair of latest deprecation/removal update that was made to the top level keyword as of provided version,
     *           and advice if any was assigned to the change
     */
    fun getTopLevelCorrection(name: String, version: SmkLanguageVersion): SmkCorrectionInfo? {
        return getKeywordCorrection(topLevelCorrection[name], version)
    }

    /**
     * @param name name of keyword to check
     * @param version version in which keyword status should be checked
     * @param parent section in which keyword was used
     * @return Pair of latest deprecation/removal update that was made to the subsection keyword as of provided version,
     *           and advice if any was assigned to the change
     */
    fun getSubsectionCorrection(name: String, version: SmkLanguageVersion, parent: String): SmkCorrectionInfo? {
        return getKeywordCorrection(subsectionCorrection[name to parent], version, false) ?: getKeywordCorrection(
            subsectionCorrection[name to null],
            version
        )
    }

    /**
     * @param name name of keyword to check
     * @param version version in which keyword status should be checked
     * @return Pair of latest deprecation/removal update that was made to the function keyword as of provided version,
     *           and advice if any was assigned to the change
     */
    fun getFunctionCorrection(
        name: String,
        version: SmkLanguageVersion
    ): SmkCorrectionInfo? {
        return getKeywordCorrection(
            functionCorrection[name],
            version
        )
    }

    fun getTopLevelIntroductionVersion(name: String): SmkLanguageVersion? {
        return topLevelIntroduction[name]
    }

    fun getSubSectionIntroductionVersion(name: String, parent: String): SmkLanguageVersion? {
        return subsectionIntroduction[name to parent] ?: subsectionIntroduction[name to null]
    }

    private fun getKeywordCorrection(
        keywords: TreeMap<SmkLanguageVersion, SmkKeywordUpdateData>?,
        version: SmkLanguageVersion,
        isGlobalChange: Boolean = true
    ): SmkCorrectionInfo? {
        val entry = keywords?.floorEntry(version) ?: return null
        return SmkCorrectionInfo(entry.value.type, entry.value.advice, entry.key, isGlobalChange)
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
        const val TOP_LEVEL_KEYWORD_TYPE = "top-level"
        const val SUBSECTION_KEYWORD_TYPE = "subsection"
        const val FUNCTION_KEYWORD_TYPE = "function"

        fun getInstance() =
            ApplicationManager.getApplication().getService(SmkFrameworkDeprecationProvider::class.java)!!
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
    val defaultVersion: String = "7.32.4"
)

data class SmkCorrectionInfo(
    val updateType: UpdateType,
    val advice: String?,
    val version: SmkLanguageVersion,
    val isGlobalChange: Boolean
)