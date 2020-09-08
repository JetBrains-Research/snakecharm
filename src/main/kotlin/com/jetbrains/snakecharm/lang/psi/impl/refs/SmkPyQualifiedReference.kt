package com.jetbrains.snakecharm.lang.psi.impl.refs

import com.intellij.lang.annotation.HighlightSeverity
import com.jetbrains.python.psi.PyQualifiedExpression
import com.jetbrains.python.psi.impl.references.PyQualifiedReference
import com.jetbrains.python.psi.resolve.PyResolveContext
import com.jetbrains.python.psi.types.TypeEvalContext
import com.jetbrains.snakecharm.lang.psi.types.AbstractSmkRuleOrCheckpointType

class SmkPyQualifiedReference(
        element: PyQualifiedExpression,
        context: PyResolveContext
) : PyQualifiedReference(element, context) {
    override fun getUnresolvedHighlightSeverity(context: TypeEvalContext): HighlightSeverity? {
        val qualifier = this.myElement.qualifier
        if (qualifier != null) {
            val type = context.getType(qualifier)
            if (type is AbstractSmkRuleOrCheckpointType<*>) {
                return HighlightSeverity.ERROR
            }
        }
        return super.getUnresolvedHighlightSeverity(context)
    }
}