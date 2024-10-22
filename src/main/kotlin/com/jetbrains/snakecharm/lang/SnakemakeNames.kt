package com.jetbrains.snakecharm.lang

/**
 * @author Roman.Chernyatchik
 * @date 2018-12-31
 *
 * Many hardcoded things were moved to snakemake_api.yaml
 */
object SnakemakeNames {
    const val RULE_KEYWORD = "rule"
    const val CHECKPOINT_KEYWORD = "checkpoint"

    const val WORKFLOW_ONSUCCESS_KEYWORD = "onsuccess"
    const val WORKFLOW_ONERROR_KEYWORD = "onerror"
    const val WORKFLOW_ONSTART_KEYWORD = "onstart"
    const val WORKFLOW_LOCALRULES_KEYWORD = "localrules"
    const val WORKFLOW_RULEORDER_KEYWORD = "ruleorder"
    const val WORKFLOW_CONFIGFILE_KEYWORD = "configfile"
    const val WORKFLOW_REPORT_KEYWORD = "report"
    const val WORKFLOW_WILDCARD_CONSTRAINTS_KEYWORD = "wildcard_constraints"
    const val WORKFLOW_SINGULARITY_KEYWORD = "singularity"
    const val WORKFLOW_INCLUDE_KEYWORD = "include"
    const val WORKFLOW_WORKDIR_KEYWORD = "workdir"
    const val WORKFLOW_PEPFILE_KEYWORD = "pepfile"
    const val WORKFLOW_PEPSCHEMA_KEYWORD = "pepschema"
    const val WORKFLOW_ENVVARS_KEYWORD = "envvars"
    const val WORKFLOW_CONTAINER_KEYWORD = "container"
    const val WORKFLOW_CONTAINERIZED_KEYWORD = "containerized"  // => 6.0.0
    const val WORKFLOW_RESOURCE_SCOPES_KEYWORD = "resource_scopes"  // => 7.11

    const val SUBWORKFLOW_KEYWORD = "subworkflow"
    const val SUBWORKFLOW_WORKDIR_KEYWORD = WORKFLOW_WORKDIR_KEYWORD
    const val SUBWORKFLOW_CONFIGFILE_KEYWORD = WORKFLOW_CONFIGFILE_KEYWORD
    const val SUBWORKFLOW_SNAKEFILE_KEYWORD = "snakefile"

    const val MODULE_KEYWORD = "module" // See: ./snakemake/modules.py, ./snakemake/parser.py::Module(GlobalKeywordState)
    const val MODULE_SNAKEFILE_KEYWORD = "snakefile"

    const val USE_KEYWORD = "use"
    const val USE_EXCLUDE_KEYWORD = "exclude"
    const val SMK_FROM_KEYWORD = "from"
    const val SMK_AS_KEYWORD = "as"
    const val SMK_WITH_KEYWORD = "with"

    const val SECTION_INPUT = "input"
    const val SECTION_OUTPUT = "output"
    const val SECTION_LOG = "log"
    const val SECTION_BENCHMARK = "benchmark"
    const val SECTION_THREADS = "threads"
    const val SECTION_WILDCARD_CONSTRAINTS = WORKFLOW_WILDCARD_CONSTRAINTS_KEYWORD
    const val SECTION_GROUP = "group"
    const val SECTION_CONDA = "conda" // >= 4.8
    const val SECTION_RESOURCES = "resources"
    const val SECTION_SHELL = "shell"
    const val SECTION_SCRIPT = "script"
    const val SECTION_WRAPPER = "wrapper"
    const val SECTION_SHADOW = "shadow"
    const val SECTION_RUN = "run"
    const val SECTION_NOTEBOOK = "notebook"
    const val SECTION_ENVMODULES = "envmodules" // >= 5.9
    const val SECTION_TEMPLATE_ENGINE = "template_engine" // >= 7.0.0

    const val RUN_SECTION_VARIABLE_RULE = "rule"
    const val RUN_SECTION_VARIABLE_JOBID = "jobid"

    const val SNAKEMAKE_METHOD_MULTIEXT = "multiext"

    /**
     * Constant that holds the module name for Snakemake input/output operations.
     * It is used as part of the code insight and symbol resolution process within the Snakemake plugin.
     *
     * For more details, refer to the class `com.jetbrains.snakecharm.codeInsight.SmkImplicitPySymbolsProvider`.
     */
    const val SNAKEMAKE_MODULE_NAME_IO = "snakemake.io"
    const val SNAKEMAKE_FQN_FUN_MIN_VERSION = "snakemake.utils.min_version"

    const val UNPACK_FUNCTION = "unpack"
    const val SMK_VARS_CONFIG = "config"
    const val SMK_VARS_PEP = "pep"
    const val SMK_VARS_RULES = "rules"
    const val SMK_VARS_CHECKPOINTS = "checkpoints"
    const val SMK_VARS_SCATTER = "scatter"
    const val SMK_VARS_GATHER = "gather"
    const val SMK_FUN_EXPAND = "expand"
    const val SMK_FUN_EXPAND_ALIAS_COLLECT = "collect"

    const val SMK_VARS_WILDCARDS = "wildcards"
    const val WILDCARDS_ACCESSOR_CLASS = "snakemake.io.Wildcards"
}