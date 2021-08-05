package com.jetbrains.snakecharm.lang.psi.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.util.elementType
import com.jetbrains.python.psi.PyElementType
import com.jetbrains.python.psi.PyElementVisitor
import com.jetbrains.python.psi.types.TypeEvalContext
import com.jetbrains.snakecharm.lang.parser.SmkTokenTypes
import com.jetbrains.snakecharm.lang.psi.*
import com.jetbrains.snakecharm.lang.psi.elementTypes.SmkElementTypes
import com.jetbrains.snakecharm.lang.psi.elementTypes.SmkStubElementTypes
import com.jetbrains.snakecharm.lang.psi.stubs.SmkUseStub
import com.jetbrains.snakecharm.lang.psi.types.SmkRuleLikeSectionType

class SmkUseImpl : SmkRuleLikeImpl<SmkUseStub, SmkUse, SmkRuleOrCheckpointArgsSection>, SmkUse {

    constructor(node: ASTNode) : super(node)
    constructor(stub: SmkUseStub) : super(stub, SmkStubElementTypes.USE_DECLARATION_STATEMENT)

    override val sectionTokenType: PyElementType = SmkTokenTypes.USE_KEYWORD

    override val wildcardsElement = SmkRuleImpl.createFakeWildcardsPsiElement(this)

    override fun getType(context: TypeEvalContext, key: TypeEvalContext.Key) = SmkRuleLikeSectionType(this)

    override fun acceptPyVisitor(pyVisitor: PyElementVisitor?) {
        when (pyVisitor) {
            is SmkElementVisitor -> pyVisitor.visitSmkUse(this)
            else -> super.acceptPyVisitor(pyVisitor)
        }
    }

    override fun getName(): String? {
        val result = getProducedRulesNames().lastOrNull()?.first ?: return null
        return if (result.contains('*')) null else result
    }

    override fun getNameNode(): ASTNode? {
        val names = getProducedRulesNames()
        if (names.isNotEmpty()) {
            return names.last().second.node
        }
        return (findChildByType(SmkElementTypes.USE_NAME_IDENTIFIER) as? PsiElement)?.node
    }

    override fun getProducedRulesNames(): List<Pair<String, PsiElement>> {
        val identifier = super.getNameNode()
        if (identifier != null) {
            return listOf(identifier.text to identifier.psi)
        }
        val importedNames: PsiElement? = findChildByType(SmkElementTypes.USE_IMPORTED_RULES_NAMES)
        val newName: PsiElement? = findChildByType(SmkElementTypes.USE_NAME_IDENTIFIER)
        if (importedNames == null) {
            return emptyList()
        }
        val originalNames = mutableListOf<PsiElement>()
        var child = importedNames.firstChild
        while (child != null) {
            if (child.elementType == SmkElementTypes.REFERENCE_EXPRESSION) {
                originalNames.add(child)
            }
            child = child.nextSibling
        }
        if (originalNames.isEmpty()) {
            return emptyList()
        }
        val result = mutableListOf<Pair<String, PsiElement>>()
        if (newName != null) {
            originalNames.forEach {
                if (it.text != "*") {
                    result.add(newName.text.replace("*", it.text) to it)
                }
            }
        } else {
            originalNames.forEach {
                if (it.text != "*") {
                    result.add(it.text to it)
                }
            }
        }
        return result
    }
}