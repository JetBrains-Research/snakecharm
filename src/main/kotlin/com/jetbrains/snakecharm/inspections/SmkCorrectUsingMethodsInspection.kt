package com.jetbrains.snakecharm.inspections

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.TokenSet
import com.jetbrains.python.PyElementTypes.CALL_EXPRESSION
import com.jetbrains.snakecharm.SnakemakeBundle
import com.jetbrains.snakecharm.lang.SnakemakeNames.METHOD_ANCIENT
import com.jetbrains.snakecharm.lang.SnakemakeNames.METHOD_DIRECTORY
import com.jetbrains.snakecharm.lang.SnakemakeNames.METHOD_PROTECTED
import com.jetbrains.snakecharm.lang.SnakemakeNames.METHOD_TEMP
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

        private fun getAllowedSectionNames(ruleOrCheckpointSectionName: String): ArrayList<String>? {

            return when (ruleOrCheckpointSectionName) {
                METHOD_ANCIENT -> arrayListOf(SECTION_INPUT)
                METHOD_PROTECTED -> arrayListOf(SECTION_OUTPUT, SECTION_LOG, SECTION_BENCHMARK)
                METHOD_DIRECTORY, METHOD_TEMP -> arrayListOf(SECTION_OUTPUT)
                else -> null
            }
        }

        private fun checkMethod(psiEl: PsiElement, ruleOrCheckpointSection: SmkRuleOrCheckpointArgsSection): PsiElement? {
            val allowedSectionNames = getAllowedSectionNames(psiEl.text) ?: return null

            if (!allowedSectionNames.contains(ruleOrCheckpointSection.firstChild.text)) {
                return psiEl
            }
            return null
        }

        override fun visitSmkRuleOrCheckpointArgsSection(st: SmkRuleOrCheckpointArgsSection) {

            if (st.containingFile !is SmkFile) {
                return
            }


            st.argumentList?.node?.getChildren(TokenSet.create(CALL_EXPRESSION))
                    ?.forEach { node ->

                        val method = checkMethod(node.firstChildNode.psi, st)

                        val message = SnakemakeBundle.message("INSP.NAME.correct.use.method.warning.message",
                                node.firstChildNode.text, st.firstChild.text)

                        if (method != null) {
                            holder.registerProblem(
                                    node.psi,
                                    message
                            )
                        }
                    }
        }
    }
}