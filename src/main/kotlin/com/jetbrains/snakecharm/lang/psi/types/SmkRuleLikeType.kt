package com.jetbrains.snakecharm.lang.psi.types

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiInvalidElementAccessException
import com.intellij.util.ProcessingContext
import com.jetbrains.python.psi.AccessDirection
import com.jetbrains.python.psi.PyExpression
import com.jetbrains.python.psi.resolve.PyResolveContext
import com.jetbrains.python.psi.resolve.RatedResolveResult
import com.jetbrains.python.psi.types.PyType
import com.jetbrains.snakecharm.codeInsight.completion.SmkCompletionUtil
import com.jetbrains.snakecharm.lang.SnakemakeNames
import com.jetbrains.snakecharm.lang.psi.SmkRuleOrCheckpoint
import com.jetbrains.snakecharm.lang.psi.impl.SmkPsiUtil

class SmkRuleLikeType(private val declaration: SmkRuleOrCheckpoint) : PyType {
    companion object {
        // Some sections in snakemake are inaccessible
        // in rules.NAME.<section>, so this set is required
        // to filter these sections for resolve and completion
        val ACCESSIBLE_SECTIONS = setOf(
                SnakemakeNames.SECTION_INPUT, SnakemakeNames.SECTION_OUTPUT, SnakemakeNames.SECTION_VERSION,
                SnakemakeNames.SECTION_WRAPPER, SnakemakeNames.SECTION_WILDCARD_CONSTRAINTS, SnakemakeNames.SECTION_MESSAGE,
                SnakemakeNames.SECTION_BENCHMARK, SnakemakeNames.SECTION_LOG, SnakemakeNames.SECTION_PARAMS,
                SnakemakeNames.SECTION_PRIORITY, SnakemakeNames.SECTION_RESOURCES
        )
    }

    override fun getName() = "Rule${declaration.name?.let { " '$it'" } ?: ""}"

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
                .map { RatedResolveResult(RatedResolveResult.RATE_NORMAL, it) }
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
            declaration.statementList.statements.filter { it.name in ACCESSIBLE_SECTIONS }

    override fun isBuiltin() = false
}
