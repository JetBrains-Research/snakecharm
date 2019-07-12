package com.jetbrains.snakecharm.lang.psi.impl

import com.intellij.lang.ASTNode
import com.jetbrains.python.psi.PyElementVisitor
import com.jetbrains.snakecharm.lang.psi.SMKElementVisitor
import com.jetbrains.snakecharm.lang.psi.SMKRule
import com.jetbrains.snakecharm.lang.psi.SMKRuleParameterListStatement
import com.jetbrains.snakecharm.lang.psi.elementTypes.SmkStubElementTypes
import com.jetbrains.snakecharm.lang.psi.stubs.SmkRuleStub

class SMKRuleImpl
    : SmkRuleLikeImpl<SmkRuleStub, SMKRule, SMKRuleParameterListStatement>, SMKRule {

    constructor(node: ASTNode): super(node)
    constructor(stub: SmkRuleStub): super(stub, SmkStubElementTypes.RULE_DECLARATION)

    override fun acceptPyVisitor(pyVisitor: PyElementVisitor) = when (pyVisitor) {
        is SMKElementVisitor -> pyVisitor.visitSMKRule(this)
        else -> super.acceptPyVisitor(pyVisitor)
    }
}