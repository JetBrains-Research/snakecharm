package com.jetbrains.snakecharm.lang

/**
 * @author Roman.Chernyatchik
 * @date 2018-12-31
 */
object SnakemakeNames {
    const val RULE_KEYWORD = "rule"
    const val CHECKPOINT_KEYWORD = "checkpoint"

    const val WORKFLOW_CONFIGFILE_KEYWORD = "configfile"
    const val WORKFLOW_REPORT_KEYWORD = "report"
    const val WORKFLOW_WILDCARD_CONSTRAINTS_KEYWORD = "wildcard_constraints"
    const val WORKFLOW_SINGULARITY_KEYWORD = "singularity"
    const val WORKFLOW_INCLUDE_KEYWORD = "include"
    const val WORKFLOW_WORKDIR_KEYWORD = "workdir"

    const val WORKFLOW_LOCALRULES_KEYWORD = "localrules"

    const val WORKFLOW_RULEORDER_KEYWORD = "ruleorder"

    const val WORKFLOW_ONSUCCESS_KEYWORD = "onsuccess"
    const val WORKFLOW_ONERROR_KEYWORD = "onerror"
    const val WORKFLOW_ONSTART_KEYWORD = "onstart"
}