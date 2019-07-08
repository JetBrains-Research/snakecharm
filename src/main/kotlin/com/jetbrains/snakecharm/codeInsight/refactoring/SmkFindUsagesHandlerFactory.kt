package com.jetbrains.snakecharm.codeInsight.refactoring

import com.intellij.find.findUsages.FindUsagesHandler
import com.intellij.find.findUsages.FindUsagesHandlerFactory
import com.intellij.psi.PsiElement
import com.jetbrains.python.findUsages.PyFunctionFindUsagesHandler
import com.jetbrains.snakecharm.lang.psi.SmkRuleLike

class SmkFindUsagesHandlerFactory: FindUsagesHandlerFactory() {
    override fun createFindUsagesHandler(element: PsiElement, forHighlightUsages: Boolean): FindUsagesHandler? {
        if (element is SmkRuleLike<*>) {
            return PyFunctionFindUsagesHandler(element)
        }
        return null
    }

    override fun canFindUsages(element: PsiElement): Boolean {
        return element is SmkRuleLike<*>
    }
}