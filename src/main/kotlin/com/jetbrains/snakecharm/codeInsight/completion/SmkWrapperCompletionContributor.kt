package com.jetbrains.snakecharm.codeInsight.completion

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ProcessingContext
import com.jetbrains.python.psi.PyStringLiteralExpression
import com.jetbrains.snakecharm.codeInsight.wrapper.SmkWrapperUtil
import com.jetbrains.snakecharm.codeInsight.wrapper.WrapperStorage
import com.jetbrains.snakecharm.lang.SnakemakeNames
import com.jetbrains.snakecharm.lang.psi.SmkRuleOrCheckpointArgsSection

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
        val wrapperDependencies = wrappers.map { it to extractToolDependencies(it) }.toMap()
        val currentTag = SmkWrapperUtil.TAG_NUMBER_REGEX.find(prefix)?.value
        if (currentTag != null) {
            result.withPrefixMatcher(
                    WrapperPrefixMatcher(prefix.replace(SmkWrapperUtil.TAG_NUMBER_REGEX, ""))
            ).addAllElements(wrappers.map { LookupElementBuilder.create(it.pathToWrapperDirectory) })
            // don't add type text to completion list items because tool versions might be incorrect for earlier tags
        } else {
            result.withPrefixMatcher(WrapperPrefixMatcher(prefix))
                    .addAllElements(wrappers.map {
                                PrioritizedLookupElement.withPriority(
                                        LookupElementBuilder.create("${it.repositoryTag}/${it.pathToWrapperDirectory}")
                                                .withTypeText(wrapperDependencies[it]?.joinToString(", ")),
                                        getPriorityByTag(it.repositoryTag, wrappers))
                            })
        }
    }

    // extract not all dependencies but only those mentioned in wrapper name
    private fun extractToolDependencies(wrapper: WrapperStorage.Wrapper): List<String> {
        val wrapperToolNames = wrapper.pathToWrapperDirectory.split("/").filterNot { it == "bio" }
        val dependencies = wrapper.environmentFileContent.substringAfter("dependencies:").split("-")
        return dependencies
                // if there's no version specified then no need to list this dependency bc it's already in the name
                .filter { wrapperToolNames.any { tool -> it.contains(tool) } && it.contains("==")}
                // using this instead of it.trim() because the file format seems to be 'tool ==ver', and 'tool==ver' looks better
                .map { it.substringBefore("==").trim() + "==" + it.substringAfter("==").trim() }
    }

    private fun getPriorityByTag(tag: String, wrappers: List<WrapperStorage.Wrapper>) =
            SmkWrapperUtil.sortTags(wrappers.map { it.repositoryTag }.distinct()).indexOf(tag).toDouble()


    private class WrapperPrefixMatcher(prefix: String) : PrefixMatcher(prefix) {
        override fun prefixMatches(name: String) = name.contains(prefix)

        override fun cloneWithPrefix(prefix: String) = WrapperPrefixMatcher(prefix)
    }
}