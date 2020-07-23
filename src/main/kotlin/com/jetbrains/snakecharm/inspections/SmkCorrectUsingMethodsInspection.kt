package com.jetbrains.snakecharm.inspections

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElement
import com.jetbrains.python.PyElementTypes.CALL_EXPRESSION
import com.jetbrains.snakecharm.SnakemakeBundle
import com.jetbrains.snakecharm.lang.psi.SmkFile
import com.jetbrains.snakecharm.lang.psi.SmkRuleOrCheckpointArgsSection

class SmkCorrectUsingMethodsInspection: SnakemakeInspection() {
    override fun buildVisitor(
            holder: ProblemsHolder,
            isOnTheFly: Boolean,
            session: LocalInspectionToolSession
    ) = object : SnakemakeInspectionVisitor(holder, session) {

        private fun checkAncient(node: PsiElement): PsiElement? {
            if (node.text != "input")
                return node
            return null
        }

        private fun checkProtected(node: PsiElement): PsiElement? {
            if (node.text != "output"
                    && node.text != "log"
                    && node.text != "benchmark")
                return node
            return null
        }

        private fun checkDirectory(node: PsiElement): PsiElement? {
            if (node.text != "output"){
                return node
            }
            return null
        }

        override fun visitSmkRuleOrCheckpointArgsSection(st: SmkRuleOrCheckpointArgsSection) {

            if (st.containingFile !is SmkFile) {
                return
            }

            st.argumentList?.node?.getChildren(null)
                    ?.filter { node -> node.elementType == CALL_EXPRESSION }
                    ?.forEach { node ->

                        val method = when (node.firstChildNode.text) {
                            "ancient" -> checkAncient(st.firstChild)
                            "protected" -> checkProtected(st.firstChild)
                            "directory" -> checkDirectory(st.firstChild)
                            else -> null
                        }

                        val message = SnakemakeBundle.message("INSP.NAME.correct.use.method.title",
                                node.firstChildNode.text, st.firstChild.text)

                        if (method != null) {
                            holder.registerProblem(
                                    node.psi,
                                    message,
                                    ProblemHighlightType.WARNING
                            )
                        }
                    }
        }
    }
}