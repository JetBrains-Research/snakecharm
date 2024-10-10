package com.jetbrains.snakecharm.codeInsight.completion

import com.intellij.patterns.PlatformPatterns
import com.jetbrains.snakecharm.lang.SnakemakeLanguageDialect

object SmkCompletionContributorPattern {
    val IN_SNAKEMAKE = PlatformPatterns.psiFile().withLanguage(SnakemakeLanguageDialect)
}