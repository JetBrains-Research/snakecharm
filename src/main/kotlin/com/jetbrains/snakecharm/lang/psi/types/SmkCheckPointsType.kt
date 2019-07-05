package com.jetbrains.snakecharm.lang.psi.types

import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiInvalidElementAccessException
import com.intellij.util.ProcessingContext
import com.jetbrains.python.psi.AccessDirection
import com.jetbrains.python.psi.PyExpression
import com.jetbrains.python.psi.resolve.PyResolveContext
import com.jetbrains.python.psi.resolve.RatedResolveResult
import com.jetbrains.python.psi.types.PyType
import com.jetbrains.snakecharm.lang.SnakemakeLanguageDialect
import com.jetbrains.snakecharm.lang.psi.SnakemakeFile

class SmkCheckPointsType(smkFile: SnakemakeFile) : PyType {
    private val checkpointNamesAndPsiElements = smkFile.collectCheckPoints()

    override fun getName() = "checkpoints"

    override fun getCompletionVariants(
            completionPrefix: String?,
            location: PsiElement,
            context: ProcessingContext?
    ): Array<Any> {
        if (!SnakemakeLanguageDialect.isInsideSmkFile(location)) {
            return emptyArray()
        }

        return checkpointNamesAndPsiElements.map { (name, psi) ->
            LookupElementBuilder
                    .create(name)
                    .withTypeText(psi.containingFile.name)
        }.toTypedArray()
    }

    override fun assertValid(message: String?) {
        // [romeo] Not sure is our type always valid or check whether any element is invalid

        val invalidItem = checkpointNamesAndPsiElements.firstOrNull { !it.second.isValid }?.second
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
            return mutableListOf()
        }

        val namedRules = checkpointNamesAndPsiElements.filter { (checkpointName, _) -> checkpointName == name }
        if (namedRules.isEmpty()) {
            return emptyList()
        }

        return namedRules.map { (_, psi) ->
            RatedResolveResult(RatedResolveResult.RATE_NORMAL, psi.getNameNode()!!.psi)
        }
    }

    override fun isBuiltin() = false
}