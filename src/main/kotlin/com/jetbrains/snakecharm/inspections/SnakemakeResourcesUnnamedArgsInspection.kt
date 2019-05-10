package com.jetbrains.snakecharm.inspections

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemsHolder
import com.jetbrains.python.inspections.PyInspectionVisitor
import com.jetbrains.python.psi.PyArgumentList
import com.jetbrains.python.psi.PyKeywordArgument
import com.jetbrains.snakecharm.SnakemakeBundle
import com.jetbrains.snakecharm.lang.psi.SnakemakeFile

class SnakemakeResourcesUnnamedArgsInspection : SnakemakeInspection() {
    override fun buildVisitor(
            holder: ProblemsHolder,
            isOnTheFly: Boolean,
            session: LocalInspectionToolSession
    ) = object : PyInspectionVisitor(holder, session) {

        override fun visitPyArgumentList(node: PyArgumentList?) {
            if (node?.containingFile !is SnakemakeFile) {
                return
            }

            val section = node.parent.firstChild
            if (!section.textMatches("resources")) {
                return
            }

            for (child in node.children) {
                if (child !is PyKeywordArgument) {
                    registerProblem(child, "Unnamed argument in 'resources' section.")
                }
            }

            super.visitPyArgumentList(node)
        }
    }

    override fun getDisplayName(): String {
        return SnakemakeBundle.message("INSP.NAME.resources.unnamed.args")
    }
}