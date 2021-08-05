package com.jetbrains.snakecharm.lang.psi

import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.parentOfType
import com.jetbrains.python.psi.PyElementVisitor
import com.jetbrains.python.psi.PyKeywordArgument
import com.jetbrains.python.psi.PyRecursiveElementVisitor
import com.jetbrains.python.psi.PyStringLiteralExpression
import com.jetbrains.snakecharm.lang.SnakemakeNames
import com.jetbrains.snakecharm.stringLanguage.lang.psi.SmkSLFile
import com.jetbrains.snakecharm.stringLanguage.lang.psi.SmkSLReferenceExpression
import com.jetbrains.snakecharm.stringLanguage.lang.psi.SmkSLSubscriptionExpression
import com.jetbrains.snakecharm.stringLanguage.lang.psi.references.SmkSLWildcardReference

/**
 * Checks if rule/checkpoint section has a string injected reference (e.g. "echo {log}") that is resolved into
 * desired target element.
 *
 * At the moment limited to 'run' and 'rule' sections
 */
class SmkSLReferencesTargetLookupVisitor(
    private val targetPsiElement: PsiElement
) : SmkElementVisitor, PyRecursiveElementVisitor() {

    var hasReferenceToTarget: Boolean = false
        private set

    override val pyElementVisitor: PyElementVisitor
        get() = this

    override fun visitSmkRuleOrCheckpointArgsSection(st: SmkRuleOrCheckpointArgsSection) {
        if (st.sectionKeyword != SnakemakeNames.SECTION_SHELL) {
            // stop traversing
            return
        }
        super.visitSmkRuleOrCheckpointArgsSection(st)
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
        val references = PsiTreeUtil.getChildrenOfType(file, SmkSLReferenceExpression::class.java)
        references?.forEach { st ->
            checkSLReference(st)
        }
        val subscriptions = PsiTreeUtil.getChildrenOfType(file, SmkSLSubscriptionExpression::class.java)
        subscriptions?.forEach { st: SmkSLSubscriptionExpression ->
            val rootOperand = st.rootOperand
            if (rootOperand is SmkSLReferenceExpression) {
                checkSLReference(rootOperand)
            }
        }
    }

    private fun checkSLReference(ref: SmkSLReferenceExpression) {
        if (ref.reference is SmkSLWildcardReference) {
            return
        }
        var element = ref.reference.resolve()
        if (element is PyKeywordArgument) {
            element = element.parentOfType<SmkRuleOrCheckpointArgsSection>()
        }
        if (element == targetPsiElement) {
            hasReferenceToTarget = true
        }
    }
}