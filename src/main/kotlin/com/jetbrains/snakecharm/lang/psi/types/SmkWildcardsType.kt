package com.jetbrains.snakecharm.lang.psi.types

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiInvalidElementAccessException
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ProcessingContext
import com.jetbrains.python.psi.AccessDirection
import com.jetbrains.python.psi.PyExpression
import com.jetbrains.python.psi.resolve.PyResolveContext
import com.jetbrains.python.psi.resolve.RatedResolveResult
import com.jetbrains.python.psi.types.PyType
import com.jetbrains.snakecharm.codeInsight.completion.SmkCompletionUtil
import com.jetbrains.snakecharm.codeInsight.resolve.SmkResolveUtil
import com.jetbrains.snakecharm.lang.SnakemakeNames
import com.jetbrains.snakecharm.lang.psi.*
import com.jetbrains.snakecharm.lang.psi.impl.SmkPsiUtil

class SmkWildcardsType(private val ruleOrCheckpoint: SmkRuleOrCheckpoint) : PyType {
    private val typeName = "Rule ${ruleOrCheckpoint.name?.let { "'$it' " } ?: ""}wildcards"
    private val wildcardsDeclarations: List<WildcardDescriptor>?

    init {
        val collector = SmkWildcardsCollector(
                visitDefiningSections = true,
                visitExpandingSections = false
        )
        ruleOrCheckpoint.accept(collector)
        wildcardsDeclarations = collector.getWildcards()
                ?.asSequence()
                ?.filterNot { it.definingSectionIdx == WildcardDescriptor.UNDEFINED_SECTION }
                ?.sortedBy { it.definingSectionIdx }
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
                resolveToFirstDeclaration(name) ?:
                return emptyList()

        return listOf(RatedResolveResult(SmkResolveUtil.RATE_NORMAL, resolveResult))
    }

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

        return wildcardsDeclarations.map {
            SmkCompletionUtil.createPrioritizedLookupElement(it.text, typeText = "wildcard")
        }.toTypedArray()
    }

    override fun isBuiltin() = false
}