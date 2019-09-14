 package com.jetbrains.snakecharm.codeInsight.refactoring

import com.intellij.find.findUsages.FindUsagesHandler
import com.intellij.find.findUsages.FindUsagesHandlerFactory
import com.intellij.psi.PsiElement
import com.jetbrains.python.findUsages.PyFunctionFindUsagesHandler
import com.jetbrains.snakecharm.codeInsight.resolve.SmkFakePsiElement
import com.jetbrains.snakecharm.lang.psi.SmkSection

 /**
  * Enables find usages for an element, e.g highlight usages
  */
class SmkFindUsagesHandlerFactory: FindUsagesHandlerFactory() {
    override fun createFindUsagesHandler(element: PsiElement, forHighlightUsages: Boolean): FindUsagesHandler? {
        if (canFindUsages(element)) {
            return PyFunctionFindUsagesHandler(element)
        }
        return null
    }

    override fun canFindUsages(element: PsiElement): Boolean {
        return (element is SmkSection || element is SmkFakePsiElement)
    }
}