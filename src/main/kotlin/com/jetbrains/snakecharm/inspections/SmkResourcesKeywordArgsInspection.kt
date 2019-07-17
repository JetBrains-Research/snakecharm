package com.jetbrains.snakecharm.inspections

import com.intellij.codeInsight.FileModificationService
import com.intellij.codeInspection.*
import com.intellij.openapi.editor.Editor
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
            if(st.sectionKeyword != SnakemakeNames.SECTION_RESOURCES) {
                return
            }

            st.argumentList?.arguments?.forEach {
                if (it !is PyKeywordArgument) {
                    val action = MoveCaretAndInsertEqualsSignAction(SmartPointerManager.createPointer(it))
                    registerProblem(
                            it,
                            SnakemakeBundle.message("INSP.NAME.resources.unnamed.args"),
                            ProblemHighlightType.ERROR,
                            action,
                            action
                    )
                }
            }
        }
    }

    override fun getDisplayName(): String = SnakemakeBundle.message("INSP.NAME.resources.unnamed.args")

    private class MoveCaretAndInsertEqualsSignAction(
            private val elementPointer: SmartPsiElementPointer<PsiElement>
    ) : HintAction, LocalQuickFixAndIntentionActionOnPsiElement(elementPointer.element) {
        override fun startInWriteAction() = true

        override fun getFamilyName(): String = SnakemakeBundle.message("INSP.INTN.name.resource.family")

        override fun showHint(editor: Editor) = true

        override fun getText(): String = SnakemakeBundle.message("INSP.INTN.name.resource.text")

        override fun invoke(
                project: Project,
                file: PsiFile,
                editor: Editor?,
                startElement: PsiElement,
                endElement: PsiElement
        ) {
            if (!FileModificationService.getInstance().prepareFileForWrite(file)) {
                return
            }

            val offset = elementPointer.element?.textOffset ?: return
            editor?.caretModel?.moveToOffset(offset)
            if (editor?.document?.isWritable == true) {
                editor.document.insertString(offset, "=")
            }
        }
    }
}