package com.jetbrains.snakecharm.codeInsight

/**
 * Also see [ImplicitPySymbolsProvider] class
 */
object SnakemakeAPI {
    const val SMK_VARS_RULES = "rules"
    const val SMK_VARS_CHECKPOINTS = "checkpoints"
    const val SMK_VARS_ATTEMPT = "attempt"

    val FUNCTIONS_ALLOWING_SMKSL_INJECTION = setOf(
        "ancient", "directory", "temp", "pipe", "temporary", "protected",
        "dynamic", "touch", "repeat", "report", "local", "expand", "shell",
        "join"
    )

    val FUNCTIONS_BANNED_FOR_WILDCARDS = listOf(
            "expand"
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
}