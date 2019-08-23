package com.jetbrains.snakecharm.stringLanguage.lang.psi.references

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.psi.PsiElement
import com.intellij.util.PlatformIcons
import com.jetbrains.python.psi.PyStatement
import com.jetbrains.python.psi.impl.references.PyQualifiedReference
import com.jetbrains.python.psi.resolve.PyResolveContext
import com.jetbrains.python.psi.resolve.RatedResolveResult
import com.jetbrains.python.psi.types.TypeEvalContext
import com.jetbrains.snakecharm.codeInsight.completion.SmkCompletionUtil
import com.jetbrains.snakecharm.lang.SnakemakeNames
import com.jetbrains.snakecharm.lang.psi.SmkRuleOrCheckpoint
import com.jetbrains.snakecharm.stringLanguage.lang.psi.elementTypes.SmkSLReferenceExpressionImpl

class SmkSLInitialReference(
        private val expr: SmkSLReferenceExpressionImpl,
        private val parentDeclaration: SmkRuleOrCheckpoint?,
        context: PyResolveContext
) : PyQualifiedReference(expr, context) {
    companion object {
        val ADDITIONAL_COMPLETION_VARIANTS = listOf(
                SnakemakeNames.SMK_VARS_RULES,
                SnakemakeNames.SMK_VARS_CHECKPOINTS,
                SnakemakeNames.SMK_VARS_WILDCARDS)
                .map {
                    SmkCompletionUtil.createPrioritizedLookupElement(
                            it,
                            PlatformIcons.PARAMETER_ICON
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
                .map { SmkCompletionUtil.createPrioritizedLookupElement(it.name!!) }

    override fun getUnresolvedHighlightSeverity(context: TypeEvalContext?): HighlightSeverity? = null
}
