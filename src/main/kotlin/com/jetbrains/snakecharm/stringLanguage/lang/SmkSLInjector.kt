package com.jetbrains.snakecharm.stringLanguage.lang

import com.intellij.lang.injection.MultiHostRegistrar
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.python.codeInsight.PyInjectionUtil.InjectionResult
import com.jetbrains.python.codeInsight.PyInjectorBase
import com.jetbrains.python.psi.*
import com.jetbrains.snakecharm.codeInsight.SnakemakeApiService
import com.jetbrains.snakecharm.lang.SnakemakeLanguageDialect
import com.jetbrains.snakecharm.lang.SnakemakeNames.SMK_FUN_EXPAND
import com.jetbrains.snakecharm.lang.SnakemakeNames.SMK_FUN_EXPAND_ALIAS_COLLECT
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
        val api = SnakemakeApiService.getInstance(project)

        val parentCallExpr = PsiTreeUtil.getParentOfType(this, PyCallExpression::class.java)
        val fqn = (parentCallExpr?.callee?.reference?.resolve() as? PyQualifiedNameOwner)?.qualifiedName

        if (fqn != null) {
            // resolved
            return api.isFunctionFqnValidForInjection(fqn)
        }

        // not-resolved or else
        val shortName = parentCallExpr?.callSimpleName()
        if (shortName != null) {
            return api.isFunctionShortNameValidForInjection(shortName)
        }
        return true
    }

    private fun PsiElement.isInValidArgsSection(): Boolean {
        val parentSection = PsiTreeUtil.getParentOfType(this, SmkRuleOrCheckpointArgsSection::class.java)
        if (parentSection != null) {
            val context = parentSection.getParentRuleOrCheckPoint().sectionKeyword
            val sectionKeyword = parentSection.sectionKeyword
            if (context != null && sectionKeyword != null) {
                val apiService = SnakemakeApiService.getInstance(project)
                return apiService.isSubsectionValidForInjection(sectionKeyword, context)
            }
        }
        return PsiTreeUtil.getParentOfType(this, SmkRunSection::class.java) != null
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
    val callSimpleName = parentCallExpr?.callSimpleName()
    return (callSimpleName == SMK_FUN_EXPAND) || (callSimpleName == SMK_FUN_EXPAND_ALIAS_COLLECT)
}

fun PyCallExpression.callSimpleName() = this.callee.let { expression ->
    @Suppress("UnstableApiUsage")
    when (expression) {
        is PyReferenceExpression -> expression.referencedName
        else -> null
    }
}
