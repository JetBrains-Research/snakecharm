package com.jetbrains.snakecharm.codeInsight.completion

import com.intellij.codeInsight.completion.*
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.psi.PsiElement
import com.intellij.util.ProcessingContext
import com.jetbrains.snakecharm.codeInsight.resolve.SmkResolveUtil
import com.jetbrains.snakecharm.lang.psi.*
import com.jetbrains.snakecharm.lang.psi.stubs.SmkCheckpointNameIndex
import com.jetbrains.snakecharm.lang.psi.stubs.SmkRuleNameIndex
import com.jetbrains.snakecharm.lang.psi.types.AbstractSmkRuleOrCheckpointType

class SmkRulesAndCheckpointsCompletionContributor : CompletionContributor() {
    init {
        extend(
                CompletionType.BASIC,
                SmkRuleNameReferenceCompletionProvider.IN_SMK_LOCALRULES_OR_RULEORDER_RULE_NAME_REFERENCE,
                SmkRuleNameReferenceCompletionProvider()
        )
    }
}

private class SmkRuleNameReferenceCompletionProvider : CompletionProvider<CompletionParameters>() {
    companion object {
        val IN_SMK_LOCALRULES_OR_RULEORDER_RULE_NAME_REFERENCE =
                psiElement()
                        .andOr(
                                psiElement().inside(psiElement(SmkWorkflowLocalrulesSection::class.java)),
                                psiElement().inside(psiElement(SmkWorkflowRuleorderSection::class.java))
                        )
                        .withParent(SmkReferenceExpression::class.java)!!
    }

    override fun addCompletions(
            parameters: CompletionParameters,
            context: ProcessingContext,
            result: CompletionResultSet
    ) = addVariantsToCompletionResultSet(collectVariantsForElement(parameters.position), parameters, result)
}

// the following functions can be used to implement completion for any section/object referring to rule names

private fun collectVariantsForElement(element: PsiElement): MutableList<Pair<String, SmkRuleOrCheckpoint>> {
    val variants = mutableListOf<Pair<String, SmkRuleOrCheckpoint>>()

    val module = ModuleUtilCore.findModuleForPsiElement(element)
    if (module != null) {
        val ruleResults = mutableListOf<SmkRule>()
        val checkpointResults = mutableListOf<SmkCheckPoint>()
        AbstractSmkRuleOrCheckpointType.addVariantFromIndex(
                SmkRuleNameIndex.KEY,
                module,
                ruleResults,
                SmkRule::class.java
        )
        AbstractSmkRuleOrCheckpointType.addVariantFromIndex(
                SmkCheckpointNameIndex.KEY,
                module,
                checkpointResults,
                SmkCheckPoint::class.java
        )
        variants.addAll(
                (ruleResults + checkpointResults)
                        .filter { (it as SmkRuleOrCheckpoint).name != null }
                        .map { (it as SmkRuleOrCheckpoint).name!! to it })
    } else {
        variants.addAll(
                (element.containingFile.originalFile as SmkFile).collectRules() +
                        (element.containingFile.originalFile as SmkFile).collectCheckPoints()
        )
    }

    return variants
}

private fun addVariantsToCompletionResultSet(
        variants: MutableList<Pair<String, SmkRuleOrCheckpoint>>,
        parameters: CompletionParameters,
        result: CompletionResultSet
) {

    if (parameters.invocationCount <= 1) {
        val includedFiles = SmkResolveUtil.getIncludedFiles(parameters.originalFile as SmkFile)
        variants.retainAll {
            it.second.containingFile.originalFile == parameters.originalFile ||
                    it.second.containingFile.originalFile in includedFiles
        }
    }

    result.addAllElements(
            variants.map { (name, elem) ->
                AbstractSmkRuleOrCheckpointType.createRuleLikeLookupItem(name, elem)
            }
    )
}