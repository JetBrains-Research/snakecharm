package com.jetbrains.snakecharm.codeInsight.completion.wrapper

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.components.service
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.PlatformIcons
import com.intellij.util.ProcessingContext
import com.jetbrains.python.psi.PyStringLiteralExpression
import com.jetbrains.snakecharm.codeInsight.completion.SmkKeywordCompletionContributor
import com.jetbrains.snakecharm.lang.SnakemakeNames
import com.jetbrains.snakecharm.lang.psi.SmkRuleOrCheckpointArgsSection

object SmkWrapperCompletionProvider : CompletionProvider<CompletionParameters>() {

    val CAPTURE = PlatformPatterns.psiElement()
            .inFile(SmkKeywordCompletionContributor.IN_SNAKEMAKE)
            .inside(SmkRuleOrCheckpointArgsSection::class.java)
            .inside(PyStringLiteralExpression::class.java)!!
    var WRAPPERS_PATH = "${System.getProperty("user.home")}/snakemake-wrappers"

    override fun addCompletions(
            parameters: CompletionParameters,
            context: ProcessingContext,
            result: CompletionResultSet
    ) {
        if (PsiTreeUtil.getParentOfType(parameters.position, SmkRuleOrCheckpointArgsSection::class.java)?.name !=
                SnakemakeNames.SECTION_WRAPPER) {
            return
        }

        val storage = parameters.position.project.service<SmkWrapperStorage>()
        storage.wrappers.forEach { wrapper ->
            if (wrapper.path.contains(result.prefixMatcher.prefix, false)) {
                result.addElement(LookupElementBuilder.create("${storage.version}/${wrapper.path}").withIcon(PlatformIcons.PARAMETER_ICON))
            }
        }
    }
}
