package com.jetbrains.snakecharm.stringLanguage.lang

import com.intellij.lang.injection.MultiHostRegistrar
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.python.codeInsight.PyInjectionUtil.InjectionResult
import com.jetbrains.python.codeInsight.PyInjectorBase
import com.jetbrains.python.psi.*
import com.jetbrains.snakecharm.codeInsight.SnakemakeAPI.FUNCTIONS_ALLOWING_SMKSL_INJECTION
import com.jetbrains.snakecharm.codeInsight.SnakemakeAPI.SECTIONS_INVALID_FOR_INJECTION
import com.jetbrains.snakecharm.codeInsight.SnakemakeAPI.SMK_FUN_EXPAND
import com.jetbrains.snakecharm.lang.SnakemakeLanguageDialect
import com.jetbrains.snakecharm.lang.psi.SmkRuleOrCheckpointArgsSection
import com.jetbrains.snakecharm.lang.psi.SmkRunSection
import com.jetbrains.snakecharm.stringLanguage.SmkSLanguage

open class SmkSLInjector : PyInjectorBase() {
    override fun getInjectedLanguage(element: PsiElement) = SmkSLanguage

    override fun elementsToInjectIn() = listOf(PyStringLiteralExpression::class.java)

    override fun registerInjection(
            registrar: MultiHostRegistrar,
            host: PsiElement
    ): InjectionResult = when {
        host.isValidForInjection() -> super.registerInjection(registrar, host)
        else -> InjectionResult.EMPTY
    }

    private fun PyStringLiteralExpression.containsLBraceOrIsEmpty(): Boolean {
        var allIsEmpty = true
        @Suppress("UnstableApiUsage")
        stringElements.forEach {
            val content = it.content
            allIsEmpty = allIsEmpty && content.isEmpty()
            if ((it is PyFormattedStringElement && content.contains("{{")) ||
                    (it !is PyFormattedStringElement && it.content.contains("{"))) {
                return true
            }
        }

        // to enable braces handler, otherwise doesn't work for first brace
        return allIsEmpty
    }

    private fun PsiElement.isInValidCallExpression(): Boolean {
        val parentCallExpr = PsiTreeUtil.getParentOfType(this, PyCallExpression::class.java)
        return parentCallExpr == null || parentCallExpr.callSimpleName() in FUNCTIONS_ALLOWING_SMKSL_INJECTION
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
                    (isInExpandCallExpression(this) ||
                            PsiTreeUtil.getParentOfType(
                            this, PyLambdaExpression::class.java) == null
                            ) &&
                    (this as PyStringLiteralExpression).containsLBraceOrIsEmpty()
}

fun isInExpandCallExpression(element: PsiElement): Boolean {
    val parentCallExpr = PsiTreeUtil.getParentOfType(element, PyCallExpression::class.java)
    return parentCallExpr?.callSimpleName() == SMK_FUN_EXPAND
}

fun PyCallExpression.callSimpleName() = this.callee.let { expression ->
    @Suppress("UnstableApiUsage")
    when (expression) {
        is PyReferenceExpression -> expression.referencedName
        else -> null
    }
}
