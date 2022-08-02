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
import com.jetbrains.snakecharm.lang.psi.elementTypes.SmkElementTypes.USE_EXCLUDE_RULES_NAMES_STATEMENT
import com.jetbrains.snakecharm.lang.psi.elementTypes.SmkElementTypes.USE_IMPORTED_RULES_NAMES
import com.jetbrains.snakecharm.lang.psi.elementTypes.SmkElementTypes.USE_NEW_NAME_PATTERN
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
        val namePattern = getNewNamePattern()
        if (namePattern != null) { // Returns name identifier if it exits
            // Example: use rule A as [new_A]
            // Example: use rule A, B from M as [new_*]
            // There are pattern instead of single node
            return namePattern.node
        }
        // There are no pattern or name node
        val originalNames = getDefinedReferencesOfImportedRuleNames()
        // Returns original names, we don't want to save one name because
        // index with this name probably already exists
        // so we save whole rules names if it is not just '*' wildcard
        if (!originalNames.isNullOrEmpty()) {
            // Example: use rule [A, B] from M
            return originalNames.first().parent.node
        }
        //Example: use rule [*] from M
        return null
    }

    override fun getProducedRulesNames(
        visitedFiles: MutableSet<PsiFile>,
    ): List<Pair<String, PsiElement>> {
        val newNamePtn = getNewNamePattern()

        if (newNamePtn != null && !newNamePtn.isWildcard()) {
            return listOf(newNamePtn.text to newNamePtn.originalElement)
        }
        val originalNames = mutableListOf<Pair<String, PsiElement>>()

        getDefinedReferencesOfImportedRuleNames()?.forEach { reference ->
            originalNames.add(reference.text to reference)
        }

        if (originalNames.isEmpty()) {
            val pairs = collectImportedRuleNameAndPsi(visitedFiles, false) ?: return emptyList()
            originalNames.addAll(pairs)
        }

        return if (newNamePtn != null) {
            val patten = newNamePtn.getValue()
            originalNames.map { (name, psi) -> patten.replace("*", name) to psi }
        } else {
            originalNames.map { (name, psi) -> name.replace("*", name) to psi }
        }
    }

    override fun getNewNamePattern() = findChildByType<SmkUseNewNamePattern>(USE_NEW_NAME_PATTERN)
    override fun getImportedNamesList() = findChildByType<SmkImportedRulesNamesList>(USE_IMPORTED_RULES_NAMES)

    override fun getExcludedRulesList() = findChildByType<SmkExcludedRulesNamesList>(USE_EXCLUDE_RULES_NAMES_STATEMENT)

    /**
     * Collects all imported rules and their names excluding ones that should be excluded
     */
    @Suppress("MoveVariableDeclarationIntoWhen")
    override fun collectImportedRuleNameAndPsi(
        visitedFiles: MutableSet<PsiFile>,
        ignoreExcludes: Boolean,
    ): List<Pair<String, SmkRuleOrCheckpoint>>? {
        val targetModule =
            ((getModuleName() as? SmkReferenceExpression)?.reference?.resolve() as? SmkModule) ?: return null

        val targetFile = targetModule.getPsiFile()
        if (targetFile !is SmkFile) {
            return null
        }

        var ruleNameAndPsiList = targetFile.collectRules(visitedFiles)

        if (!ignoreExcludes) {
            // Filter excluded rule names
            getExcludedRulesList()?.let { excludedRulesNamesList ->
                val excludedNames = excludedRulesNamesList.names()
                ruleNameAndPsiList = ruleNameAndPsiList.filter { (name, _) ->
                    name !in excludedNames
                }
            }
        }

        return ruleNameAndPsiList.map { (name, ruleLike) -> name to ruleLike }
    }


    override fun getModuleName() =
        (findChildByType(SmkTokenTypes.SMK_FROM_KEYWORD) as? PsiElement)?.nextSibling?.nextSibling

    override fun getDefinedReferencesOfImportedRuleNames(): Array<SmkReferenceExpression>? =
        PsiTreeUtil.getChildrenOfType(
            findChildByType(USE_IMPORTED_RULES_NAMES),
            SmkReferenceExpression::class.java
        )

    override fun getImportedRules(): List<SmkRuleOrCheckpoint>? {
        val targets = getDefinedReferencesOfImportedRuleNames()?.mapNotNull {
            it.reference.resolve() as? SmkRuleOrCheckpoint
        }
        return targets ?: collectImportedRuleNameAndPsi(mutableSetOf())?.map { it.second }
    }
}