package com.jetbrains.snakecharm.stringLanguage.lang.psi

import com.intellij.lang.ASTNode
import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiPolyVariantReference
import com.intellij.psi.PsiPolyVariantReferenceBase
import com.intellij.psi.ResolveResult
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.python.psi.PyCallExpression
import com.jetbrains.python.psi.PyElementVisitor
import com.jetbrains.python.psi.PyExpression
import com.jetbrains.python.psi.impl.PyReferenceExpressionImpl
import com.jetbrains.python.psi.impl.references.PyQualifiedReference
import com.jetbrains.python.psi.resolve.PyResolveContext
import com.jetbrains.snakecharm.codeInsight.SnakemakeAPI.FUNCTIONS_BANNED_FOR_WILDCARDS
import com.jetbrains.snakecharm.codeInsight.SnakemakeAPI.WILDCARDS_EXPANDING_SECTIONS_KEYWORDS
import com.jetbrains.snakecharm.lang.psi.BaseSmkSLReferenceExpression
import com.jetbrains.snakecharm.lang.psi.types.SmkWildcardsType
import com.jetbrains.snakecharm.stringLanguage.lang.SmkSLElementVisitor
import com.jetbrains.snakecharm.stringLanguage.lang.callSimpleName
import com.jetbrains.snakecharm.stringLanguage.lang.psi.references.SmkSLInitialReference
import com.jetbrains.snakecharm.stringLanguage.lang.psi.references.SmkSLWildcardReference

class SmkSLReferenceExpressionImpl(node: ASTNode) : PyReferenceExpressionImpl(node),
        BaseSmkSLReferenceExpression {

    override fun getNameIdentifier() = nameElement?.psi

    override fun getName() = referencedName

    override fun acceptPyVisitor(pyVisitor: PyElementVisitor) = when (pyVisitor) {
        is SmkSLElementVisitor -> pyVisitor.visitSmkSLReferenceExpression(this)
        else -> super.acceptPyVisitor(pyVisitor)
    }

    override fun getReference(context: PyResolveContext): PsiPolyVariantReference {
        val parentSection = containingSection()
        val parentDeclaration = parentSection?.getParentRuleOrCheckPoint()

        if (parentDeclaration != null && parentSection.sectionKeyword in WILDCARDS_EXPANDING_SECTIONS_KEYWORDS) {
            return if (!isQualified) {
                SmkSLWildcardReference(this, SmkWildcardsType(parentDeclaration))
            } else {
                SmkEmptyReference(this)
            }
        }

        if (qualifier != null) {
            return PyQualifiedReference(this, context)
        }

        return SmkSLInitialReference(this, parentDeclaration, context)
    }

    override fun getQualifier(): PyExpression? =
            children.firstOrNull { it is PyExpression } as PyExpression?

    override fun toString(): String {
        return "SmkSLReferenceExpressionImpl: " + this.referencedName
    }

    companion object {
        private fun BaseSmkSLReferenceExpression.isInValidCallExpression(): Boolean {
            val host = InjectedLanguageManager.getInstance(project).getInjectionHost(this)
            val callExpression = PsiTreeUtil.getParentOfType(host, PyCallExpression::class.java)
            return callExpression == null || callExpression.callSimpleName() !in FUNCTIONS_BANNED_FOR_WILDCARDS
        }

        fun isWildcard(expr: BaseSmkSLReferenceExpression) =
                PsiTreeUtil.getParentOfType(expr, BaseSmkSLReferenceExpression::class.java) == null &&
                        (expr.containingRuleOrCheckpointSection()?.isWildcardsExpandingSection() ?: false) &&
                        expr.text.isNotEmpty() &&
                        expr.isInValidCallExpression()
    }
}

class SmkEmptyReference<T: PsiElement>(element: T): PsiPolyVariantReferenceBase<T>(element) {
    override fun multiResolve(incompleteCode: Boolean) = ResolveResult.EMPTY_ARRAY
}