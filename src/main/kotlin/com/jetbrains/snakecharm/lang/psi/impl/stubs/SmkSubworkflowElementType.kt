package com.jetbrains.snakecharm.lang.psi.impl.stubs

import com.intellij.lang.ASTNode
import com.intellij.psi.stubs.StubElement
import com.jetbrains.snakecharm.lang.psi.SmkSubworkflow
import com.jetbrains.snakecharm.lang.psi.impl.SmkSubworkflowImpl
import com.jetbrains.snakecharm.lang.psi.stubs.SmkSubworkflowStub

class SmkSubworkflowElementType
    : SmkRuleLikeElementType<SmkSubworkflowStub, SmkSubworkflow>("SMK_SUBWORKFLOW_DECLARATION", null) {

    override fun createPsi(stub: SmkSubworkflowStub) = SmkSubworkflowImpl(stub)

    override fun createElement(node: ASTNode) = SmkSubworkflowImpl(node)

    override fun createStub(name: String?, parentStub: StubElement<*>?) =
            SmkSubworkflowStubImpl(name, parentStub)

    override fun createStub(psi: SmkSubworkflow, parentStub: StubElement<*>?) =
            SmkSubworkflowStubImpl(psi.name, parentStub)
}