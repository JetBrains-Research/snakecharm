package com.jetbrains.snakecharm.lang.psi.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.util.childrenOfType
import com.jetbrains.python.psi.PyExpression
import com.jetbrains.python.psi.impl.PyElementImpl
import com.jetbrains.snakecharm.lang.psi.SmkExcludedRulesNamesList

class SmkExcludedRulesNamesListImpl(node: ASTNode) : PyElementImpl(node), SmkExcludedRulesNamesList {
    override fun namesPsi() = childrenOfType<PyExpression>()
    override fun names() = namesPsi().map { it.text }
}