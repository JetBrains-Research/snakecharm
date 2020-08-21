package com.jetbrains.snakecharm.lang

import com.intellij.lang.injection.MultiHostRegistrar
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.python.codeInsight.PyInjectionUtil
import com.jetbrains.python.codeInsight.PyInjectorBase
import com.jetbrains.python.codeInsight.regexp.PythonRegexpLanguage
import com.jetbrains.python.psi.PyStringLiteralExpression
import com.jetbrains.python.psi.impl.PyKeywordArgumentImpl
import com.jetbrains.snakecharm.lang.SnakemakeNames.WORKFLOW_WILDCARD_CONSTRAINTS_KEYWORD
import com.jetbrains.snakecharm.lang.psi.SmkRuleOrCheckpointArgsSection
import com.jetbrains.snakecharm.lang.psi.SmkWorkflowArgsSection

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
        val parentPyKeywordArgumentImpl = PsiTreeUtil.getParentOfType(this, PyKeywordArgumentImpl::class.java)

        return (parentPyKeywordArgumentImpl != null && parentPyKeywordArgumentImpl.isInWildcardConstraintsSection())
    }

    private fun PsiElement.isInWildcardConstraintsSection(): Boolean {
        val parentSmkRuleOrCheckpointArgsSection = PsiTreeUtil.getParentOfType(this, SmkRuleOrCheckpointArgsSection::class.java)

        val parentSmkWorkflowArgsSection = PsiTreeUtil.getParentOfType(this, SmkWorkflowArgsSection::class.java)

        return parentSmkRuleOrCheckpointArgsSection?.name == WORKFLOW_WILDCARD_CONSTRAINTS_KEYWORD ||
                parentSmkWorkflowArgsSection?.firstChild?.text == WORKFLOW_WILDCARD_CONSTRAINTS_KEYWORD
    }

    private fun PsiElement.isValidForInjection(): Boolean =
            SnakemakeLanguageDialect.isInsideSmkFile(this) &&
                    isInWildcardConstraintsSection() &&
                    isInPyKeywordArgsSection()
}
