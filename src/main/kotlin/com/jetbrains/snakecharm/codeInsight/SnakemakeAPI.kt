package com.jetbrains.snakecharm.codeInsight

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.jetbrains.snakecharm.codeInsight.SnakemakeAPI.EXECUTION_SECTIONS_KEYWORDS
import com.jetbrains.snakecharm.codeInsight.SnakemakeAPI.SMK_VARS_WILDCARDS
import com.jetbrains.snakecharm.framework.SmkAPISubsectionContextAndDirective
import com.jetbrains.snakecharm.framework.SmkSupportProjectSettings
import com.jetbrains.snakecharm.framework.SmkSupportProjectSettingsListener
import com.jetbrains.snakecharm.framework.SnakemakeFrameworkAPIProvider
import com.jetbrains.snakecharm.framework.snakemakeAPIAnnotations.SmkAPIAnnParsingContextType
import com.jetbrains.snakecharm.lang.SmkLanguageVersion
import com.jetbrains.snakecharm.lang.SnakemakeNames
import com.jetbrains.snakecharm.lang.SnakemakeNames.MODULE_CONFIG_KEYWORD
import com.jetbrains.snakecharm.lang.SnakemakeNames.MODULE_META_WRAPPER_KEYWORD
import com.jetbrains.snakecharm.lang.SnakemakeNames.MODULE_REPLACE_PREFIX_KEYWORD
import com.jetbrains.snakecharm.lang.SnakemakeNames.MODULE_SKIP_VALIDATION_KEYWORD
import com.jetbrains.snakecharm.lang.SnakemakeNames.MODULE_SNAKEFILE_KEYWORD
import com.jetbrains.snakecharm.lang.SnakemakeNames.RULE_KEYWORD
import com.jetbrains.snakecharm.lang.SnakemakeNames.SECTION_BENCHMARK
import com.jetbrains.snakecharm.lang.SnakemakeNames.SECTION_CWL
import com.jetbrains.snakecharm.lang.SnakemakeNames.SECTION_INPUT
import com.jetbrains.snakecharm.lang.SnakemakeNames.SECTION_LOG
import com.jetbrains.snakecharm.lang.SnakemakeNames.SECTION_NOTEBOOK
import com.jetbrains.snakecharm.lang.SnakemakeNames.SECTION_OUTPUT
import com.jetbrains.snakecharm.lang.SnakemakeNames.SECTION_PARAMS
import com.jetbrains.snakecharm.lang.SnakemakeNames.SECTION_RESOURCES
import com.jetbrains.snakecharm.lang.SnakemakeNames.SECTION_RUN
import com.jetbrains.snakecharm.lang.SnakemakeNames.SECTION_SCRIPT
import com.jetbrains.snakecharm.lang.SnakemakeNames.SECTION_SHELL
import com.jetbrains.snakecharm.lang.SnakemakeNames.SECTION_TEMPLATE_ENGINE
import com.jetbrains.snakecharm.lang.SnakemakeNames.SECTION_THREADS
import com.jetbrains.snakecharm.lang.SnakemakeNames.SECTION_VERSION
import com.jetbrains.snakecharm.lang.SnakemakeNames.SECTION_WRAPPER
import com.jetbrains.snakecharm.lang.SnakemakeNames.SMK_AS_KEYWORD
import com.jetbrains.snakecharm.lang.SnakemakeNames.SMK_FROM_KEYWORD
import com.jetbrains.snakecharm.lang.SnakemakeNames.SMK_WITH_KEYWORD
import com.jetbrains.snakecharm.lang.SnakemakeNames.SNAKEMAKE_IO_METHOD_ANCIENT
import com.jetbrains.snakecharm.lang.SnakemakeNames.SNAKEMAKE_IO_METHOD_DIRECTORY
import com.jetbrains.snakecharm.lang.SnakemakeNames.SNAKEMAKE_IO_METHOD_DYNAMIC
import com.jetbrains.snakecharm.lang.SnakemakeNames.SNAKEMAKE_IO_METHOD_ENSURE
import com.jetbrains.snakecharm.lang.SnakemakeNames.SNAKEMAKE_IO_METHOD_PIPE
import com.jetbrains.snakecharm.lang.SnakemakeNames.SNAKEMAKE_IO_METHOD_PROTECTED
import com.jetbrains.snakecharm.lang.SnakemakeNames.SNAKEMAKE_IO_METHOD_REPEAT
import com.jetbrains.snakecharm.lang.SnakemakeNames.SNAKEMAKE_IO_METHOD_REPORT
import com.jetbrains.snakecharm.lang.SnakemakeNames.SNAKEMAKE_IO_METHOD_TEMP
import com.jetbrains.snakecharm.lang.SnakemakeNames.SNAKEMAKE_IO_METHOD_TOUCH
import com.jetbrains.snakecharm.lang.SnakemakeNames.SNAKEMAKE_IO_METHOD_UNPACK
import com.jetbrains.snakecharm.lang.SnakemakeNames.USE_EXCLUDE_KEYWORD
import com.jetbrains.snakecharm.lang.SnakemakeNames.WORKFLOW_CONFIGFILE_KEYWORD
import com.jetbrains.snakecharm.lang.SnakemakeNames.WORKFLOW_CONTAINERIZED_KEYWORD
import com.jetbrains.snakecharm.lang.SnakemakeNames.WORKFLOW_CONTAINER_KEYWORD
import com.jetbrains.snakecharm.lang.SnakemakeNames.WORKFLOW_ENVVARS_KEYWORD
import com.jetbrains.snakecharm.lang.SnakemakeNames.WORKFLOW_INCLUDE_KEYWORD
import com.jetbrains.snakecharm.lang.SnakemakeNames.WORKFLOW_PEPFILE_KEYWORD
import com.jetbrains.snakecharm.lang.SnakemakeNames.WORKFLOW_PEPSCHEMA_KEYWORD
import com.jetbrains.snakecharm.lang.SnakemakeNames.WORKFLOW_REPORT_KEYWORD
import com.jetbrains.snakecharm.lang.SnakemakeNames.WORKFLOW_SINGULARITY_KEYWORD
import com.jetbrains.snakecharm.lang.SnakemakeNames.WORKFLOW_WILDCARD_CONSTRAINTS_KEYWORD
import com.jetbrains.snakecharm.lang.SnakemakeNames.WORKFLOW_WORKDIR_KEYWORD
import com.jetbrains.snakecharm.lang.parser.SnakemakeLexer
import io.ktor.util.*
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.collections.immutable.toImmutableSet

