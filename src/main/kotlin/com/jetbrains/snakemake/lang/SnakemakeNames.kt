package com.jetbrains.snakemake.lang

/**
 * @author Roman.Chernyatchik
 * @date 2018-12-31
 */
object SnakemakeNames {
    const val RULE_KEYWORD = "rule"
    const val CHECKPOINT_KEYWORD = "checkpoint"

    const val WORKFLOW_CONFIGFILE = "configfile"
    const val WORKFLOW_REPORT = "report"
    const val WORKFLOW_WILDCARD_CONSTRAINTS = "wildcard_constraints"
    const val WORKFLOW_SINGULARITY = "singularity"
    const val WORKFLOW_INCLUDE = "include"
    const val WORKFLOW_WORKDIR = "workdir"

    const val WORKFLOW_LOCALRULES = "localrules"

    const val WORKFLOW_RULEORDER = "ruleorder"

    const val WORKFLOW_ONSUCCESS = "onsuccess"
    const val WORKFLOW_ONERROR = "onerror"
    const val WORKFLOW_ONSTART = "onstart"
}