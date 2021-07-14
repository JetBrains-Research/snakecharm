package com.jetbrains.snakecharm.lang.psi

import com.intellij.lang.ASTNode
import com.jetbrains.python.psi.PyElementType
import com.jetbrains.python.psi.PyElementVisitor
import com.jetbrains.snakecharm.lang.parser.SmkTokenTypes
import com.jetbrains.snakecharm.lang.psi.elementTypes.SmkStubElementTypes
import com.jetbrains.snakecharm.lang.psi.impl.SmkRuleLikeImpl
import com.jetbrains.snakecharm.lang.psi.stubs.SmkModuleStub

class SmkModuleImpl : SmkRuleLikeImpl<SmkModuleStub, SmkModule, SmkModuleArgsSection>, SmkModule {

    constructor(node: ASTNode) : super(node)
    constructor(stub: SmkModuleStub) : super(stub, SmkStubElementTypes.MODULE_DECLARATION_STATEMENT)

    override val sectionTokenType: PyElementType = SmkTokenTypes.MODULE_KEYWORD

    override fun acceptPyVisitor(pyVisitor: PyElementVisitor?) {
        when (pyVisitor) {
            is SmkElementVisitor -> pyVisitor.visitSmkModule(this)
            else -> super.acceptPyVisitor(pyVisitor)
        }
    }
}