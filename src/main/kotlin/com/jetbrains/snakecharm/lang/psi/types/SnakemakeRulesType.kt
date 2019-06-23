package com.jetbrains.snakecharm.lang.psi.types

import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.psi.PsiElement
import com.intellij.util.ProcessingContext
import com.jetbrains.python.psi.AccessDirection
import com.jetbrains.python.psi.PyExpression
import com.jetbrains.python.psi.resolve.PyResolveContext
import com.jetbrains.python.psi.resolve.RatedResolveResult
import com.jetbrains.python.psi.types.PyType
import com.jetbrains.snakecharm.lang.SnakemakeLanguageDialect
import com.jetbrains.snakecharm.lang.psi.SnakemakeFile

class SnakemakeRulesType(smkFile: SnakemakeFile) : PyType {
    private val ruleNamesAndPsiElements = smkFile.collectRules()

    override fun getName() = "rules"

    override fun getCompletionVariants(
            completionPrefix: String?,
            location: PsiElement,
            context: ProcessingContext?
    ): Array<Any> {
        if (!SnakemakeLanguageDialect.isInsideSmkFile(location)) {
            return emptyArray()
        }

        return ruleNamesAndPsiElements.map { (name, psi) ->
            LookupElementBuilder
                    .create(name)
                    .withTypeText(psi.containingFile.name)
        }.toTypedArray()
    }

    override fun assertValid(message: String?) { }

    override fun resolveMember(
            name: String,
            location: PyExpression?,
            direction: AccessDirection,
            resolveContext: PyResolveContext
    ): List<RatedResolveResult> {

        if (!SnakemakeLanguageDialect.isInsideSmkFile(location)) {
            return mutableListOf()
        }

        val namedRules = ruleNamesAndPsiElements.filter { (ruleName, _) -> ruleName == name }
        if (namedRules.isEmpty()) {
            return emptyList()
        }

        return namedRules.map { (_, psi) ->
            RatedResolveResult(RatedResolveResult.RATE_NORMAL, psi)
        }
    }

    override fun isBuiltin() = false
}