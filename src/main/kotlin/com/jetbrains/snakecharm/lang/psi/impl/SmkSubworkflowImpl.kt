package com.jetbrains.snakecharm.lang.psi.impl

import com.intellij.lang.ASTNode
import com.jetbrains.python.psi.PyElementVisitor
import com.jetbrains.snakecharm.lang.parser.SmkTokenTypes
import com.jetbrains.snakecharm.lang.psi.SmkElementVisitor
import com.jetbrains.snakecharm.lang.psi.SmkSubworkflow
import com.jetbrains.snakecharm.lang.psi.SmkSubworkflowArgsSection
import com.jetbrains.snakecharm.lang.psi.elementTypes.SmkStubElementTypes
import com.jetbrains.snakecharm.lang.psi.stubs.SmkSubworkflowStub

class SmkSubworkflowImpl:
        SmkRuleLikeImpl<SmkSubworkflowStub, SmkSubworkflow, SmkSubworkflowArgsSection>, SmkSubworkflow {

    constructor(node: ASTNode): super(node)
    constructor(stub: SmkSubworkflowStub): super(stub, SmkStubElementTypes.SUBWORKFLOW_DECLARATION_STATEMENT)

    override val sectionTokenType = SmkTokenTypes.SUBWORKFLOW_KEYWORD

    override fun acceptPyVisitor(pyVisitor: PyElementVisitor) {
        when (pyVisitor) {
            is SmkElementVisitor -> pyVisitor.visitSmkSubworkflow(this)
            else -> super.acceptPyVisitor(pyVisitor)
        }
    }
}