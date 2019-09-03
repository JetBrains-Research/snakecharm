package com.jetbrains.snakecharm.lang.psi.types

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiInvalidElementAccessException
import com.intellij.util.PlatformIcons
import com.intellij.util.ProcessingContext
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

class SmkRuleOrCheckpointSectionArgumentType(
        val section: SmkRuleOrCheckpointArgsSection
) : PyType, SmkAvailableForSubscriptionType {

    private val typeName: String = section.sectionKeyword?.let { "$it:" } ?: "section"

    override fun getName() = typeName

    override fun getDeclarationElement(): SmkRuleOrCheckpointArgsSection = section

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

        return resolveResult
    }

    override fun resolveMemberByIndex(
            idx: Int,
            location: PyExpression?,
            direction: AccessDirection,
            resolveContext: PyResolveContext
    ): List<RatedResolveResult> {
        val args = getSectionArgs()

        if (args != null && isSimpleArgsList(args)) {
            val resolveResult = ResolveResultList()
            if (idx in args.indices) {
                resolveResult.poke(args[idx], SmkResolveUtil.RATE_NORMAL)
            }
            return resolveResult
        }
        return emptyList()
    }

    private fun getSectionArgs(): Array<out PyExpression>? = section.argumentList?.arguments

    override fun getCompletionVariants(
            completionPrefix: String?,
            location: PsiElement,
            context: ProcessingContext?
    ): Array<LookupElement> {
        val (list, priority) = getCompletionVariantsAndPriority(completionPrefix, location, context)

        return if (list.isEmpty()) {
            LookupElementBuilder.EMPTY_ARRAY
        } else {
            list.map { SmkCompletionUtil.createPrioritizedLookupElement(it, priority) }.toTypedArray()
        }

    }

    override fun getCompletionVariantsAndPriority(
            completionPrefix: String?,
            location: PsiElement,
            context: ProcessingContext?
    ): Pair<List<LookupElementBuilder>, Double> {
        val priority = SmkCompletionUtil.SECTIONS_KEYS_PRIORITY

        if (!SmkPsiUtil.isInsideSnakemakeOrSmkSLFile(location)) {
            return emptyList<LookupElementBuilder>() to priority
        }

        val results = arrayListOf<LookupElementBuilder>()
        val args = getSectionArgs()

        @Suppress("FoldInitializerAndIfToElvis")
        if (args == null) {
            return emptyList<LookupElementBuilder>() to priority
        }

        val sectionKeyword = section.sectionKeyword
        val typeText = "$sectionKeyword section key"
        args.filterIsInstance<PyKeywordArgument>().forEach {
            val item = LookupElementBuilder
                    .create(it.name!!)
                    .withTypeText(typeText)
                    .withIcon(PlatformIcons.PARAMETER_ICON)
            results.add(item)
        }

        if (results.isEmpty()) {
            return emptyList<LookupElementBuilder>() to priority
        }
        return results to priority
    }

    override fun getPositionArgsNumber(location: PsiElement): Int {
        if (!SmkPsiUtil.isInsideSnakemakeOrSmkSLFile(location)) {
            return 0
        }

        val args = getSectionArgs()

        if (args == null || !isSimpleArgsList(args)) {
            return 0
        }

        return args.size
    }

    private fun isSimpleArgsList(args: Array<out PyExpression>): Boolean {
        return args.firstOrNull {
            it is PyStarArgument || (it is PyCallExpression && it.callSimpleName() == UNPACK_FUNCTION)
        } == null
    }

    override fun isBuiltin() = false
}
