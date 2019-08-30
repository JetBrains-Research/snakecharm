package com.jetbrains.snakecharm.lang.psi.types

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiInvalidElementAccessException
import com.intellij.util.PlatformIcons
import com.intellij.util.ProcessingContext
import com.jetbrains.python.psi.AccessDirection
import com.jetbrains.python.psi.PyExpression
import com.jetbrains.python.psi.PyKeywordArgument
import com.jetbrains.python.psi.resolve.PyResolveContext
import com.jetbrains.python.psi.resolve.RatedResolveResult
import com.jetbrains.python.psi.types.PyType
import com.jetbrains.snakecharm.codeInsight.completion.SmkCompletionUtil
import com.jetbrains.snakecharm.codeInsight.resolve.SmkResolveUtil
import com.jetbrains.snakecharm.lang.psi.SmkRuleOrCheckpointArgsSection
import com.jetbrains.snakecharm.lang.psi.impl.SmkPsiUtil

class SmkSectionType(
        val section: SmkRuleOrCheckpointArgsSection
) : PyType, SmkAvailableForSubscriptionType {

    private val typeName: String = "Section${section.sectionKeyword?.let { " '$it'" } ?: ""}"

    override fun getName() = typeName

    override fun assertValid(message: String?) {
        if (!section.isValid) {
            throw PsiInvalidElementAccessException(section, message)
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

        val keywordArgs = getKeywordArgs() ?: return emptyList()
        return keywordArgs
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

        val keywordArgs = getKeywordArgs() ?: return emptyArray()
        return keywordArgs
                .map {
                    SmkCompletionUtil.createPrioritizedLookupElement(
                        it.name!!,
                        PlatformIcons.PARAMETER_ICON
                    )
                }
                .toTypedArray()
    }

    private fun getKeywordArgs()
            = section.argumentList?.arguments?.filterIsInstance<PyKeywordArgument>()

    override fun isBuiltin() = false
}
