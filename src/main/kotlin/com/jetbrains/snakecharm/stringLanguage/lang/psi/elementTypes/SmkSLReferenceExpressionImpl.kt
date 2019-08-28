package com.jetbrains.snakecharm.stringLanguage.lang.psi.elementTypes

import com.intellij.lang.ASTNode
import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.psi.PsiPolyVariantReference
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.python.psi.PyElementVisitor
import com.jetbrains.python.psi.PyExpression
import com.jetbrains.python.psi.impl.PyReferenceExpressionImpl
import com.jetbrains.python.psi.impl.references.PyQualifiedReference
import com.jetbrains.python.psi.resolve.PyResolveContext
import com.jetbrains.snakecharm.lang.psi.SmkRuleOrCheckpoint
import com.jetbrains.snakecharm.lang.psi.SmkRuleOrCheckpointArgsSection
import com.jetbrains.snakecharm.lang.psi.SmkSLReferenceExpression
import com.jetbrains.snakecharm.lang.psi.types.SmkWildcardsType
import com.jetbrains.snakecharm.stringLanguage.SmkSLElementVisitor
import com.jetbrains.snakecharm.stringLanguage.lang.psi.references.SmkSLInitialReference
import com.jetbrains.snakecharm.stringLanguage.lang.psi.references.SmkSLWildcardReference

class SmkSLReferenceExpressionImpl(
        node: ASTNode
) : PyReferenceExpressionImpl(node), SmkSLReferenceExpression {
    override fun getName() = super<SmkSLReferenceExpression>.getName()

    override fun acceptPyVisitor(pyVisitor: PyElementVisitor) = when (pyVisitor) {
        is SmkSLElementVisitor -> pyVisitor.visitSmkSLReferenceExpression(this)
        else -> super.acceptPyVisitor(pyVisitor)
    }

    override fun getReference(context: PyResolveContext): PsiPolyVariantReference {
        if (qualifier != null) {
            return PyQualifiedReference(this, context)
        }

        val languageManager = InjectedLanguageManager.getInstance(project)
        val host = languageManager.getInjectionHost(this)
        val parentSection =
                PsiTreeUtil.getParentOfType(host, SmkRuleOrCheckpointArgsSection::class.java)
        val parentDeclaration =
                PsiTreeUtil.getParentOfType(host, SmkRuleOrCheckpoint::class.java)

        if (parentDeclaration == null ||
                parentSection?.name !in SmkRuleOrCheckpointArgsSection.KEYWORDS_CONTAINING_WILDCARDS) {
            return SmkSLInitialReference(this, parentDeclaration, context)
        }

        return SmkSLWildcardReference(this, SmkWildcardsType(parentDeclaration))
    }

    override fun getQualifier(): PyExpression? =
            children.firstOrNull { it is SmkSLReferenceExpression } as PyExpression?

    companion object {
        fun isWildcard(expr: SmkSLReferenceExpression) =
                PsiTreeUtil.getParentOfType(expr, SmkSLReferenceExpression::class.java) == null &&
                        (expr.containingRuleOrCheckpointSection()?.isWildcardsAllowedSection() ?: false) &&
                        expr.text.isNotEmpty()
    }
}