/**
 * Also see [ImplicitPySymbolsProvider] class
 */
object SnakemakeAPI {
    const val UNPACK_FUNCTION = "unpack"

    const val SMK_VARS_CONFIG = "config"
    const val SMK_VARS_PEP = "pep"
    const val SMK_VARS_RULES = "rules"
    const val SMK_VARS_CHECKPOINTS = "checkpoints"
    const val SMK_VARS_SCATTER = "scatter"
    const val SMK_VARS_GATHER = "gather"
    const val SMK_VARS_ATTEMPT = "attempt"
    const val SMK_FUN_EXPAND = "expand"

    val FUNCTIONS_ALLOWING_SMKSL_INJECTION = setOf(
        "ancient", "directory", "temp", "pipe", "temporary", "protected",
        "dynamic", "touch", "repeat", "report", "local", "expand", "shell",
        "join", "multiext"
    )

    val FUNCTIONS_BANNED_FOR_WILDCARDS = listOf(
        SMK_FUN_EXPAND
    )

    const val SMK_VARS_WILDCARDS = "wildcards"
    const val WILDCARDS_ACCESSOR_CLASS = "snakemake.io.Wildcards"

    /**
     * Also see [ImplicitPySymbolsProvider], it also processes 'InputFiles', etc. symbols
     */
    val SECTION_ACCESSOR_CLASSES = mapOf(
        "snakemake.io.InputFiles" to "input",
        "snakemake.io.OutputFiles" to "output",
        "snakemake.io.Params" to "params",
        "snakemake.io.Log" to "log",
        "snakemake.io.Resources" to "resources"
    )
    const val SNAKEMAKE_MODULE_NAME_IO_PY = "io.py"
    const val SNAKEMAKE_MODULE_NAME_UTILS_PY = "utils.py"

