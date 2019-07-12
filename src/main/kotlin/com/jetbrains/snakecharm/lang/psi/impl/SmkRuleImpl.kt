package com.jetbrains.snakecharm.lang.psi.impl

import com.intellij.lang.ASTNode
import com.jetbrains.python.psi.PyElementVisitor
import com.jetbrains.snakecharm.lang.psi.SMKElementVisitor
import com.jetbrains.snakecharm.lang.psi.SmkRuleArgsSection
import com.jetbrains.snakecharm.lang.psi.SmkRule
import com.jetbrains.snakecharm.lang.psi.elementTypes.SmkStubElementTypes
import com.jetbrains.snakecharm.lang.psi.stubs.SmkRuleStub

class SmkRuleImpl
    : SmkRuleLikeImpl<SmkRuleStub, SmkRule, SmkRuleArgsSection>, SmkRule {

    constructor(node: ASTNode): super(node)
    constructor(stub: SmkRuleStub): super(stub, SmkStubElementTypes.RULE_DECLARATION)

    override fun acceptPyVisitor(pyVisitor: PyElementVisitor) = when (pyVisitor) {
        is SMKElementVisitor -> pyVisitor.visitSMKRule(this)
        else -> super.acceptPyVisitor(pyVisitor)
    }
}