package com.jetbrains.snakecharm.codeInsight

/**
 * Also see [ImplicitPySymbolsProvider] classe
 */
object SnakemakeAPI {
    const val SMK_VARS_RULES = "rules"
    const val SMK_VARS_CHECKPOINTS = "checkpoints"
    const val SMK_VARS_WILDCARDS = "wildcards"
    const val SMK_VARS_ATTEMPT = "attempt"

    val FUNCTIONS_ALLOWING_SMKSL_INJECTION = setOf(
        "ancient", "directory", "temp", "pipe", "temporary", "protected",
        "dynamic", "touch", "repeat", "report", "local", "expand", "shell",
        "join"
    )

    val FUNCTIONS_BANNED_FOR_WILDCARDS = listOf(
            "expand"
    )


}