package com.jetbrains.snakecharm.lang

import com.intellij.lang.injection.MultiHostRegistrar
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.python.codeInsight.PyInjectionUtil
import com.jetbrains.python.codeInsight.PyInjectorBase
import com.jetbrains.python.codeInsight.regexp.PythonRegexpLanguage
import com.jetbrains.python.psi.PyKeywordArgument
import com.jetbrains.python.psi.PyStringLiteralExpression
import com.jetbrains.snakecharm.lang.SnakemakeNames.SECTION_WILDCARD_CONSTRAINTS
import com.jetbrains.snakecharm.lang.SnakemakeNames.WORKFLOW_WILDCARD_CONSTRAINTS_KEYWORD
import com.jetbrains.snakecharm.lang.psi.SmkArgsSection

open class SmkWildcardConstraintsRegExpInjector : PyInjectorBase() {
    override fun getInjectedLanguage(element: PsiElement) = PythonRegexpLanguage.INSTANCE!!

    override fun elementsToInjectIn() = listOf(PyStringLiteralExpression::class.java)

    override fun registerInjection(
            registrar: MultiHostRegistrar,
            host: PsiElement
    ): PyInjectionUtil.InjectionResult = when {
        host.isValidForInjection() -> super.registerInjection(registrar, host)
        else -> PyInjectionUtil.InjectionResult.EMPTY
    }

    private fun PsiElement.isInPyKeywordArgsSection(): Boolean {
        val parentPyKeywordArgumentImpl = PsiTreeUtil.getParentOfType(this, PyKeywordArgument::class.java)

        return (parentPyKeywordArgumentImpl != null && parentPyKeywordArgumentImpl.isInWildcardConstraintsSection())
    }

    private fun PsiElement.isInWildcardConstraintsSection(): Boolean {
        val keyword = PsiTreeUtil.getParentOfType(this, SmkArgsSection::class.java)?.sectionKeyword

        @Suppress("KotlinConstantConditions")
        return keyword == WORKFLOW_WILDCARD_CONSTRAINTS_KEYWORD ||
                keyword == SECTION_WILDCARD_CONSTRAINTS
    }

    private fun PsiElement.isValidForInjection(): Boolean =
            SnakemakeLanguageDialect.isInsideSmkFile(this) &&
                    isInWildcardConstraintsSection() &&
                    isInPyKeywordArgsSection()
}
