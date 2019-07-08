package com.jetbrains.snakecharm.codeInsight.completion

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ProcessingContext
import com.jetbrains.snakecharm.lang.psi.SMKWorkflowLocalRulesStatement
import com.jetbrains.snakecharm.lang.psi.SMKWorkflowRuleOrderStatement
import com.jetbrains.snakecharm.lang.psi.SnakemakeFile

class SMKRuleReferenceCompletionContributor : CompletionContributor() {
    init {
        extend(CompletionType.BASIC, RuleReferenceCompletionProvider.CAPTURE, RuleReferenceCompletionProvider)
    }
}

object RuleReferenceCompletionProvider : CompletionProvider<CompletionParameters>() {
    private val IN_LOCALRULES = PlatformPatterns.psiElement().inside(SMKWorkflowLocalRulesStatement::class.java)
    private val IN_RULEORDER = PlatformPatterns.psiElement().inside(SMKWorkflowRuleOrderStatement::class.java)

    val CAPTURE = PlatformPatterns.psiElement().andOr(IN_LOCALRULES, IN_RULEORDER)

    override fun addCompletions(
            parameters: CompletionParameters,
            context: ProcessingContext,
            result: CompletionResultSet
    ) {
        val element = parameters.position
        val rules = PsiTreeUtil.getParentOfType(element, SnakemakeFile::class.java)?.collectRules() ?: emptyList()
        val checkpoints = PsiTreeUtil.getParentOfType(element, SnakemakeFile::class.java)
                ?.collectCheckPoints()
                ?: emptyList()
        result.addAllElements(rules.map { it.first }.map { LookupElementBuilder.create(it) })
        result.addAllElements(checkpoints.map { it.first }.map { LookupElementBuilder.create(it) })
    }
}