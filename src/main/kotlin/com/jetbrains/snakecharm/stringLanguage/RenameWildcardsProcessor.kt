package com.jetbrains.snakecharm.stringLanguage

import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.search.LocalSearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.refactoring.rename.RenamePsiElementProcessor
import com.jetbrains.snakecharm.lang.psi.SmkRuleOrCheckpoint
import com.jetbrains.snakecharm.stringLanguage.lang.psi.elementTypes.SmkSLReferenceExpressionImpl

class RenameWildcardsProcessor : RenamePsiElementProcessor() { //RenamePyElementProcessor()
    override fun canProcessElement(element: PsiElement) =
            element is SmkSLReferenceExpressionImpl

    // We want to search for wildcard references only in containing rule/checkpoint
    override fun findReferences(element: PsiElement): Collection<PsiReference> {
        if (element !is SmkSLReferenceExpressionImpl) {
            return super.findReferences(element)
        }

        val languageManager = InjectedLanguageManager.getInstance(element.project)
        val host = languageManager.getInjectionHost(element)
        val parentDeclaration = PsiTreeUtil.getParentOfType(host, SmkRuleOrCheckpoint::class.java)
                ?: return emptyList()

        return ReferencesSearch.search(element, LocalSearchScope(parentDeclaration)).toList()
    }
}
