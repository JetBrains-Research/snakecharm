package com.jetbrains.snakecharm.inspections

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemsHolder
import com.jetbrains.snakecharm.lang.psi.SmkRuleOrCheckpoint

abstract class AbstractSmkWildcardsInspectionVisitor<T>(
        holder: ProblemsHolder,
        session: LocalInspectionToolSession
) : SnakemakeInspectionVisitor(holder, session) {
    protected var currentDeclaration: SmkRuleOrCheckpoint? = null
        private set
    protected var currentGeneratedWildcards: List<String> = emptyList()
        private set

    protected fun updateDeclarationAndWildcards(elem: T, getDeclaration: (T) -> SmkRuleOrCheckpoint?) {
        val declaration = getDeclaration(elem) ?: return

        if (declaration !== currentDeclaration) {
            currentDeclaration = declaration
            currentGeneratedWildcards = declaration.collectWildcards().map { it.second }
        }
    }
}