package com.jetbrains.snakecharm.string_language.lang.psi

import com.intellij.lang.ASTNode
import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.psi.PsiPolyVariantReference
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.python.psi.PyExpression
import com.jetbrains.python.psi.impl.PyReferenceExpressionImpl
import com.jetbrains.python.psi.impl.references.PyQualifiedReference
import com.jetbrains.python.psi.resolve.PyResolveContext
import com.jetbrains.snakecharm.lang.psi.SmkRuleOrCheckpoint

class SmkSLReferenceExpression(node: ASTNode) : PyReferenceExpressionImpl(node) {
    override fun getReference(context: PyResolveContext): PsiPolyVariantReference {
        if (qualifier == null) {
            val languageManager = InjectedLanguageManager.getInstance(project)
            val host = languageManager.getInjectionHost(this)
            val parentDeclaration =
                    PsiTreeUtil.getParentOfType(host, SmkRuleOrCheckpoint::class.java)

            return SmkSLInitialReference(this, parentDeclaration, context)
        }

        return PyQualifiedReference(this, context)
    }

    override fun getQualifier(): PyExpression? =
            children.firstOrNull { it is SmkSLReferenceExpression } as PyExpression?
}
