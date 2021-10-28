package com.jetbrains.snakecharm.inspections

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemsHolder
import com.jetbrains.snakecharm.SnakemakeBundle
import com.jetbrains.snakecharm.lang.psi.SmkUse

class SmkSeveralRulesAreOverriddenAsOneInspection : SnakemakeInspection() {
    override fun buildVisitor(
        holder: ProblemsHolder,
        isOnTheFly: Boolean,
        session: LocalInspectionToolSession
    ) = object : SnakemakeInspectionVisitor(holder, session) {

        override fun visitSmkUse(use: SmkUse) {
            val name = use.nameIdentifier
            if (name == null || name.text.contains('*')) {
                // There are pattern in name, or there are no name (which means, that 'use' section doesn't change names)
                return
            }
            val overridden = use.getImportedRuleNames()

            if (overridden != null && overridden.size == 1) {
                // There are only one rule reference
                return
            }

            if (!use.usePatternToDefineOverriddenRules() && overridden == null) {
                // It doesn't have '*' symbol in imported rules part
                return
            }

            // There are '*' symbol instead of list of rules, or several rules were overridden
            registerProblem(name, SnakemakeBundle.message("INSP.NAME.only.last.rule.will.be.overridden"))
        }
    }
}