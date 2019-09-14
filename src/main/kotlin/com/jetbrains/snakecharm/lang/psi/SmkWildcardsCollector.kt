package com.jetbrains.snakecharm.lang.psi

import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.python.psi.PyCallExpression
import com.jetbrains.python.psi.PyElementVisitor
import com.jetbrains.python.psi.PyRecursiveElementVisitor
import com.jetbrains.python.psi.PyStringLiteralExpression
import com.jetbrains.snakecharm.codeInsight.SnakemakeAPI.FUNCTIONS_BANNED_FOR_WILDCARDS
import com.jetbrains.snakecharm.codeInsight.SnakemakeAPI.WILDCARDS_DEFINING_SECTIONS_KEYWORDS
import com.jetbrains.snakecharm.stringLanguage.lang.callSimpleName
import com.jetbrains.snakecharm.stringLanguage.lang.psi.SmkSLFile
import com.jetbrains.snakecharm.stringLanguage.lang.psi.SmkSLLanguageElement
import com.jetbrains.snakecharm.stringLanguage.lang.psi.SmkSLReferenceExpressionImpl

/**
 * For containers which includes:
 *  * rule, checkpoint, sections - collects wildcards in sections according to settings in constructor
 *  * call expression - ignore some methods, which don't introduce wildcards, e.g. 'expand'
 *  * string literals - collect all injections
 *
 *  @param visitDefiningSections Visit or not downstream sections which introduces new wildcards
 *  @param visitExpandingSections Visit or not downstream sections which allows wildcards usage w/o 'wildcards.' prefix
 *  @param visitAllSections If true visit all sections (including run) ignoring [visitDefiningSections] and [visitExpandingSections] flags
 */
class SmkWildcardsCollector(
        private val visitDefiningSections: Boolean,
        private val visitExpandingSections: Boolean,
        private val visitAllSections: Boolean = false
) : SmkElementVisitor, PyRecursiveElementVisitor() {
    private val wildcardsElements = mutableListOf<WildcardDescriptor>()
    private var atLeastOneInjectionVisited = false
    private var currentSectionIdx: Byte = -1

    /**
     * @return List of all wildcard element usages and its names or null if no string literals were found
     *
     * No string literals found means that code doesn't contain wildcards injections and even candidate places
     * for them so user has not only zero wildcards, but now possible wildcards declarations
     */
    fun getWildcards(): List<WildcardDescriptor>? =
            if (atLeastOneInjectionVisited) wildcardsElements else null

    fun getWildcardsNames() = getWildcards()?.asSequence()?.map { it.text }?.distinct()?.toList()

    override val pyElementVisitor: PyElementVisitor
        get() = this

    override fun visitSmkRunSection(st: SmkRunSection) {
        if (visitAllSections) {
            super.visitSmkRunSection(st)
        }
    }

    override fun visitSmkRuleOrCheckpointArgsSection(st: SmkRuleOrCheckpointArgsSection) {
        try {
            currentSectionIdx = WILDCARDS_DEFINING_SECTIONS_KEYWORDS.indexOf(st.sectionKeyword).toByte()

            if (
                    visitAllSections ||
                    (visitDefiningSections && st.isWildcardsDefiningSection()) ||
                    (visitExpandingSections && st.isWildcardsExpandingSection())
            ) {
                super.visitSmkRuleOrCheckpointArgsSection(st)
                return
            }
        } finally {
            currentSectionIdx = -1
        }
    }

    override fun visitPyCallExpression(node: PyCallExpression) {   //format
        if (node.callSimpleName() !in FUNCTIONS_BANNED_FOR_WILDCARDS) {
            super.visitPyCallExpression(node)
        }
    }

    override fun visitPyStringLiteralExpression(stringLiteral: PyStringLiteralExpression) {
        atLeastOneInjectionVisited = true
        val languageManager = InjectedLanguageManager.getInstance(stringLiteral.project)
        val injectedFiles =
                languageManager.getInjectedPsiFiles(stringLiteral)
                        ?.map { it.first }
                        ?.filterIsInstance<SmkSLFile>()
                        ?: return

        injectedFiles.forEach { collectWildcardsNames(it) }
    }

    private fun collectWildcardsNames(file: SmkSLFile) {

        val injections = PsiTreeUtil.getChildrenOfType(file, SmkSLLanguageElement::class.java)
        injections?.forEach { injection ->
            val st = PsiTreeUtil.getChildOfType(injection, SmkSLReferenceExpressionImpl::class.java)
            if (st != null) {
                wildcardsElements.add(WildcardDescriptor(
                        st, st.text,
                        if (currentSectionIdx == (-1).toByte()) WildcardDescriptor.UNDEFINED_SECTION else currentSectionIdx
                ))
            }
        }
    }
}
data class WildcardDescriptor(
        val psi: SmkSLReferenceExpressionImpl,
        val text: String,
        val definingSectionRate: Byte
) {
    companion object {
        const val UNDEFINED_SECTION: Byte = Byte.MAX_VALUE
    }
}