package com.jetbrains.snakecharm.lang.psi.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.elementType
import com.jetbrains.python.PyTokenTypes
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
        val namePattern = getNameIdentifierPattern()
        if (namePattern != null) { // Returns name patter if it exits
            return namePattern.node
        }
        val originalNames = getImportedRuleNames()
        // Returns original names, we don't want to save one name because
        // index with this name probably already exists
        // so we save whole rules names if it is not just '*' wildcard
        if (originalNames != null && originalNames.isNotEmpty()) {
            return originalNames.first().parent.node
        }
        return null
    }

    override fun getProducedRulesNames(visitedFiles: MutableSet<PsiFile>): List<Pair<String, PsiElement>> {
        val identifier = super.getNameNode()
        if (identifier != null) {
            return listOf(identifier.text to identifier.psi)
        }
        val newName = getNameIdentifierPattern()
        val originalNames = mutableListOf<Pair<String, PsiElement>>()
        getImportedRuleNames()?.forEach { reference ->
            originalNames.add(reference.text to reference)
        }
        if (originalNames.isEmpty()) {
            val moduleRef = (getModuleName() as? SmkReferenceExpression) ?: return emptyList()
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

    /**
     * Returns a [PsiElement] which sets pattern of produced rule names
     */
    private fun getNameIdentifierPattern() = findChildByType(SmkElementTypes.USE_NAME_IDENTIFIER) as? PsiElement

    override fun getModuleName() =
        (findChildByType(SmkTokenTypes.SMK_FROM_KEYWORD) as? PsiElement)?.nextSibling?.nextSibling

    override fun getImportedRuleNames(): Array<SmkReferenceExpression>? =
        PsiTreeUtil.getChildrenOfType(
            findChildByType(SmkElementTypes.USE_IMPORTED_RULES_NAMES),
            SmkReferenceExpression::class.java
        )

    override fun usePatternToDefineOverriddenRules(): Boolean {
        val importedRulesPart = findChildByType(SmkElementTypes.USE_IMPORTED_RULES_NAMES) as? PsiElement ?: return false
        return (PsiTreeUtil.collectElements(importedRulesPart) { el -> el.elementType == PyTokenTypes.MULT }).isNotEmpty()
    }
}