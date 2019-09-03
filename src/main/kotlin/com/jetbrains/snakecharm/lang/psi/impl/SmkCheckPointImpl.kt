package com.jetbrains.snakecharm.lang.psi.impl

import com.intellij.lang.ASTNode
import com.jetbrains.python.psi.PyElementVisitor
import com.jetbrains.python.psi.PyFile
import com.jetbrains.python.psi.types.TypeEvalContext
import com.jetbrains.snakecharm.lang.parser.SmkTokenTypes
import com.jetbrains.snakecharm.lang.psi.SmkCheckPoint
import com.jetbrains.snakecharm.lang.psi.SmkElementVisitor
import com.jetbrains.snakecharm.lang.psi.SmkRuleOrCheckpointArgsSection
import com.jetbrains.snakecharm.lang.psi.elementTypes.SmkStubElementTypes
import com.jetbrains.snakecharm.lang.psi.stubs.SmkCheckpointStub
import com.jetbrains.snakecharm.lang.psi.types.CheckpointType

class SmkCheckPointImpl
    : SmkRuleLikeImpl<SmkCheckpointStub, SmkCheckPoint, SmkRuleOrCheckpointArgsSection>, SmkCheckPoint {

    override val wildcardsElement = SmkRuleImpl.createFakeWildcardsPsiElement(this)
    val checkpointDelegate = SmkRuleImpl.createFakeWildcardsPsiElement(this)

    override fun getType(context: TypeEvalContext, key: TypeEvalContext.Key): CheckpointType {
        // TODO maybe better is to return PyClassType without intermediate checkpoint type?
        val pyClass = CheckpointType.resolveDeclarationClass(containingFile as PyFile, context)
        val cpType = pyClass?.getType(context, key)
        return CheckpointType(this, cpType)
    }

    constructor(node: ASTNode): super(node)
    constructor(stub: SmkCheckpointStub): super(stub, SmkStubElementTypes.CHECKPOINT_DECLARATION_STATEMENT)

    override val sectionTokenType = SmkTokenTypes.CHECKPOINT_KEYWORD

    override fun acceptPyVisitor(pyVisitor: PyElementVisitor) = when (pyVisitor) {
        is SmkElementVisitor -> pyVisitor.visitSmkCheckPoint(this)
        else -> super.acceptPyVisitor(pyVisitor)
    }
}