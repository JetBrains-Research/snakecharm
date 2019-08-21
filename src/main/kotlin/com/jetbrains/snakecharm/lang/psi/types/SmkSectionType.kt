package com.jetbrains.snakecharm.lang.psi.types

import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.psi.PsiElement
import com.intellij.util.PlatformIcons
import com.intellij.util.ProcessingContext
import com.jetbrains.python.psi.AccessDirection
import com.jetbrains.python.psi.PyExpression
import com.jetbrains.python.psi.PyKeywordArgument
import com.jetbrains.python.psi.resolve.PyResolveContext
import com.jetbrains.python.psi.resolve.RatedResolveResult
import com.jetbrains.python.psi.types.PyType
import com.jetbrains.snakecharm.lang.SnakemakeNames
import com.jetbrains.snakecharm.lang.psi.SmkRuleOrCheckpointArgsSection
import com.jetbrains.snakecharm.lang.psi.impl.SmkPsiUtil

class SmkSectionType(
        val section: SmkRuleOrCheckpointArgsSection
) : PyType, SmkAvailableForSubscriptionType {
    override fun getName() = "section"

    override fun assertValid(message: String?) {
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

        val keywordArgs = getKeywordArgs() ?: return emptyList()
        return keywordArgs
                .filter { it.name == name }
                .map {
                    RatedResolveResult(RatedResolveResult.RATE_NORMAL, it)
                }
    }

    override fun getCompletionVariants(
            completionPrefix: String?,
            location: PsiElement,
            context: ProcessingContext?
    ): Array<out Any> {
        if (!SmkPsiUtil.isInsideSnakemakeOrSmkSLFile(location)) {
            return emptyArray()
        }

        val keywordArgs = getKeywordArgs() ?: return emptyArray()
        return keywordArgs
                .map {
                    LookupElementBuilder
                            .create(it.name!!)
                            .withIcon(PlatformIcons.PARAMETER_ICON)
                }
                .toTypedArray()
    }

    private fun getKeywordArgs()
            = section.argumentList?.arguments?.filterIsInstance<PyKeywordArgument>()

    override fun isBuiltin() = false
}
