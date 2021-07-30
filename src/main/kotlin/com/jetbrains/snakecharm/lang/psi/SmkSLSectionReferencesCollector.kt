package com.jetbrains.snakecharm.lang.psi

import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.elementType
import com.intellij.psi.util.parentOfType
import com.jetbrains.python.PyElementTypes
import com.jetbrains.python.psi.PyElementVisitor
import com.jetbrains.python.psi.PyKeywordArgument
import com.jetbrains.python.psi.PyRecursiveElementVisitor
import com.jetbrains.python.psi.PyStringLiteralExpression
import com.jetbrains.snakecharm.lang.psi.elementTypes.SmkElementTypes
import com.jetbrains.snakecharm.stringLanguage.lang.psi.SmkSLFile
import com.jetbrains.snakecharm.stringLanguage.lang.psi.SmkSLReferenceExpressionImpl
import com.jetbrains.snakecharm.stringLanguage.lang.psi.SmkSLSubscriptionExpressionImpl

class SmkSLSectionReferencesCollector(
    private val searchingType: String,
    private val isRuleOrCheckpointAppropriate: (SmkRuleOrCheckpoint) -> Boolean = { true }
) : SmkElementVisitor, PyRecursiveElementVisitor() {
    private val sections = mutableListOf<PsiElement>()

    fun getSections(): List<PsiElement> = sections

    override val pyElementVisitor: PyElementVisitor
        get() = this

    override fun visitSmkRule(rule: SmkRule) {
        if (isRuleOrCheckpointAppropriate(rule)) {
            super.visitSmkRule(rule)
        }
    }

    override fun visitSmkCheckPoint(checkPoint: SmkCheckPoint) {
        if (isRuleOrCheckpointAppropriate(checkPoint)) {
            super.visitSmkCheckPoint(checkPoint)
        }
    }

    override fun visitPyStringLiteralExpression(node: PyStringLiteralExpression) {
        val languageManager = InjectedLanguageManager.getInstance(node.project)
        val injectedFiles =
            languageManager.getInjectedPsiFiles(node)
                ?.map { it.first }
                ?.filterIsInstance<SmkSLFile>()
                ?: return

        injectedFiles.forEach(::collectResolvedReferences)
    }

    private fun collectResolvedReferences(file: SmkSLFile) {
        val references = PsiTreeUtil.getChildrenOfType(file, SmkSLReferenceExpressionImpl::class.java)
        references?.forEach { st ->
            checkSLReference(st)
        }
        val subscriptions = PsiTreeUtil.getChildrenOfType(file, SmkSLSubscriptionExpressionImpl::class.java)
        subscriptions?.forEach { st ->
            val child = (st.node.firstChildNode.psi as? SmkSLReferenceExpressionImpl)
            if (child != null) {
                checkSLReference(child)
            }
        }
    }

    private fun checkSLReference(ref: SmkSLReferenceExpressionImpl) {
        if (ref.isWildcard()) {
            return
        }
        var element = ref.reference.resolve()
        if (element.elementType == PyElementTypes.KEYWORD_ARGUMENT_EXPRESSION) {
            element = (element as PyKeywordArgument).parentOfType<SmkRuleOrCheckpointArgsSection>()
        }
        if (
            (element.elementType == SmkElementTypes.RULE_OR_CHECKPOINT_ARGS_SECTION_STATEMENT
                    && (element as SmkRuleOrCheckpointArgsSection).name == searchingType)
        ) {
            sections.add(element)
        }
    }
}