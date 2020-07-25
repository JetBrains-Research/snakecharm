package com.jetbrains.snakecharm.inspections

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElement
import com.jetbrains.python.PyElementTypes.CALL_EXPRESSION
import com.jetbrains.snakecharm.SnakemakeBundle
import com.jetbrains.snakecharm.lang.SnakemakeNames.METHOD_ANCIENT
import com.jetbrains.snakecharm.lang.SnakemakeNames.METHOD_DIRECTORY
import com.jetbrains.snakecharm.lang.SnakemakeNames.METHOD_PROTECTED
import com.jetbrains.snakecharm.lang.SnakemakeNames.SECTION_BENCHMARK
import com.jetbrains.snakecharm.lang.SnakemakeNames.SECTION_INPUT
import com.jetbrains.snakecharm.lang.SnakemakeNames.SECTION_LOG
import com.jetbrains.snakecharm.lang.SnakemakeNames.SECTION_OUTPUT
import com.jetbrains.snakecharm.lang.psi.SmkFile
import com.jetbrains.snakecharm.lang.psi.SmkRuleOrCheckpointArgsSection

class SmkCorrectUsingMethodsInspection: SnakemakeInspection() {
    override fun buildVisitor(
            holder: ProblemsHolder,
            isOnTheFly: Boolean,
            session: LocalInspectionToolSession
    ) = object : SnakemakeInspectionVisitor(holder, session) {

        private fun checkAncient(psiEl: PsiElement): PsiElement? {
            if (psiEl.text != SECTION_INPUT)
                return psiEl
            return null
        }

        private fun checkProtected(psiEl: PsiElement): PsiElement? {
            val psiElText = psiEl.text

            if (psiElText != SECTION_OUTPUT
                    && psiElText != SECTION_LOG
                    && psiElText != SECTION_BENCHMARK)
                return psiEl
            return null
        }

        private fun checkDirectory(psiEl: PsiElement): PsiElement? {
            if (psiEl.text != SECTION_OUTPUT){
                return psiEl
            }
            return null
        }

        override fun visitSmkRuleOrCheckpointArgsSection(st: SmkRuleOrCheckpointArgsSection) {

            if (st.containingFile !is SmkFile) {
                return
            }

            val firstChildSection = st.firstChild

            st.argumentList?.node?.getChildren(null)
                    ?.filter { node -> node.elementType == CALL_EXPRESSION }
                    ?.forEach { node ->

                        val method = when (node.firstChildNode.text) {
                            METHOD_ANCIENT -> checkAncient(firstChildSection)
                            METHOD_PROTECTED -> checkProtected(firstChildSection)
                            METHOD_DIRECTORY -> checkDirectory(firstChildSection)
                            else -> null
                        }

                        val message = SnakemakeBundle.message("INSP.NAME.correct.use.method.title",
                                node.text, st.firstChild.text)

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