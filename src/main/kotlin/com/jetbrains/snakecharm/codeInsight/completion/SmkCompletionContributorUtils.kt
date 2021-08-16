package com.jetbrains.snakecharm.codeInsight.completion

import com.intellij.psi.PsiElement
import com.intellij.psi.util.elementType
import com.jetbrains.python.psi.PyElementType
import com.jetbrains.python.psi.PyReferenceExpression

class SmkCompletionContributorUtils {
    companion object {
        fun checkTokenSequence(tokenList: List<Any>, element: PsiElement): Boolean {
            var currentToken = element
            return tokenList.all { token ->
                currentToken = currentToken.prevSibling
                if (currentToken is PyReferenceExpression) {
                    currentToken = currentToken.lastChild
                }
                token is PyElementType && token == currentToken.elementType ||
                        token is String && token == currentToken.text
            }
        }
    }
}