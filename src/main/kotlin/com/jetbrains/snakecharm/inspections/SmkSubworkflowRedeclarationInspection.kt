package com.jetbrains.snakecharm.inspections

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.jetbrains.snakecharm.SnakemakeBundle
import com.jetbrains.snakecharm.lang.psi.SmkFile
import com.jetbrains.snakecharm.lang.psi.SmkSubworkflow

class SmkSubworkflowRedeclarationInspection : SnakemakeInspection() {
    override fun buildVisitor(
        holder: ProblemsHolder,
        isOnTheFly: Boolean,
        session: LocalInspectionToolSession,
    ) = object : SnakemakeInspectionVisitor(holder, getContext(session)) {
        private val subworkflowsNameAndPsi = (session.file as? SmkFile)?.collectSubworkflows() ?: emptyList()

        override fun visitSmkSubworkflow(subworkflow: SmkSubworkflow) {
            val name = subworkflow.name ?: return

            if (subworkflow !== subworkflowsNameAndPsi.findLast { it.first == name }?.second) {
                registerProblem(subworkflow.originalElement,
                    SnakemakeBundle.message("INSP.NAME.subworkflow.redeclaration"),
                    ProblemHighlightType.LIKE_UNUSED_SYMBOL)
            }
        }
    }

    override fun getDisplayName(): String = SnakemakeBundle.message("INSP.NAME.subworkflow.redeclaration")
}
