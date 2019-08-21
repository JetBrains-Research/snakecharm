package com.jetbrains.snakecharm.lang.psi.impl

import com.intellij.lang.ASTNode
import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.psi.PsiReference
import com.jetbrains.python.psi.PyElementVisitor
import com.jetbrains.python.psi.PyStringLiteralExpression
import com.jetbrains.python.psi.types.PyType
import com.jetbrains.python.psi.types.TypeEvalContext
import com.jetbrains.snakecharm.lang.SnakemakeNames
import com.jetbrains.snakecharm.lang.psi.SmkConfigfileReference
import com.jetbrains.snakecharm.lang.psi.SmkElementVisitor
import com.jetbrains.snakecharm.lang.psi.SmkRuleOrCheckpointArgsSection
import com.jetbrains.snakecharm.lang.psi.types.SmkSectionType

open class SmkRuleOrCheckpointArgsSectionImpl(node: ASTNode): SmkArgsSectionImpl(node), SmkRuleOrCheckpointArgsSection {
    override fun getType(context: TypeEvalContext, key: TypeEvalContext.Key)
            = SmkSectionType(this)

    override fun acceptPyVisitor(pyVisitor: PyElementVisitor) = when (pyVisitor) {
        is SmkElementVisitor -> pyVisitor.visitSmkRuleOrCheckpointArgsSection(this)
        else -> super.acceptPyVisitor(pyVisitor)
    }

    override fun getReference(): PsiReference? =
        when (this.name) {
            SnakemakeNames.SECTION_CONDA -> getCondaSectionReference()
            else -> null
        }

    private fun getCondaSectionReference() : PsiReference? {
        val stringLiteral =
                argumentList?.arguments
                        ?.firstOrNull { it is PyStringLiteralExpression }
                        as? PyStringLiteralExpression ?: return null

        // No reference if language is injected
        val languageManager = InjectedLanguageManager.getInstance(project)
        if (languageManager.getInjectedPsiFiles(stringLiteral) != null) {
            return null
        }

        val offsetInParent = SnakemakeNames.SECTION_CONDA.length + stringLiteral.startOffsetInParent
        return SmkConfigfileReference(
                this,
                SmkPsiUtil.getReferenceRange(stringLiteral).shiftRight(offsetInParent),
                stringLiteral.stringValue
        )
    }
}