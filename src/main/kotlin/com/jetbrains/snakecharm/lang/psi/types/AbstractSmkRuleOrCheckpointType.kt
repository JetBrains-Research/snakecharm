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
import com.jetbrains.snakecharm.lang.SnakemakeLanguageDialect
import com.jetbrains.snakecharm.lang.SnakemakeNames
import com.jetbrains.snakecharm.lang.psi.SMKRule
import com.jetbrains.snakecharm.lang.psi.SmkRuleOrCheckpoint
import com.jetbrains.snakecharm.lang.psi.SnakemakeFile


class SmkRulesType(
        containingRule: SMKRule?,
        smkFile: SnakemakeFile
) : AbstractSmkRuleOrCheckpointType<SMKRule>(
        containingRule, smkFile.collectRules(), SnakemakeNames.SMK_VARS_RULES
)

abstract class AbstractSmkRuleOrCheckpointType<T: SmkRuleOrCheckpoint>(
        private val containingRule: T?,
        private val nameAndDeclarationElement: List<Pair<String, T>>,
        private val typeName: String
) : PyType {

    override fun getName() = typeName

    override fun getCompletionVariants(
            completionPrefix: String?,
            location: PsiElement,
            context: ProcessingContext?
    ): Array<Any> {
        if (!SnakemakeLanguageDialect.isInsideSmkFile(location)) {
            return emptyArray()
        }

        return nameAndDeclarationElement
                .filter { (_, elem) -> elem != containingRule }
                .map { (name, elem) ->
                    PrioritizedLookupElement.withPriority(
                            LookupElementBuilder
                                    .createWithSmartPointer(name, elem)
                                    .withTypeText(elem.containingFile.name)
                                    .withIcon(elem.getIcon(0))
                            ,
                            PythonCompletionWeigher.WEIGHT_DELTA.toDouble()
                    )
                }.toTypedArray()
    }

    override fun assertValid(message: String?) {
        // [romeo] Not sure is our type always valid or check whether any element is invalid

        val invalidItem = nameAndDeclarationElement.firstOrNull { (_, elem) -> !elem.isValid }?.second
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

        val elementsWithSameName = nameAndDeclarationElement.filter { (ruleName, _) ->
            ruleName == name
        }
        if (elementsWithSameName.isEmpty()) {
            return emptyList()
        }

        return elementsWithSameName.map { (_, element) ->
            RatedResolveResult(RatedResolveResult.RATE_NORMAL, element)
        }
    }

    override fun isBuiltin() = false
}