package com.jetbrains.snakecharm.lang.psi.types

import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiInvalidElementAccessException
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ProcessingContext
import com.jetbrains.python.psi.AccessDirection
import com.jetbrains.python.psi.PyExpression
import com.jetbrains.python.psi.resolve.PyResolveContext
import com.jetbrains.python.psi.resolve.RatedResolveResult
import com.jetbrains.python.psi.types.PyType
import com.jetbrains.snakecharm.SnakemakeBundle
import com.jetbrains.snakecharm.codeInsight.completion.SmkCompletionUtil
import com.jetbrains.snakecharm.codeInsight.resolve.SmkResolveUtil
import com.jetbrains.snakecharm.lang.SnakemakeNames
import com.jetbrains.snakecharm.lang.psi.*
import com.jetbrains.snakecharm.lang.psi.impl.SmkPsiUtil

class SmkWildcardsType(private val ruleOrCheckpoint: SmkRuleOrCheckpoint) : PyType, SmkAvailableForSubscriptionType {
    private val typeName = "Rule ${ruleOrCheckpoint.name?.let { "'$it' " } ?: ""}wildcards"

    /**
     * Null if failed to parse wildcards declarations
     */
    private val wildcardsDeclarations: List<WildcardDescriptor>? by lazy {
        if (!ruleOrCheckpoint.isValid) {
            return@lazy null
        }
        val collector = SmkWildcardsCollector(
                visitDefiningSections = true,
                // do not affect completion / resolve if output section cannot be parsed
                visitExpandingSections = true
        )
        ruleOrCheckpoint.accept(collector)

        collector.getWildcards()
                ?.asSequence()
                ?.sortedBy { it.definingSectionRate }
                ?.distinctBy { it.text }
                ?.toList()
    }
    
    override fun getName() = typeName

    override fun assertValid(message: String?) {
        if (!ruleOrCheckpoint.isValid) {
            throw PsiInvalidElementAccessException(ruleOrCheckpoint, message)
        }
    }

    override fun resolveMember(
            name: String,
            location: PyExpression?,
            direction: AccessDirection,
            resolveContext: PyResolveContext
    ): List<RatedResolveResult> {
        if (!SmkPsiUtil.isInsideSnakemakeOrSmkSLFile(location)) {
            return emptyList()
        }

        val resolveResult =
                resolveToFileWildcardConstraintsKwarg(name) ?:
                resolveToRuleWildcardConstraintsKwarg(name) ?:
                resolveToFirstDeclaration(name)

        // if not empty result
        if (resolveResult != null) {
            return listOf(RatedResolveResult(SmkResolveUtil.RATE_NORMAL, resolveResult))
        }

        return emptyList()
    }

    override fun resolveMemberByIndex(
            idx: Int, location: PyExpression?, direction: AccessDirection, resolveContext: PyResolveContext
    ): List<RatedResolveResult> = emptyList() // not supported

    /**
     * Access by an index not supported
     */
    override fun getPositionArgsNumber(location: PsiElement) = 0

    private fun resolveToRuleWildcardConstraintsKwarg(name: String) =
            getRuleWildcardConstraintsSection()?.argumentList?.getKeywordArgument(name)

    private fun resolveToFirstDeclaration(name: String) =
            wildcardsDeclarations?.firstOrNull { it.text == name }?.psi

    private fun resolveToFileWildcardConstraintsKwarg(name: String) = getFileWildcardConstraintsSections()
                .asSequence()
                .mapNotNull { section -> section.argumentList?.getKeywordArgument(name) }
                .firstOrNull()

    private fun getFileWildcardConstraintsSections() =
            PsiTreeUtil.findChildrenOfType(
                    ruleOrCheckpoint.containingFile,
                    SmkWorkflowArgsSection::class.java
            ).filter { 
                it.sectionKeyword == SnakemakeNames.SECTION_WILDCARD_CONSTRAINTS 
            }

    private fun getRuleWildcardConstraintsSection() =
            ruleOrCheckpoint.getSections()
                    .asSequence()
                    .filterIsInstance<SmkRuleOrCheckpointArgsSection>()
                    .find { it.sectionKeyword == SnakemakeNames.SECTION_WILDCARD_CONSTRAINTS }

    override fun getCompletionVariants(
            completionPrefix: String?,
            location: PsiElement,
            context: ProcessingContext?
    ): Array<out Any> {
        if (!SmkPsiUtil.isInsideSnakemakeOrSmkSLFile(location) || wildcardsDeclarations == null) {
            return emptyArray()
        }

        return wildcardsDeclarations!!.map {
            SmkCompletionUtil.createPrioritizedLookupElement(
                    it.text, it.psi,
                    typeText = SnakemakeBundle.message("TYPES.rule.wildcard.type.text")
            )
        }.toTypedArray()
    }

    fun getWildcards() = wildcardsDeclarations?.map { it.text }

    override fun getCompletionVariantsAndPriority(
            completionPrefix: String?, location: PsiElement, context: ProcessingContext?
    ) = emptyList<LookupElementBuilder>() to 0.0

    override fun isBuiltin() = false
}