    /**
     * Sections that execute external script with access to 'snakemake' object, i.e to 'snakemake.input',
     * 'snakemake.params' etc settings. So we cannot verify that log section mentioned in rule is
     * unused.
     */
    val EXECUTION_SECTIONS_THAT_ACCEPTS_SNAKEMAKE_PARAMS_OBJ_FROM_RULE = setOf(
        SECTION_WRAPPER, SECTION_NOTEBOOK, SECTION_SCRIPT, SECTION_CWL,
        SECTION_TEMPLATE_ENGINE
    )

    val EXECUTION_SECTIONS_KEYWORDS = setOf(
        SECTION_SHELL,
        *EXECUTION_SECTIONS_THAT_ACCEPTS_SNAKEMAKE_PARAMS_OBJ_FROM_RULE.toTypedArray()
    )

    // List of top-level sections
    // XXX: cannot move to SnakemakeAPIProjectService because it is used to create Lexer, Parser, WordScanner, Highlighter
    //      and not everywhere we could pass project instance
    val TOPLEVEL_ARGS_SECTION_KEYWORDS = setOf(
        WORKFLOW_CONFIGFILE_KEYWORD,
        WORKFLOW_REPORT_KEYWORD,
        WORKFLOW_SINGULARITY_KEYWORD,
        WORKFLOW_WILDCARD_CONSTRAINTS_KEYWORD,
        WORKFLOW_INCLUDE_KEYWORD,
        WORKFLOW_WORKDIR_KEYWORD,
        WORKFLOW_ENVVARS_KEYWORD,
        WORKFLOW_CONTAINER_KEYWORD,
        WORKFLOW_CONTAINERIZED_KEYWORD,
        WORKFLOW_PEPSCHEMA_KEYWORD,
        WORKFLOW_PEPFILE_KEYWORD
    )

    /**
     * For codeInsight
     */
    val SUBWORKFLOW_SECTIONS_KEYWORDS = setOf(
        SnakemakeNames.SUBWORKFLOW_WORKDIR_KEYWORD,
        SnakemakeNames.SUBWORKFLOW_SNAKEFILE_KEYWORD,
        SnakemakeNames.SUBWORKFLOW_CONFIGFILE_KEYWORD
    )

    /**
     * For uses codeInsight
     */

    val USE_DECLARATION_KEYWORDS = setOf(
        RULE_KEYWORD,
        SMK_FROM_KEYWORD,
        SMK_AS_KEYWORD,
        SMK_WITH_KEYWORD,
        USE_EXCLUDE_KEYWORD
    )


    /**
     * Ordered list of sections which defines wildcards
     */
    val WILDCARDS_DEFINING_SECTIONS_KEYWORDS = listOf(
        SECTION_OUTPUT, SECTION_LOG, SECTION_BENCHMARK
    )


    /**
     * Peppy config keys in yaml file
     */
    const val PEPPY_CONFIG_PEP_VERSION = "pep_version"
    const val PEPPY_CONFIG_SAMPLE_TABLE = "sample_table"
    const val PEPPY_CONFIG_SUBSAMPLE_TABLE = "subsample_table"
    const val PEPPY_CONFIG_SAMPLE_MODIFIERS = "sample_modifiers"
    const val PEPPY_CONFIG_PROJECT_MODIFIERS = "project_modifiers"

    @Suppress("unused")
    val PEPPY_CONFIG_TEXT_KEYS = setOf(
        PEPPY_CONFIG_PEP_VERSION, PEPPY_CONFIG_SAMPLE_TABLE, PEPPY_CONFIG_SUBSAMPLE_TABLE,
    )

    @Suppress("unused")
    val PEPPY_CONFIG_MAPPING_KEYS = setOf(
        PEPPY_CONFIG_SAMPLE_MODIFIERS, PEPPY_CONFIG_PROJECT_MODIFIERS
    )


