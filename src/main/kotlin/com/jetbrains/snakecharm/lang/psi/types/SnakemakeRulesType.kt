package com.jetbrains.snakecharm.lang.psi.types

import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.psi.PsiElement
import com.intellij.util.ArrayUtil
import com.intellij.util.ProcessingContext
import com.jetbrains.python.psi.AccessDirection
import com.jetbrains.python.psi.PyExpression
import com.jetbrains.python.psi.resolve.PyResolveContext
import com.jetbrains.python.psi.resolve.RatedResolveResult
import com.jetbrains.python.psi.types.PyType
import com.jetbrains.snakecharm.lang.SnakemakeLanguageDialect
import com.jetbrains.snakecharm.lang.psi.SnakemakeFile
import java.util.*

class SnakemakeRulesType : PyType {
    override fun getName() = "rules"

    override fun getCompletionVariants(
            completionPrefix: String?,
            location: PsiElement?,
            context: ProcessingContext?
    ): Array<Any> {
        if (!checkLanguage(location)) {
            return emptyArray()
        }

        val rules = getRules(location)
        val result = ArrayList<Any>()
        rules.forEach { result.add(LookupElementBuilder.create(it.first).withTypeText(it.second.containingFile.name)) }

        return ArrayUtil.toObjectArray(result)
    }

    override fun assertValid(message: String?) { }

    override fun resolveMember(
            name: String,
            location: PyExpression?,
            direction: AccessDirection,
            resolveContext: PyResolveContext
    ): MutableList<out RatedResolveResult> {
        if (!checkLanguage(location)) {
            return mutableListOf()
        }

        val rules = getRules(location)
        val namedRule = rules.find { (ruleName, _) -> ruleName == name }
        return if (namedRule != null) {
            return mutableListOf(RatedResolveResult(RatedResolveResult.RATE_NORMAL, namedRule.second))
        } else {
            mutableListOf()
        }

    }

    override fun isBuiltin() = false

    private fun checkLanguage(element: PsiElement?): Boolean =
            element?.containingFile?.language == SnakemakeLanguageDialect

    private fun getRules(element: PsiElement?) =
            (element?.containingFile as? SnakemakeFile)?.collectRules() ?: emptyList()

}