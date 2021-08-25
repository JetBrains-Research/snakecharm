package com.jetbrains.snakecharm.lang.psi.types

import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiInvalidElementAccessException
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.parentOfType
import com.intellij.util.ProcessingContext
import com.jetbrains.python.psi.AccessDirection
import com.jetbrains.python.psi.PyExpression
import com.jetbrains.python.psi.PyKeywordArgument
import com.jetbrains.python.psi.resolve.PyResolveContext
import com.jetbrains.python.psi.resolve.RatedResolveResult
import com.jetbrains.python.psi.types.PyStructuralType
import com.jetbrains.snakecharm.SnakemakeBundle
import com.jetbrains.snakecharm.codeInsight.completion.SmkCompletionUtil
import com.jetbrains.snakecharm.codeInsight.resolve.SmkResolveUtil
import com.jetbrains.snakecharm.lang.SnakemakeNames
import com.jetbrains.snakecharm.lang.psi.*
import com.jetbrains.snakecharm.lang.psi.impl.SmkPsiUtil

class SmkWildcardsType(private val ruleOrCheckpoint: SmkRuleOrCheckpoint) : PyStructuralType(
    emptySet(), false
), SmkAvailableForSubscriptionType {

    private val typeName = "Rule ${ruleOrCheckpoint.name?.let { "'$it' " } ?: ""}wildcards"

    /**
     * Null if failed to parse wildcards declarations
     */
    private val wildcardsDeclarations: List<WildcardDescriptor>? by lazy {
        if (!ruleOrCheckpoint.isValid) {
            return@lazy null
        }
//        val collector = SmkWildcardsCollector(
//            visitDefiningSections = true,
//            // do not affect completion / resolve if output section cannot be parsed
//            visitExpandingSections = true
//        )
//        ruleOrCheckpoint.accept(collector)
//
//        collector.getWildcards()
//            ?.asSequence()
//            ?.sortedBy { it.definingSectionRate }
//            ?.distinctBy { it.text }
//            ?.toList()
        val collector = AdvanceWildcardsCollector(
            visitDefiningSections = true,
            visitExpandingSections = true,
            collectDescriptors = true,
            ruleLike = ruleOrCheckpoint,
            cachedWildcardsByRule = null
        )

        collector.getDefinedWildcardDescriptors()
            .asSequence()
            .sortedBy { it.definingSectionRate }
            .distinctBy { it.text }
            .toList()
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
            resolveToFileWildcardConstraintsKwarg(name) ?: resolveToRuleWildcardConstraintsKwarg(name)
            ?: resolveToFirstDeclaration(name)

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
    override fun getPositionArgsPreviews(location: PsiElement): List<String?> = emptyList()

    private fun resolveToRuleWildcardConstraintsKwarg(name: String): PyKeywordArgument? {
        return if (ruleOrCheckpoint !is SmkUse) {
            getRuleWildcardConstraintsSection(ruleOrCheckpoint)?.argumentList?.getKeywordArgument(name)
        } else {
            getUseWildcardsConstraintsSection(ruleOrCheckpoint, name)
        }
    }

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

    private fun getRuleWildcardConstraintsSection(ruleLikeElement: SmkRuleOrCheckpoint) =
        ruleLikeElement.getSections()
            .asSequence()
            .filterIsInstance<SmkRuleOrCheckpointArgsSection>()
            .find { it.sectionKeyword == SnakemakeNames.SECTION_WILDCARD_CONSTRAINTS }

    private fun getUseWildcardsConstraintsSection(use: SmkUse, name: String) =
        use.getImportedRuleNames()
            ?.asSequence()
            ?.mapNotNull { getWildcardsConstraintsSectionFromSectionReference(it, name) }
            ?.firstOrNull()

    private fun getWildcardsConstraintsSectionFromSectionReference(
        reference: SmkReferenceExpression,
        name: String
    ): PyKeywordArgument? {
        var resolveResult = reference.reference.resolve()
        while (resolveResult is SmkReferenceExpression) {
            val referenceContainer = resolveResult.parentOfType<SmkUse>()
            if (referenceContainer != null) {
                val argsSection =
                    getRuleWildcardConstraintsSection(referenceContainer)?.argumentList?.getKeywordArgument(name)
                if (argsSection != null) {
                    return argsSection
                }
            }
            resolveResult = resolveResult.reference.resolve()
        }

        if (resolveResult !is SmkRuleOrCheckpoint) {
            resolveResult = resolveResult?.parentOfType<SmkRuleOrCheckpoint>() ?: return null
        }

        if (resolveResult is SmkUse) {
            val argsSection = resolveResult.getImportedRuleNames()?.lastOrNull()
                ?.let { getWildcardsConstraintsSectionFromSectionReference(it, name) }
            if (argsSection != null) {
                return argsSection
            }
        }

        return getRuleWildcardConstraintsSection(resolveResult as SmkRuleOrCheckpoint)?.argumentList?.getKeywordArgument(
            name
        )
    }

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

    val wildcards: Set<String>? by lazy {
        wildcardsDeclarations?.map { it.text }?.toHashSet()
    }

    override fun getAttributeNames(): Set<String> = wildcards ?: emptySet()

    override fun getCompletionVariantsAndPriority(
        completionPrefix: String?, location: PsiElement, context: ProcessingContext?
    ) = emptyList<LookupElementBuilder>() to 0.0

    override fun isBuiltin() = false
}