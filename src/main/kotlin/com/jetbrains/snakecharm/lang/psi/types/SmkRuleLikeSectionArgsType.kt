package com.jetbrains.snakecharm.lang.psi.types

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiInvalidElementAccessException
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.PlatformIcons
import com.intellij.util.ProcessingContext
import com.jetbrains.python.psi.*
import com.jetbrains.python.psi.impl.ResolveResultList
import com.jetbrains.python.psi.resolve.PyResolveContext
import com.jetbrains.python.psi.resolve.RatedResolveResult
import com.jetbrains.python.psi.types.PyStructuralType
import com.jetbrains.snakecharm.SnakemakeBundle
import com.jetbrains.snakecharm.codeInsight.SnakemakeAPI.UNPACK_FUNCTION
import com.jetbrains.snakecharm.codeInsight.completion.SmkCompletionUtil
import com.jetbrains.snakecharm.codeInsight.resolve.SmkResolveUtil
import com.jetbrains.snakecharm.lang.SnakemakeNames
import com.jetbrains.snakecharm.lang.psi.SmkRuleOrCheckpointArgsSection
import com.jetbrains.snakecharm.lang.psi.SmkSection
import com.jetbrains.snakecharm.lang.psi.impl.SmkPsiUtil
import com.jetbrains.snakecharm.stringLanguage.lang.callSimpleName

class SmkRuleLikeSectionArgsType(
    val section: SmkRuleOrCheckpointArgsSection,
) : PyStructuralType(
    getSectionArgsNames(getSectionArgs(section)),
    false
), SmkAvailableForSubscriptionType {
    companion object {
        private const val MAX_PREVIEW_LENGTH = 20

        private fun getSectionArgs(section: SmkRuleOrCheckpointArgsSection) = section.argumentList?.arguments
        private fun getSectionArgsNames(args: Array<PyExpression>?) =
            args?.filterIsInstance<PyKeywordArgument>()?.mapNotNull { it.name }?.toSet() ?: emptySet()
    }

    private val typeName: String = section.sectionKeyword ?: "section"
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
        resolveContext: PyResolveContext,
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

    private fun findProducedElementByIndex(
        idx: Int,
    ): PsiElement? {
        if (sectionArgs != null && isSimpleArgsList(sectionArgs) && idx >= 0) {
            var pos = idx

            for (exp in sectionArgs) {
                val argNumber = countProducedElements(exp)
                if (pos < argNumber) {
                    return getProducedElementByIndex(exp, pos)
                }
                pos -= argNumber
            }
        }
        return null
    }

    override fun resolveMemberByIndex(
        idx: Int,
        location: PyExpression?,
        direction: AccessDirection,
        resolveContext: PyResolveContext,
    ): List<RatedResolveResult> {
        val psiElement = findProducedElementByIndex(idx) ?: return emptyList()

        val resolveResult = ResolveResultList()
        resolveResult.poke(psiElement, SmkResolveUtil.RATE_NORMAL)
        return resolveResult
    }

    override fun getCompletionVariants(
        completionPrefix: String?,
        location: PsiElement,
        context: ProcessingContext?,
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
        context: ProcessingContext?,
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
        val typeText = when (sectionKeyword) {
            null -> SnakemakeBundle.message("TYPES.rule.section.arg.keyword.unknown.section.type.text")
            else -> SnakemakeBundle.message("TYPES.rule.section.arg.keyword.type.text", sectionKeyword)
        }
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

    override fun getPositionArgsPreviews(location: PsiElement): List<String?> {
        if (!SmkPsiUtil.isInsideSnakemakeOrSmkSLFile(location)) {
            return emptyList()
        }

        if (sectionArgs == null || !isSimpleArgsList(sectionArgs)) {
            return emptyList()
        }

        // Uses this instead of "sectionsArgs.size" in order to add support for 'multiext' snakemake method
        // It will work incorrectly if there are another function with name 'multiext'
        val n = sectionArgs.sumOf { countProducedElements(it) }
        return (0 until n).map { i ->
            val el = findProducedElementByIndex(i)
            psiElementArgPreview(if (el !is PyKeywordArgument) el else el.valueExpression)
        }
    }

    private fun psiElementArgPreview(el: PsiElement?): String? {
        if (el == null) {
            return null
        }

        val textPreview = when (el) {
            is PyStringLiteralExpression -> {
                val containingCall =
                    PsiTreeUtil.getParentOfType(el, PyCallExpression::class.java, true, SmkSection::class.java)
                var text = el.stringValue
                if (containingCall != null && containingCall.isCalleeText(SnakemakeNames.SNAKEMAKE_METHOD_MULTIEXT)) {
                    val multiExtFirstArg = containingCall.arguments.first()
                    if (multiExtFirstArg is PyStringLiteralExpression) {
                        text = "${multiExtFirstArg.stringValue}$text"
                    }
                }
                text
            }

            else -> el.text
        }

        return when {
            textPreview.length < MAX_PREVIEW_LENGTH -> textPreview
            else -> "...${textPreview.takeLast(MAX_PREVIEW_LENGTH - 3)}"
        }.replace("\n", "")
    }

    private fun isSimpleArgsList(args: Array<out PyExpression>): Boolean {
        return args.firstOrNull {
            it is PyStarArgument || (it is PyCallExpression && it.callSimpleName() == UNPACK_FUNCTION)
        } == null
    }

    /**
     * Counts a number of elements that [PyExpression] returns. E.g.:
     *  * "foo.txt" - one element
     *  * multiext("foo.", "txt", "log") - two elements ("foo.txt", "foo.log")
     *
     * Note, that at this stage, it supports smart counting only for 'multiext' function
     */
    private fun countProducedElements(exp: PyExpression) = if (exp is PyCallExpression
        && exp.isCalleeText(SnakemakeNames.SNAKEMAKE_METHOD_MULTIEXT)
    ) exp.arguments.size - 1 else 1

    /**
     * Returns [ind] produced element of that [PyExpression]. E.g.:
     *  * ("foo.txt", 0) -> "foo.txt"
     *  * (multiext("foo.", "txt", "log"), 1) -> "log" argument
     *
     * Note, that at this stage, it supports smart access only for 'multiext' function
     *
     * @throws IndexOutOfBoundsException if [ind] is out of bounds of list of function produced elements
     */
    private fun getProducedElementByIndex(exp: PyExpression, ind: Int) = if (exp is PyCallExpression
        && exp.isCalleeText(SnakemakeNames.SNAKEMAKE_METHOD_MULTIEXT)
    ) exp.arguments[ind + 1] else exp

    override fun isBuiltin() = false
}
