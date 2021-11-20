package com.jetbrains.snakecharm.inspections

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.jetbrains.python.psi.PyArgumentList
import com.jetbrains.python.psi.PyKeywordArgument
import com.jetbrains.snakecharm.SnakemakeBundle
import com.jetbrains.snakecharm.lang.psi.SmkArgsSection
import com.jetbrains.snakecharm.lang.psi.SmkRuleOrCheckpointArgsSection
import com.jetbrains.snakecharm.lang.psi.SmkSubworkflowArgsSection
import com.jetbrains.snakecharm.lang.psi.SmkWorkflowArgsSection

class SmkSectionDuplicatedArgsInspection : SnakemakeInspection() {
    override fun buildVisitor(
            holder: ProblemsHolder,
            isOnTheFly: Boolean,
            session: LocalInspectionToolSession
    ) = object : SnakemakeInspectionVisitor(holder, session) {

        override fun visitSmkSubworkflowArgsSection(st: SmkSubworkflowArgsSection) {
            checkArgumentList(st.argumentList, st)
        }

        override fun visitSmkRuleOrCheckpointArgsSection(st: SmkRuleOrCheckpointArgsSection) {
            checkArgumentList(st.argumentList, st)
        }

        override fun visitSmkWorkflowArgsSection(st: SmkWorkflowArgsSection) {
            checkArgumentList(st.argumentList, st)
        }

        private fun checkArgumentList(
                argumentList: PyArgumentList?,
                section: SmkArgsSection
        ) {
            val args = argumentList?.arguments ?: emptyArray()
            if (args.size > 1) {
                val setOfDeclaredArguments = mutableSetOf<String>()

                args.forEach { arg ->

                    /* PyKeywordArgument is checked by SmkSyntaxErrorAnnotator */
                    if (arg !is PyKeywordArgument) {
                        val text = arg.text

                        if (text in setOfDeclaredArguments) {
                            registerProblem(
                                    arg,
                                    SnakemakeBundle.message("INSP.NAME.section.duplicated.args.message",
                                            section.sectionKeyword!!),
                                    RemoveArgumentQuickFix
                            )
                        } else {
                            setOfDeclaredArguments.add(text)
                        }
                    }
                }
            }
        }
    }

    private object RemoveArgumentQuickFix : LocalQuickFix {
        override fun getFamilyName() = SnakemakeBundle.message("INSP.INTN.remove.duplicated.arg")

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            WriteCommandAction.runWriteCommandAction(project) { descriptor.psiElement.delete() }
        }
    }

    override fun getDisplayName(): String = SnakemakeBundle.message("INSP.NAME.section.duplicated.args")
}
