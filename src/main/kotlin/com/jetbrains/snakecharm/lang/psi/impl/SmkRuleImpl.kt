package com.jetbrains.snakecharm.lang.psi.impl

import com.intellij.lang.ASTNode
import com.intellij.util.PlatformIcons
import com.jetbrains.python.psi.PyElementVisitor
import com.jetbrains.python.psi.PyTypedElement
import com.jetbrains.python.psi.types.PyType
import com.jetbrains.python.psi.types.TypeEvalContext
import com.jetbrains.snakecharm.codeInsight.resolve.SmkFakePsiElement
import com.jetbrains.snakecharm.lang.SnakemakeNames
import com.jetbrains.snakecharm.lang.parser.SmkTokenTypes
import com.jetbrains.snakecharm.lang.psi.SmkElementVisitor
import com.jetbrains.snakecharm.lang.psi.SmkRule
import com.jetbrains.snakecharm.lang.psi.SmkRuleOrCheckpoint
import com.jetbrains.snakecharm.lang.psi.SmkRuleOrCheckpointArgsSection
import com.jetbrains.snakecharm.lang.psi.elementTypes.SmkStubElementTypes
import com.jetbrains.snakecharm.lang.psi.stubs.SmkRuleStub
import com.jetbrains.snakecharm.lang.psi.types.SmkRuleLikeSectionType
import com.jetbrains.snakecharm.lang.psi.types.SmkWildcardsType

class SmkRuleImpl
    : SmkRuleLikeImpl<SmkRuleStub, SmkRule, SmkRuleOrCheckpointArgsSection>, SmkRule {

    override val wildcardsElement = createFakeWildcardsPsiElement(this)

    override fun getType(context: TypeEvalContext, key: TypeEvalContext.Key)
            = SmkRuleLikeSectionType(this)

    constructor(node: ASTNode): super(node)
    constructor(stub: SmkRuleStub): super(stub, SmkStubElementTypes.RULE_DECLARATION_STATEMENT)

    override val sectionTokenType = SmkTokenTypes.RULE_KEYWORD

    override fun acceptPyVisitor(pyVisitor: PyElementVisitor) = when (pyVisitor) {
        is SmkElementVisitor -> pyVisitor.visitSmkRule(this)
        else -> super<SmkRuleLikeImpl>.acceptPyVisitor(pyVisitor)
    }

    companion object {
        fun createFakeWildcardsPsiElement(element: SmkRuleOrCheckpoint)
                = SmkWildcardFakePsiElement(element)
    }
}

class SmkWildcardFakePsiElement(val element: SmkRuleOrCheckpoint): SmkFakePsiElement(
        element, SnakemakeNames.SMK_VARS_WILDCARDS, PlatformIcons.PARAMETER_ICON
), PyTypedElement {
    override fun getType(typeEvalContext: TypeEvalContext, key: TypeEvalContext.Key): PyType = SmkWildcardsType(element)
}