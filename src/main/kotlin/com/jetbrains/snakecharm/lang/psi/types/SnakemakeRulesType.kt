package com.jetbrains.snakecharm.lang.psi.types

import com.intellij.codeInsight.completion.PrioritizedLookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiInvalidElementAccessException
import com.intellij.util.ProcessingContext
import com.jetbrains.python.codeInsight.completion.PythonCompletionWeigher
import com.jetbrains.python.psi.AccessDirection
import com.jetbrains.python.psi.PyExpression
import com.jetbrains.python.psi.resolve.PyResolveContext
import com.jetbrains.python.psi.resolve.RatedResolveResult
import com.jetbrains.python.psi.types.PyType
import com.jetbrains.snakecharm.SnakemakeIcons
import com.jetbrains.snakecharm.lang.SnakemakeLanguageDialect
import com.jetbrains.snakecharm.lang.psi.SnakemakeFile

class SmkRulesType(smkFile: SnakemakeFile) : PyType {
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
            PrioritizedLookupElement.withPriority(
                    LookupElementBuilder
                            .createWithSmartPointer(name, psi)
                            .withTypeText(psi.containingFile.name)
                            .withIcon(SnakemakeIcons.FILE)
                    ,
                    PythonCompletionWeigher.WEIGHT_DELTA.toDouble()
            )
        }.toTypedArray()
    }

    override fun assertValid(message: String?) {
        // [romeo] Not sure is our type always valid or check whether any element is invalid

        val invalidItem = ruleNamesAndPsiElements.firstOrNull { !it.second.isValid }?.second
        if (invalidItem != null) {
            throw PsiInvalidElementAccessException(invalidItem, invalidItem.javaClass.toString() + ": " + message)
        }

    }

    override fun resolveMember(
            name: String,
            location: PyExpression?,
            direction: AccessDirection,
            resolveContext: PyResolveContext
    ): List<RatedResolveResult> {

        if (!SnakemakeLanguageDialect.isInsideSmkFile(location)) {
            return emptyList()
        }

        val namedRules = ruleNamesAndPsiElements.filter { (ruleName, _) -> ruleName == name }
        if (namedRules.isEmpty()) {
            return emptyList()
        }

        return namedRules.map { (_, ruleElement) ->
            RatedResolveResult(RatedResolveResult.RATE_NORMAL, ruleElement)
        }
    }

    override fun isBuiltin() = false
}