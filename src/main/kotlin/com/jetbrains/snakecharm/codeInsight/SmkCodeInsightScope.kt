package com.jetbrains.snakecharm.codeInsight

import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.snakecharm.lang.psi.SmkRunSection

enum class SmkCodeInsightScope {
    TOP_LEVEL,
    RULELIKE_RUN_SECTION,
    PEP_OBJECT;

    fun includes(second: SmkCodeInsightScope) = when (this) {
        TOP_LEVEL -> second == TOP_LEVEL
        RULELIKE_RUN_SECTION -> second == TOP_LEVEL || second == RULELIKE_RUN_SECTION
        PEP_OBJECT -> second == PEP_OBJECT
    }

    companion object {

        private fun isPep(anchor: PsiElement): Boolean =
            anchor.text == "pep"

        operator fun get(anchor: PsiElement) = when {
            isPep(anchor) -> PEP_OBJECT
            PsiTreeUtil.getParentOfType(anchor, SmkRunSection::class.java) != null -> RULELIKE_RUN_SECTION
            else -> TOP_LEVEL
        }
    }
}