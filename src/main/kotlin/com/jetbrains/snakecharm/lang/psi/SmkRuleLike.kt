package com.jetbrains.snakecharm.lang.psi

import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.PsiNamedElement
import com.jetbrains.python.psi.PyStatementListContainer

interface SmkRuleLike<out S : SmkSection>: SmkSection, PyStatementListContainer,
        //ScopeOwner,
        PsiNamedElement, PsiNameIdentifierOwner {

    fun getSections(): List<SmkSection>
    fun getSectionByName(sectionName: String): S?
}