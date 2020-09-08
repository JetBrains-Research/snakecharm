package com.jetbrains.snakecharm.inspections

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import com.jetbrains.python.inspections.PyUnboundLocalVariableInspection
import com.jetbrains.python.psi.PyReferenceExpression
import com.jetbrains.snakecharm.lang.psi.SmkReferenceExpression

class SmkPyUnboundLocalVariableInspection: PyUnboundLocalVariableInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean, session: LocalInspectionToolSession): PsiElementVisitor {
        // Call super to init private functions feature
        super.buildVisitor(holder, isOnTheFly, session)

        return object : PyUnboundLocalVariableInspection.Visitor(holder, session) {
            override fun visitPyReferenceExpression(node: PyReferenceExpression) {
                if (!node.isValid) {
                    return
                }
                if (node is SmkReferenceExpression) {
                    // no error here, disable inspection FP, api required
                    // e.g. PyResolveUtil.allowForwardReferences(node)
                    return
                }
                return super.visitPyReferenceExpression(node)
            }
        }
    }
}