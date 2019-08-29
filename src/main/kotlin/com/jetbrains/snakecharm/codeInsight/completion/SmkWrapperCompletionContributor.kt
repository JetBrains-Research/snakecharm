package com.jetbrains.snakecharm.codeInsight.completion

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.lang.jvm.actions.constructorRequest
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ProcessingContext
import com.jetbrains.python.psi.PyStringLiteralExpression
import com.jetbrains.snakecharm.codeInsight.wrapper.WrapperStorage
import com.jetbrains.snakecharm.lang.SnakemakeNames
import com.jetbrains.snakecharm.lang.psi.SmkRuleOrCheckpointArgsSection
import java.util.regex.Pattern

class SmkWrapperCompletionContributor : CompletionContributor() {
    init {
        extend(CompletionType.BASIC, SmkWrapperCompletionProvider.WRAPPER_ARGUMENT, SmkWrapperCompletionProvider())
    }
}

class SmkWrapperCompletionProvider : CompletionProvider<CompletionParameters>() {
    companion object {
        val WRAPPER_ARGUMENT = PlatformPatterns.psiElement()
                .inFile(SmkKeywordCompletionContributor.IN_SNAKEMAKE)
                .inside(SmkRuleOrCheckpointArgsSection::class.java)
                .inside(PyStringLiteralExpression::class.java)!!

        // TODO same as in background process, can it be moved somewhere? Wrapper class perhaps?
        val tagNumberRegex = Regex("^v?(\\d*)\\.(\\d*)\\.(\\d*)/")
    }

    override fun addCompletions(
            parameters: CompletionParameters,
            context: ProcessingContext,
            result: CompletionResultSet
    ) {
        val containingSection = PsiTreeUtil.getParentOfType(
                parameters.position,
                SmkRuleOrCheckpointArgsSection::class.java
        )
        if (containingSection?.name != SnakemakeNames.SECTION_WRAPPER) {
            return
        }

        val wrappers = WrapperStorage.getInstance().getWrapperList()
        val prefix = parameters.position.text.substringBefore(result.prefixMatcher.prefix).removePrefix("\"") +
                result.prefixMatcher.prefix
        if (tagNumberRegex.containsMatchIn(prefix)) {
            result.withPrefixMatcher(WrapperPrefixMatcher(prefix.replace(tagNumberRegex, "")))
                    .addAllElements(wrappers
                            .map { it.pathToWrapperDirectory }
                            .map { LookupElementBuilder.create(it) })
        } else {
            result.withPrefixMatcher(WrapperPrefixMatcher(prefix))
                    .addAllElements(wrappers
                            .map { "${it.repositoryTag}/${it.pathToWrapperDirectory}" }
                            .map { LookupElementBuilder.create(it) })
        }
    }

    private class WrapperPrefixMatcher(prefix: String) : PrefixMatcher(prefix) {
        override fun prefixMatches(name: String) = name.contains(prefix)

        override fun cloneWithPrefix(prefix: String) = WrapperPrefixMatcher(prefix)
    }
}