    val IO_FLAG_2_SUPPORTED_SECTION: HashMap<String, List<String>> = hashMapOf(
        SNAKEMAKE_IO_METHOD_ANCIENT to listOf(SECTION_INPUT),
        SNAKEMAKE_IO_METHOD_PROTECTED to listOf(SECTION_OUTPUT, SECTION_LOG, SECTION_BENCHMARK),
        SNAKEMAKE_IO_METHOD_DIRECTORY to listOf(SECTION_OUTPUT),
        SNAKEMAKE_IO_METHOD_REPORT to listOf(SECTION_OUTPUT),
        SNAKEMAKE_IO_METHOD_TEMP to listOf(SECTION_INPUT, SECTION_OUTPUT),
        SNAKEMAKE_IO_METHOD_TOUCH to listOf(SECTION_OUTPUT, SECTION_LOG, SECTION_BENCHMARK),
        SNAKEMAKE_IO_METHOD_PIPE to listOf(SECTION_OUTPUT),
        SNAKEMAKE_IO_METHOD_REPEAT to listOf(SECTION_BENCHMARK),
        SNAKEMAKE_IO_METHOD_UNPACK to listOf(SECTION_INPUT),
        SNAKEMAKE_IO_METHOD_DYNAMIC to listOf(SECTION_OUTPUT),
        SNAKEMAKE_IO_METHOD_ENSURE to listOf(SECTION_OUTPUT)
    )

    val SMK_API_PKG_NAME_SMK = "snakemake"
    val SMK_API_PKG_NAME_SMK_MINIMAL = "snakemake-minimal"
    val SMK_API_VERS_6_1 = "6.1"
}

@Service
class SnakemakeAPIService {
    val RULE_OR_CHECKPOINT_ARGS_SECTION_KEYWORDS = SnakemakeFrameworkAPIProvider.getInstance()
        .collectAllPossibleRuleOrCheckpointSubsectionKeywords()

    val RULE_OR_CHECKPOINT_SECTION_KEYWORDS = (RULE_OR_CHECKPOINT_ARGS_SECTION_KEYWORDS + setOf(SECTION_RUN))

    val USE_SECTIONS_KEYWORDS = RULE_OR_CHECKPOINT_SECTION_KEYWORDS + SnakemakeFrameworkAPIProvider.getInstance()
        .collectAllPossibleUseSubsectionKeywords() - EXECUTION_SECTIONS_KEYWORDS - SECTION_RUN

    /**
     * For modules codeInsight
     */
    val MODULE_SECTIONS_KEYWORDS = setOf(
        MODULE_SNAKEFILE_KEYWORD,
        MODULE_CONFIG_KEYWORD,
        MODULE_SKIP_VALIDATION_KEYWORD,
        MODULE_META_WRAPPER_KEYWORD,
        MODULE_REPLACE_PREFIX_KEYWORD
    ) + SnakemakeFrameworkAPIProvider.getInstance()
        .collectAllPossibleModuleSubsectionKeywords()

    companion object {
        fun getInstance() = ApplicationManager.getApplication().getService(SnakemakeAPIService::class.java)!!
    }
}

@Service(Service.Level.PROJECT)
class SnakemakeAPIProjectService(val project: Project): Disposable {
    private var state: SnakemakeAPIProjectState = SnakemakeAPIProjectState.EMPTY

    /**
     * Checks if a given keyword requires only one argument
     *
     * @param keyword The keyword to be checked.
     * @param contextKeywordOrType The context keyword or type to check against.
     * @return `true` if the keyword is a single argument section keyword within the given context, otherwise `false`.
     */
    fun isSubsectionSingleArgumentOnly(keyword: String, contextKeywordOrType: String): Boolean {
        val list =  if (contextKeywordOrType == SmkAPIAnnParsingContextType.TOP_LEVEL.typeStr) {
            state.contextType2SingleArgSectionKeywords[SmkAPIAnnParsingContextType.TOP_LEVEL.typeStr]
        } else {
            state.contextType2SingleArgSectionKeywords[contextKeywordOrType]
        }
        return list?.contains(keyword) == true
    }

