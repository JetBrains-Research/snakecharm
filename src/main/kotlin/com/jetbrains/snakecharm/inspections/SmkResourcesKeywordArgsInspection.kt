package com.jetbrains.snakecharm.inspections

import com.intellij.codeInsight.FileModificationService
import com.intellij.codeInspection.*
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.jetbrains.python.psi.PyKeywordArgument
import com.jetbrains.snakecharm.SnakemakeBundle
import com.jetbrains.snakecharm.lang.SnakemakeNames
import com.jetbrains.snakecharm.lang.psi.SmkRuleOrCheckpointArgsSection

class SmkResourcesKeywordArgsInspection : SnakemakeInspection() {
    override fun buildVisitor(
            holder: ProblemsHolder,
            isOnTheFly: Boolean,
            session: LocalInspectionToolSession
    ) = object : SnakemakeInspectionVisitor(holder, session) {
        override fun visitSmkRuleOrCheckpointArgsSection(st: SmkRuleOrCheckpointArgsSection) {
            if (st.sectionKeyword != SnakemakeNames.SECTION_RESOURCES) {
                return
            }

            st.argumentList?.arguments?.forEach {
                if (it !is PyKeywordArgument) {
                    registerProblem(
                            it,
                            SnakemakeBundle.message("INSP.NAME.resources.unnamed.args"),
                            ProblemHighlightType.GENERIC_ERROR,
                            null, MoveCaretAndInsertEqualsSignQuickFix()
                    )
                }
            }
        }
    }

    override fun getDisplayName(): String = SnakemakeBundle.message("INSP.NAME.resources.unnamed.args")

    private class MoveCaretAndInsertEqualsSignQuickFix : LocalQuickFix {
        override fun getFamilyName() = SnakemakeBundle.message("INSP.INTN.name.resource.family")

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val offset = descriptor.psiElement.textOffset
            val editor = FileEditorManager.getInstance(project).selectedTextEditor
            editor?.caretModel?.moveToOffset(offset)
            if (editor?.document?.isWritable == true) {
                editor.document.insertString(offset, "=")
            }
        }
    }
}