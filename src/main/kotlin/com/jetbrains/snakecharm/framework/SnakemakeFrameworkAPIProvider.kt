package com.jetbrains.snakecharm.framework

import com.intellij.openapi.application.Application
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.jetbrains.snakecharm.SnakemakePluginUtil
import com.jetbrains.snakecharm.SnakemakeTestUtil
import com.jetbrains.snakecharm.lang.SmkLanguageVersion
import com.jetbrains.snakecharm.lang.SnakemakeNames
import com.jetbrains.snakecharm.lang.SnakemakeNames.CHECKPOINT_KEYWORD
import com.jetbrains.snakecharm.lang.SnakemakeNames.RULE_KEYWORD
import com.jetbrains.snakecharm.lang.SnakemakeNames.USE_KEYWORD
import io.ktor.util.*
import org.jetbrains.annotations.TestOnly
import org.yaml.snakeyaml.LoaderOptions
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.CustomClassLoaderConstructor
import org.yaml.snakeyaml.introspector.BeanAccess
import java.io.InputStream
import java.nio.file.Path
import java.util.*
import kotlin.io.path.exists
import kotlin.io.path.inputStream

@Service
class SnakemakeFrameworkAPIProvider(
    application: Application? // required to launch outside of IDEA process
) {
    private lateinit var topLevelCorrection: Map<String, TreeMap<SmkLanguageVersion, SmkKeywordUpdateData>>
    private lateinit var functionCorrection: Map<String, TreeMap<SmkLanguageVersion, SmkKeywordUpdateData>>
    private lateinit var subsectionCorrection: Map<Pair<String, String>, TreeMap<SmkLanguageVersion, SmkKeywordUpdateData>>

    private lateinit var subsectionIntroduction: Map<Pair<String, String>, SmkLanguageVersion>
    private lateinit var subsectionRemoval: Map<Pair<String, String>, SmkLanguageVersion>
    private lateinit var subsectionDeprecation: Map<Pair<String, String>, SmkLanguageVersion>
    private lateinit var topLevelIntroduction: Map<String, SmkLanguageVersion>
    private lateinit var topLevelRemoval: Map<String, SmkLanguageVersion>
    private lateinit var topLevelDeprecation: Map<String, SmkLanguageVersion>

    private lateinit var defaultVersion: String

    init {
        if (application != null) {
            if (application.isUnitTestMode) {
                reinitializeInTests()
            } else {
                val pluginSandboxPath = SnakemakePluginUtil.getPluginSandboxPath(
                    SnakemakeFrameworkAPIProvider::class.java
                )
                initialize(pluginSandboxPath.resolve("extra/snakemake_api.yaml"))
            }
        }
    }

    /**
     * Constructor for service creation in IDEA (default usage)
     */
    constructor() : this(ApplicationManager.getApplication())



    @TestOnly
    fun reinitializeInTests() {
        require(ApplicationManager.getApplication().isUnitTestMode)
        initialize(SnakemakeTestUtil.getTestDataPath().parent.resolve("snakemake_api.yaml"))
    }

    @TestOnly
    fun reinitializeInTests(input: InputStream) {
        initialize(input)
    }

    private fun initialize(snakemakeApiFile: Path) {
        requireNotNull(!snakemakeApiFile.exists()) {
            "Missing wrappers bundle in plugin bundle: '$snakemakeApiFile' doesn't exist"
        }
        initialize(snakemakeApiFile.inputStream())

    }
    private fun initialize(inputStream: InputStream?) {
        val yaml = Yaml(CustomClassLoaderConstructor(SmkDeprecationData::class.java.classLoader, LoaderOptions()))
        yaml.setBeanAccess(BeanAccess.FIELD) // it must be set to be able to set vals
        val deprecationData = yaml.loadAs(inputStream, SmkDeprecationData::class.java)
        val topLevelData = emptyMap<String, TreeMap<SmkLanguageVersion, SmkKeywordUpdateData>>().toMutableMap()
        val functionData = emptyMap<String, TreeMap<SmkLanguageVersion, SmkKeywordUpdateData>>().toMutableMap()
        val subsectionData =
            emptyMap<Pair<String, String>, TreeMap<SmkLanguageVersion, SmkKeywordUpdateData>>().toMutableMap()

        val subsectionIntroductionsMap = emptyMap<Pair<String, String>, SmkLanguageVersion>().toMutableMap()
        val subsectionDeprecationsMap = emptyMap<Pair<String, String>, SmkLanguageVersion>().toMutableMap()
        val subsectionRemovalMap = emptyMap<Pair<String, String>, SmkLanguageVersion>().toMutableMap()
        val topLevelIntroductionsMap = emptyMap<String, SmkLanguageVersion>().toMutableMap()
        val topLevelDeprecationsMap = emptyMap<String, SmkLanguageVersion>().toMutableMap()
        val topLevelRemovalMap = emptyMap<String, SmkLanguageVersion>().toMutableMap()

        defaultVersion = deprecationData.defaultVersion
        for (data in deprecationData.changelog) {
            val version = SmkLanguageVersion(data.version)

            val mapGetter = { src: SmkDeprecationKeywordData, newValue: SmkKeywordUpdateData ->
                val srcType = src.type
                when (srcType) {
                    SmkAPIKeywordContextType.FUNCTION.typeStr, SmkAPIKeywordContextType.TOP_LEVEL.typeStr -> {
                        val dataToInsert = if (srcType == SmkAPIKeywordContextType.FUNCTION.typeStr) functionData else topLevelData
                        dataToInsert.getOrPut(src.name) { TreeMap() }[version] = newValue
                    }
                    // else: subsection type:
                    else -> {
                        if (srcType == SmkAPIKeywordContextType.RULE_LIKE.typeStr) {
                            RULE_LIKE_KEYWORDS.forEach { directive ->
                                subsectionData.getOrPut(src.name to directive) { TreeMap() }[version] = newValue
                            }
                        }
                        else {
                            subsectionData.getOrPut(src.name to srcType) { TreeMap() }[version] = newValue
                        }
                    }
                }
            }


            putKeywordDataIntoMap(data.deprecated, UpdateType.DEPRECATED, mapGetter)
            putKeywordDataIntoMap(data.removed, UpdateType.REMOVED, mapGetter)

            fillEventsMapWithKeywordsAndVersions(topLevelIntroductionsMap, version, subsectionIntroductionsMap, data.introduced)
            fillEventsMapWithKeywordsAndVersions(topLevelDeprecationsMap, version, subsectionDeprecationsMap, data.deprecated)
            fillEventsMapWithKeywordsAndVersions(topLevelRemovalMap, version, subsectionRemovalMap, data.removed)
        }

        topLevelCorrection = topLevelData
        functionCorrection = functionData
        subsectionCorrection = subsectionData

        subsectionIntroduction = subsectionIntroductionsMap
        topLevelIntroduction = topLevelIntroductionsMap
        subsectionRemoval = subsectionRemovalMap
        topLevelRemoval = topLevelRemovalMap
        subsectionDeprecation = subsectionDeprecationsMap
        topLevelDeprecation = topLevelDeprecationsMap
    }

    private fun fillEventsMapWithKeywordsAndVersions(
        topLevelEventsMap: MutableMap<String, SmkLanguageVersion>,
        version: SmkLanguageVersion,
        subsectionEventsMap: MutableMap<Pair<String, String>, SmkLanguageVersion>,
        events: List<SmkDeprecationKeywordData>
        )
    {
        for (event in events) {
            when (event.type) {
                SmkAPIKeywordContextType.TOP_LEVEL.typeStr -> topLevelEventsMap[event.name] = version
                SmkAPIKeywordContextType.FUNCTION.typeStr -> {
                    // TODO do nothing, functions processed isn't implemented, issue: ......
                }
                SmkAPIKeywordContextType.RULE_LIKE.typeStr -> {
                    RULE_LIKE_KEYWORDS.forEach { directive ->
                        subsectionEventsMap[event.name to directive] = version
                    }
                }
                else -> {
                    subsectionEventsMap[event.name to event.type] = version
                }
            }
        }
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
     * @param contextSectionKeyword section in which keyword was used
     * @return Pair of latest deprecation/removal update that was made to the subsection keyword as of provided version,
     *           and advice if any was assigned to the change
     */
    fun getSubsectionCorrection(name: String, version: SmkLanguageVersion, contextSectionKeyword: String): SmkCorrectionInfo? =
        // XXX: at the moment we don't have any subsections with context `section`, that was global in the past
        getKeywordCorrection(subsectionCorrection[name to contextSectionKeyword], version, false)

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
    fun getTopLevelRemovedVersion(name: String): SmkLanguageVersion? {
        return topLevelRemoval[name]
    }
    fun getTopLevelDeprecationVersion(name: String): SmkLanguageVersion? {
        return topLevelDeprecation[name]
    }

    fun getSubSectionIntroductionVersion(name: String, contextSectionKeyword: String): SmkLanguageVersion? =
        subsectionIntroduction[name to contextSectionKeyword]

    fun getSubSectionDeprecationVersion(name: String, contextSectionKeyword: String): SmkLanguageVersion? {
        return subsectionDeprecation[name to contextSectionKeyword]
    }
    fun getSubSectionRemovalVersion(name: String, contextSectionKeyword: String): SmkLanguageVersion? {
        return subsectionRemoval[name to contextSectionKeyword]
    }

    fun collectAllPossibleUseSubsectionKeywords() = collectAllPossibleSubsectionKeywords { type ->
        type == USE_KEYWORD
    }

    fun collectAllPossibleModuleSubsectionKeywords() = collectAllPossibleSubsectionKeywords { type ->
        type == SnakemakeNames.MODULE_KEYWORD
    }

    fun collectAllPossibleRuleOrCheckpointSubsectionKeywords() = collectAllPossibleSubsectionKeywords { type ->
            (type == RULE_KEYWORD) || (type == CHECKPOINT_KEYWORD)
        }

    private fun collectAllPossibleSubsectionKeywords(ctxTypeFilter: (String) -> Boolean): Set<String> {
        val mutableSet = mutableSetOf<String>()

        // Collect all sections ignoring deprecation/removal/introduction marks:
        listOf(subsectionIntroduction.keys, subsectionDeprecation.keys, subsectionRemoval.keys).forEach { keys ->
            mutableSet.addAll(
                keys.filter { (_, type) -> ctxTypeFilter(type) }
                    .map { (name, _) -> name }
            )
        }
        return mutableSet.unmodifiable()
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
        /**
         * For Snakemake YAML api descriptor
         */
        private val RULE_LIKE_KEYWORDS = setOf(
            RULE_KEYWORD, CHECKPOINT_KEYWORD, USE_KEYWORD
        )

        fun getInstance() =
            ApplicationManager.getApplication().getService(SnakemakeFrameworkAPIProvider::class.java)!!
    }
}
enum class SmkAPIKeywordContextType(val typeStr: String) {
    TOP_LEVEL("top-level"),
    RULE_LIKE("rule-like"),
    FUNCTION("function"),
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
    val defaultVersion: String = "0.0.0",
    val annotationsFormatVersion: Int = 0
)

data class SmkCorrectionInfo(
    val updateType: UpdateType,
    val advice: String?,
    val version: SmkLanguageVersion,
    val isGlobalChange: Boolean // XXX seems we don't need it any more, was for `global` subsections mainly
)