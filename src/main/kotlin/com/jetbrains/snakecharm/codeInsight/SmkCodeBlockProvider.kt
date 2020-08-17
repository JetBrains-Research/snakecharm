package com.jetbrains.snakecharm.codeInsight

import com.intellij.codeInsight.editorActions.CodeBlockProvider
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.elementType
import com.jetbrains.python.psi.PyStatement
import com.jetbrains.python.psi.PyStatementList
import com.jetbrains.snakecharm.lang.psi.elementTypes.SmkElementTypes.RULE_OR_CHECKPOINT_ARGS_SECTION_STATEMENT

class SmkCodeBlockProvider: CodeBlockProvider {
    override fun getCodeBlockRange(editor: Editor?, psiFile: PsiFile?): TextRange? {
        var caretOffset = editor?.caretModel?.offset

        var element = caretOffset?.let { psiFile?.findElementAt(it) } ?: return null

        while (caretOffset > 0 && element is PsiWhiteSpace) {
            caretOffset--
            element = psiFile?.findElementAt(caretOffset)!!
        }

        var statement = PsiTreeUtil.getParentOfType(element, PyStatement::class.java)

        if (statement != null) {

            if (statement.elementType != RULE_OR_CHECKPOINT_ARGS_SECTION_STATEMENT) {
                var statementList = PsiTreeUtil.findChildOfType(statement, PyStatementList::class.java)

                // if the statement above caret is not a block statement, look above for a statement list and then find the statement above
                // that statement list
                if (statementList == null) {
                    statementList = PsiTreeUtil.getParentOfType(statement, PyStatementList::class.java)
                    if (statementList != null) {
                        statement = PsiTreeUtil.getParentOfType(statementList, PyStatement::class.java)
                    }
                }
            }

            if (statement != null) {
                // if we're in the beginning of the statement already, pressing Ctrl-[ again should move the caret one statement higher
                val statementStart = statement.textRange.startOffset
                val statementEnd = statement.textRange.endOffset

                if (caretOffset == statementStart || caretOffset == statementEnd) {
                    val statementAbove = PsiTreeUtil.getParentOfType(statement, PyStatement::class.java)
                    if (statementAbove != null) {
                        return if (caretOffset == statementStart) {
                            TextRange(statementAbove.textRange.startOffset, statementEnd)
                        } else {
                            TextRange(statementStart, statementAbove.textRange.endOffset)
                        }
                    }
                }
                return statement.textRange
            }
        }
        return null
    }
}