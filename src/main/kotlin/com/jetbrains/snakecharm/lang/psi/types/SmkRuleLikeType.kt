package com.jetbrains.snakecharm.lang.psi.types

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiInvalidElementAccessException
import com.intellij.util.ProcessingContext
import com.jetbrains.python.psi.AccessDirection
import com.jetbrains.python.psi.PyExpression
import com.jetbrains.python.psi.resolve.PyResolveContext
import com.jetbrains.python.psi.resolve.RatedResolveResult
import com.jetbrains.python.psi.types.PyType
import com.jetbrains.snakecharm.codeInsight.SnakemakeAPI.RULE_TYPE_ACCESSIBLE_SECTIONS
import com.jetbrains.snakecharm.codeInsight.completion.SmkCompletionUtil
import com.jetbrains.snakecharm.codeInsight.resolve.SmkResolveUtil
import com.jetbrains.snakecharm.lang.psi.SmkRuleOrCheckpoint
import com.jetbrains.snakecharm.lang.psi.impl.SmkPsiUtil

class SmkRuleLikeType(private val declaration: SmkRuleOrCheckpoint) : PyType {
    override fun getName() = "${declaration.sectionTokenType}${declaration.name?.let { " $it" } ?: ""}"

    override fun assertValid(message: String?) {
        if (!declaration.isValid) {
            throw PsiInvalidElementAccessException(declaration, message)
        }
    }

    override fun resolveMember(
            name: String,
            location: PyExpression?,
            direction: AccessDirection,
            resolveContext: PyResolveContext
    ): List<RatedResolveResult> {
        if (!SmkPsiUtil.isInsideSnakemakeOrSmkSLFile(location)) {
            return emptyList()
        }

        return getAccessibleStatements()
                .filter { it.name == name }
                .map { RatedResolveResult(SmkResolveUtil.RATE_NORMAL, it) }
    }

    override fun getCompletionVariants(
            completionPrefix: String?,
            location: PsiElement,
            context: ProcessingContext?
    ): Array<out Any> {
        if (!SmkPsiUtil.isInsideSnakemakeOrSmkSLFile(location)) {
            return emptyArray()
        }

        return getAccessibleStatements()
                .map { SmkCompletionUtil.createPrioritizedLookupElement(it.name!!) }
                .toTypedArray()
    }

    private fun getAccessibleStatements() =
            declaration.statementList.statements.filter { it.name in RULE_TYPE_ACCESSIBLE_SECTIONS }

    override fun isBuiltin() = false
}
