package com.jetbrains.snakecharm.lang.psi.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.python.psi.PyElementType
import com.jetbrains.python.psi.PyElementVisitor
import com.jetbrains.python.psi.types.TypeEvalContext
import com.jetbrains.snakecharm.lang.parser.SmkTokenTypes
import com.jetbrains.snakecharm.lang.psi.*
import com.jetbrains.snakecharm.lang.psi.elementTypes.SmkElementTypes
import com.jetbrains.snakecharm.lang.psi.elementTypes.SmkElementTypes.USE_IMPORTED_RULES_NAMES
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
            // Example: use rule A as new_A
            // Here we can detect name node by default
            return identifier
        }
        val namePattern = getNameIdentifierPattern()
        if (namePattern != null) { // Returns name patter if it exits
            // Example: use rule A, B from M as new_*
            // There are pattern instead of single node
            return namePattern.node
        }
        // There are no pattern or name node
        val originalNames = getDefinedReferencesOfImportedRuleNames()
        // Returns original names, we don't want to save one name because
        // index with this name probably already exists
        // so we save whole rules names if it is not just '*' wildcard
        if (originalNames != null && originalNames.isNotEmpty()) {
            // Example: use rule A, B from M
            return originalNames.first().parent.node
        }
        //Example: use rule * from M
        return null
    }

    override fun getProducedRulesNames(visitedFiles: MutableSet<PsiFile>): List<Pair<String, PsiElement>> {
        val identifier = super.getNameNode()
        if (identifier != null) {
            return listOf(identifier.text to identifier.psi)
        }
        val newName = getNameIdentifierPattern()
        val originalNames = mutableListOf<Pair<String, PsiElement>>()
        getDefinedReferencesOfImportedRuleNames()?.forEach { reference ->
            originalNames.add(reference.text to reference)
        }
        if (originalNames.isEmpty()) {
            val pairs = getPairsOfImportedRulesAndNames(visitedFiles) ?: return emptyList()
            originalNames.addAll(pairs.map { it.first to it.second })
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

    /**
     * Collects all imported rules and their names
     */
    private fun getPairsOfImportedRulesAndNames(visitedFiles: MutableSet<PsiFile>): List<Pair<String, SmkRuleOrCheckpoint>>? {
        val module = ((getModuleName() as? SmkReferenceExpression)?.reference?.resolve() as? SmkModule) ?: return null
        val file = module.getPsiFile()
        return if (file != null && file is SmkFile) {
            file.advancedCollectRules(visitedFiles).map { it.first to it.second }
        } else {
            null
        }
    }

    override fun getModuleName() =
        (findChildByType(SmkTokenTypes.SMK_FROM_KEYWORD) as? PsiElement)?.nextSibling?.nextSibling

    override fun getDefinedReferencesOfImportedRuleNames(): Array<SmkReferenceExpression>? =
        PsiTreeUtil.getChildrenOfType(
            findChildByType(USE_IMPORTED_RULES_NAMES),
            SmkReferenceExpression::class.java
        )

    override fun getImportedRules(): List<SmkRuleOrCheckpoint>? =
        getDefinedReferencesOfImportedRuleNames()?.mapNotNull { it.reference.resolve() as? SmkRuleOrCheckpoint }
            ?: getPairsOfImportedRulesAndNames(mutableSetOf())?.map { it.second }

    override fun nameIdentifierIsWildcard() = nameIdentifier?.let {
        it is SmkUseNameIdentifier && it.textContains('*')
    } ?: false
}