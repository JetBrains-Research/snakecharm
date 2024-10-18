package com.jetbrains.snakecharm.codeInsight

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.jetbrains.snakecharm.codeInsight.SnakemakeApi.SMK_VARS_WILDCARDS
import com.jetbrains.snakecharm.framework.SmkApiSubsectionContextAndDirective
import com.jetbrains.snakecharm.framework.SmkSupportProjectSettings
import com.jetbrains.snakecharm.framework.SmkSupportProjectSettingsListener
import com.jetbrains.snakecharm.framework.SnakemakeApiYamlAnnotationsService
import com.jetbrains.snakecharm.framework.snakemakeAPIAnnotations.SmkApiAnnotationKeywordDeprecationParams
import com.jetbrains.snakecharm.framework.snakemakeAPIAnnotations.SmkApiAnnotationParsingContextType
import com.jetbrains.snakecharm.framework.snakemakeAPIAnnotations.SmkApiAnnotationParsingContextType.TOP_LEVEL
import com.jetbrains.snakecharm.lang.SmkLanguageVersion
import com.jetbrains.snakecharm.lang.SnakemakeNames
import com.jetbrains.snakecharm.lang.SnakemakeNames.CHECKPOINT_KEYWORD
import com.jetbrains.snakecharm.lang.SnakemakeNames.MODULE_KEYWORD
import com.jetbrains.snakecharm.lang.SnakemakeNames.RULE_KEYWORD
import com.jetbrains.snakecharm.lang.SnakemakeNames.SECTION_BENCHMARK
import com.jetbrains.snakecharm.lang.SnakemakeNames.SECTION_LOG
import com.jetbrains.snakecharm.lang.SnakemakeNames.SECTION_OUTPUT
import com.jetbrains.snakecharm.lang.SnakemakeNames.SECTION_RUN
import com.jetbrains.snakecharm.lang.SnakemakeNames.SECTION_SHELL
import com.jetbrains.snakecharm.lang.SnakemakeNames.SMK_AS_KEYWORD
import com.jetbrains.snakecharm.lang.SnakemakeNames.SMK_FROM_KEYWORD
import com.jetbrains.snakecharm.lang.SnakemakeNames.SMK_WITH_KEYWORD
import com.jetbrains.snakecharm.lang.SnakemakeNames.USE_EXCLUDE_KEYWORD
import com.jetbrains.snakecharm.lang.SnakemakeNames.USE_KEYWORD
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
 * Also see [SmkImplicitPySymbolsProvider] class
 */
object SnakemakeApi {
    const val UNPACK_FUNCTION = "unpack"

    const val SMK_VARS_CONFIG = "config"
    const val SMK_VARS_PEP = "pep"
    const val SMK_VARS_RULES = "rules"
    const val SMK_VARS_CHECKPOINTS = "checkpoints"
    const val SMK_VARS_SCATTER = "scatter"
    const val SMK_VARS_GATHER = "gather"
    const val SMK_FUN_EXPAND = "expand"

    val FUNCTIONS_BANNED_FOR_WILDCARDS = listOf(
        // TODO: Is possible to move into YAML
        SMK_FUN_EXPAND
    )

    const val SMK_VARS_WILDCARDS = "wildcards"
    const val WILDCARDS_ACCESSOR_CLASS = "snakemake.io.Wildcards"

    /**
     * Also see [SmkImplicitPySymbolsProvider], it also processes 'InputFiles', etc. symbols
     */
    // TODO: Is possible to move into YAML
    val SECTION_ACCESSOR_CLASSES = mapOf(
        "snakemake.io.InputFiles" to "input",
        "snakemake.io.OutputFiles" to "output",
        "snakemake.io.Params" to "params",
        "snakemake.io.Log" to "log",
        "snakemake.io.Resources" to "resources"
    )

    // List of top-level sections
    // XXX: cannot move to SnakemakeApiService because it is used to create Lexer, Parser, WordScanner, Highlighter
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

