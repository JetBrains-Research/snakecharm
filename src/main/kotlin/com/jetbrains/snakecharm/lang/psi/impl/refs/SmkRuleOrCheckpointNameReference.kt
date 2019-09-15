package com.jetbrains.snakecharm.lang.psi.impl.refs

import com.intellij.lang.annotation.HighlightSeverity
import com.jetbrains.python.psi.AccessDirection
import com.jetbrains.python.psi.impl.references.PyReferenceImpl
import com.jetbrains.python.psi.resolve.PyResolveContext
import com.jetbrains.python.psi.resolve.RatedResolveResult
import com.jetbrains.python.psi.types.TypeEvalContext
import com.jetbrains.snakecharm.SnakemakeBundle
import com.jetbrains.snakecharm.lang.psi.SmkFile
import com.jetbrains.snakecharm.lang.psi.SmkReferenceExpression
import com.jetbrains.snakecharm.lang.psi.types.SmkCheckpointType
import com.jetbrains.snakecharm.lang.psi.types.SmkRulesType

class SmkRuleOrCheckpointNameReference(
        element: SmkReferenceExpression,
        context: PyResolveContext
) : PyReferenceImpl(element, context) { //.Poly<SmkReferenceExpression>(element, textRange, true), PsiReferenceEx {
    override fun getElement() = super.getElement() as SmkReferenceExpression

    /**
     *  In case of undefined name after 'rules.' or 'checkpoints.'
     *  Severity is handled by [SmkPyQualifiedReference]
     */
    override fun getUnresolvedDescription() = SnakemakeBundle.message(
            "INSP.py.unresolved.ref.rule.like.ref.message", element.name ?: "<unnamed>"
    )


    override fun getUnresolvedHighlightSeverity(typeEvalContext: TypeEvalContext?) = HighlightSeverity.ERROR!!


    override fun resolveInner(): List<RatedResolveResult> {
        val smkFile = element.containingFile
        if (smkFile !is SmkFile) {
            return emptyList()
        }

        val name = element.text
        val results = arrayListOf<RatedResolveResult>()
        val ctx = AccessDirection.of(this.myElement)

        results.addAll(SmkRulesType(null, smkFile).resolveMember(name, element, ctx, myContext))
        results.addAll(SmkCheckpointType(null, smkFile).resolveMember(name, element, ctx, myContext))

        return results
    }
}