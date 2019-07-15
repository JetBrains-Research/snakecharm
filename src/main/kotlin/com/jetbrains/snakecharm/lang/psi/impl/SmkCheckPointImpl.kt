package com.jetbrains.snakecharm.lang.psi.impl

import com.intellij.lang.ASTNode
import com.jetbrains.python.psi.PyElementVisitor
import com.jetbrains.snakecharm.lang.parser.SnakemakeTokenTypes
import com.jetbrains.snakecharm.lang.psi.SmkCheckPoint
import com.jetbrains.snakecharm.lang.psi.SmkElementVisitor
import com.jetbrains.snakecharm.lang.psi.SmkRuleOrCheckpointArgsSection
import com.jetbrains.snakecharm.lang.psi.elementTypes.SmkStubElementTypes
import com.jetbrains.snakecharm.lang.psi.stubs.SmkCheckpointStub

class SmkCheckPointImpl
    : SmkRuleLikeImpl<SmkCheckpointStub, SmkCheckPoint, SmkRuleOrCheckpointArgsSection>, SmkCheckPoint {

    constructor(node: ASTNode): super(node)
    constructor(stub: SmkCheckpointStub): super(stub, SmkStubElementTypes.CHECKPOINT_DECLARATION_STATEMENT)

    override val sectionTokenType = SnakemakeTokenTypes.CHECKPOINT_KEYWORD

    override fun acceptPyVisitor(pyVisitor: PyElementVisitor) = when (pyVisitor) {
        is SmkElementVisitor -> pyVisitor.visitSmkCheckPoint(this)
        else -> super.acceptPyVisitor(pyVisitor)
    }
}