    val SMK_API_PKG_NAME_SMK = "snakemake"
    val SMK_API_PKG_NAME_SMK_MINIMAL = "snakemake-minimal"
    val SMK_API_VERS_6_1 = "6.1"
}

@Service(Service.Level.PROJECT)
class SnakemakeApiService(val project: Project): Disposable {
    private var state: SnakemakeApiStateForLangLevel = SnakemakeApiStateForLangLevel.EMPTY

    /**
     * Checks if a given keyword requires only one argument
     *
     * @param keyword The keyword to be checked.
     * @param contextKeywordOrType The context keyword or type to check against.
     * @return `true` if the keyword is a single argument section keyword within the given context, otherwise `false`.
     */
    fun isSubsectionSingleArgumentOnly(keyword: String, contextKeywordOrType: String): Boolean {
        val list =  if (contextKeywordOrType == TOP_LEVEL.typeStr) {
            state.contextType2SingleArgSectionKeywords[TOP_LEVEL.typeStr]
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
        val list =  if (contextKeywordOrType == TOP_LEVEL.typeStr) {
            state.contextType2PositionalOnlySectionKeywords[TOP_LEVEL.typeStr]
        } else {
            state.contextType2PositionalOnlySectionKeywords[contextKeywordOrType]
        }
        return list?.contains(keyword) == true
    }

    @Suppress("unused")
    fun isTopLevelArgsSectionKeyword(keyword: String): Boolean {
        // XXX: Used in parsing/lexing, cannot collect from YAML file
        return keyword in SnakemakeApi.TOPLEVEL_ARGS_SECTION_KEYWORDS
    }

    fun isTopLevelKeyword(keyword: String): Boolean {
        // XXX: Used in parsing/lexing, cannot collect from YAML file
        return keyword in SnakemakeLexer.KEYWORD_LIKE_SECTION_NAME_2_TOKEN_TYPE
    }

    @Suppress("unused")
    fun getTopLevelsKeywords(): Set<String> {
        // XXX: Used in parsing/lexing, cannot collect from YAML file
        return SnakemakeLexer.KEYWORD_LIKE_SECTION_NAME_2_TOKEN_TYPE.keys.unmodifiable()
    }

    fun getSubsectionPossibleLambdaParamNames(): Set<String> = state.subsectionsAllPossibleArgNames

    fun isFunctionFqnValidForInjection(fqn: String) = fqn in state.funFqnValidForInjection

    fun isFunctionShortNameValidForInjection(shortName: String) = shortName in state.funShortNamesValidForInjection

    fun isSubsectionValidForInjection(keyword: String, contextKeywordOrType: String): Boolean {
        require(contextKeywordOrType != SmkApiAnnotationParsingContextType.FUNCTION.typeStr)
        require(contextKeywordOrType != TOP_LEVEL.typeStr)

        val keywords = state.contextType2NotValidForInjectionSubsectionKeywords[contextKeywordOrType]
        // our default is to allow injection in all sections => if section not registered, or event context type => true
        return (keywords == null) || (keyword !in keywords)
    }
    /**
     * Get set of lambda/function arguments for rule/checkpoint sections that supports support such access
     */
    fun getLambdaArgsForSubsection(keyword: String?, contextKeywordOrType: String?): Array<String> {
        if (keyword != null && contextKeywordOrType != null) {
            val key = SmkApiSubsectionContextAndDirective(contextKeywordOrType, keyword)
            return state.contextTypeAndSubsection2LambdaArgs[key] ?: emptyArray<String>()
        }
        return emptyArray()
    }

    fun getFunctionSectionsRestrictionsByFqn(fqn: String): Array<String> {
        return state.funFqnToSectionRestrictionList[fqn] ?: emptyArray<String>()
    }

    fun getFunctionDeprecationByFqn(fqn: String): Pair<SmkLanguageVersion, SmkApiAnnotationKeywordDeprecationParams>? {
        val entry = state.funFqnDeprecations[fqn]
        return if (entry == null) null else (entry.key to entry.value)
    }

    fun getFunctionDeprecationByShortName(fqn: String): Pair<SmkLanguageVersion, SmkApiAnnotationKeywordDeprecationParams>? {
        val entry = state.funShortNameDeprecations[fqn]
        return if (entry == null) null else (entry.key to entry.value)
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

    /**
     * For type inference:
     * In SnakemakeSL some sections are inaccessible in `shell: "{<section>}"` and other sections, which doesn't
     * expand wildcards.
     */
    fun isSubsectionAccessibleAsPlaceholder(keyword: String?, contextKeywordOrType: String?): Boolean   {
        if (keyword != null && contextKeywordOrType != null) {
            val keywords = state.contextType2AccessibleAccessibleAsPlaceholderSubsectionKeywords[contextKeywordOrType]
            return (keywords != null) && (keyword in keywords)
        }
        return false
    }

    fun getSubsectionAccessibleAsPlaceholder(contextKeywordOrType: String?): Set<String>   {
        if (contextKeywordOrType != null) {
            val keywords = state.contextType2AccessibleAccessibleAsPlaceholderSubsectionKeywords[contextKeywordOrType]
            if (keywords != null) {
                return keywords.toSet()
            }
        }
        return emptySet()
    }

    /**
     * Sections that execute external script with access to 'snakemake' object, i.e to 'snakemake.input',
     * 'snakemake.params' etc settings. So we cannot verify that log section mentioned in rule is
     * unused.
     */
    fun getExecutionSectionsKeywordsThatAcceptsSnakemakeObj(): Set<String> =
        getExecutionSectionsKeyword() - setOf(SECTION_SHELL, SECTION_RUN)

    fun getAllPossibleExecutionSectionsKeyword(): Set<String> = state.allPossibleExecutionSectionKeywords

    fun getExecutionSectionsKeyword(): Set<String> {
        // assume the same sections for rules & checkpoints:
        val keywords = state.contextType2ExecutionSectionSubsectionKeywords[RULE_KEYWORD]
        if (keywords != null) {
            return keywords.toSet()
        }
        return emptySet()
    }

    /**
     * For modules codeInsight
     */
    fun getAllPossibleModuleSectionKeywords(): Set<String> = state.allPossibleModuleSectionKeywords

    @Suppress("unused")
    fun getModuleAllSectionTypesKeywords(): Set<String> = state.contextType2SubsectionKeywords[MODULE_KEYWORD] ?: emptySet()

    @Suppress("unused")
    fun getModuleArgsSectionKeywords(): Set<String> = state.contextType2ArgsSubsectionKeywords[MODULE_KEYWORD] ?: emptySet()

    fun getAllPossibleRuleOrCheckpointArgsSectionKeywords(): Set<String> = state.allPossibleRuleOrCheckpointSectionKeywords

    @Suppress("unused")
    fun getRuleOrCheckpointAllSectionTypesKeywords(): Set<String> =
        (state.contextType2SubsectionKeywords[RULE_KEYWORD] ?: emptySet()) +
                (state.contextType2SubsectionKeywords[CHECKPOINT_KEYWORD] ?: emptySet())

    fun getRuleOrCheckpointArgsSectionKeywords(): Set<String> =
        (state.contextType2ArgsSubsectionKeywords[RULE_KEYWORD] ?: emptySet()) +
                (state.contextType2ArgsSubsectionKeywords[CHECKPOINT_KEYWORD] ?: emptySet())

    fun getAllPossibleUseSectionKeywordsIncludingExecSections() = state.allPossibleUseSectionKeywordsIncludingExecSections

    fun getUseSectionKeywords(): Set<String> = state.contextType2SubsectionKeywords[USE_KEYWORD] ?: emptySet()

    private fun doRefresh(version: String?) {
        val newState = if (version == null) {
            SnakemakeApiStateForLangLevel.EMPTY
        } else {
            val yamlApi = SnakemakeApiYamlAnnotationsService.getInstance()

            val contextType2SubsectionKeywords = HashMap<String, MutableSet<String>>()
            val contextType2ArgsSubsectionKeywords = HashMap<String, MutableSet<String>>()
            val contextType2SingleArgSectionKeywords = HashMap<String, MutableList<String>>()
            val contextType2PositionalOnlySectionKeywords = HashMap<String, MutableList<String>>()
            val contextTypeAndSubsection2LambdaArgs = HashMap<SmkApiSubsectionContextAndDirective, Array<String>>()
            val contextType2NotValidForInjectionSubsectionKeywords = HashMap<String, MutableSet<String>>()
            val contextType2WildcardsExpandingSubsectionKeywords = HashMap<String, MutableSet<String>>()
            val contextType2AccessibleInRuleObjectSubsectionKeywords = HashMap<String, MutableSet<String>>()
            val contextType2AccessibleAccessibleAsPlaceholderSubsectionKeywords = HashMap<String, MutableSet<String>>()
            val contextType2ExecutionSectionSubsectionKeywords = HashMap<String, MutableSet<String>>()
            val funFqnToSectionRestrictionList = HashMap<String, Array<String>>()
            val funFqnValidForInjection = mutableSetOf<String>()

            val smkLangVers = SmkLanguageVersion(version)

            // add top-level data:
            val toplevelIntroductions = yamlApi.getToplevelIntroductions(smkLangVers)
            contextType2SingleArgSectionKeywords.put(
                TOP_LEVEL.typeStr,
                toplevelIntroductions.mapNotNull { (directiveKeyword, versAndParams) ->
                    val (_, params) = versAndParams
                    if (params.multipleArgsAllowed) null else directiveKeyword
                }.toMutableList()
            )
            contextType2PositionalOnlySectionKeywords.put(
                TOP_LEVEL.typeStr,
                toplevelIntroductions.mapNotNull { (directiveKeyword, versAndParams) ->
                    val (_, params) = versAndParams
                    if (params.keywordArgsAllowed) null else directiveKeyword
                }.toMutableList()
            )

            contextType2SubsectionKeywords.put(
                TOP_LEVEL.typeStr,
                toplevelIntroductions.map { (directiveKeyword, _) ->
                    directiveKeyword
                }.toMutableSet()
            )

            contextType2ArgsSubsectionKeywords.put(
                TOP_LEVEL.typeStr,
                toplevelIntroductions.mapNotNull { (directiveKeyword, versAndParams) ->
                    val (_, params) = versAndParams
                    if (params.isArgsSection) directiveKeyword else null
                }.toMutableSet()
            )

            toplevelIntroductions.forEach { (directiveKeyword, _) ->
                require(isTopLevelKeyword(directiveKeyword)) {
                    "YAML format error: '$directiveKeyword' should be one of" +
                            " [${
                                SnakemakeLexer.KEYWORD_LIKE_SECTION_NAME_2_TOKEN_TYPE.keys.sorted()
                                    .joinToString(separator = ", ")
                            }]. " +
                            "Please file a feature request at https://github.com/JetBrains-Research/snakecharm/issues"
                }
            }

            // add subsections data:
            val subsectionIntroductions = yamlApi.getSubsectionsIntroductions(smkLangVers)
            subsectionIntroductions.forEach { (ctxAndName, versAndParams) ->
                val (_, params) = versAndParams
                val (context, directiveKeyword) = ctxAndName

                if (context != USE_KEYWORD || !params.isExecutionSection) {
                    // for USE block - do not add execution sections, they are not supported
                    // for rules/checkpoints - add all
                    contextType2SubsectionKeywords.getOrPut(context) { mutableSetOf<String>() }.add(directiveKeyword)
                }

                if (params.isArgsSection) {
                    val keywords = contextType2ArgsSubsectionKeywords.getOrPut(context) { mutableSetOf<String>() }
                    keywords.add(directiveKeyword)

                }

                if (!params.multipleArgsAllowed) {
                    val keywords = contextType2SingleArgSectionKeywords.getOrPut(context) { arrayListOf<String>() }
                    keywords.add(directiveKeyword)
                }

                if (!params.keywordArgsAllowed) {
                    val keywords = contextType2PositionalOnlySectionKeywords.getOrPut(context) { arrayListOf<String>() }
                    keywords.add(directiveKeyword)
                }
                if (!params.isPlaceholderInjectionAllowed) {
                    val keywords = contextType2NotValidForInjectionSubsectionKeywords.getOrPut(context) {
                        mutableSetOf<String>()
                    }
                    keywords.add(directiveKeyword)
                }
                if (params.isPlaceholderExpandedToWildcard) {
                    val keywords = contextType2WildcardsExpandingSubsectionKeywords.getOrPut(context) {
                        mutableSetOf<String>()
                    }
                    keywords.add(directiveKeyword)
                }
                if (params.isAccessibleInRuleObj) {
                    val keywords = contextType2AccessibleInRuleObjectSubsectionKeywords.getOrPut(context) {
                        mutableSetOf<String>()
                    }
                    keywords.add(directiveKeyword)
                }
                if (params.isAccessibleAsPlaceholder) {
                    val keywords = contextType2AccessibleAccessibleAsPlaceholderSubsectionKeywords.getOrPut(context) {
                        mutableSetOf<String>()
                    }
                    keywords.add(directiveKeyword)
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
                if (params.isExecutionSection) {
                    if (context == RULE_KEYWORD || context == CHECKPOINT_KEYWORD) {
                        val keywords = contextType2ExecutionSectionSubsectionKeywords.getOrPut(context) {
                            mutableSetOf<String>()
                        }
                        keywords.add(directiveKeyword)
                    }
                }
            }

            // functions
            val funcIntroductionsByFqn = yamlApi.getFunctionIntroductionsByFqn(smkLangVers)
            funcIntroductionsByFqn.forEach { (fqn, versAndParams) ->
                val (_, params) = versAndParams

                if (params.limitToSections.isNotEmpty()) {
                    val value = params.limitToSections.toTypedArray()
                    require(value.isNotEmpty()) {
                        "YAML format error: 'limitToSections' should not be empty for function '${fqn}'."
                    }
                    funFqnToSectionRestrictionList.put(fqn, value)
                }

                if (params.isPlaceholderInjectionAllowed) {
                    funFqnValidForInjection.add(fqn)
                }
            }

            val allPossibleRuleOrCheckpointSectionKeywords =
                yamlApi.collectAllPossibleRuleOrCheckpointSubsectionKeywords() + setOf(SECTION_RUN)

            val allPossibleExecutionSectionKeywords = yamlApi.collectAllPossibleRuleOrCheckpointExecutionKeywords()

            val allPossibleUseSectionKeywordsIncludingExecSections = yamlApi.collectAllPossibleUseSubsectionKeywordsIncludingExecutionSections()

            val allPossibleModuleSectionKeywords = yamlApi.collectAllPossibleModuleSubsectionKeywords()


            SnakemakeApiStateForLangLevel(
                contextType2SubsectionKeywords = contextType2SubsectionKeywords.toImmutableMap(),
                contextType2ArgsSubsectionKeywords = contextType2ArgsSubsectionKeywords.toImmutableMap(),
                contextType2SingleArgSectionKeywords = contextType2SingleArgSectionKeywords.toImmutableMap(),
                contextType2PositionalOnlySectionKeywords = contextType2PositionalOnlySectionKeywords.toImmutableMap(),
                contextTypeAndSubsection2LambdaArgs = contextTypeAndSubsection2LambdaArgs.toImmutableMap(),
                contextType2NotValidForInjectionSubsectionKeywords = contextType2NotValidForInjectionSubsectionKeywords.toImmutableMap(),
                contextType2WildcardsExpandingSubsectionKeywords = contextType2WildcardsExpandingSubsectionKeywords.toImmutableMap(),
                contextType2AccessibleInRuleObjectSubsectionKeywords = contextType2AccessibleInRuleObjectSubsectionKeywords.toImmutableMap(),
                contextType2AccessibleAccessibleAsPlaceholderSubsectionKeywords = contextType2AccessibleAccessibleAsPlaceholderSubsectionKeywords.toImmutableMap(),
                contextType2ExecutionSectionSubsectionKeywords = contextType2ExecutionSectionSubsectionKeywords.toImmutableMap(),
                funFqnToSectionRestrictionList = funFqnToSectionRestrictionList.toImmutableMap(),

                funShortNameDeprecations = yamlApi.getFunctionDeprecationsByShortName(smkLangVers).toMap().toImmutableMap(),
                funFqnDeprecations = yamlApi.getFunctionDeprecationsByFqn(smkLangVers).toMap().toImmutableMap(),
                funFqnValidForInjection = funFqnValidForInjection.toImmutableSet(),
                funShortNamesValidForInjection = funFqnValidForInjection.map() { fqn -> fqn.split(".").last()}.toImmutableSet(),
                allPossibleRuleOrCheckpointSectionKeywords = allPossibleRuleOrCheckpointSectionKeywords.toImmutableSet(),
                allPossibleUseSectionKeywordsIncludingExecSections = allPossibleUseSectionKeywordsIncludingExecSections.toImmutableSet(),
                allPossibleModuleSectionKeywords = allPossibleModuleSectionKeywords.toImmutableSet(),
                allPossibleExecutionSectionKeywords = allPossibleExecutionSectionKeywords.toImmutableSet(),
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
        fun getInstance(project: Project) = project.getService(SnakemakeApiService::class.java)!!
    }
}

internal data class SnakemakeApiStateForLangLevel(
    val contextType2SingleArgSectionKeywords: Map<String, List<String>>,
    val contextType2PositionalOnlySectionKeywords: Map<String, List<String>>,
    val contextTypeAndSubsection2LambdaArgs:  Map<SmkApiSubsectionContextAndDirective, Array<String>>,
    val contextType2NotValidForInjectionSubsectionKeywords:  Map<String, Set<String>>,
    val contextType2WildcardsExpandingSubsectionKeywords:  Map<String, Set<String>>,
    val contextType2ExecutionSectionSubsectionKeywords:  Map<String, Set<String>>,
    val contextType2AccessibleInRuleObjectSubsectionKeywords:  Map<String, Set<String>>,
    val contextType2AccessibleAccessibleAsPlaceholderSubsectionKeywords:  Map<String, Set<String>>,
    val contextType2SubsectionKeywords:  Map<String, Set<String>>,
    val contextType2ArgsSubsectionKeywords:  Map<String, Set<String>>,
    val funFqnToSectionRestrictionList: Map<String, Array<String>>,
    val funShortNameDeprecations: Map<String, Map.Entry<SmkLanguageVersion, SmkApiAnnotationKeywordDeprecationParams>>,
    val funFqnDeprecations: Map<String, Map.Entry<SmkLanguageVersion, SmkApiAnnotationKeywordDeprecationParams>>,
    val funFqnValidForInjection: Set<String>,
    val funShortNamesValidForInjection: Set<String>,
    val allPossibleRuleOrCheckpointSectionKeywords: Set<String>,
    val allPossibleUseSectionKeywordsIncludingExecSections: Set<String>,
    val allPossibleModuleSectionKeywords: Set<String>,
    val allPossibleExecutionSectionKeywords: Set<String>,
) {
    val subsectionsAllPossibleArgNames = contextTypeAndSubsection2LambdaArgs.values.flatMap { it.asIterable() }.toImmutableSet()

    companion object {
        val EMPTY = SnakemakeApiStateForLangLevel(
            emptyMap(), emptyMap(), emptyMap(), emptyMap(), emptyMap(), emptyMap(), emptyMap(), emptyMap(),
            emptyMap(), emptyMap(), emptyMap(), emptyMap(), emptyMap(), emptySet(), emptySet(), emptySet(),
            emptySet(), emptySet(), emptySet()
        )
    }
}