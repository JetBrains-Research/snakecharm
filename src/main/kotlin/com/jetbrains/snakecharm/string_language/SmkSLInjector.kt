package com.jetbrains.snakecharm.string_language

import com.intellij.lang.injection.MultiHostRegistrar
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.python.codeInsight.PyInjectionUtil.InjectionResult
import com.jetbrains.python.codeInsight.PyInjectorBase
import com.jetbrains.python.psi.PyCallExpression
import com.jetbrains.python.psi.PyStringLiteralExpression
import com.jetbrains.snakecharm.codeInsight.completion.SMKImplicitPySymbolsCompletionContributor
import com.jetbrains.snakecharm.lang.SnakemakeLanguageDialect
import com.jetbrains.snakecharm.lang.SnakemakeNames
import com.jetbrains.snakecharm.lang.psi.SmkRuleOrCheckpointArgsSection
import com.jetbrains.snakecharm.lang.psi.SmkRunSection

class SmkSLInjector : PyInjectorBase() {
    companion object {
        val SECTIONS_INVALID_FOR_INJECTION = setOf(
                SnakemakeNames.SECTION_SHADOW, SnakemakeNames.SECTION_WILDCARD_CONSTRAINTS,
                SnakemakeNames.SECTION_WRAPPER, SnakemakeNames.SECTION_VERSION, SnakemakeNames.SECTION_THREADS,
                SnakemakeNames.SECTION_PRIORITY, SnakemakeNames.SECTION_SINGULARITY)
    }

    override fun getInjectedLanguage(element: PsiElement) = SmkSL

    override fun elementsToInjectIn() = listOf(PyStringLiteralExpression::class.java)

    override fun registerInjection(
            registrar: MultiHostRegistrar,
            host: PsiElement): InjectionResult =
            if (host.isValidForInjection())
                super.registerInjection(registrar,host)
            else
                InjectionResult.EMPTY

    private fun PyStringLiteralExpression.containsRBrace() =
            stringValue.contains('{')

    private fun PsiElement.isInValidCallExpression(): Boolean {
        val parentCallExpression =
                PsiTreeUtil.getParentOfType(this, PyCallExpression::class.java)
        return parentCallExpression == null ||
               parentCallExpression.firstChild.text in SMKImplicitPySymbolsCompletionContributor.FUNCTIONS_VALID_FOR_INJECTION
    }

    private fun PsiElement.isInValidArgsSection(): Boolean {
        val parentSection =
                PsiTreeUtil.getParentOfType(this, SmkRuleOrCheckpointArgsSection::class.java)
        return (parentSection != null && parentSection.name !in SECTIONS_INVALID_FOR_INJECTION) ||
                PsiTreeUtil.getParentOfType(this, SmkRunSection::class.java) != null
    }

    private fun PsiElement.isValidForInjection() =
            SnakemakeLanguageDialect.isInsideSmkFile(this) &&
            isInValidArgsSection() &&
            isInValidCallExpression() &&
            (this as PyStringLiteralExpression).containsRBrace()
}
