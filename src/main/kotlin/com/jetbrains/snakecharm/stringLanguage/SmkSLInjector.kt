package com.jetbrains.snakecharm.stringLanguage

import com.intellij.lang.injection.MultiHostRegistrar
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.python.codeInsight.PyInjectionUtil.InjectionResult
import com.jetbrains.python.codeInsight.PyInjectorBase
import com.jetbrains.python.psi.PyCallExpression
import com.jetbrains.python.psi.PyFormattedStringElement
import com.jetbrains.python.psi.PyLambdaExpression
import com.jetbrains.python.psi.PyStringLiteralExpression
import com.jetbrains.snakecharm.codeInsight.completion.SMKImplicitPySymbolsCompletionContributor.Companion.FUNCTIONS_VALID_FOR_INJECTION
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
            host: PsiElement
    ): InjectionResult = when {
        host.isValidForInjection() -> super.registerInjection(registrar, host)
        else -> InjectionResult.EMPTY
    }

    private fun PyStringLiteralExpression.containsLBrace(): Boolean {
        stringElements.forEach {
            if ((it is PyFormattedStringElement && it.content.contains("{{") ) ||
                (it !is PyFormattedStringElement && it.content.contains("{"))) {
                return true
            }
        }

        return false
    }

    private fun PsiElement.isInValidCallExpression(): Boolean {
        val parentCallExpr = PsiTreeUtil.getParentOfType(this, PyCallExpression::class.java)
        return parentCallExpr == null || parentCallExpr.firstChild.text in FUNCTIONS_VALID_FOR_INJECTION
    }

    private fun PsiElement.isInValidArgsSection(): Boolean {
        val parentSection = PsiTreeUtil.getParentOfType(this, SmkRuleOrCheckpointArgsSection::class.java)

        return (parentSection != null && parentSection.name !in SECTIONS_INVALID_FOR_INJECTION) ||
                PsiTreeUtil.getParentOfType(this, SmkRunSection::class.java) != null
    }

    private fun PsiElement.isValidForInjection() =
            SnakemakeLanguageDialect.isInsideSmkFile(this) &&
            isInValidArgsSection() &&
            isInValidCallExpression() &&
            PsiTreeUtil.getParentOfType(this, PyLambdaExpression::class.java) == null &&
            (this as PyStringLiteralExpression).containsLBrace()
}
