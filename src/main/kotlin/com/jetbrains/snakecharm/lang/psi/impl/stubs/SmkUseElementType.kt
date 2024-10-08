package com.jetbrains.snakecharm.lang.psi.impl.stubs

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubElement
import com.jetbrains.snakecharm.lang.psi.SmkUse
import com.jetbrains.snakecharm.lang.psi.impl.SmkUseImpl
import com.jetbrains.snakecharm.lang.psi.stubs.SmkUseNameIndexCompanion.KEY
import com.jetbrains.snakecharm.lang.psi.stubs.SmkUseStub

class SmkUseElementType
    : SmkRuleLikeElementType<SmkUseStub, SmkUse>("SMK_USE_DECLARATION_STATEMENT", KEY) {

    override fun createStub(name: String?, parentStub: StubElement<*>?): SmkUseStub =
        SmkUseStubImpl(name, parentStub)

    override fun createStub(psi: SmkUse, parentStub: StubElement<out PsiElement>?): SmkUseStub =
        createStub(psi.name, parentStub)

    override fun createPsi(stub: SmkUseStub): SmkUse = SmkUseImpl(stub)

    override fun createElement(node: ASTNode): PsiElement = SmkUseImpl(node)
}