package com.jetbrains.snakecharm.lang.psi.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiReference
import com.jetbrains.python.psi.PyElementVisitor
import com.jetbrains.python.psi.PyStringLiteralExpression
import com.jetbrains.snakecharm.lang.SnakemakeNames
import com.jetbrains.snakecharm.lang.psi.SmkElementVisitor
import com.jetbrains.snakecharm.lang.psi.SmkIncludeReference
import com.jetbrains.snakecharm.lang.psi.SmkModuleArgsSection

class SmkModuleArgsSectionImpl(node: ASTNode) : SmkArgsSectionImpl(node), SmkModuleArgsSection {
    override fun acceptPyVisitor(pyVisitor: PyElementVisitor?) {
        when (pyVisitor) {
            is SmkElementVisitor -> pyVisitor.visitSmkModuleArgsSection(this)
            else -> super<SmkArgsSectionImpl>.acceptPyVisitor(pyVisitor)
        }
    }

    override fun getReference(): PsiReference? {
        if (sectionKeyword == SnakemakeNames.MODULE_SNAKEFILE_KEYWORD) {
            val stringLiteral =
                argumentList?.arguments
                    ?.firstOrNull { it is PyStringLiteralExpression }
                        as? PyStringLiteralExpression ?: return null

            val offsetInParent = (sectionKeyword ?: return null).length + stringLiteral.startOffsetInParent
            return SmkIncludeReference(
                this,
                SmkPsiUtil.getReferenceRange(stringLiteral).shiftRight(offsetInParent),
                stringLiteral, stringLiteral.stringValue
            )
        }
        return null
    }
}