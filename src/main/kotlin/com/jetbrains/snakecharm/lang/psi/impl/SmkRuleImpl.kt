package com.jetbrains.snakecharm.lang.psi.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.jetbrains.python.psi.PyElementVisitor
import com.jetbrains.python.psi.types.TypeEvalContext
import com.jetbrains.snakecharm.lang.parser.SnakemakeTokenTypes
import com.jetbrains.snakecharm.lang.psi.*
import com.jetbrains.snakecharm.lang.psi.elementTypes.SmkStubElementTypes
import com.jetbrains.snakecharm.lang.psi.stubs.SmkRuleStub
import com.jetbrains.snakecharm.lang.psi.types.SmkRuleLikeType

class SmkRuleImpl
    : SmkRuleLikeImpl<SmkRuleStub, SmkRule, SmkRuleOrCheckpointArgsSection>, SmkRule {
    override fun getType(context: TypeEvalContext, key: TypeEvalContext.Key)
            = SmkRuleLikeType(this)

    constructor(node: ASTNode): super(node)
    constructor(stub: SmkRuleStub): super(stub, SmkStubElementTypes.RULE_DECLARATION_STATEMENT)

    override val sectionTokenType = SnakemakeTokenTypes.RULE_KEYWORD

    override fun acceptPyVisitor(pyVisitor: PyElementVisitor) = when (pyVisitor) {
        is SmkElementVisitor -> pyVisitor.visitSmkRule(this)
        else -> super.acceptPyVisitor(pyVisitor)
    }

    override fun collectWildcards(): List<Pair<String, PsiElement>> {
        val wildcardsCollector = SmkWildcardsCollector()
        wildcardsCollector.visitSmkRule(this)

        return wildcardsCollector.collectedWildcards.toList()
    }
}