    /**
     * Determines if a given section in a Snakemake file only allows positional arguments and disallows keyword arguments.
     *
     * @param keyword The specific keyword being checked.
     * @param contextKeywordOrType The context or type of keyword, indicating whether it is a top-level workflow, module, or subworkflow.
     * @return A Boolean indicating whether the given section only allows positional arguments.
     */
    fun isSubsectionWithOnlyPositionalArguments(keyword: String, contextKeywordOrType: String): Boolean {
        val list =  if (contextKeywordOrType == SmkAPIAnnParsingContextType.TOP_LEVEL.typeStr) {
            state.contextType2PositionalOnlySectionKeywords[SmkAPIAnnParsingContextType.TOP_LEVEL.typeStr]
        } else {
            state.contextType2PositionalOnlySectionKeywords[contextKeywordOrType]
        }
        return list?.contains(keyword) == true
    }

    fun isTopLevelArgsSectionKeyword(keyword: String): Boolean {
        // XXX: Used in parsing/lexing, cannot collect from YAML file
        return keyword in SnakemakeAPI.TOPLEVEL_ARGS_SECTION_KEYWORDS
    }

    fun isTopLevelKeyword(keyword: String): Boolean {
        // XXX: Used in parsing/lexing, cannot collect from YAML file
        return keyword in SnakemakeLexer.KEYWORD_LIKE_SECTION_NAME_2_TOKEN_TYPE
    }

    fun getTopLevelsKeywords(): Set<String> {
        // XXX: Used in parsing/lexing, cannot collect from YAML file
        return SnakemakeLexer.KEYWORD_LIKE_SECTION_NAME_2_TOKEN_TYPE.keys.unmodifiable()
    }

    fun getSubsectionPossibleLambdaParamNames(): Set<String> = state.subsectionsAllPossibleArgNames

    fun isSubsectionValidForInjection(keyword: String, contextKeywordOrType: String): Boolean {
        val keywords = state.contextType2NotValidForInjectionSubsectionKeywords[contextKeywordOrType]
        return (keywords != null) && (keyword !in keywords)
    }
    /**
     * Get set of lambda/function arguments for rule/checkpoint sections that supports support such access
     */
    fun getLambdaArgsForSubsection(keyword: String?, contextKeywordOrType: String?): Array<String> {
        if (keyword != null && contextKeywordOrType != null) {
            val key = SmkAPISubsectionContextAndDirective(contextKeywordOrType, keyword)
            return state.contextTypeAndSubsection2LambdaArgs[key] ?: emptyArray<String>()
        }
        return emptyArray()
    }

    /**
     * Determines if the given keyword is part of the wildcards expanding sections.
     * These sections consider all injection identifiers to be wildcards
     *
     * @param keyword the keyword to check, which may be null.
     * @param contextKeywordOrType the context keyword or type, currently not utilized.
     * @return true if the keyword is found within the set of wildcards expanding sections, false otherwise.
     */
    fun isSubsectionWildcardsExpanding(keyword: String?, contextKeywordOrType: String?): Boolean   {
        if (keyword != null && contextKeywordOrType != null) {
            val keywords = state.contextType2WildcardsExpandingSubsectionKeywords[contextKeywordOrType]
            return (keywords != null) && (keyword in keywords)
        }
        return false
    }

    /**
     * For type inference:
     * Some sections in snakemake are inaccessible after `rules.NAME.<section>`, so this set is required
     * to filter these sections for resolve and completion
     */
    fun isSubsectionAccessibleInRuleObject(keyword: String?, contextKeywordOrType: String?): Boolean   {
        if (keyword != null && contextKeywordOrType != null) {
            val keywords = state.contextType2AccessibleInRuleObjectSubsectionKeywords[contextKeywordOrType]
            return (keywords != null) && (keyword in keywords)
        }
        return false
    }

    private val SMK_SL_INITIAL_TYPE_ACCESSIBLE_SECTIONS = setOf(
        SECTION_INPUT,
        SECTION_OUTPUT, SECTION_LOG,
        SECTION_THREADS, SECTION_PARAMS,
        SECTION_RESOURCES,
        SECTION_VERSION
    )
    /**
     * For type inference:
     * In SnakemakeSL some sections are inaccessible in `shell: "{<section>}"` and other sections, which doesn't
     * expand wildcards.
     */
    fun isSubsectionAccessibleAsPlaceholder(keyword: String?, contextKeywordOrType: String?): Boolean   {
        return keyword in SMK_SL_INITIAL_TYPE_ACCESSIBLE_SECTIONS

//        if (keyword != null && contextKeywordOrType != null) {
//            val keywords = state.contextType2AccessibleInRuleObjectSubsectionKeywords[contextKeywordOrType]
//            return (keywords != null) && (keyword in keywords)
//        }
        return false
//        return false
    }

