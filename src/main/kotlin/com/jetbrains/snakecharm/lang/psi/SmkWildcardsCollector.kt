package com.jetbrains.snakecharm.lang.psi

import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.python.psi.PyElementVisitor
import com.jetbrains.python.psi.PyRecursiveElementVisitor
import com.jetbrains.python.psi.PyStringLiteralExpression
import com.jetbrains.snakecharm.stringLanguage.SmkSLFile
import com.jetbrains.snakecharm.stringLanguage.lang.psi.elementTypes.SmkSLLanguageElement
import com.jetbrains.snakecharm.stringLanguage.lang.psi.elementTypes.SmkSLReferenceExpressionImpl

class SmkWildcardsCollector : SmkElementVisitor, PyRecursiveElementVisitor() {
    private val wildcardsElements = mutableListOf<Pair<SmkSLReferenceExpression, String>>()

    /**
     * @return List of all wildcard element usages and its names
     */
    fun getWildcards(): List<Pair<SmkSLReferenceExpression, String>> = wildcardsElements

    /**
     * @return List of first mention of wildcard (element and name pairs)
     */
    fun getWildcardsFirstMentions(): List<Pair<SmkSLReferenceExpression, String>> = wildcardsElements
            .distinctBy { (_, name) -> name }

    override val pyElementVisitor: PyElementVisitor
        get() = this

    override fun visitSmkRuleOrCheckpointArgsSection(st: SmkRuleOrCheckpointArgsSection) {
        if (st.name in SmkRuleOrCheckpointArgsSection.KEYWORDS_CONTAINING_WILDCARDS) {
            super.visitSmkRuleOrCheckpointArgsSection(st)
        }
    }

    override fun visitPyStringLiteralExpression(stringLiteral: PyStringLiteralExpression) {
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
                wildcardsElements.add(st to st.text)
            }
        }
    }
}
