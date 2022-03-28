package com.jetbrains.snakecharm.lang.psi.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
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
        val namePattern = getUseNameIdentifier()
        if (namePattern != null) { // Returns name identifier if it exits
            // Example: use rule A as new_A
            // Example: use rule A, B from M as new_*
            // There are pattern instead of single node
            return namePattern.node
        }
        // There are no pattern or name node
        val originalNames = getImportedRulesNames()?.arguments()
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
        val newName = getUseNameIdentifier()
        if (newName != null && !newName.text.contains('*')){
            return listOf(newName.text to newName.originalElement)
        }
        val originalNames = mutableListOf<Pair<String, PsiElement>>()
        getImportedRulesNames()?.arguments()?.forEach { reference ->
            originalNames.add(reference.text to reference)
        }
        if (originalNames.isEmpty()) {
            val pairs = getPairsOfImportedRulesAndNames(visitedFiles) ?: return emptyList()
            originalNames.addAll(pairs)
        }
        return if (newName != null) {
            originalNames.map { newName.text.replace("*", it.first) to it.second }
        } else {
            originalNames.map { it.first.replace("*", it.first) to it.second }
        }
    }

    /**
     * Returns a [SmkUseNameIdentifier] which may sets pattern of produced rule names or may be a simple name identifier
     */
    private fun getUseNameIdentifier() = findChildByType(SmkElementTypes.USE_NAME_IDENTIFIER) as? SmkUseNameIdentifier

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

    override fun getImportedRulesNames() = findChildByType<SmkImportedRulesNames>(USE_IMPORTED_RULES_NAMES)

    override fun getImportedRulesAndResolveThem(): List<SmkRuleOrCheckpoint>? =
        getImportedRulesNames()?.resolveArguments()
            ?: getPairsOfImportedRulesAndNames(mutableSetOf())?.map { it.second }

    override fun nameIdentifierIsWildcard() = (nameIdentifier as? SmkUseNameIdentifier)?.isWildcard() ?: false
}