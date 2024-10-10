package com.jetbrains.snakecharm.lang.psi.impl.stubs

import com.intellij.lang.ASTNode
import com.intellij.psi.stubs.StubElement
import com.jetbrains.snakecharm.lang.psi.SmkRule
import com.jetbrains.snakecharm.lang.psi.impl.SmkRuleImpl
import com.jetbrains.snakecharm.lang.psi.stubs.SmkRuleNameIndexCompanion.KEY
import com.jetbrains.snakecharm.lang.psi.stubs.SmkRuleStub

class SmkRuleElementType
    : SmkRuleLikeElementType<SmkRuleStub, SmkRule>("SMK_RULE_DECLARATION_STATEMENT", KEY) {

    override fun createPsi(stub: SmkRuleStub) = SmkRuleImpl(stub)

    override fun createElement(node: ASTNode) = SmkRuleImpl(node)

    override fun createStub(name: String?, parentStub: StubElement<*>?) =
            SmkRuleStubImpl(name, parentStub)

    override fun createStub(psi: SmkRule, parentStub: StubElement<*>?) =
            createStub(psi.name, parentStub)
}