    fun getSubsectionAccessibleAsPlaceholder(contextKeywordOrType: String?): Set<String>   {
        if (contextKeywordOrType == null){
            return emptySet()
        }
        return SMK_SL_INITIAL_TYPE_ACCESSIBLE_SECTIONS
    }

    private fun doRefresh(version: String?) {
        val newState = if (version == null) {
            SnakemakeAPIProjectState.EMPTY
        } else {
            val apiProvider = SnakemakeFrameworkAPIProvider.getInstance()

            val contextType2SingleArgSectionKeywords = HashMap<String, MutableList<String>>()
            val contextType2PositionalOnlySectionKeywords = HashMap<String, MutableList<String>>()
            val contextTypeAndSubsection2LambdaArgs = HashMap<SmkAPISubsectionContextAndDirective, Array<String>>()
            val contextType2NotValidForInjectionSubsectionKeywords = HashMap<String, MutableSet<String>>()
            val contextType2WildcardsExpandingSubsectionKeywords = HashMap<String, MutableSet<String>>()
            val contextType2AccessibleInRuleObjectSubsectionKeywords = HashMap<String, MutableSet<String>>()

            // add top-level data:
            val toplevelIntroductions = apiProvider.getToplevelIntroductions(SmkLanguageVersion(version))
            contextType2SingleArgSectionKeywords.put(
                SmkAPIAnnParsingContextType.TOP_LEVEL.typeStr,
                toplevelIntroductions.mapNotNull { (name, e) ->
                    if (e.value.multipleArgsAllowed) null else name
                }.toMutableList()
            )
            contextType2PositionalOnlySectionKeywords.put(
                SmkAPIAnnParsingContextType.TOP_LEVEL.typeStr,
                toplevelIntroductions.mapNotNull { (name, e) ->
                    if (e.value.keywordArgsAllowed) null else name
                }.toMutableList()
            )
            toplevelIntroductions.mapNotNull { (name, e) ->
                if (!e.value.isSection) null else name
            }.forEach { keyword ->
                require(isTopLevelKeyword(keyword)) {
                    "YAML format error: '$keyword' should be one of" +
                            " [${SnakemakeLexer.KEYWORD_LIKE_SECTION_NAME_2_TOKEN_TYPE.keys.sorted().joinToString(separator = ", ")}]. " +
                            "Please file a feature request at https://github.com/JetBrains-Research/snakecharm/issues"
                }
            }

            // add subsections data:
            val subsectionIntroductions = apiProvider.getSubsectionsIntroductions(SmkLanguageVersion(version))
            subsectionIntroductions.forEach { (ctxAndName, versAndParams) ->
                val (_, params) = versAndParams
                if (!params.multipleArgsAllowed) {
                    val context = ctxAndName.contextType
                    val keywords = contextType2SingleArgSectionKeywords.getOrPut(context) { arrayListOf<String>() }
                    keywords.add(ctxAndName.directiveKeyword)
                }

                if (!params.keywordArgsAllowed) {
                    val context = ctxAndName.contextType
                    val keywords = contextType2PositionalOnlySectionKeywords.getOrPut(context) { arrayListOf<String>() }
                    keywords.add(ctxAndName.directiveKeyword)
                }
                if (!params.isPlaceholderInjectionAllowed) {
                    val context = ctxAndName.contextType
                    val keywords = contextType2NotValidForInjectionSubsectionKeywords.getOrPut(context) { mutableSetOf<String>() }
                    keywords.add(ctxAndName.directiveKeyword)
                }
                if (params.isPlaceholderExpandedToWildcard) {
                    val context = ctxAndName.contextType
                    val keywords = contextType2WildcardsExpandingSubsectionKeywords.getOrPut(context) { mutableSetOf<String>() }
                    keywords.add(ctxAndName.directiveKeyword)
                }
                if (params.isAccessibleInRuleObj) {
                    val context = ctxAndName.contextType
                    val keywords = contextType2AccessibleInRuleObjectSubsectionKeywords.getOrPut(context) { mutableSetOf<String>() }
                    keywords.add(ctxAndName.directiveKeyword)
                }

                if (params.lambdaArgs.isNotEmpty()) {
                    val value = versAndParams.second.lambdaArgs.toTypedArray()
                    require(value.isNotEmpty()) {
                        "YAML format error: 'lambda_args' should not be empty for directive '${ctxAndName.directiveKeyword}'."
                    }
                    require(value[0] == SMK_VARS_WILDCARDS) {
                        "YAML format error: first lambda argument for directive '${ctxAndName.directiveKeyword}' should be '${SMK_VARS_WILDCARDS}'. If not" +
                                " then please file a feature request at https://github.com/JetBrains-Research/snakecharm/issues ."
                    }
                    contextTypeAndSubsection2LambdaArgs.put(ctxAndName, value)
                }
            }

            SnakemakeAPIProjectState(
                contextType2SingleArgSectionKeywords = contextType2SingleArgSectionKeywords.toImmutableMap(),
                contextType2PositionalOnlySectionKeywords = contextType2PositionalOnlySectionKeywords.toImmutableMap(),
                contextTypeAndSubsection2LambdaArgs = contextTypeAndSubsection2LambdaArgs.toImmutableMap(),
                contextType2NotValidForInjectionSubsectionKeywords=contextType2NotValidForInjectionSubsectionKeywords.toImmutableMap(),
                contextType2WildcardsExpandingSubsectionKeywords=contextType2WildcardsExpandingSubsectionKeywords.toImmutableMap(),
                contextType2AccessibleInRuleObjectSubsectionKeywords = contextType2AccessibleInRuleObjectSubsectionKeywords.toImmutableMap()
            )
        }
        state = newState
    }

