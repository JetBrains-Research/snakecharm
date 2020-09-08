package com.jetbrains.snakecharm.stringLanguage.codeInsight

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.search.LocalSearchScope
import com.intellij.psi.search.SearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.refactoring.rename.RenamePsiElementProcessor
import com.jetbrains.snakecharm.lang.psi.SmkRuleOrCheckpoint
import com.jetbrains.snakecharm.stringLanguage.lang.psi.SmkSLReferenceExpressionImpl

class SmkSLRenameWildcardsProcessor : RenamePsiElementProcessor() { //RenamePyElementProcessor()
    override fun canProcessElement(element: PsiElement) =
            element is SmkSLReferenceExpressionImpl

    override fun findReferences(
            element: PsiElement,
            searchScope: SearchScope,
            searchInCommentsAndStrings: Boolean
    ): Collection<PsiReference> {
        if (element !is SmkSLReferenceExpressionImpl) {
            return emptyList()
        }

         // We want to search for wildcard references only in containing rule/checkpoint
        val parentDeclaration = PsiTreeUtil.getParentOfType(
                element.injectionHost(), SmkRuleOrCheckpoint::class.java
        ) ?: return emptyList()

        return ReferencesSearch.search(element, LocalSearchScope(parentDeclaration)).toList()
    }
}
