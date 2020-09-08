package com.jetbrains.snakecharm.lang.psi.impl

import com.intellij.lang.ASTNode
import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.psi.PsiReference
import com.jetbrains.python.psi.PyElementVisitor
import com.jetbrains.python.psi.PyStringLiteralExpression
import com.jetbrains.python.psi.types.TypeEvalContext
import com.jetbrains.snakecharm.lang.SnakemakeNames
import com.jetbrains.snakecharm.lang.psi.*
import com.jetbrains.snakecharm.lang.psi.types.SmkRuleLikeSectionArgsType

open class SmkRuleOrCheckpointArgsSectionImpl(node: ASTNode): SmkArgsSectionImpl(node),
        SmkRuleOrCheckpointArgsSection
{
    override fun getType(context: TypeEvalContext, key: TypeEvalContext.Key)
            = SmkRuleLikeSectionArgsType(this)

    override fun acceptPyVisitor(pyVisitor: PyElementVisitor) = when (pyVisitor) {
        is SmkElementVisitor -> pyVisitor.visitSmkRuleOrCheckpointArgsSection(this)
        else -> super.acceptPyVisitor(pyVisitor)
    }

    override fun getReference(): PsiReference? =
        when (this.sectionKeyword) {
            SnakemakeNames.SECTION_CONDA -> getSimplePathRelatedSectionReference { stringLiteral, offsetInParent ->
                SmkCondaEnvReference(
                    this,
                    SmkPsiUtil.getReferenceRange(stringLiteral).shiftRight(offsetInParent),
                    stringLiteral, stringLiteral.stringValue
                )
            }
            SnakemakeNames.SECTION_NOTEBOOK -> getSimplePathRelatedSectionReference { stringLiteral, offsetInParent ->
                SmkNotebookReference(
                    this,
                    SmkPsiUtil.getReferenceRange(stringLiteral).shiftRight(offsetInParent),
                    stringLiteral, stringLiteral.stringValue
                )
            }
            else -> null
        }

    private fun getSimplePathRelatedSectionReference(
        refFun: (PyStringLiteralExpression, Int) -> SmkFileReference
    ) : PsiReference? {
        val stringLiteral =
                argumentList?.arguments
                        ?.firstOrNull { it is PyStringLiteralExpression }
                        as? PyStringLiteralExpression ?: return null

        // No reference if language is injected
        val languageManager = InjectedLanguageManager.getInstance(project)
        if (languageManager.getInjectedPsiFiles(stringLiteral) != null) {
            return null
        }

        val offsetInParent = sectionKeyword!!.length + stringLiteral.startOffsetInParent
        return refFun(stringLiteral, offsetInParent)
    }
}