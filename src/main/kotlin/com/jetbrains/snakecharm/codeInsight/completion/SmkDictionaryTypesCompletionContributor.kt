package com.jetbrains.snakecharm.codeInsight.completion

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ProcessingContext
import com.jetbrains.python.PyTokenTypes
import com.jetbrains.python.psi.PySubscriptionExpression
import com.jetbrains.python.psi.types.TypeEvalContext
import com.jetbrains.snakecharm.lang.psi.SmkRunSection
import com.jetbrains.snakecharm.lang.psi.impl.SmkPsiUtil
import com.jetbrains.snakecharm.lang.psi.types.SmkAvailableForSubscriptionType

class SmkDictionaryTypesCompletionContributor : CompletionContributor() {
    init {
        extend(
                CompletionType.BASIC,
                psiElement()
                        .inFile(SmkKeywordCompletionContributor.IN_SNAKEMAKE)
                        .and(psiElement().inside(PySubscriptionExpression::class.java))
                        .and(psiElement().inside(SmkRunSection::class.java)),
                SmkDictionaryTypesCompletionProvider
        )
    }

    override fun beforeCompletion(context: CompletionInitializationContext) {
        if (!SmkPsiUtil.isInsideSnakemakeOrSmkSLFile(context.file)) {
            return
        }

        val prevPos = context.startOffset - 1
        if (prevPos >= 0) {
            val prevPosition = context.file.findElementAt(prevPos)
            if (prevPosition != null && prevPosition.node.elementType in PyTokenTypes.STRING_NODES) {
                // prev pos is string => use defaults
                return
            }
        }

        val position = context.file.findElementAt(context.startOffset) ?: return

        val subscriptionExpression = PsiTreeUtil.getParentOfType(position, PySubscriptionExpression::class.java)
        if (subscriptionExpression != null) {
            val endPost = subscriptionExpression.textOffset + subscriptionExpression.textLength - 1
            if (endPost >= 0) {
                val pos = context.file.findElementAt(endPost)
                if (pos?.node?.elementType == PyTokenTypes.RBRACKET) {
                    context.offsetMap.addOffset(CompletionInitializationContext.IDENTIFIER_END_OFFSET, endPost)
                }
            }
        }
    }
}

object SmkDictionaryTypesCompletionProvider: CompletionProvider<CompletionParameters>() {
    override fun addCompletions(
            parameters: CompletionParameters,
            context: ProcessingContext,
            result: CompletionResultSet) {

        val original = parameters.originalPosition ?: return
        val offset = parameters.offset

        // subscription is like: "input[key]"
        val subscription = PsiTreeUtil.getParentOfType(original, PySubscriptionExpression::class.java) ?: return
        val operand = subscription.operand

        val type = TypeEvalContext.codeCompletion(original.project, original.containingFile).getType(operand)
        if (type is SmkAvailableForSubscriptionType) {
            val prefix = original.containingFile.text.substring(original.textOffset, parameters.offset)
            // here need position with dummy identifier to differ [<carret>'ff'] from ['<carret>ff']
            val variants = type.getCompletionVariants(prefix, parameters.position, context)
            variants.asSequence().filterIsInstance<LookupElement>().forEach {
                result.addElement(it)
            }
        }
//        val resolvedElement = PyResolveUtil.fullResolveLocally(operand as PyReferenceExpression)
    }
}


