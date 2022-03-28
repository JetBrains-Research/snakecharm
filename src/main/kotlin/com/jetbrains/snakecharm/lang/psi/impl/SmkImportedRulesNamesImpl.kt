package com.jetbrains.snakecharm.lang.psi.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.python.psi.impl.PyElementImpl
import com.jetbrains.snakecharm.lang.psi.SmkImportedRulesNames
import com.jetbrains.snakecharm.lang.psi.SmkReferenceExpression
import com.jetbrains.snakecharm.lang.psi.SmkRuleOrCheckpoint

class SmkImportedRulesNamesImpl(node: ASTNode) : PyElementImpl(node), SmkImportedRulesNames {

    override fun arguments(): Array<SmkReferenceExpression>? =
        PsiTreeUtil.getChildrenOfType(this, SmkReferenceExpression::class.java)

    override fun argumentsNames() = arguments()?.mapNotNull { it.name }

    override fun resolveArguments() = arguments()?.mapNotNull { it.reference.resolve() as? SmkRuleOrCheckpoint }
}