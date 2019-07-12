package com.jetbrains.snakecharm.lang.psi

import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.PsiNamedElement
import com.jetbrains.python.psi.PyElement
import com.jetbrains.python.psi.PyStatement
import com.jetbrains.python.psi.PyStatementListContainer

interface SmkRuleLike<out S : SmkSection>: PyElement, PyStatementListContainer, PyStatement,
        //ScopeOwner,
        PsiNamedElement, PsiNameIdentifierOwner {

    fun getSections(): List<SmkSection>
    fun getSectionByName(sectionName: String): S?
}