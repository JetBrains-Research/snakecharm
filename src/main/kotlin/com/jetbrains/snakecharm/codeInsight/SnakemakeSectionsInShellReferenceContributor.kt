package com.jetbrains.snakecharm.codeInsight

import com.intellij.codeInsight.completion.CompletionInitializationContext
import com.intellij.openapi.util.TextRange
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ProcessingContext
import com.jetbrains.python.psi.PyCallExpression
import com.jetbrains.python.psi.PyStringLiteralExpression
import com.jetbrains.python.psi.PyTargetExpression
import com.jetbrains.snakecharm.codeInsight.completion.SmkKeywordCompletionContributor
import com.jetbrains.snakecharm.codeInsight.resolve.SmkResolveUtil
import com.jetbrains.snakecharm.lang.SnakemakeNames
import com.jetbrains.snakecharm.lang.psi.SmkRuleOrCheckpointArgsSection
import com.jetbrains.snakecharm.lang.psi.SmkRunSection
import com.jetbrains.snakecharm.lang.psi.impl.refs.SmkSectionReference
import com.jetbrains.snakecharm.lang.psi.impl.refs.SmkVariableReference
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
                    private val sectionPattern =
                            Pattern.compile("\\{([_a-zA-Z]\\w*)?(\\.([_a-zA-Z]\\w*)?|([^.a-z_]|\\z))")

                    override fun getReferencesByElement(
                            element: PsiElement,
                            context: ProcessingContext
                    ): Array<PsiReference> {
                        if (element !is PyStringLiteralExpression) {
                            return emptyArray()
                        }

                        val sectionReferences = mutableListOf<PsiReference>()
                        val sectionMatcher = sectionPattern.matcher(element.text)
                        val toplevelVariables = SmkResolveUtil.collectToplevelVariables(element)

                        if (insideRuleSection.accepts(element)) {
                            val isShellCommand = PsiTreeUtil.getParentOfType(
                                    element, SmkRuleOrCheckpointArgsSection::class.java
                            )?.sectionKeyword == SnakemakeNames.SECTION_SHELL

                            if (isShellCommand) {
                                addSectionReferences(element, sectionMatcher, sectionReferences, toplevelVariables)
                            }
                        } else {
                            val isShellCallExpression =
                                    PsiTreeUtil.getParentOfType(element, PyCallExpression::class.java)!!
                                            .callee?.name == SnakemakeNames.SECTION_SHELL
                            if (isShellCallExpression) {
                                addSectionReferences(element, sectionMatcher, sectionReferences, toplevelVariables)
                            }
                        }

                        return sectionReferences.toTypedArray()
                    }
                }
        )
    }

    private fun addSectionReferences(
            element: PyStringLiteralExpression,
            sectionMatcher: Matcher,
            sectionReferences: MutableList<PsiReference>,
            toplevelVariables: List<PyTargetExpression>
    ) {
        while (sectionMatcher.find()) {
            val sectionName = sectionMatcher.group(1)
                    ?.replace(CompletionInitializationContext.DUMMY_IDENTIFIER_TRIMMED, "")
            val argumentName = sectionMatcher.group(3)

            if (sectionName == null) {
                continue
            }

            val variable = toplevelVariables.find { it.name == sectionName }
            if (variable != null) {
                sectionReferences.add(SmkVariableReference(
                        element,
                        TextRange(sectionMatcher.start(1), sectionMatcher.start(1) + sectionName.length),
                        variable
                ))
            } else if (sectionName.isEmpty() || ALLOWED_IN_SHELL_WITHOUT_KEYWORDS.any { it.startsWith(sectionName) }) {
                sectionReferences.add(SmkSectionReference(
                        element,
                        TextRange(sectionMatcher.start(1), sectionMatcher.start(1) + sectionName.length),
                        if (sectionName.isEmpty()) null else sectionName
                ))
            } else {
                // unresolved reference for variable
                sectionReferences.add(SmkVariableReference(
                        element,
                        TextRange(sectionMatcher.start(1), sectionMatcher.start(1) + sectionName.length),
                        variable
                ))
            }

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