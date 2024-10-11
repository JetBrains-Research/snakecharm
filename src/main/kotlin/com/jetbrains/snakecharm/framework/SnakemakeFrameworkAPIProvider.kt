package com.jetbrains.snakecharm.framework

import com.intellij.openapi.application.Application
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.jetbrains.snakecharm.SnakemakePluginUtil
import com.jetbrains.snakecharm.SnakemakeTestUtil
import com.jetbrains.snakecharm.framework.snakemakeAPIAnnotations.*
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
    private lateinit var topLevelName2Deprecations: Map<String, TreeMap<SmkLanguageVersion, SmkKeywordDeprecationParams>>
    private lateinit var functionName2Deprecations: Map<String, TreeMap<SmkLanguageVersion, SmkKeywordDeprecationParams>>
    private lateinit var subsectionName2Deprecations: Map<Pair<String, String>, TreeMap<SmkLanguageVersion, SmkKeywordDeprecationParams>>

    private lateinit var topLevelName2Introduction: Map<String, TreeMap<SmkLanguageVersion, SmkKeywordIntroductionParams>>
    private lateinit var subsectionName2Introduction: Map<Pair<String, String>, TreeMap<SmkLanguageVersion, SmkKeywordIntroductionParams>>

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
        val yaml = Yaml(CustomClassLoaderConstructor(SmkAPIAnnParsingConfig::class.java.classLoader, LoaderOptions()))
        yaml.setBeanAccess(BeanAccess.FIELD) // it must be set to be able to set vals
        val yamlData = yaml.loadAs(inputStream, SmkAPIAnnParsingConfig::class.java)

        // Deprecation Data
        val functionDeprecationInfo = emptyMap<String, TreeMap<SmkLanguageVersion, SmkKeywordDeprecationParams>>().toMutableMap()
        val topLevelDeprecationInfo = emptyMap<String, TreeMap<SmkLanguageVersion, SmkKeywordDeprecationParams>>().toMutableMap()
        val subsectionDeprecationInfo =
            emptyMap<Pair<String, String>, TreeMap<SmkLanguageVersion, SmkKeywordDeprecationParams>>().toMutableMap()

        // Introduction Data
        val topLevelIntroductionInfo = emptyMap<String, TreeMap<SmkLanguageVersion, SmkKeywordIntroductionParams>>().toMutableMap()
        val subsectionIntroductionInfo =
            emptyMap<Pair<String, String>, TreeMap<SmkLanguageVersion, SmkKeywordIntroductionParams>>().toMutableMap()

        defaultVersion = yamlData.defaultVersion
        for (versionData in yamlData.changelog) {
            val languageVersion = SmkLanguageVersion(versionData.version)

            fillDeprecationInfo(languageVersion, versionData, functionDeprecationInfo, topLevelDeprecationInfo, subsectionDeprecationInfo)
            filIntroductionData(languageVersion, versionData, topLevelIntroductionInfo, subsectionIntroductionInfo)
        }

        topLevelName2Deprecations = topLevelDeprecationInfo
        functionName2Deprecations = functionDeprecationInfo
        subsectionName2Deprecations = subsectionDeprecationInfo

        topLevelName2Introduction = topLevelIntroductionInfo
        subsectionName2Introduction = subsectionIntroductionInfo
    }

    private fun filIntroductionData(
        languageVersion: SmkLanguageVersion,
        versionChangeNotes: SmkAPIAnnParsingVersionRecord,
        topLevelIntroductionData: MutableMap<String, TreeMap<SmkLanguageVersion, SmkKeywordIntroductionParams>>,
        subsectionIntroductionData: MutableMap<Pair<String, String>, TreeMap<SmkLanguageVersion, SmkKeywordIntroductionParams>>,
    ) {
        // Process:
        // here 'introduced' and 'override' are synonyms, just for better understanding use 2 different types in
        // annotations, like first was introduced, than changed
        for (record: SmkAPIAnnParsingIntroductionRecord in versionChangeNotes.introduced) {
            updateBasedOnRecContextType(
                record, SmkKeywordIntroductionParams.createFrom(record),
                languageVersion, null, topLevelIntroductionData, subsectionIntroductionData
            )
        }

        for (record: SmkAPIAnnParsingIntroductionRecord in versionChangeNotes.override) {
            updateBasedOnRecContextType(
                record, SmkKeywordIntroductionParams.createFrom(record),
                languageVersion, null, topLevelIntroductionData, subsectionIntroductionData
            )
        }
    }

    private fun fillDeprecationInfo(
        languageVersion: SmkLanguageVersion,
        versionChangeNotes: SmkAPIAnnParsingVersionRecord,
        functionDeprecationInfo: MutableMap<String, TreeMap<SmkLanguageVersion, SmkKeywordDeprecationParams>>,
        topLevelDeprecationInfo: MutableMap<String, TreeMap<SmkLanguageVersion, SmkKeywordDeprecationParams>>,
        subsectionDeprecationInfo: MutableMap<Pair<String, String>, TreeMap<SmkLanguageVersion, SmkKeywordDeprecationParams>>
    ) {
        // info from 'deprecated section'
        for (record: SmkAPIAnnParsingDeprecationRecord in versionChangeNotes.deprecated) {
            updateBasedOnRecContextType(
                record, SmkKeywordDeprecationParams.createFrom(SmkAPIAnnDeprecationType.DEPRECATED, record),
                languageVersion, functionDeprecationInfo, topLevelDeprecationInfo, subsectionDeprecationInfo
            )
        }

        // info from 'removed section'
        for (record: SmkAPIAnnParsingDeprecationRecord in versionChangeNotes.removed) {
            updateBasedOnRecContextType(
                record, SmkKeywordDeprecationParams.createFrom(SmkAPIAnnDeprecationType.REMOVED, record),
                languageVersion, functionDeprecationInfo, topLevelDeprecationInfo, subsectionDeprecationInfo
            )
        }
    }

    private fun <A:SmkAPIAnnParsingAbstractRecord, B> updateBasedOnRecContextType(
        rec: A,
        params: B,
        languageVersion: SmkLanguageVersion,
        functionsInfo: MutableMap<String, TreeMap<SmkLanguageVersion, B>>?,
        topLevelInfo: MutableMap<String, TreeMap<SmkLanguageVersion, B>>,
        subsectionsInfo: MutableMap<Pair<String, String>, TreeMap<SmkLanguageVersion, B>>
    ) {
        val typeOrContext = rec.type
        when (typeOrContext) {
            SmkAPIAnnParsingContextType.FUNCTION.typeStr -> {
                if (functionsInfo != null) {
                    functionsInfo.getOrPut(rec.name) { TreeMap() }[languageVersion] = params
                }
            }

            SmkAPIAnnParsingContextType.TOP_LEVEL.typeStr -> {
                topLevelInfo.getOrPut(rec.name) { TreeMap() }[languageVersion] = params
            }
            // else: subsection type:
            else -> {
                if (typeOrContext == SmkAPIAnnParsingContextType.RULE_LIKE.typeStr) {
                    RULE_LIKE_KEYWORDS.forEach { directive ->
                        subsectionsInfo.getOrPut(rec.name to directive) { TreeMap() }[languageVersion] =
                            params
                    }
                } else {
                    subsectionsInfo.getOrPut(rec.name to typeOrContext) { TreeMap() }[languageVersion] =
                        params
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
    fun getTopLevelDeprecation(name: String, version: SmkLanguageVersion): SmkAPIAnnDeprecationInfo? {
        return getKeywordDeprecation(topLevelName2Deprecations[name], version)
    }

    /**
     * @param name name of keyword to check
     * @param version version in which keyword status should be checked
     * @param contextSectionKeyword section in which keyword was used
     * @return Pair of latest deprecation/removal update that was made to the subsection keyword as of provided version,
     *           and advice if any was assigned to the change
     */
    fun getSubsectionDeprecation(name: String, version: SmkLanguageVersion, contextSectionKeyword: String): SmkAPIAnnDeprecationInfo? =
        // XXX: at the moment we don't have any subsections with context `section`, that was global in the past
        getKeywordDeprecation(subsectionName2Deprecations[name to contextSectionKeyword], version, false)

    /**
     * @param name name of keyword to check
     * @param version version in which keyword status should be checked
     * @return Pair of latest deprecation/removal update that was made to the function keyword as of provided version,
     *           and advice if any was assigned to the change
     */
    fun getFunctionDeprecation(
        name: String,
        version: SmkLanguageVersion
    ): SmkAPIAnnDeprecationInfo? {
        return getKeywordDeprecation(
            functionName2Deprecations[name],
            version
        )
    }

    fun getTopLevelIntroductionVersion(name: String): SmkLanguageVersion? =
        topLevelName2Introduction[name]?.firstEntry()?.key

    fun getTopLevelRemovedVersion(name: String): SmkLanguageVersion? =
        topLevelName2Deprecations[name]?.let { deprecations ->
            val latestDeprecated = deprecations.descendingMap().asIterable().firstOrNull() { it.value.type == SmkAPIAnnDeprecationType.REMOVED }
            latestDeprecated?.key
        }

    fun getTopLevelDeprecationVersion(name: String): SmkLanguageVersion? =
        topLevelName2Deprecations[name]?.let { deprecations ->
            val latestDeprecated = deprecations.descendingMap().asIterable().firstOrNull() { it.value.type == SmkAPIAnnDeprecationType.DEPRECATED }
            latestDeprecated?.key
        }

    fun getSubSectionIntroductionVersion(name: String, contextSectionKeyword: String): SmkLanguageVersion? =
        subsectionName2Introduction[name to contextSectionKeyword]?.firstEntry()?.key

    fun getSubSectionDeprecationVersion(name: String, contextSectionKeyword: String): SmkLanguageVersion? =
        subsectionName2Deprecations[name to contextSectionKeyword]?.let { deprecations ->
            val latestDeprecated = deprecations.descendingMap().asIterable().firstOrNull() { it.value.type == SmkAPIAnnDeprecationType.DEPRECATED }
            latestDeprecated?.key
        }

    fun getSubSectionRemovalVersion(name: String, contextSectionKeyword: String): SmkLanguageVersion? =
        subsectionName2Deprecations[name to contextSectionKeyword]?.let { deprecations ->
            val latestDeprecated = deprecations.descendingMap().asIterable().firstOrNull() { it.value.type == SmkAPIAnnDeprecationType.REMOVED }
            latestDeprecated?.key
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
        listOf(subsectionName2Introduction.keys, subsectionName2Deprecations.keys).forEach { keys ->
            mutableSet.addAll(
                keys.filter { (_, type) -> ctxTypeFilter(type) }
                    .map { (name, _) -> name }
            )
        }
        return mutableSet.unmodifiable()
    }

    private fun getKeywordDeprecation(
        keywords: TreeMap<SmkLanguageVersion, SmkKeywordDeprecationParams>?,
        version: SmkLanguageVersion,
        isGlobalChange: Boolean = true
    ): SmkAPIAnnDeprecationInfo? {
        val entry = keywords?.floorEntry(version) ?: return null
        return SmkAPIAnnDeprecationInfo(entry.value.type, entry.value.advice, entry.key, isGlobalChange)
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