package com.jetbrains.snakecharm.lang.psi

import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.python.psi.PyElementVisitor
import com.jetbrains.python.psi.PyRecursiveElementVisitor
import com.jetbrains.python.psi.PyStringLiteralExpression
import com.jetbrains.snakecharm.stringLanguage.SmkSLFile
import com.jetbrains.snakecharm.stringLanguage.lang.psi.elementTypes.SmkSLLanguageElement
import com.jetbrains.snakecharm.stringLanguage.lang.psi.elementTypes.SmkSLReferenceExpressionImpl

class SmkWildcardsCollector : SmkElementVisitor, PyRecursiveElementVisitor() {
    val collectedWildcards = mutableMapOf<String, PsiElement>()

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
        val language =
                PsiTreeUtil.getChildrenOfType(file, SmkSLLanguageElement::class.java)

        language?.forEach {
            val statement =
                    PsiTreeUtil.getChildOfType(it, SmkSLReferenceExpressionImpl::class.java)
                            ?: return@forEach

            if (!collectedWildcards.containsKey(statement.text)) {
                collectedWildcards[statement.text] = statement
            }
        }
    }
}
