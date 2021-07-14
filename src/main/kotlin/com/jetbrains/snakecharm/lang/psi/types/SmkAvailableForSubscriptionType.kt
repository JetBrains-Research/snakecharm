package com.jetbrains.snakecharm.lang.psi.types

import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.psi.PsiElement
import com.intellij.util.ProcessingContext
import com.jetbrains.python.psi.AccessDirection
import com.jetbrains.python.psi.PyExpression
import com.jetbrains.python.psi.resolve.PyResolveContext
import com.jetbrains.python.psi.resolve.RatedResolveResult
import com.jetbrains.python.psi.types.PyType


// In SnakemakeSL it is possible to write constructions
// like: "{input[param1]}". So this interface is needed to group
// together types that are accessible from subscription expression
interface SmkAvailableForSubscriptionType : PyType {
    fun getPositionArgsPreviews(location: PsiElement): List<String?>
    fun getPositionArgsNumber(location: PsiElement): Int = getPositionArgsPreviews(location).size

    fun getCompletionVariantsAndPriority(
            completionPrefix: String?,
            location: PsiElement,
            context: ProcessingContext?
    ): Pair<List<LookupElementBuilder>, Double>

    fun resolveMemberByIndex(
            idx: Int,
            location: PyExpression?,
            direction: AccessDirection,
            resolveContext: PyResolveContext
    ): List<RatedResolveResult>
}