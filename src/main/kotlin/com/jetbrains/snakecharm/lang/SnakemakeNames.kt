package com.jetbrains.snakecharm.lang

/**
 * @author Roman.Chernyatchik
 * @date 2018-12-31
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

    const val SUBWORKFLOW_KEYWORD = "subworkflow"
    const val SUBWORKFLOW_WORKDIR_KEYWORD = WORKFLOW_WORKDIR_KEYWORD
    const val SUBWORKFLOW_CONFIGFILE_KEYWORD = WORKFLOW_CONFIGFILE_KEYWORD
    const val SUBWORKFLOW_SNAKEFILE_KEYWORD = "snakefile"

    const val MODULE_KEYWORD = "module"
    const val MODULE_SNAKEFILE_KEYWORD = "snakefile"
    const val MODULE_CONFIG_KEYWORD = "config"
    const val MODULE_SKIP_VALIDATION_KEYWORD = "skip_validation"
    const val MODULE_META_WRAPPER_KEYWORD = "meta_wrapper"
    const val MODULE_REPLACE_PREFIX_KEYWORD = "replace_prefix"

    const val USE_KEYWORD = "use"
    const val USE_EXCLUDE_KEYWORD = "exclude"
    const val SMK_FROM_KEYWORD = "from"
    const val SMK_AS_KEYWORD = "as"
    const val SMK_WITH_KEYWORD = "with"

    const val SECTION_INPUT = "input"
    const val SECTION_OUTPUT = "output"
    const val SECTION_LOG = "log"
    const val SECTION_BENCHMARK = "benchmark"
    const val SECTION_VERSION = "version"
    const val SECTION_MESSAGE = "message"
    const val SECTION_THREADS = "threads"
    const val SECTION_SINGULARITY = WORKFLOW_SINGULARITY_KEYWORD
    const val SECTION_PRIORITY = "priority"
    const val SECTION_WILDCARD_CONSTRAINTS = WORKFLOW_WILDCARD_CONSTRAINTS_KEYWORD
    const val SECTION_GROUP = "group"
    const val SECTION_CONDA = "conda" // >= 4.8
    const val SECTION_RESOURCES = "resources"
    const val SECTION_PARAMS = "params"
    const val SECTION_SHELL = "shell"
    const val SECTION_SCRIPT = "script"
    const val SECTION_WRAPPER = "wrapper"
    const val SECTION_CWL = "cwl"
    const val SECTION_SHADOW = "shadow"
    const val SECTION_RUN = "run"
    const val SECTION_CACHE = "cache" // >= 5.12.0
    const val SECTION_CONTAINER = WORKFLOW_CONTAINER_KEYWORD
    const val SECTION_CONTAINERIZED = WORKFLOW_CONTAINERIZED_KEYWORD
    const val SECTION_NOTEBOOK = "notebook"
    const val SECTION_ENVMODULES = "envmodules" // >= 5.9
    const val SECTION_NAME = "name" // >= 5.31
    const val SECTION_HANDOVER = "handover" // >= 6.2
    const val SECTION_DEFAULT_TARGET = "default_target" // >= 5.12.0
    const val SECTION_RETRIES = "retries" // >= 7.7.0

    const val RUN_SECTION_VARIABLE_RULE = "rule"
    const val RUN_SECTION_VARIABLE_JOBID = "jobid"

    const val SNAKEMAKE_IO_METHOD_ANCIENT = "ancient"
    const val SNAKEMAKE_IO_METHOD_PROTECTED = "protected"
    const val SNAKEMAKE_IO_METHOD_DIRECTORY = "directory"
    const val SNAKEMAKE_IO_METHOD_TEMP = "temp"
    const val SNAKEMAKE_IO_METHOD_REPORT = "report"
    const val SNAKEMAKE_IO_METHOD_TOUCH = "touch"
    const val SNAKEMAKE_IO_METHOD_PIPE = "pipe"
    const val SNAKEMAKE_IO_METHOD_REPEAT = "repeat"
    const val SNAKEMAKE_IO_METHOD_UNPACK = "unpack"
    const val SNAKEMAKE_IO_METHOD_DYNAMIC = "dynamic"
    const val SNAKEMAKE_IO_METHOD_ENSURE = "ensure" // >= 7.7.0

    const val SNAKEMAKE_METHOD_MULTIEXT = "multiext"
}