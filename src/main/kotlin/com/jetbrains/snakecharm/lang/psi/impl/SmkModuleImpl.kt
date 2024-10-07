package com.jetbrains.snakecharm.lang.psi.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiFile
import com.jetbrains.python.psi.PyElementType
import com.jetbrains.python.psi.PyElementVisitor
import com.jetbrains.snakecharm.lang.SnakemakeNames
import com.jetbrains.snakecharm.lang.parser.SmkTokenTypes
import com.jetbrains.snakecharm.lang.psi.SmkElementVisitor
import com.jetbrains.snakecharm.lang.psi.SmkModule
import com.jetbrains.snakecharm.lang.psi.SmkModuleArgsSection
import com.jetbrains.snakecharm.lang.psi.elementTypes.SmkStubElementTypes
import com.jetbrains.snakecharm.lang.psi.stubs.SmkModuleStub

class SmkModuleImpl : SmkRuleLikeImpl<SmkModuleStub, SmkModule, SmkModuleArgsSection>, SmkModule {

    constructor(node: ASTNode) : super(node)
    constructor(stub: SmkModuleStub) : super(stub, SmkStubElementTypes.MODULE_DECLARATION_STATEMENT)

    override val sectionTokenType: PyElementType = SmkTokenTypes.MODULE_KEYWORD

    override fun acceptPyVisitor(pyVisitor: PyElementVisitor?) {
        when (pyVisitor) {
            is SmkElementVisitor -> pyVisitor.visitSmkModule(this)
            else -> super<SmkRuleLikeImpl>.acceptPyVisitor(pyVisitor)
        }
    }

    override fun getPsiFile(): PsiFile? {
        val path = getSectionByName(SnakemakeNames.MODULE_SNAKEFILE_KEYWORD)
        return path?.reference?.resolve() as? PsiFile
    }
}