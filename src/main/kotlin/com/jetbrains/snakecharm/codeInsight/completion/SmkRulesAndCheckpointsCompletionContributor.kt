package com.jetbrains.snakecharm.codeInsight.completion

import com.intellij.codeInsight.completion.*
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.patterns.PlatformPatterns.psiElement
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
                SmkRulesAndCheckpointsCompletionProvider.IN_SMK_RULE_REFERENCE,
                SmkRulesAndCheckpointsCompletionProvider()
        )
    }
}

private class SmkRulesAndCheckpointsCompletionProvider : CompletionProvider<CompletionParameters>() {
    companion object {
        val IN_SMK_RULE_REFERENCE =
                psiElement()
                        .inFile(SmkKeywordCompletionContributor.IN_SNAKEMAKE)
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
    ) {
        val results = mutableListOf<Pair<String, SmkRuleOrCheckpoint>>()

        val element = parameters.position

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
            results.addAll(
                    (ruleResults + checkpointResults)
                            .filter { (it as SmkRuleOrCheckpoint).name != null }
                            .map { (it as SmkRuleOrCheckpoint).name!! to it })
        } else {
            results.addAll(
                    (parameters.originalFile as SmkFile).collectRules() +
                            (parameters.originalFile as SmkFile).collectCheckPoints()
            )
        }

        if (parameters.invocationCount <= 1) {
            val includedFiles = SmkResolveUtil.getIncludedFiles(parameters.originalFile as SmkFile)
            result.addAllElements(
                    results.filter {
                        it.second.containingFile.originalFile == parameters.originalFile ||
                                it.second.containingFile.originalFile in includedFiles
                    }.map { (name, elem) ->
                        AbstractSmkRuleOrCheckpointType.createRuleLikeLookupItem(name, elem)
                    }
            )
        } else {
            result.addAllElements(
                    results.map { (name, elem) ->
                        AbstractSmkRuleOrCheckpointType.createRuleLikeLookupItem(name, elem)
                    }
            )
        }
    }
}