package com.jetbrains.snakecharm.lang.psi.impl.stubs

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubElement
import com.jetbrains.snakecharm.lang.psi.SmkModule
import com.jetbrains.snakecharm.lang.psi.impl.SmkModuleImpl
import com.jetbrains.snakecharm.lang.psi.stubs.SmkModuleStub

class SmkModuleElementType
    : SmkRuleLikeElementType<SmkModuleStub, SmkModule>("SMK_MODULE_DECLARATION_STATEMENT", null) {

    override fun createStub(name: String?, parentStub: StubElement<*>?): SmkModuleStub =
        SmkModuleStubImpl(name, parentStub)

    override fun createStub(psi: SmkModule, parentStub: StubElement<out PsiElement>?): SmkModuleStub =
        SmkModuleStubImpl(psi.name, parentStub)

    override fun createPsi(stub: SmkModuleStub): SmkModule = SmkModuleImpl(stub)

    override fun createElement(node: ASTNode): PsiElement = SmkModuleImpl(node)

}