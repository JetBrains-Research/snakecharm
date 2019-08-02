package com.jetbrains.snakecharm.codeInsight

import com.intellij.openapi.util.TextRange
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ProcessingContext
import com.jetbrains.python.psi.PyCallExpression
import com.jetbrains.python.psi.PyStringLiteralExpression
import com.jetbrains.snakecharm.codeInsight.completion.SmkKeywordCompletionContributor
import com.jetbrains.snakecharm.lang.SnakemakeNames
import com.jetbrains.snakecharm.lang.psi.SmkRuleOrCheckpointArgsSection
import com.jetbrains.snakecharm.lang.psi.SmkRunSection
import com.jetbrains.snakecharm.lang.psi.impl.refs.SmkSectionReference
import java.util.regex.Matcher
import java.util.regex.Pattern

class SnakemakeSectionsInShellReferenceContributor : PsiReferenceContributor() {
    companion object {
        private val ALLOWED_IN_SHELL_WITH_KEYWORDS = setOf(
                SnakemakeNames.SECTION_PARAMS,
                SnakemakeNames.SECTION_INPUT,
                SnakemakeNames.SECTION_OUTPUT,
                SnakemakeNames.SECTION_RESOURCES,
                SnakemakeNames.SECTION_LOG
        )

        val ALLOWED_IN_SHELL_WITHOUT_KEYWORDS = setOf(
                SnakemakeNames.SECTION_THREADS,
                SnakemakeNames.SECTION_VERSION,
                *ALLOWED_IN_SHELL_WITH_KEYWORDS.toTypedArray()
        )
    }

    private val insideRuleSection = PlatformPatterns.psiElement()
            .inFile(SmkKeywordCompletionContributor.IN_SNAKEMAKE)
            .inside(SmkRuleOrCheckpointArgsSection::class.java)
    private val insideCallExpressionInRuleRunParameter = PlatformPatterns.psiElement()
            .inFile(SmkKeywordCompletionContributor.IN_SNAKEMAKE)
            .inside(PyCallExpression::class.java)
            .inside(SmkRunSection::class.java)

    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        registrar.registerReferenceProvider(
                PlatformPatterns
                        .psiElement(PyStringLiteralExpression::class.java)
                        .andOr(insideRuleSection, insideCallExpressionInRuleRunParameter),
                object : PsiReferenceProvider() {
                    private val sectionPattern = Pattern.compile("\\{([a-z]*)(\\.([_a-zA-Z]\\w*)?)?")

                    override fun getReferencesByElement(
                            element: PsiElement,
                            context: ProcessingContext
                    ): Array<PsiReference> {
                        val sectionReferences = mutableListOf<PsiReference>()
                        val sectionMatcher = sectionPattern.matcher(element.text)

                        if (insideRuleSection.accepts(element)) {
                            val isShellCommand = PsiTreeUtil.getParentOfType(
                                    element, SmkRuleOrCheckpointArgsSection::class.java
                            )?.sectionKeyword == SnakemakeNames.SECTION_SHELL

                            if (isShellCommand) {
                                addSectionReferences(element, sectionMatcher, sectionReferences)
                            }
                        } else {
                            val isShellCallExpression =
                                    PsiTreeUtil.getParentOfType(element, PyCallExpression::class.java)!!
                                            .callee?.name == SnakemakeNames.SECTION_SHELL
                            if (isShellCallExpression) {
                                addSectionReferences(element, sectionMatcher, sectionReferences)
                            }
                        }

                        return sectionReferences.toTypedArray()
                    }
                }
        )
    }

    private fun addSectionReferences(
            element: PsiElement,
            sectionMatcher: Matcher,
            sectionReferences: MutableList<PsiReference>
    ) {
        while (sectionMatcher.find()) {
            val sectionName = sectionMatcher.group(1)
            val argumentName = sectionMatcher.group(3)
            sectionReferences.add(SmkSectionReference(
                    element as PyStringLiteralExpression,
                    TextRange(sectionMatcher.start(1), sectionMatcher.end(1)),
                    if (sectionName.isEmpty()) null else sectionName
            ))

            if (argumentName != null) {
                sectionReferences.add(SmkSectionReference(
                        element,
                        TextRange(sectionMatcher.start(3), sectionMatcher.end(3)),
                        sectionName
                ))
            }
        }

    }
}