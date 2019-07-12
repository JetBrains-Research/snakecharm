package com.jetbrains.snakecharm.lang.psi.impl

import com.intellij.lang.ASTNode
import com.jetbrains.python.psi.PyElementVisitor
import com.jetbrains.snakecharm.lang.psi.SMKCheckPoint
import com.jetbrains.snakecharm.lang.psi.SMKElementVisitor
import com.jetbrains.snakecharm.lang.psi.SMKRuleParameterListStatement
import com.jetbrains.snakecharm.lang.psi.elementTypes.SmkStubElementTypes
import com.jetbrains.snakecharm.lang.psi.stubs.SmkCheckpointStub

class SMKCheckPointImpl
    : SmkRuleLikeImpl<SmkCheckpointStub, SMKCheckPoint, SMKRuleParameterListStatement>, SMKCheckPoint {

    constructor(node: ASTNode): super(node)
    constructor(stub: SmkCheckpointStub): super(stub, SmkStubElementTypes.CHECKPOINT_DECLARATION)

    override fun acceptPyVisitor(pyVisitor: PyElementVisitor) = when (pyVisitor) {
        is SMKElementVisitor -> pyVisitor.visitSMKCheckPoint(this)
        else -> super.acceptPyVisitor(pyVisitor)
    }
}