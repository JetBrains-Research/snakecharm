package com.jetbrains.snakecharm.inspections

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.LocalQuickFixOnPsiElement
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.util.elementType
import com.intellij.refactoring.suggested.startOffset
import com.jetbrains.python.PyTokenTypes
import com.jetbrains.python.psi.PyCallExpression
import com.jetbrains.snakecharm.SnakemakeBundle
import com.jetbrains.snakecharm.lang.psi.SmkRuleOrCheckpointArgsSection

class SmkMultilineFunctionCallInspection : SnakemakeInspection() {
    override fun buildVisitor(
        holder: ProblemsHolder,
        isOnTheFly: Boolean,
        session: LocalInspectionToolSession,
    ) = object : SnakemakeInspectionVisitor(holder, getContext(session)) {
        override fun visitSmkRuleOrCheckpointArgsSection(st: SmkRuleOrCheckpointArgsSection) {
            val args = st.argumentList ?: return

            if (st.multilineSectionDefinition()) {
                return
            }

            val invalidWhitespaces = mutableListOf<PsiElement>()
            args.arguments.forEach { psi ->
                if (psi is PyCallExpression) {
                    collectNewlinesInMultilineCall(psi, invalidWhitespaces)
                }
            }

            val invalidWsPnts = invalidWhitespaces.map { SmartPointerManager.createPointer(it) }
            invalidWhitespaces.forEach {
                registerProblem(
                    it,
                    SnakemakeBundle.message("INSP.NAME.multiline.func.call"),
                    ShiftToNextLine(st, invalidWsPnts)
                )
            }
        }
    }

    /**
     * Checks whether there are any whitespaces in [expression] argument list,
     * if so, checks if it contains new line character,
     * if so, adds whitespace nodes to [incorrectElements]
     */
    private fun collectNewlinesInMultilineCall(
        expression: PyCallExpression,
        incorrectElements: MutableList<PsiElement>,
    ) {
        var element = expression.argumentList?.firstChild
        while (element != null) {
            if (element.elementType == TokenType.WHITE_SPACE && element.text.contains('\n')) {
                incorrectElements.add(element)
                break
            }
            element = element.nextSibling
        }
    }

    /**
     * Quick-Fix is based on document, so preview not available for such implementation.
     */
    private class ShiftToNextLine(expr: PsiElement, val incorrectElements: List<SmartPsiElementPointer<PsiElement>>) :
        LocalQuickFixOnPsiElement(expr) {

        override fun getFamilyName() = SnakemakeBundle.message("INSP.NAME.multiline.func.call.fix")

        override fun getText() = familyName

        override fun invoke(project: Project, file: PsiFile, startElement: PsiElement, endElement: PsiElement) {
            val doc = PsiDocumentManager.getInstance(project).getDocument(file)
            val indentCandidate = startElement.prevSibling
            if (indentCandidate !is PsiWhiteSpace || doc == null) {
                return
            }
            val argumentList = (startElement as SmkRuleOrCheckpointArgsSection).argumentList ?: return
            val indent = indentCandidate.text.replace("\n", "")
            // Moves every argument list element to new line
            argumentList.arguments.forEach { expression ->
                doc.insertString(expression.startOffset, "\n$indent$indent")
                PsiDocumentManager.getInstance(project).commitDocument(doc)
            }
            // Deletes every incorrect whitespace
            // If there are END_OF_LINE_COMMENT, new whitespace will be inserted automatically
            // Otherwise, we need to insert it manually
            incorrectElements.mapNotNull { it.element }.forEach { space ->
                val hasComment = space.prevSibling.elementType == PyTokenTypes.END_OF_LINE_COMMENT
                val offset = space.startOffset
                space.delete()
                PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(doc)
                if (!hasComment) {
                    doc.insertString(offset, "\n$indent$indent$indent")
                    PsiDocumentManager.getInstance(project).commitDocument(doc)
                }
            }
        }
    }
}