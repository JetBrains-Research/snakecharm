package com.jetbrains.snakecharm.lang.psi

import com.intellij.psi.PsiNameIdentifierOwner
import com.jetbrains.python.psi.PyElementType
import com.jetbrains.python.psi.PyStatement
import com.jetbrains.python.psi.PyStatementListContainer

interface SmkRuleLike<out S : SmkSection>: SmkSection, SmkToplevelSection, PyStatementListContainer,
        PyStatement,
        //ScopeOwner,
        PsiNameIdentifierOwner {

    val sectionTokenType: PyElementType
    fun getSections(): List<SmkSection>
    fun getSectionByName(sectionName: String): S?
}