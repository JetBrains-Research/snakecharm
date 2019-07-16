package com.jetbrains.snakecharm.codeInsight

import com.intellij.psi.PsiElement
import com.intellij.psi.util.parentOfType
import com.jetbrains.snakecharm.lang.psi.SmkRunSection

enum class SmkCodeInsightScope {
    TOP_LEVEL,
    RULELIKE_RUN_SECTION;

    fun includes(second: SmkCodeInsightScope) = when (this) {
         TOP_LEVEL -> second == TOP_LEVEL
         RULELIKE_RUN_SECTION -> second == TOP_LEVEL || second == RULELIKE_RUN_SECTION
    }

    companion object {
        operator fun get(anchor: PsiElement) = when {
            anchor.parentOfType<SmkRunSection>() != null -> RULELIKE_RUN_SECTION
            else -> TOP_LEVEL
        }
    }
}