    fun initOnStartup(smkSettings: SmkSupportProjectSettings) {
        val connection = project.messageBus.connect()

        connection.subscribe(SmkSupportProjectSettings.TOPIC, object : SmkSupportProjectSettingsListener {
            override fun stateChanged(
                newSettings: SmkSupportProjectSettings,
                oldState: SmkSupportProjectSettings.State,
                sdkRenamed: Boolean,
                sdkRemoved: Boolean
            ) {
                if (!newSettings.snakemakeSupportEnabled) {
                    // otherwise update later on enabled
                    return
                }

                val sdkNameNotChanged = oldState.snakemakeLanguageVersion == newSettings.snakemakeLanguageVersion
                if (sdkNameNotChanged) {
                    return
                }

                doRefresh(newSettings.snakemakeLanguageVersion)
            }

            override fun enabled(newSettings: SmkSupportProjectSettings) {
                doRefresh(newSettings.snakemakeLanguageVersion)
            }
        })
        Disposer.register(this, connection)


        doRefresh(smkSettings.snakemakeLanguageVersion)
    }

    override fun dispose() {
        // Do nothing, used as parent disposable
    }

    companion object {
        fun getInstance(project: Project) = project.getService(SnakemakeAPIProjectService::class.java)!!
    }
}

internal data class SnakemakeAPIProjectState(
    val contextType2SingleArgSectionKeywords: Map<String, List<String>>,
    val contextType2PositionalOnlySectionKeywords: Map<String, List<String>>,
    val contextTypeAndSubsection2LambdaArgs:  Map<SmkAPISubsectionContextAndDirective, Array<String>>,
    val contextType2NotValidForInjectionSubsectionKeywords:  Map<String, Set<String>>,
    val contextType2WildcardsExpandingSubsectionKeywords:  Map<String, Set<String>>,
    val contextType2AccessibleInRuleObjectSubsectionKeywords:  Map<String, Set<String>>,
) {
    val subsectionsAllPossibleArgNames = contextTypeAndSubsection2LambdaArgs.values.flatMap { it.asIterable() }.toImmutableSet()

    companion object {
        val EMPTY = SnakemakeAPIProjectState(emptyMap(), emptyMap(), emptyMap(), emptyMap(), emptyMap(), emptyMap())
    }
}