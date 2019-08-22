package com.jetbrains.snakecharm.string_language.lang.psi

import com.intellij.codeInsight.completion.PrioritizedLookupElement
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.psi.PsiElement
import com.intellij.util.PlatformIcons
import com.jetbrains.python.codeInsight.completion.PythonCompletionWeigher
import com.jetbrains.python.psi.PyStatement
import com.jetbrains.python.psi.impl.references.PyQualifiedReference
import com.jetbrains.python.psi.resolve.PyResolveContext
import com.jetbrains.python.psi.resolve.RatedResolveResult
import com.jetbrains.python.psi.types.TypeEvalContext
import com.jetbrains.snakecharm.codeInsight.completion.SmkCompletionUtil
import com.jetbrains.snakecharm.lang.SnakemakeNames
import com.jetbrains.snakecharm.lang.psi.SmkRuleOrCheckpoint

class SmkSLInitialReference(
        private val expr: SmkSLReferenceExpression,
        private val parentDeclaration: SmkRuleOrCheckpoint?,
        context: PyResolveContext
) : PyQualifiedReference(expr, context) {
    companion object {
        val ADDITIONAL_COMPLETION_VARIANTS = listOf(
                SnakemakeNames.SMK_VARS_RULES,
                SnakemakeNames.SMK_VARS_CHECKPOINTS,
                SnakemakeNames.SMK_VARS_WILDCARDS)
                .map {
                    PrioritizedLookupElement.withPriority(
                            LookupElementBuilder
                                    .create(it)
                                    .withIcon(PlatformIcons.PARAMETER_ICON),
                            SmkCompletionUtil.RULES_AND_CHECKPOINTS_PRIORITY
                    )
                }

        // In SnakemakeSL some sections are inaccessible in
        // "{<section>}". So this set is needed for filtering.
        val ACCESSIBLE_SECTIONS = setOf(
                SnakemakeNames.SECTION_INPUT, SnakemakeNames.SECTION_OUTPUT, SnakemakeNames.SECTION_VERSION,
                SnakemakeNames.SECTION_THREADS, SnakemakeNames.SECTION_LOG, SnakemakeNames.SECTION_PARAMS,
                SnakemakeNames.SECTION_RESOURCES
        )
    }

    private val variants: Array<LookupElement>

    init {
        val variantsList = mutableListOf<LookupElement>()
        variantsList.addAll(createCompletionVariantsFromDeclaration())
        variantsList.addAll(ADDITIONAL_COMPLETION_VARIANTS)
        variants = variantsList.toTypedArray()
    }

    override fun multiResolve(incompleteCode: Boolean): Array<RatedResolveResult> {
        val resolveResult = resolve() ?: return emptyArray()
        return arrayOf(RatedResolveResult(RatedResolveResult.RATE_HIGH, resolveResult))
    }

    override fun resolve(): PsiElement? {
        val referencedName = expr.referencedName
        return collectAccessibleSectionsFromDeclaration()
                .firstOrNull { it.name == referencedName }
    }

    override fun getVariants() = variants

    private fun collectAccessibleSectionsFromDeclaration(): List<PyStatement> {
        if (parentDeclaration == null) {
            return emptyList()
        }

        return parentDeclaration.statementList.statements
                .filter { it.name in ACCESSIBLE_SECTIONS }
    }

    private fun createCompletionVariantsFromDeclaration(): List<LookupElement> =
        collectAccessibleSectionsFromDeclaration()
                .filter { it.name in ACCESSIBLE_SECTIONS }
                .map {
                    PrioritizedLookupElement.withPriority(
                            LookupElementBuilder
                                    .create(it.name!!)
                                    .withIcon(PlatformIcons.PROPERTY_ICON),
                            PythonCompletionWeigher.WEIGHT_DELTA.toDouble()
                    )
                }

    override fun getUnresolvedHighlightSeverity(context: TypeEvalContext?): HighlightSeverity? = null
}
