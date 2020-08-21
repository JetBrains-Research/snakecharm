package com.jetbrains.snakecharm.codeInsight.completion.wrapper

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.components.service
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ProcessingContext
import com.jetbrains.python.psi.PyArgumentList
import com.jetbrains.snakecharm.codeInsight.completion.SmkKeywordCompletionContributor
import com.jetbrains.snakecharm.lang.psi.SmkRuleOrCheckpoint
import com.jetbrains.snakecharm.lang.psi.SmkRuleOrCheckpointArgsSection

object SmkWrapperArgsCompletionProvider : CompletionProvider<CompletionParameters>() {
    val CAPTURE = PlatformPatterns.psiElement()
            .inFile(SmkKeywordCompletionContributor.IN_SNAKEMAKE)
            .inside(SmkRuleOrCheckpointArgsSection::class.java)
            .inside(PyArgumentList::class.java)!!

    override fun addCompletions(
            parameters: CompletionParameters,
            context: ProcessingContext,
            result: CompletionResultSet
    ) {
        val wrapper = PsiTreeUtil
               .getParentOfType(parameters.position, SmkRuleOrCheckpoint::class.java)
               ?.getSectionByName("wrapper") ?: return

        val storage = parameters.position.project.service<SmkWrapperStorage>().wrapperStorage.find { wrapper.argumentList!!.text.contains(it.path) } ?: return
        val name = PsiTreeUtil.getParentOfType(parameters.position, SmkRuleOrCheckpointArgsSection::class.java)?.name ?: return
        if (name in storage.args.keys) {
            storage.args[name]?.forEach {
                result.addElement(LookupElementBuilder.create(it))
            }
        }
    }
}