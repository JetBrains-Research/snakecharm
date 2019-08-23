package com.jetbrains.snakecharm.lang.psi.types

import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ProcessingContext
import com.jetbrains.python.psi.AccessDirection
import com.jetbrains.python.psi.PyExpression
import com.jetbrains.python.psi.resolve.PyResolveContext
import com.jetbrains.python.psi.resolve.RatedResolveResult
import com.jetbrains.python.psi.types.PyType
import com.jetbrains.snakecharm.codeInsight.completion.SmkCompletionUtil
import com.jetbrains.snakecharm.lang.SnakemakeNames
import com.jetbrains.snakecharm.lang.psi.SmkRuleOrCheckpoint
import com.jetbrains.snakecharm.lang.psi.SmkRuleOrCheckpointArgsSection
import com.jetbrains.snakecharm.lang.psi.SmkWorkflowArgsSection
import com.jetbrains.snakecharm.stringLanguage.SmkSL

class SmkWildcardsType(private val parentDeclaration: SmkRuleOrCheckpoint) : PyType {
    private val wildcardsNamesAndPsi = parentDeclaration.collectWildcards()

    override fun getName() = "wildcards"

    override fun assertValid(message: String?) {
    }

    override fun resolveMember(
            name: String,
            location: PyExpression?,
            direction: AccessDirection,
            resolveContext: PyResolveContext
    ): List<RatedResolveResult> {
        if (!SmkSL.isInsideSmkSLFile(location)) {
            return emptyList()
        }

        val resolveResult =
                resolveToFileWildcardConstraintsKwarg(name) ?:
                resolveToRuleWildcardConstraintsKwarg(name) ?:
                resolveToFirstDeclaration(name) ?:
                        return emptyList()

        return listOf(RatedResolveResult(RatedResolveResult.RATE_NORMAL, resolveResult))
    }

    private fun resolveToRuleWildcardConstraintsKwarg(name: String) =
            getRuleWildcardConstraintsSection()?.argumentList?.getKeywordArgument(name)

    private fun resolveToFirstDeclaration(name: String) =
            wildcardsNamesAndPsi.firstOrNull { it.first == name }?.second

    private fun resolveToFileWildcardConstraintsKwarg(name: String): PsiElement? {
        val sections = getFileWildcardConstraintsSections()
        sections.forEach {
            return it.argumentList?.getKeywordArgument(name) ?: return@forEach
        }

        return null
    }

    private fun getFileWildcardConstraintsSections() =
            PsiTreeUtil.findChildrenOfType(
                    parentDeclaration.containingFile,
                    SmkWorkflowArgsSection::class.java
            ).filter { it.sectionKeyword == SnakemakeNames.SECTION_WILDCARD_CONSTRAINTS }

    private fun getRuleWildcardConstraintsSection() =
            parentDeclaration
                    .statementList
                    .statements
                    .find { it.name == SnakemakeNames.SECTION_WILDCARD_CONSTRAINTS }
                    as SmkRuleOrCheckpointArgsSection?

    override fun getCompletionVariants(
            completionPrefix: String?,
            location: PsiElement,
            context: ProcessingContext?
    ): Array<out Any> {
        if (!SmkSL.isInsideSmkSLFile(location)) {
            return emptyArray()
        }

        return wildcardsNamesAndPsi
                .map { SmkCompletionUtil.createPrioritizedLookupElement(it.first) }
                .toTypedArray()
    }

    override fun isBuiltin() = false
}