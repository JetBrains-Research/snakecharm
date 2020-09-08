package com.jetbrains.snakecharm.inspections

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiWhiteSpace
import com.jetbrains.snakecharm.SnakemakeBundle
import com.jetbrains.snakecharm.lang.psi.SmkFile

class SmkAvoidTabWhitespace : SnakemakeInspection() {
    override fun buildVisitor(
        holder: ProblemsHolder,
        isOnTheFly: Boolean,
        session: LocalInspectionToolSession
    ) = object : SnakemakeInspectionVisitor(holder, session) {
        override fun visitWhiteSpace(space: PsiWhiteSpace) {
            if (space.containingFile is SmkFile) {
                val text =  space.text
                if (text.startsWith("\n") && '\t' in text) {
                    registerProblem(space, SnakemakeBundle.message("INSP.NAME.codestyle.avoid.whitespace.tab"))
                }
            }
        }
    }
}