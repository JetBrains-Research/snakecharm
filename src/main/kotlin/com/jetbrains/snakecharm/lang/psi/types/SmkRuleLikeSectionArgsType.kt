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
import com.jetbrains.python.psi.types.PyStructuralType
import com.jetbrains.snakecharm.codeInsight.SnakemakeAPI.UNPACK_FUNCTION
import com.jetbrains.snakecharm.codeInsight.completion.SmkCompletionUtil
import com.jetbrains.snakecharm.codeInsight.resolve.SmkResolveUtil
import com.jetbrains.snakecharm.lang.SnakemakeNames
import com.jetbrains.snakecharm.lang.psi.SmkRuleOrCheckpointArgsSection
import com.jetbrains.snakecharm.lang.psi.impl.SmkPsiUtil
import com.jetbrains.snakecharm.stringLanguage.lang.callSimpleName

class SmkRuleLikeSectionArgsType(
        val section: SmkRuleOrCheckpointArgsSection
) : PyStructuralType(
    getSectionArgsNames(getSectionArgs(section)),
    false
), SmkAvailableForSubscriptionType {

    companion object {
        private fun getSectionArgs(section: SmkRuleOrCheckpointArgsSection)
                = section.argumentList?.arguments
        private fun getSectionArgsNames(args: Array<PyExpression>?)
                = args?.filterIsInstance<PyKeywordArgument>()?.mapNotNull { it.name }?.toSet() ?: emptySet()
    }

    private val typeName: String = section.sectionKeyword?.let { "$it:" } ?: "section"
    private val sectionArgs: Array<out PyExpression>? = getSectionArgs(section)

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

        @Suppress("FoldInitializerAndIfToElvis")
        if (sectionArgs == null) {
            return emptyList()
        }

        val resolveResult = ResolveResultList()
        sectionArgs.filterIsInstance<PyKeywordArgument>()
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
        if (sectionArgs != null && isSimpleArgsList(sectionArgs) && idx >= 0) {
            val resolveResult = ResolveResultList()
            var pos = idx

            val el = sectionArgs.firstOrNull {
                pos -= countProducedElements(it)
                pos < 0
            }
            if (el != null) {
                resolveResult.poke(el, SmkResolveUtil.RATE_NORMAL)
                return resolveResult
            }
        }
        return emptyList()
    }

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

        @Suppress("FoldInitializerAndIfToElvis")
        if (sectionArgs == null) {
            return emptyList<LookupElementBuilder>() to priority
        }

        val sectionKeyword = section.sectionKeyword
        val typeText = "$sectionKeyword section key"
        attributeNames.forEach { name ->
            val item = LookupElementBuilder
                    .create(name)
                    .withTypeText(typeText)
                    .withIcon(PlatformIcons.FIELD_ICON)
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

        if (sectionArgs == null || !isSimpleArgsList(sectionArgs)) {
            return 0
        }

        // Uses this instead of "sectionsArgs.size" in order to add support for 'multiext' snakemake method
        // It will work incorrectly if there are another function with name 'multiext'
        return sectionArgs.sumOf { countProducedElements(it) }
    }

    private fun isSimpleArgsList(args: Array<out PyExpression>): Boolean {
        return args.firstOrNull {
            it is PyStarArgument || (it is PyCallExpression && it.callSimpleName() == UNPACK_FUNCTION)
        } == null
    }

    /**
     * Counts a number of elements that [PyExpression] returns.
     * E.g. "foo.txt" - one element
     * multiext("f.", "t", "d") - two elements ["f.t", "f.d"]
     *
     * Note, that it supports smart counting only for 'multiext' function
     */
    private fun countProducedElements(exp: PyExpression) = if (exp is PyCallExpression
        && exp.callee?.name.equals(SnakemakeNames.SNAKEMAKE_METHOD_MULTIEXT)
    ) exp.arguments.size - 1 else 1

    override fun isBuiltin() = false
}
