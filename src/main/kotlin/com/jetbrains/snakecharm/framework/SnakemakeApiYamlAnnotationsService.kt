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
import kotlinx.collections.immutable.toImmutableSet
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

/**
 * YAML snakemake api description state & parser
 */
@Service
class SnakemakeApiYamlAnnotationsService(
    application: Application? // required to launch outside of IDEA process
) {
    private lateinit var topLevelName2Deprecations: Map<String, TreeMap<SmkLanguageVersion, SmkApiAnnotationKeywordDeprecationParams>>
    private lateinit var functionFqn2Deprecations: Map<String, TreeMap<SmkLanguageVersion, SmkApiAnnotationKeywordDeprecationParams>>
    private lateinit var functionShortName2Deprecations: Map<String, TreeMap<SmkLanguageVersion, SmkApiAnnotationKeywordDeprecationParams>>
    private lateinit var subsectionName2Deprecations: Map<SmkApiSubsectionContextAndDirective, TreeMap<SmkLanguageVersion, SmkApiAnnotationKeywordDeprecationParams>>

    private lateinit var functionFqn2Introduction: Map<String, TreeMap<SmkLanguageVersion, SmkApiAnnotationKeywordIntroductionParams>>
    private lateinit var functionShortName2Introduction: Map<String, TreeMap<SmkLanguageVersion, SmkApiAnnotationKeywordIntroductionParams>>
    private lateinit var topLevelName2Introduction: Map<String, TreeMap<SmkLanguageVersion, SmkApiAnnotationKeywordIntroductionParams>>
    private lateinit var subsectionName2Introduction: Map<SmkApiSubsectionContextAndDirective, TreeMap<SmkLanguageVersion, SmkApiAnnotationKeywordIntroductionParams>>

    private lateinit var defaultVersion: String

    init {
        if (application != null) {
            if (application.isUnitTestMode) {
                reinitializeInTests()
            } else {
                val pluginSandboxPath = SnakemakePluginUtil.getPluginSandboxPath(
                    SnakemakeApiYamlAnnotationsService::class.java
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
        val yaml = Yaml(CustomClassLoaderConstructor(SmkApiAnnotationParsingConfig::class.java.classLoader, LoaderOptions()))
        yaml.setBeanAccess(BeanAccess.FIELD) // it must be set to be able to set vals
        val yamlData = yaml.loadAs(inputStream, SmkApiAnnotationParsingConfig::class.java)

        // Deprecation Data
        val functionDeprecationByFqnInfo = emptyMap<String, TreeMap<SmkLanguageVersion, SmkApiAnnotationKeywordDeprecationParams>>().toMutableMap()
        val functionDeprecationByShortnameInfo = emptyMap<String, TreeMap<SmkLanguageVersion, SmkApiAnnotationKeywordDeprecationParams>>().toMutableMap()
        val topLevelDeprecationInfo = emptyMap<String, TreeMap<SmkLanguageVersion, SmkApiAnnotationKeywordDeprecationParams>>().toMutableMap()
        val subsectionDeprecationInfo =
            emptyMap<SmkApiSubsectionContextAndDirective, TreeMap<SmkLanguageVersion, SmkApiAnnotationKeywordDeprecationParams>>().toMutableMap()

        // Introduction Data
        val topLevelIntroductionInfo = emptyMap<String, TreeMap<SmkLanguageVersion, SmkApiAnnotationKeywordIntroductionParams>>().toMutableMap()
        val funIntroductionByFqnInfo = emptyMap<String, TreeMap<SmkLanguageVersion, SmkApiAnnotationKeywordIntroductionParams>>().toMutableMap()
        val funFqnIntroductionByShortnameInfo = emptyMap<String, TreeMap<SmkLanguageVersion, SmkApiAnnotationKeywordIntroductionParams>>().toMutableMap()
        val subsectionIntroductionInfo =
            emptyMap<SmkApiSubsectionContextAndDirective, TreeMap<SmkLanguageVersion, SmkApiAnnotationKeywordIntroductionParams>>().toMutableMap()

        defaultVersion = yamlData.defaultVersion
        for (versionData in yamlData.changelog) {
            val languageVersion = SmkLanguageVersion(versionData.version)

            fillDeprecationInfo(languageVersion, versionData, functionDeprecationByFqnInfo, functionDeprecationByShortnameInfo, topLevelDeprecationInfo, subsectionDeprecationInfo)
            filIntroductionData(languageVersion, versionData, funIntroductionByFqnInfo, funFqnIntroductionByShortnameInfo, topLevelIntroductionInfo, subsectionIntroductionInfo)
        }

        topLevelName2Deprecations = topLevelDeprecationInfo
        functionFqn2Deprecations = functionDeprecationByFqnInfo
        functionShortName2Deprecations = functionDeprecationByShortnameInfo
        subsectionName2Deprecations = subsectionDeprecationInfo

        functionFqn2Introduction = funIntroductionByFqnInfo
        functionShortName2Introduction = funFqnIntroductionByShortnameInfo
        topLevelName2Introduction = topLevelIntroductionInfo
        subsectionName2Introduction = subsectionIntroductionInfo
    }

    private fun filIntroductionData(
        languageVersion: SmkLanguageVersion,
        versionChangeNotes: SmkApiAnnotationParsingVersionRecord,
        functionDeprecationByFqnInfo: MutableMap<String, TreeMap<SmkLanguageVersion, SmkApiAnnotationKeywordIntroductionParams>>,
        functionDeprecationByShortnameInfo: MutableMap<String, TreeMap<SmkLanguageVersion, SmkApiAnnotationKeywordIntroductionParams>>,
        topLevelIntroductionData: MutableMap<String, TreeMap<SmkLanguageVersion, SmkApiAnnotationKeywordIntroductionParams>>,
        subsectionIntroductionData: MutableMap<SmkApiSubsectionContextAndDirective, TreeMap<SmkLanguageVersion, SmkApiAnnotationKeywordIntroductionParams>>,
    ) {
        // Process:
        // here 'introduced' and 'override' are synonyms, just for better understanding use 2 different types in
        // annotations, like first was introduced, than changed
        for (record: SmkApiAnnotationParsingIntroductionRecord in versionChangeNotes.introduced) {
            updateBasedOnRecContextType(
                record, SmkApiAnnotationKeywordIntroductionParams.createFrom(record),
                languageVersion, functionDeprecationByFqnInfo, functionDeprecationByShortnameInfo, topLevelIntroductionData, subsectionIntroductionData
            )
        }

        for (record: SmkApiAnnotationParsingIntroductionRecord in versionChangeNotes.override) {
            updateBasedOnRecContextType(
                record, SmkApiAnnotationKeywordIntroductionParams.createFrom(record),
                languageVersion, functionDeprecationByFqnInfo, functionDeprecationByShortnameInfo, topLevelIntroductionData, subsectionIntroductionData
            )
        }
    }

    private fun fillDeprecationInfo(
        languageVersion: SmkLanguageVersion,
        versionChangeNotes: SmkApiAnnotationParsingVersionRecord,
        functionDeprecationByFqnInfo: MutableMap<String, TreeMap<SmkLanguageVersion, SmkApiAnnotationKeywordDeprecationParams>>,
        functionDeprecationByShortnameInfo: MutableMap<String, TreeMap<SmkLanguageVersion, SmkApiAnnotationKeywordDeprecationParams>>,
        topLevelDeprecationInfo: MutableMap<String, TreeMap<SmkLanguageVersion, SmkApiAnnotationKeywordDeprecationParams>>,
        subsectionDeprecationInfo: MutableMap<SmkApiSubsectionContextAndDirective, TreeMap<SmkLanguageVersion, SmkApiAnnotationKeywordDeprecationParams>>
    ) {
        // info from 'deprecated section'
        for (record: SmkApiAnnotationParsingDeprecationRecord in versionChangeNotes.deprecated) {
            updateBasedOnRecContextType(
                record, SmkApiAnnotationKeywordDeprecationParams.createFrom(false, record),
                languageVersion, functionDeprecationByFqnInfo, functionDeprecationByShortnameInfo,
                topLevelDeprecationInfo, subsectionDeprecationInfo
            )
        }

        // info from 'removed section'
        for (record: SmkApiAnnotationParsingDeprecationRecord in versionChangeNotes.removed) {
            updateBasedOnRecContextType(
                record, SmkApiAnnotationKeywordDeprecationParams.createFrom(true, record),
                languageVersion, functionDeprecationByFqnInfo, functionDeprecationByShortnameInfo,
                topLevelDeprecationInfo, subsectionDeprecationInfo
            )
        }
    }

    private fun <A:SmkApiAnnotationParsingAbstractRecord, B> updateBasedOnRecContextType(
        rec: A,
        params: B,
        languageVersion: SmkLanguageVersion,
        functionsFqnInfo: MutableMap<String, TreeMap<SmkLanguageVersion, B>>?,
        functionsShortNameInfo: MutableMap<String, TreeMap<SmkLanguageVersion, B>>?,
        topLevelInfo: MutableMap<String, TreeMap<SmkLanguageVersion, B>>,
        subsectionsInfo: MutableMap<SmkApiSubsectionContextAndDirective, TreeMap<SmkLanguageVersion, B>>
    ) {
        val typeOrContext = rec.type
        when (typeOrContext) {
            SmkApiAnnotationParsingContextType.FUNCTION.typeStr -> {
                if (functionsFqnInfo != null) {
                    functionsFqnInfo.getOrPut(rec.name) { TreeMap() }[languageVersion] = params
                }
                if (functionsShortNameInfo != null) {
                    // Is fail-safe thing, that could work not ideal when these names are identical
                    val shortName = rec.name.split('.').last()
                    functionsShortNameInfo.getOrPut(shortName) { TreeMap() }[languageVersion] = params
                }
            }

            SmkApiAnnotationParsingContextType.TOP_LEVEL.typeStr -> {
                topLevelInfo.getOrPut(rec.name) { TreeMap() }[languageVersion] = params
            }
            // else: subsection type:
            else -> {
                if (typeOrContext == SmkApiAnnotationParsingContextType.RULE_LIKE.typeStr) {
                    RULE_LIKE_KEYWORDS.forEach { ctxDirective ->
                        subsectionsInfo.getOrPut(SmkApiSubsectionContextAndDirective(ctxDirective, rec.name)) { TreeMap() }[languageVersion] =
                            params
                    }
                } else {
                    subsectionsInfo.getOrPut(SmkApiSubsectionContextAndDirective(typeOrContext, rec.name)) { TreeMap() }[languageVersion] =
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
    fun getTopLevelDeprecation(name: String, version: SmkLanguageVersion): Pair<SmkLanguageVersion, SmkApiAnnotationKeywordDeprecationParams>? =
        getKeywordDeprecation(topLevelName2Deprecations[name], version)

    /**
     * @param name name of keyword to check
     * @param version version in which keyword status should be checked
     * @param contextSectionKeyword section in which keyword was used
     * @return Pair of latest deprecation/removal update that was made to the subsection keyword as of provided version,
     *           and advice if any was assigned to the change
     */
    fun getSubsectionDeprecation(name: String, version: SmkLanguageVersion, contextSectionKeyword: String): Pair<SmkLanguageVersion, SmkApiAnnotationKeywordDeprecationParams>? =
        getKeywordDeprecation(subsectionName2Deprecations[SmkApiSubsectionContextAndDirective(contextSectionKeyword, name)], version)

    /**
     * @param name name of keyword to check
     * @param version version in which keyword status should be checked
     * @return Pair of latest deprecation/removal update that was made to the function keyword as of provided version,
     *           and advice if any was assigned to the change
     */
    fun getFunctionDeprecationByFqn(
        fqn: String,
        version: SmkLanguageVersion
    ):  Pair<SmkLanguageVersion, SmkApiAnnotationKeywordDeprecationParams>? = getKeywordDeprecation(
        functionFqn2Deprecations[fqn],
        version
    )

    fun getFunctionDeprecationByShortName(
        name: String,
        version: SmkLanguageVersion
    ):  Pair<SmkLanguageVersion, SmkApiAnnotationKeywordDeprecationParams>? = getKeywordDeprecation(
        functionShortName2Deprecations[name],
        version
    )

    fun getTopLevelIntroductionVersion(name: String): SmkLanguageVersion? =
        topLevelName2Introduction[name]?.firstEntry()?.key

    fun getTopLevelRemovedVersion(name: String): SmkLanguageVersion? =
        topLevelName2Deprecations[name]?.let { deprecations ->
            val latestDeprecated = deprecations.descendingMap().asIterable().firstOrNull { it.value.itemRemoved }
            latestDeprecated?.key
        }

    fun getTopLevelDeprecationVersion(name: String): SmkLanguageVersion? =
        topLevelName2Deprecations[name]?.let { deprecations ->
            val latestDeprecated = deprecations.descendingMap().asIterable().firstOrNull { !it.value.itemRemoved }
            latestDeprecated?.key
        }

    fun getSubSectionIntroductionVersion(name: String, contextSectionKeyword: String): SmkLanguageVersion? =
        subsectionName2Introduction[SmkApiSubsectionContextAndDirective(contextSectionKeyword, name)]?.firstEntry()?.key

    fun getSubSectionDeprecationVersion(name: String, contextSectionKeyword: String): SmkLanguageVersion? =
        subsectionName2Deprecations[SmkApiSubsectionContextAndDirective(contextSectionKeyword, name)]?.let { deprecations ->
            val latestDeprecated = deprecations.descendingMap().asIterable().firstOrNull { !it.value.itemRemoved }
            latestDeprecated?.key
        }

    fun getSubSectionRemovalVersion(name: String, contextSectionKeyword: String): SmkLanguageVersion? =
        subsectionName2Deprecations[SmkApiSubsectionContextAndDirective(contextSectionKeyword, name)]?.let { deprecations ->
            val latestDeprecated = deprecations.descendingMap().asIterable().firstOrNull { it.value.itemRemoved }
            latestDeprecated?.key
        }

    @Suppress("unused")
    fun collectAllPossibleTopLevelSectionsKeywords(): Set<String> = topLevelName2Introduction.keys

    fun collectAllPossibleTopLevelArgsSectionsKeywords(): Set<String> {
        val keywords = mutableSetOf<String>()
        topLevelName2Introduction.forEach { keyword, tree ->
            if (tree.values.any() {it.isArgsSection}) {
                keywords.add(keyword)
            }
        }
        return keywords
    }

    fun collectAllPossibleUseSubsectionKeywordsIncludingExecutionSections(): Set<String> = collectAllPossibleSubsectionKeywords { type ->
        type == USE_KEYWORD
    }

    fun collectAllPossibleModuleSubsectionKeywords() = collectAllPossibleSubsectionKeywords { type ->
        type == SnakemakeNames.MODULE_KEYWORD
    }

    fun collectAllPossibleRuleOrCheckpointSubsectionKeywords() = collectAllPossibleSubsectionKeywords { type ->
        (type == RULE_KEYWORD) || (type == CHECKPOINT_KEYWORD)
    }

    fun collectAllPossibleRuleOrCheckpointExecutionKeywords(): Set<String> {
        val keywords = mutableSetOf<String>()

        // collect from all possible versions
        subsectionName2Introduction.forEach { contextAndDirective, treeMap ->
            val (type, keyword) = contextAndDirective
            if ((type == RULE_KEYWORD) || (type == CHECKPOINT_KEYWORD)) {  // use sections doesn't support execution flag
                val executionSection = treeMap.values.firstOrNull() { it.isExecutionSection }
                if (executionSection != null) {
                    keywords.add(keyword)
                }
            }
        }
        return keywords.toImmutableSet()
    }

    private fun collectAllPossibleSubsectionKeywords(ctxTypeFilter: (String) -> Boolean): Set<String> {
        val mutableSet = mutableSetOf<String>()

        // Collect all sections ignoring deprecation/removal/introduction marks:
        listOf(subsectionName2Introduction.keys, subsectionName2Deprecations.keys).forEach { keys ->
            mutableSet.addAll(
                keys.filter { ctxTypeFilter(it.contextType) }
                    .map { it.directiveKeyword }
            )
        }
        return mutableSet.unmodifiable()
    }

    fun getSubsectionIntroduction(
        name: String, version: SmkLanguageVersion, contextSectionKeyword: String
    ): Map.Entry<SmkLanguageVersion, SmkApiAnnotationKeywordIntroductionParams>? {
        val vers2ParamsTree = subsectionName2Introduction[SmkApiSubsectionContextAndDirective(contextSectionKeyword, name)]
        return vers2ParamsTree?.floorEntry(version)
    }

    fun getToplevelIntroduction(
        name: String, version: SmkLanguageVersion
    ): Map.Entry<SmkLanguageVersion, SmkApiAnnotationKeywordIntroductionParams>? {
        val vers2ParamsTree = topLevelName2Introduction[name]
        return vers2ParamsTree?.floorEntry(version)
    }

    fun getFunctionIntroductionsByFqn(
        version: SmkLanguageVersion
    ): List<Pair<String, Map.Entry<SmkLanguageVersion, SmkApiAnnotationKeywordIntroductionParams>>> = functionFqn2Introduction.mapNotNull { (directive, vers2ParamsTree) ->
        val entry = vers2ParamsTree.floorEntry(version)
        if (entry == null) null else (directive to entry)
    }

    fun getFunctionDeprecationsByFqn(
        version: SmkLanguageVersion
    ): List<Pair<String, Map.Entry<SmkLanguageVersion, SmkApiAnnotationKeywordDeprecationParams>>> = functionFqn2Deprecations.mapNotNull { (directive, vers2ParamsTree) ->
        val entry = vers2ParamsTree.floorEntry(version)
        if (entry == null) null else (directive to entry)
    }

    fun getFunctionDeprecationsByShortName(
        version: SmkLanguageVersion
    ): List<Pair<String, Map.Entry<SmkLanguageVersion, SmkApiAnnotationKeywordDeprecationParams>>> = functionShortName2Deprecations.mapNotNull { (directive, vers2ParamsTree) ->
        val entry = vers2ParamsTree.floorEntry(version)
        if (entry == null) null else (directive to entry)
    }

    fun getToplevelIntroductions(
        version: SmkLanguageVersion
    ): List<Pair<String, Map.Entry<SmkLanguageVersion, SmkApiAnnotationKeywordIntroductionParams>>> = topLevelName2Introduction.mapNotNull { (directive, vers2ParamsTree) ->
        val entry = vers2ParamsTree.floorEntry(version)
        if (entry == null) null else (directive to entry)
    }

    fun getSubsectionsIntroductions(
        version: SmkLanguageVersion
    ): List<Pair<SmkApiSubsectionContextAndDirective, Pair<SmkLanguageVersion, SmkApiAnnotationKeywordIntroductionParams>>> {
        return subsectionName2Introduction.mapNotNull { (ctxAndDirective, vers2ParamsTree) ->
            val entry = vers2ParamsTree.floorEntry(version)
            if (entry == null) null else (ctxAndDirective to (entry.key to entry.value))
        }
    }

    private fun getKeywordDeprecation(
        keywords: TreeMap<SmkLanguageVersion, SmkApiAnnotationKeywordDeprecationParams>?,
        version: SmkLanguageVersion
    ): Pair<SmkLanguageVersion, SmkApiAnnotationKeywordDeprecationParams>? {
        val floorEntry = keywords?.floorEntry(version)
        if (floorEntry == null) {
            return null
        }
        return floorEntry.key to floorEntry.value
    }

    companion object {
        /**
         * For Snakemake YAML api descriptor
         */
        private val RULE_LIKE_KEYWORDS = setOf(
            RULE_KEYWORD, CHECKPOINT_KEYWORD, USE_KEYWORD
        )

        fun getInstance() =
            ApplicationManager.getApplication().getService(SnakemakeApiYamlAnnotationsService::class.java)!!
    }
}

data class SmkApiSubsectionContextAndDirective(
    val contextType: String,
    val directiveKeyword: String
)