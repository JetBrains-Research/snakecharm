package com.jetbrains.snakecharm.lang.psi.impl.stubs

import com.intellij.lang.ASTNode
import com.intellij.psi.stubs.StubElement
import com.jetbrains.snakecharm.lang.psi.SMKRule
import com.jetbrains.snakecharm.lang.psi.impl.SMKRuleImpl
import com.jetbrains.snakecharm.lang.psi.stubs.SmkRuleNameIndex.Companion.KEY
import com.jetbrains.snakecharm.lang.psi.stubs.SmkRuleStub

class SmkRuleElementType
    : SmkRuleLikeElementType<SmkRuleStub, SMKRule>("SMK_RULE_DECLARATION", KEY){

    override fun createPsi(stub: SmkRuleStub) = SMKRuleImpl(stub)

    override fun createElement(node: ASTNode) = SMKRuleImpl(node)

    override fun createStub(name: String?, parentStub: StubElement<*>?) =
            SmkRuleStubImpl(name, parentStub)

    override fun createStub(psi: SMKRule, parentStub: StubElement<*>?) =
            createStub(psi.name, parentStub)
}