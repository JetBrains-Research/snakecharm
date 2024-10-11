package com.jetbrains.snakecharm.codeInsight

import com.jetbrains.snakecharm.codeInsight.SnakemakeAPICompanion.RULE_OR_CHECKPOINT_ARGS_SECTION_KEYWORDS_HARDCODED
import com.jetbrains.snakecharm.framework.SnakemakeFrameworkAPIProvider
import com.jetbrains.snakecharm.lang.SnakemakeNames
import com.jetbrains.snakecharm.lang.SnakemakeNames.MODULE_CONFIG_KEYWORD
import com.jetbrains.snakecharm.lang.SnakemakeNames.MODULE_META_WRAPPER_KEYWORD
import com.jetbrains.snakecharm.lang.SnakemakeNames.MODULE_REPLACE_PREFIX_KEYWORD
import com.jetbrains.snakecharm.lang.SnakemakeNames.MODULE_SKIP_VALIDATION_KEYWORD
import com.jetbrains.snakecharm.lang.SnakemakeNames.MODULE_SNAKEFILE_KEYWORD
import com.jetbrains.snakecharm.lang.SnakemakeNames.RULE_KEYWORD
import com.jetbrains.snakecharm.lang.SnakemakeNames.SECTION_BENCHMARK
import com.jetbrains.snakecharm.lang.SnakemakeNames.SECTION_CACHE
import com.jetbrains.snakecharm.lang.SnakemakeNames.SECTION_CONDA
import com.jetbrains.snakecharm.lang.SnakemakeNames.SECTION_CONTAINER
import com.jetbrains.snakecharm.lang.SnakemakeNames.SECTION_CONTAINERIZED
import com.jetbrains.snakecharm.lang.SnakemakeNames.SECTION_CWL
import com.jetbrains.snakecharm.lang.SnakemakeNames.SECTION_DEFAULT_TARGET
import com.jetbrains.snakecharm.lang.SnakemakeNames.SECTION_ENVMODULES
import com.jetbrains.snakecharm.lang.SnakemakeNames.SECTION_GROUP
import com.jetbrains.snakecharm.lang.SnakemakeNames.SECTION_HANDOVER
import com.jetbrains.snakecharm.lang.SnakemakeNames.SECTION_INPUT
import com.jetbrains.snakecharm.lang.SnakemakeNames.SECTION_LOG
import com.jetbrains.snakecharm.lang.SnakemakeNames.SECTION_MESSAGE
import com.jetbrains.snakecharm.lang.SnakemakeNames.SECTION_NAME
import com.jetbrains.snakecharm.lang.SnakemakeNames.SECTION_NOTEBOOK
import com.jetbrains.snakecharm.lang.SnakemakeNames.SECTION_OUTPUT
import com.jetbrains.snakecharm.lang.SnakemakeNames.SECTION_PARAMS
import com.jetbrains.snakecharm.lang.SnakemakeNames.SECTION_PRIORITY
import com.jetbrains.snakecharm.lang.SnakemakeNames.SECTION_RESOURCES
import com.jetbrains.snakecharm.lang.SnakemakeNames.SECTION_RETRIES
import com.jetbrains.snakecharm.lang.SnakemakeNames.SECTION_RUN
import com.jetbrains.snakecharm.lang.SnakemakeNames.SECTION_SCRIPT
import com.jetbrains.snakecharm.lang.SnakemakeNames.SECTION_SHADOW
import com.jetbrains.snakecharm.lang.SnakemakeNames.SECTION_SHELL
import com.jetbrains.snakecharm.lang.SnakemakeNames.SECTION_SINGULARITY
import com.jetbrains.snakecharm.lang.SnakemakeNames.SECTION_TEMPLATE_ENGINE
import com.jetbrains.snakecharm.lang.SnakemakeNames.SECTION_THREADS
import com.jetbrains.snakecharm.lang.SnakemakeNames.SECTION_VERSION
import com.jetbrains.snakecharm.lang.SnakemakeNames.SECTION_WILDCARD_CONSTRAINTS
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

    /**
     * Rule or checkpoint sections that allows only single argument
     */
    val SINGLE_ARGUMENT_SECTIONS_KEYWORDS = setOf(
        SECTION_SHELL, SECTION_SCRIPT, SECTION_WRAPPER,
        SECTION_CWL, SECTION_BENCHMARK, SECTION_VERSION,
        SECTION_MESSAGE, SECTION_THREADS, SECTION_SINGULARITY,
        SECTION_PRIORITY, SECTION_CONDA, SECTION_GROUP,
        SECTION_SHADOW, SECTION_CACHE, SECTION_NOTEBOOK, SECTION_CONTAINER, SECTION_TEMPLATE_ENGINE,
        SECTION_HANDOVER, SECTION_CONTAINERIZED,
        SECTION_DEFAULT_TARGET,
        SECTION_RETRIES
    )

    /**
     * Workflow top-level sections that allows only single argument
     */
    val SINGLE_ARGUMENT_WORKFLOWS_KEYWORDS = setOf(
        WORKFLOW_CONTAINERIZED_KEYWORD, WORKFLOW_CONTAINER_KEYWORD,
        WORKFLOW_SINGULARITY_KEYWORD, WORKFLOW_WORKDIR_KEYWORD
    )

    // List of top-level sections
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

    val RULE_OR_CHECKPOINT_ARGS_SECTION_KEYWORDS = RULE_OR_CHECKPOINT_ARGS_SECTION_KEYWORDS_HARDCODED +
            SnakemakeFrameworkAPIProvider.getInstance().collectAllPossibleRuleOrCheckpointSubsectionKeywords()

    val RULE_OR_CHECKPOINT_SECTION_KEYWORDS = (RULE_OR_CHECKPOINT_ARGS_SECTION_KEYWORDS + setOf(SECTION_RUN))

    /**
     * For codeInsight
     */
    val SUBWORKFLOW_SECTIONS_KEYWORDS = setOf(
        SnakemakeNames.SUBWORKFLOW_WORKDIR_KEYWORD,
        SnakemakeNames.SUBWORKFLOW_SNAKEFILE_KEYWORD,
        SnakemakeNames.SUBWORKFLOW_CONFIGFILE_KEYWORD
    )

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

    /**
     * For uses codeInsight
     */
    val USE_SECTIONS_KEYWORDS = RULE_OR_CHECKPOINT_SECTION_KEYWORDS + SnakemakeFrameworkAPIProvider.getInstance()
        .collectAllPossibleUseSubsectionKeywords() - EXECUTION_SECTIONS_KEYWORDS - SECTION_RUN

    val USE_DECLARATION_KEYWORDS = setOf(
        RULE_KEYWORD,
        SMK_FROM_KEYWORD,
        SMK_AS_KEYWORD,
        SMK_WITH_KEYWORD,
        USE_EXCLUDE_KEYWORD
    )

    /**
     * For type inference:
     * Some sections in snakemake are inaccessible after `rules.NAME.<section>`, so this set is required
     * to filter these sections for resolve and completion
     */
    val RULE_TYPE_ACCESSIBLE_SECTIONS = setOf(
        SECTION_INPUT,
        SECTION_LOG,
        SECTION_OUTPUT,
        SECTION_PARAMS,
        SECTION_RESOURCES,
        SECTION_VERSION,

        SECTION_MESSAGE,
        SECTION_WILDCARD_CONSTRAINTS,
        SECTION_BENCHMARK,
        SECTION_PRIORITY,
        SECTION_WRAPPER
    )

    /**
     * For type inference:
     * In SnakemakeSL some sections are inaccessible in `shell: "{<section>}"` and other sections, which doesn't
     * expand wildcards.
     */
    val SMK_SL_INITIAL_TYPE_ACCESSIBLE_SECTIONS = setOf(
        SECTION_INPUT,
        SECTION_OUTPUT, SECTION_LOG,
        SECTION_THREADS, SECTION_PARAMS,
        SECTION_RESOURCES,
        SECTION_VERSION
    )

    val SECTIONS_INVALID_FOR_INJECTION = setOf(
        SECTION_WILDCARD_CONSTRAINTS,
        SECTION_SHADOW,
        SECTION_WRAPPER,
        SECTION_VERSION, SECTION_THREADS,
        SECTION_PRIORITY, SECTION_SINGULARITY, SECTION_CACHE,
        SECTION_CONTAINER, SECTION_CONTAINERIZED, SECTION_NOTEBOOK,
        SECTION_ENVMODULES, SECTION_HANDOVER, SECTION_DEFAULT_TARGET,
        SECTION_RETRIES,
        SECTION_TEMPLATE_ENGINE
    )

    /**
     * This sections considers all injection identifiers to be wildcards
     *
     * TODO: Consider implementing this as PSI interface in order not to compare keyword string each time
     */
    val WILDCARDS_EXPANDING_SECTIONS_KEYWORDS = setOf(
        SECTION_INPUT, SECTION_OUTPUT, SECTION_CONDA,
        SECTION_RESOURCES, SECTION_GROUP, SECTION_BENCHMARK,
        SECTION_LOG, SECTION_PARAMS
    )

    /**
     * Ordered list of sections which defines wildcards
     *
     * TODO: Consider implementing this as PSI interface in order not to compare keyword string each time
     */
    val WILDCARDS_DEFINING_SECTIONS_KEYWORDS = listOf(
        SECTION_OUTPUT, SECTION_LOG, SECTION_BENCHMARK
    )

    /**
     * Set of rule/checkpoint sections that supports lambdas/callable arguments
     */
    val ALLOWED_LAMBDA_OR_CALLABLE_ARGS = mapOf(
        SECTION_INPUT to arrayOf(SMK_VARS_WILDCARDS),
        SECTION_GROUP to arrayOf(SMK_VARS_WILDCARDS),
        SECTION_PARAMS to arrayOf(
            SMK_VARS_WILDCARDS,
            SECTION_INPUT,
            SECTION_OUTPUT,
            SECTION_RESOURCES,
            SECTION_THREADS
        ),
        SECTION_RESOURCES to arrayOf(
            SMK_VARS_WILDCARDS,
            SECTION_INPUT,
            SECTION_THREADS,
            SMK_VARS_ATTEMPT
        ),
        SECTION_THREADS to arrayOf(
            SMK_VARS_WILDCARDS,
            SECTION_INPUT,
            SMK_VARS_ATTEMPT
        ),
        SECTION_CONDA to arrayOf(
            SMK_VARS_WILDCARDS,
            SECTION_PARAMS,
            SECTION_INPUT,
        ),
    )
    val SECTION_LAMBDA_ARG_POSSIBLE_PARAMS: Set<String> =
        ALLOWED_LAMBDA_OR_CALLABLE_ARGS.values.flatMap { it.asIterable() }.toMutableSet().also {
            it.addAll(RULE_OR_CHECKPOINT_ARGS_SECTION_KEYWORDS)
        }

    /**
     * Rule/checkpoint sections that does not allow keyword arguments
     */
    val SECTIONS_WHERE_KEYWORD_ARGS_PROHIBITED = setOf(
        SECTION_BENCHMARK, SECTION_VERSION, SECTION_MESSAGE, SECTION_SHELL, SECTION_THREADS, SECTION_SINGULARITY,
        SECTION_PRIORITY, SECTION_GROUP, SECTION_SHADOW, SECTION_CONDA, SECTION_SCRIPT, SECTION_WRAPPER,
        SECTION_CWL, SECTION_NOTEBOOK, SECTION_CACHE, SECTION_CONTAINER, SECTION_CONTAINERIZED, SECTION_ENVMODULES,
        SECTION_NAME, SECTION_HANDOVER, SECTION_DEFAULT_TARGET, SECTION_RETRIES,
        SECTION_TEMPLATE_ENGINE
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


    /**
     * Workflow top-level sections that does not allow keyword args
     */
    val WORKFLOWS_WHERE_KEYWORD_ARGS_PROHIBITED = setOf(
        WORKFLOW_CONTAINERIZED_KEYWORD, WORKFLOW_CONTAINER_KEYWORD,
        WORKFLOW_SINGULARITY_KEYWORD
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

object SnakemakeAPICompanion {
    /**
     * For codeInsight
     */
    val RULE_OR_CHECKPOINT_ARGS_SECTION_KEYWORDS_HARDCODED = setOf(
        // hardcoded list missing in 'snakmake_api.yaml'
        SECTION_OUTPUT, SECTION_INPUT, SECTION_PARAMS, SECTION_LOG, SECTION_RESOURCES,
        SECTION_BENCHMARK, SECTION_MESSAGE, SECTION_SHELL, SECTION_THREADS,
        SECTION_PRIORITY, SECTION_GROUP, SECTION_SHADOW,
    )
}