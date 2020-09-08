package com.jetbrains.snakecharm.codeInsight.completion

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementDecorator
import com.intellij.codeInsight.lookup.LookupElementPresentation
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.PlatformIcons
import com.intellij.util.ProcessingContext
import com.jetbrains.python.PyTokenTypes
import com.jetbrains.python.psi.PySubscriptionExpression
import com.jetbrains.python.psi.types.TypeEvalContext
import com.jetbrains.snakecharm.lang.SnakemakeLanguageDialect
import com.jetbrains.snakecharm.lang.psi.SmkRunSection
import com.jetbrains.snakecharm.lang.psi.types.SmkAvailableForSubscriptionType
import com.jetbrains.snakecharm.stringLanguage.lang.psi.references.SmkSLSubscriptionKeyReference.Companion.indexArgTypeText
import java.util.*

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
        if (!SnakemakeLanguageDialect.isInsideSmkFile(context.file)) {
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

        // subscription is like: "input[key]"
        val subscription = PsiTreeUtil.getParentOfType(original, PySubscriptionExpression::class.java) ?: return
        val operand = subscription.operand

        // here need position with dummy identifier to differ [<carret>'ff'] from ['<carret>ff']
        val location = parameters.position
        val inStringLiteralKey = location.node.elementType in PyTokenTypes.STRING_NODES
        if (inStringLiteralKey) {
            // In this case, args section completion/resolve is provided by reference:
            // [SmkSectionNameArgInPySubscriptionLikeReference]
            return
        }

        val type = TypeEvalContext.codeCompletion(original.project, original.containingFile).getType(operand)
        if (type is SmkAvailableForSubscriptionType) {
            val prefix = original.containingFile.text.substring(original.textOffset, parameters.offset)

            val (variants, priority) = type.getCompletionVariantsAndPriority(prefix, location, context)
            variants.asSequence().forEach { originalItem ->
                val item = ReplaceLookupStringDecorator(
                        originalItem, "'${originalItem.lookupString}'"
                )
                result.addElement(SmkCompletionUtil.createPrioritizedLookupElement(item, priority))
            }

            val typeText = indexArgTypeText(type)
            (0 until type.getPositionArgsNumber(location)).forEach { idx ->
                val item = SmkCompletionUtil.createPrioritizedLookupElement(
                        idx.toString(), null,
                        PlatformIcons.PARAMETER_ICON,
                        priority = SmkCompletionUtil.SUBSCRIPTION_INDEXES_PRIORITY,
                        typeText = typeText
                )
                result.addElement(item)
            }

        }
//        val resolvedElement = PyResolveUtil.fullResolveLocally(operand as PyReferenceExpression)
    }
}

class ReplaceLookupStringDecorator<T: LookupElement>(
        item: T,
        private val newLookupString: String
) : LookupElementDecorator<T>(item) {
    private val allLookups: Set<String> by lazy { Collections.unmodifiableSet(setOf(newLookupString)) }

    override fun getLookupString() = newLookupString

    override fun getAllLookupStrings() = allLookups

    override fun renderElement(presentation: LookupElementPresentation) {
        delegate.renderElement(presentation)
        presentation.itemText = lookupString
    }

}
