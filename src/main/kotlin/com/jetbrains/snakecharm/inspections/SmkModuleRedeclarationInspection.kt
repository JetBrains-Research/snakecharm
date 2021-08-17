package com.jetbrains.snakecharm.inspections

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.jetbrains.snakecharm.SnakemakeBundle
import com.jetbrains.snakecharm.lang.psi.SmkFile
import com.jetbrains.snakecharm.lang.psi.SmkModule

class SmkModuleRedeclarationInspection : SnakemakeInspection() {
    override fun buildVisitor(
        holder: ProblemsHolder,
        isOnTheFly: Boolean,
        session: LocalInspectionToolSession
    ) = object : SnakemakeInspectionVisitor(holder, session) {
        private val modulesNameAndPsi = (session.file as? SmkFile)?.collectModules() ?: emptyList()

        override fun visitSmkModule(module: SmkModule) {
            val name = module.name ?: return

            if (module !== modulesNameAndPsi.findLast { it.first == name }?.second) {
                registerProblem(
                    module.originalElement,
                    SnakemakeBundle.message("INSP.NAME.module.redeclaration"),
                    ProblemHighlightType.LIKE_UNUSED_SYMBOL
                )
            }
        }
    }

    override fun getDisplayName(): String = SnakemakeBundle.message("INSP.NAME.module.redeclaration")
}