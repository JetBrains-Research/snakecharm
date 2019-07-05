package com.jetbrains.snakecharm.lang.validation

import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.python.psi.PyElementVisitor
import com.jetbrains.python.psi.PyReturnStatement
import com.jetbrains.python.validation.ReturnAnnotator
import com.jetbrains.snakecharm.lang.psi.SMKElementVisitor
import com.jetbrains.snakecharm.lang.psi.SMKRuleRunParameter
import com.jetbrains.snakecharm.lang.psi.SMKWorkflowPythonBlockParameter


object SmkReturnAnnotator : ReturnAnnotator(), SMKElementVisitor {
    override val pyElementVisitor: PyElementVisitor
        get() = this

    override fun visitPyReturnStatement(node: PyReturnStatement) {
        PsiTreeUtil.getParentOfType(
                node, SMKRuleRunParameter::class.java, SMKWorkflowPythonBlockParameter::class.java
        ) ?: super.visitPyReturnStatement(node)
    }
}