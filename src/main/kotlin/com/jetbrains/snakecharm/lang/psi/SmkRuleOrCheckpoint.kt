package com.jetbrains.snakecharm.lang.psi

import com.intellij.psi.PsiElement
import com.jetbrains.python.psi.PyTypedElement

interface SmkRuleOrCheckpoint : SmkRuleLike<SmkRuleOrCheckpointArgsSection>, PyTypedElement {
    fun collectWildcards(): List<Pair<String, PsiElement>>
}