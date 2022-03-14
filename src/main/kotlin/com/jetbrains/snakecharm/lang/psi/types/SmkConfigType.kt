package com.jetbrains.snakecharm.lang.psi.types

import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.components.service
import com.intellij.psi.PsiElement
import com.intellij.util.ProcessingContext
import com.jetbrains.python.psi.AccessDirection
import com.jetbrains.python.psi.PyExpression
import com.jetbrains.python.psi.resolve.PyResolveContext
import com.jetbrains.python.psi.resolve.RatedResolveResult
import com.jetbrains.python.psi.types.PyStructuralType
import com.jetbrains.snakecharm.codeInsight.completion.SmkCompletionUtil
import com.jetbrains.snakecharm.codeInsight.completion.yamlKeys.SmkYAMLKeysStorage
import com.jetbrains.snakecharm.lang.psi.SmkFile
import com.jetbrains.snakecharm.lang.psi.SmkRuleOrCheckpoint

class SmkConfigType(private val ruleOrCheckpoint: SmkRuleOrCheckpoint) :
    PyStructuralType(emptySet(), false),
    SmkAvailableForSubscriptionType {

    /**
     * Indexation is forbidden for top-level keys
     */
    override fun getPositionArgsPreviews(location: PsiElement): List<String?> = emptyList()

    /**
     * Indexation is forbidden for top-level keys
     */
    override fun resolveMemberByIndex(
        idx: Int,
        location: PyExpression?,
        direction: AccessDirection,
        resolveContext: PyResolveContext
    ): List<RatedResolveResult> = emptyList()

    override fun getCompletionVariants(
        completionPrefix: String?,
        location: PsiElement?,
        context: ProcessingContext?
    ) = completionVariants().toTypedArray()

    override fun getCompletionVariantsAndPriority(
        completionPrefix: String?,
        location: PsiElement,
        context: ProcessingContext?
    ) = completionVariants().map { LookupElementBuilder.create(it) } to SmkCompletionUtil.WORKFLOW_GLOBALS_PRIORITY

    private fun completionVariants(): List<String> {
        val containingFile =
            ruleOrCheckpoint.containingFile as? SmkFile ?: return emptyList()
        val storage = ruleOrCheckpoint.project.service<SmkYAMLKeysStorage>()
        return storage.getTopLevelKeysVariants(containingFile)
    }

    override fun resolveMember(
        name: String,
        location: PyExpression?,
        direction: AccessDirection,
        resolveContext: PyResolveContext
    ): MutableList<out RatedResolveResult> {
        val result = mutableListOf<RatedResolveResult>()
        val containingFile = ruleOrCheckpoint.containingFile as? SmkFile ?: return result
        val storage = ruleOrCheckpoint.project.service<SmkYAMLKeysStorage>()
        val yamlKeyValue = storage.getTopLevelYAMLKeyValueByName(containingFile, name) ?: return mutableListOf()
        result.add(RatedResolveResult(RatedResolveResult.RATE_HIGH, yamlKeyValue))
        return result
    }
}