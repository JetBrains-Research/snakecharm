package com.jetbrains.snakecharm.lang.psi.types

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiInvalidElementAccessException
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.PlatformIcons
import com.intellij.util.ProcessingContext
import com.jetbrains.python.PyTokenTypes
import com.jetbrains.python.psi.*
import com.jetbrains.python.psi.impl.ResolveResultList
import com.jetbrains.python.psi.resolve.PyResolveContext
import com.jetbrains.python.psi.resolve.RatedResolveResult
import com.jetbrains.python.psi.types.PyType
import com.jetbrains.snakecharm.codeInsight.SnakemakeAPI.UNPACK_FUNCTION
import com.jetbrains.snakecharm.codeInsight.completion.SmkCompletionUtil
import com.jetbrains.snakecharm.codeInsight.resolve.SmkResolveUtil
import com.jetbrains.snakecharm.lang.psi.SmkRuleOrCheckpointArgsSection
import com.jetbrains.snakecharm.lang.psi.impl.SmkPsiUtil
import com.jetbrains.snakecharm.stringLanguage.lang.callSimpleName
import com.jetbrains.snakecharm.stringLanguage.lang.psi.SmkSLElement
import com.jetbrains.snakecharm.stringLanguage.lang.psi.SmkSLSubscriptionKeyExpression

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

        val args = getSectionArgs()

        @Suppress("FoldInitializerAndIfToElvis")
        if (args == null) {
            return emptyList()
        }

        val resolveResult = ResolveResultList()
        args.filterIsInstance<PyKeywordArgument>()
                .filter { it.name == name }
                .forEach {
                    resolveResult.poke(it, SmkResolveUtil.RATE_NORMAL)
                }

        if (isSimpleArgsList(args)) {
            val asIdx = name.toIntOrNull()
            if (asIdx != null && asIdx < args.size)
                resolveResult.poke(args[asIdx], SmkResolveUtil.RATE_NORMAL)
        }

        return resolveResult
    }

    private fun getSectionArgs(): Array<out PyExpression>? = section.argumentList?.arguments

    override fun getCompletionVariants(
            completionPrefix: String?,
            location: PsiElement,
            context: ProcessingContext?
    ): Array<LookupElement> {
        if (!SmkPsiUtil.isInsideSnakemakeOrSmkSLFile(location)) {
            return LookupElementBuilder.EMPTY_ARRAY
        }

        val inSmkSlInjection = location is SmkSLElement
        val inStringLiteralKey = location.node.elementType in PyTokenTypes.STRING_NODES
        val inPySubscription = PsiTreeUtil.getParentOfType(location, PySubscriptionExpression::class.java) != null
        val inSmkSubscription = location is SmkSLSubscriptionKeyExpression

        val results = arrayListOf<LookupElement>()
        val args = getSectionArgs()

        @Suppress("FoldInitializerAndIfToElvis")
        if (args == null) {
            return LookupElementBuilder.EMPTY_ARRAY
        }

        val sectionKeyword = section.sectionKeyword
        val typeText = "$sectionKeyword section key"
        args.filterIsInstance<PyKeywordArgument>().forEach {
            results.add(createLookupItem(
                    it.name!!, typeText,
                    false, inSmkSlInjection, inPySubscription, inStringLiteralKey
            ))
        }
        if ((inSmkSubscription || inPySubscription) && (inSmkSlInjection || !inStringLiteralKey) && isSimpleArgsList(args)) {
            args.indices.forEach { i ->
                results.add(createLookupItem(
                        i.toString(), typeText,
                        true, inSmkSlInjection, inPySubscription, inStringLiteralKey
                ))
            }
        }

        if (results.isEmpty()) {
            LookupElementBuilder.EMPTY_ARRAY
        }
        return results.toTypedArray()
    }

    private fun createLookupItem(
            text: String,
            typeText: String,
            isIdx: Boolean,
            inSmkSlInjection: Boolean,
            inPySubscription: Boolean,
            inStringLiteralKey: Boolean
    ): LookupElement {
        val lookupString = when {
            !inStringLiteralKey && !isIdx && !inSmkSlInjection && inPySubscription -> "'$text'"
            else -> text
        }

        return SmkCompletionUtil.createPrioritizedLookupElement(
                lookupString,
                PlatformIcons.PARAMETER_ICON,
                typeText
        )
    }

    private fun isSimpleArgsList(args: Array<out PyExpression>): Boolean {
        return args.firstOrNull {
            it is PyStarArgument || (it is PyCallExpression && it.callSimpleName() == UNPACK_FUNCTION)
        } == null
    }

    override fun isBuiltin() = false
}
