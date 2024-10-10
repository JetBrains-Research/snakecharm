package com.jetbrains.snakecharm.lang.psi.impl.stubs

import com.intellij.lang.ASTNode
import com.intellij.psi.stubs.StubElement
import com.jetbrains.snakecharm.lang.psi.SmkCheckPoint
import com.jetbrains.snakecharm.lang.psi.impl.SmkCheckPointImpl
import com.jetbrains.snakecharm.lang.psi.stubs.SmkCheckpointNameIndexCompanion.KEY
import com.jetbrains.snakecharm.lang.psi.stubs.SmkCheckpointStub

class SmkCheckpointElementType
    : SmkRuleLikeElementType<SmkCheckpointStub, SmkCheckPoint>(
        "SMK_CHECKPOINT_DECLARATION_STATEMENT", KEY
){

    override fun createPsi(stub: SmkCheckpointStub) = SmkCheckPointImpl(stub)

    override fun createElement(node: ASTNode) = SmkCheckPointImpl(node)

    override fun createStub(name: String?, parentStub: StubElement<*>?) =
            SmkCheckpointStubImpl(name, parentStub)

    override fun createStub(psi: SmkCheckPoint, parentStub: StubElement<*>?) =
            createStub(psi.name, parentStub)
}