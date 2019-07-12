package com.jetbrains.snakecharm.lang.psi.impl

import com.intellij.lang.ASTNode
import com.jetbrains.python.psi.PyElementVisitor
import com.jetbrains.snakecharm.lang.psi.SMKElementVisitor
import com.jetbrains.snakecharm.lang.psi.SmkSubworkflowArgsSection
import com.jetbrains.snakecharm.lang.psi.SmkSubworkflow
import com.jetbrains.snakecharm.lang.psi.elementTypes.SmkStubElementTypes
import com.jetbrains.snakecharm.lang.psi.stubs.SmkSubworkflowStub

class SmkSubworkflowImpl:
        SmkRuleLikeImpl<SmkSubworkflowStub, SmkSubworkflow, SmkSubworkflowArgsSection>, SmkSubworkflow {

    constructor(node: ASTNode): super(node)
    constructor(stub: SmkSubworkflowStub): super(stub, SmkStubElementTypes.SUBWORKFLOW_DECLARATION_STATEMENT)

    override fun acceptPyVisitor(pyVisitor: PyElementVisitor) {
        when (pyVisitor) {
            is SMKElementVisitor -> pyVisitor.visitSMKSubworkflow(this)
            else -> super.acceptPyVisitor(pyVisitor)
        }
    }
}