package com.jetbrains.snakecharm.codeInsight

import com.intellij.codeInsight.editorActions.CodeBlockProvider
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.python.psi.PyStatement
import com.jetbrains.snakecharm.lang.psi.SmkRule
import com.jetbrains.snakecharm.lang.psi.SmkRuleOrCheckpointArgsSection

class SmkCodeBlockProvider: CodeBlockProvider {
    override fun getCodeBlockRange(editor: Editor?, psiFile: PsiFile?): TextRange? {
        var caretOffset = editor?.getCaretModel()?.getOffset()

        var element = caretOffset?.let { psiFile?.findElementAt(it) } ?: return null

        while (caretOffset > 0 && element is PsiWhiteSpace) {
            caretOffset--
            element = psiFile?.findElementAt(caretOffset)!!
        }

        var statement = PsiTreeUtil.getParentOfType(element, PyStatement::class.java)

        if (statement != null) {
            var statementList = PsiTreeUtil.findChildOfType(statement, SmkRuleOrCheckpointArgsSection::class.java)

            // if the statement above caret is not a block statement, look above for a statement list and then find the statement above
            // that statement list
            if (statementList == null) {
                statementList = PsiTreeUtil.getParentOfType(statement, SmkRuleOrCheckpointArgsSection::class.java)
                if (statementList != null) {
                    statement = PsiTreeUtil.getParentOfType(statementList, SmkRuleOrCheckpointArgsSection::class.java)
                }
            }

            if (statement != null) {
                // if we're in the beginning of the statement already, pressing Ctrl-[ again should move the caret one statement higher
                val statementStart = statement.textRange.startOffset
                var statementEnd = statement.textRange.endOffset
                while (statementEnd > statementStart && psiFile!!.findElementAt(statementEnd) is PsiWhiteSpace) {
                    statementEnd--
                }
                if (caretOffset == statementStart || caretOffset == statementEnd) {
                    val statementAbove = PsiTreeUtil.getParentOfType(statement, SmkRule::class.java)
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