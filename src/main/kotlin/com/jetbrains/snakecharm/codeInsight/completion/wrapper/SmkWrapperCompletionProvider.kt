package com.jetbrains.snakecharm.codeInsight.completion.wrapper

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.PlatformIcons
import com.intellij.util.ProcessingContext
import com.jetbrains.python.psi.PyStringLiteralExpression
import com.jetbrains.snakecharm.codeInsight.completion.SmkKeywordCompletionContributor
import com.jetbrains.snakecharm.lang.SnakemakeNames
import com.jetbrains.snakecharm.lang.psi.SmkRuleOrCheckpointArgsSection

object SmkWrapperCompletionProvider : CompletionProvider<CompletionParameters>() {
    private val WRAPPER_VERSION_REGEXP = Regex("(\\d+\\.\\d+\\.\\d+|master|latest).*")

    val CAPTURE = PlatformPatterns.psiElement()
            .inFile(SmkKeywordCompletionContributor.IN_SNAKEMAKE)
            .inside(SmkRuleOrCheckpointArgsSection::class.java)
            .inside(PyStringLiteralExpression::class.java)!!

    override fun addCompletions(
            parameters: CompletionParameters,
            context: ProcessingContext,
            result: CompletionResultSet
    ) {
        val parentSection = PsiTreeUtil.getParentOfType(
            parameters.position,
            SmkRuleOrCheckpointArgsSection::class.java
        )

        if (SnakemakeNames.SECTION_WRAPPER != parentSection?.sectionKeyword) {
            return
        }

        val storage = parameters.position.project.getService(SmkWrapperStorage::class.java)
        val version: String
        val prefix: String
        var doNotFilterByPrefix: Boolean = false

        if (WRAPPER_VERSION_REGEXP.matches(result.prefixMatcher.prefix)) {
            version = result.prefixMatcher.prefix.substringBefore('/')
            prefix = result.prefixMatcher.prefix.substringAfter('/', "")
        } else {
            version = storage.version
            val rawPrefix = result.prefixMatcher.prefix
            when {
                rawPrefix.startsWith("${version}/") -> {
                    prefix = rawPrefix.substring(version.length + 1)
                }
                else -> {
                    doNotFilterByPrefix = version.startsWith(rawPrefix)
                    prefix = rawPrefix.substringAfterLast('/')
                }
            }
        }

        storage.wrappers.forEach { wrapper ->
            if (doNotFilterByPrefix || wrapper.path.contains(prefix, false)) {
                result.addElement(
                    LookupElementBuilder
                        .create("$version/${wrapper.path}")
                        .withIcon(PlatformIcons.PARAMETER_ICON)
                )
            }
        }
    }
}
