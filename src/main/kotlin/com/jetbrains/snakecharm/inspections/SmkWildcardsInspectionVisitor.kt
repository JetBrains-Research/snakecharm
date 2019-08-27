package com.jetbrains.snakecharm.inspections

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemsHolder
import com.jetbrains.snakecharm.lang.psi.SmkRuleOrCheckpoint

abstract class SmkWildcardsInspectionVisitor<T>(
        holder: ProblemsHolder,
        session: LocalInspectionToolSession
) : SnakemakeInspectionVisitor(holder, session) {
    private var currentDeclaration: SmkRuleOrCheckpoint? = null
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