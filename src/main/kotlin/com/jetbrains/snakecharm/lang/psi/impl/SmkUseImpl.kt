package com.jetbrains.snakecharm.lang.psi.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
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

    override fun getNameNode(): ASTNode? {
        val identifier = super.getNameNode()
        if (identifier != null) { // Returns new name if we know it
            return identifier
        }
        val namePattern = findChildByType(SmkElementTypes.USE_NAME_IDENTIFIER) as? PsiElement
        if (namePattern != null) { // Returns name patter if it exits
            return namePattern.node
        }
        val originalNames = findChildByType(SmkElementTypes.USE_IMPORTED_RULES_NAMES) as? PsiElement
        // Returns original names, we don't want to save one name because
        // index with this name probably already exists
        // so we save whole rules names if it is not just '*' wildcard
        if (originalNames != null && originalNames.text != "*") {
            return originalNames.node
        }
        return null
    }

    override fun getProducedRulesNames(visitedFiles: MutableSet<PsiFile>): List<Pair<String, PsiElement>> {
        val identifier = super.getNameNode()
        if (identifier != null) {
            return listOf(identifier.text to identifier.psi)
        }
        val importedNames: PsiElement? = findChildByType(SmkElementTypes.USE_IMPORTED_RULES_NAMES)
        val newName: PsiElement? = findChildByType(SmkElementTypes.USE_NAME_IDENTIFIER)
        if (importedNames == null) {
            return emptyList()
        }
        val originalNames = mutableListOf<Pair<String, PsiElement>>()
        var child = importedNames.firstChild
        while (child != null) {
            if (child.elementType == SmkElementTypes.REFERENCE_EXPRESSION) {
                originalNames.add(child.text to child)
            }
            child = child.nextSibling
        }
        if (originalNames.isEmpty()) {
            val moduleRef =
                (findChildByType(SmkElementTypes.REFERENCE_EXPRESSION) as? SmkReferenceExpression) ?: return emptyList()
            val module = moduleRef.reference.resolve()
            if (module == null || module !is SmkModule) {
                return emptyList()
            }
            val file = module.getPsiFile()
            if (file != null && file is SmkFile) {
                originalNames.addAll(file.advancedCollectRules(visitedFiles).map { it.first to it.second })
            } else {
                return emptyList()
            }
        }
        return if (newName != null) {
            originalNames.map { newName.text.replace("*", it.first) to it.second }
        } else {
            originalNames.map { it.first.replace("*", it.first) to it.second }
        }
    }

    override fun getModuleReference() =
        node.findChildByType(SmkTokenTypes.SMK_FROM_KEYWORD)?.psi?.nextSibling?.nextSibling as? SmkReferenceExpression

    override fun getOverriddenRuleReferences(): List<SmkReferenceExpression>? =
        node.findChildByType(SmkElementTypes.USE_IMPORTED_RULES_NAMES)?.psi?.children?.filterIsInstance<SmkReferenceExpression>()
}