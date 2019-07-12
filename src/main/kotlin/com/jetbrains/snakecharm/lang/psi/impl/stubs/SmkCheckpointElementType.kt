package com.jetbrains.snakecharm.lang.psi.impl.stubs

import com.intellij.lang.ASTNode
import com.intellij.psi.stubs.StubElement
import com.jetbrains.snakecharm.lang.psi.SMKCheckPoint
import com.jetbrains.snakecharm.lang.psi.impl.SMKCheckPointImpl
import com.jetbrains.snakecharm.lang.psi.stubs.SmkCheckpointNameIndex.Companion.KEY
import com.jetbrains.snakecharm.lang.psi.stubs.SmkCheckpointStub

class SmkCheckpointElementType
    : SmkRuleLikeElementType<SmkCheckpointStub, SMKCheckPoint>("SMK_CHECKPOINT_DECLARATION", KEY){

    override fun createPsi(stub: SmkCheckpointStub) = SMKCheckPointImpl(stub)

    override fun createElement(node: ASTNode) = SMKCheckPointImpl(node)

    override fun createStub(name: String?, parentStub: StubElement<*>?) =
            SmkCheckpointStubImpl(name, parentStub)

    override fun createStub(psi: SMKCheckPoint, parentStub: StubElement<*>?) =
            createStub(psi.name, parentStub)
}