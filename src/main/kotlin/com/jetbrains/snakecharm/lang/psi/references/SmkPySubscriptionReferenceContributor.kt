package com.jetbrains.snakecharm.lang.psi.references

import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.patterns.StandardPatterns.instanceOf
import com.intellij.patterns.StandardPatterns.or
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ProcessingContext
import com.jetbrains.python.psi.*
import com.jetbrains.python.psi.types.TypeEvalContext
import com.jetbrains.snakecharm.codeInsight.completion.SmkCompletionContributorPattern
import com.jetbrains.snakecharm.lang.psi.SmkRuleLike
import com.jetbrains.snakecharm.lang.psi.types.SmkAvailableForSubscriptionType
import com.jetbrains.snakecharm.stringLanguage.lang.callSimpleName

/**
 * Adds reference on subscription expression keys, e.g. output['arg']. Operator reference doesn't allow
 * reference providers, so cannot add referent to output[0].
 */
class SmkPySubscriptionReferenceContributor : PsiReferenceContributor() {
    private val IN_SUBSCRIPTION_PATTERN = psiElement(PyStringLiteralExpression::class.java)
        .inFile(SmkCompletionContributorPattern.IN_SNAKEMAKE)
        .inside(
            true,
            instanceOf(PySubscriptionExpression::class.java),
            or(
                instanceOf(PyReferenceExpression::class.java),
                instanceOf(PyCallExpression::class.java)
            )
        )
        .inside(instanceOf(SmkRuleLike::class.java))

    private val IN_GET_ACCESSOR_PATTERN = psiElement(PyStringLiteralExpression::class.java)
        .inFile(SmkCompletionContributorPattern.IN_SNAKEMAKE)
        .inside(instanceOf(PyArgumentList::class.java))
        .inside(instanceOf(PyCallExpression::class.java))
        .inside(instanceOf(SmkRuleLike::class.java))

    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        registrar.registerReferenceProvider(
                IN_SUBSCRIPTION_PATTERN, SmkSectionNameArgInSubscriptionReferenceProvider
        )
        registrar.registerReferenceProvider(
                IN_GET_ACCESSOR_PATTERN, SmkSectionNameArgInGetAccessorReferenceProvider
        )

    }
}

object SmkSectionNameArgInGetAccessorReferenceProvider : PsiReferenceProvider() {
    override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
        val psiString = element as PyStringLiteralExpression
        val argList = PsiTreeUtil.getParentOfType(element, PyArgumentList::class.java)
        if (argList != null) {
            val pyCall = PsiTreeUtil.getParentOfType(argList, PyCallExpression::class.java)
            if (pyCall?.callSimpleName() == "get") {
                val callee = pyCall.callee
                if (callee is PyQualifiedExpression) {
                    val qualifier = callee.qualifier
                    if (qualifier != null) {
                        val type = TypeEvalContext.codeCompletion(element.project, element.containingFile).getType(qualifier)
                        if (type is SmkAvailableForSubscriptionType) {
                            return arrayOf(
                                    SmkSectionNameArgInPySubscriptionLikeReference(psiString, type)
                            )
                        }
                    }
                }
            }
        }
        return PsiReference.EMPTY_ARRAY
    }
}
object SmkSectionNameArgInSubscriptionReferenceProvider : PsiReferenceProvider() {
    override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
        val psiString = element as PyStringLiteralExpression
        val subscriptionExpr = PsiTreeUtil.getParentOfType(
                element, PySubscriptionExpression::class.java
        )

        if (subscriptionExpr != null) {
            val operand = subscriptionExpr.operand
            val type = TypeEvalContext.codeCompletion(element.project, element.containingFile).getType(operand)
            if (type is SmkAvailableForSubscriptionType) {
                return arrayOf(
                        SmkSectionNameArgInPySubscriptionLikeReference(psiString, type)
                )
            }

        }
        return PsiReference.EMPTY_ARRAY
    }
}