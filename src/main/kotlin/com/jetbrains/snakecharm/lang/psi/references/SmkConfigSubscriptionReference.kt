package com.jetbrains.snakecharm.lang.psi.references

import com.intellij.openapi.components.service
import com.intellij.psi.PsiPolyVariantReferenceBase
import com.intellij.psi.ResolveResult
import com.jetbrains.python.psi.*
import com.jetbrains.python.psi.resolve.RatedResolveResult
import com.jetbrains.python.psi.types.TypeEvalContext
import com.jetbrains.snakecharm.codeInsight.completion.yamlKeys.SmkYAMLKeysStorage
import com.jetbrains.snakecharm.stringLanguage.lang.psi.references.SmkSLSubscriptionKeyReference

class SmkConfigSubscriptionReference(
    private val element: PyStringLiteralExpression,
    private val operand: PySubscriptionExpression
) : PsiPolyVariantReferenceBase<PyStringLiteralExpression>(element), PsiReferenceEx {

    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
        val name = element.reference?.canonicalText ?: return emptyArray()
        val storage = element.project.service<SmkYAMLKeysStorage>()
        val result = storage.resolveToOperandChildByName(operand, name) ?: return emptyArray()
        return arrayOf(RatedResolveResult(RatedResolveResult.RATE_HIGH, result))
    }

    override fun getVariants(): Array<String> {
        val storage = element.project.service<SmkYAMLKeysStorage>()
        return storage.getCompletionVariantsForOperand(operand)
    }

    override fun getUnresolvedHighlightSeverity(context: TypeEvalContext?) =
        SmkSLSubscriptionKeyReference.INSPECTION_SEVERITY

    override fun getUnresolvedDescription() = SmkSLSubscriptionKeyReference.unresolvedErrorMsg(element)
}