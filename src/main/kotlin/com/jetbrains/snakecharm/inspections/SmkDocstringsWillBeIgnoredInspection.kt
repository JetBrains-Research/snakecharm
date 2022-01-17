package com.jetbrains.snakecharm.inspections

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemsHolder
import com.jetbrains.snakecharm.SnakemakeBundle
import com.jetbrains.snakecharm.lang.psi.*

class SmkDocstringsWillBeIgnoredInspection : SnakemakeInspection() {
    override fun buildVisitor(
        holder: ProblemsHolder,
        isOnTheFly: Boolean,
        session: LocalInspectionToolSession
    ) = object : SnakemakeInspectionVisitor(holder, getContext(session)) {

        override fun visitSmkRule(rule: SmkRule) {
            visitSMKRuleLike(rule)
        }

        override fun visitSmkCheckPoint(checkPoint: SmkCheckPoint) {
            visitSMKRuleLike(checkPoint)
        }

        fun visitSMKRuleLike(rule: SmkRuleLike<SmkArgsSection>) {
            val docstrings = rule.getStringLiteralExpressions().drop(1)
            if (docstrings.isEmpty()) {
                return
            }
            for (docstring in docstrings) {
                registerProblem(docstring, SnakemakeBundle.message("INSP.NAME.docstrings.will.be.ignored"))
            }
        }
    }
}