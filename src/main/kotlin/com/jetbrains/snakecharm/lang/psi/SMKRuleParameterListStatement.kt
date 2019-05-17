package com.jetbrains.snakecharm.lang.psi

import com.intellij.lang.ASTNode
import com.jetbrains.python.psi.PyArgumentList
import com.jetbrains.python.psi.PyElementVisitor
import com.jetbrains.python.psi.PyStatement
import com.jetbrains.python.psi.impl.PyElementImpl
import com.jetbrains.snakecharm.inspections.SnakemakeInspectionVisitor
import com.jetbrains.snakecharm.lang.validation.SnakemakeAnnotator

class SMKRuleParameterListStatement(node: ASTNode): PyElementImpl(node), PyStatement { // PyNamedElementContainer
    companion object {
        val PARAMS_NAMES = setOf(
                "output", "input", "params", "log", "resources",
                "benchmark", "version", "message", "shell", "threads", "singularity",
                "priority", "benchmark", "wildcard_constraints", "group", "shadow",
                "conda", // >= 4.8
                "script", "wrapper", "cwl"
        )
    }

    val argumentList: PyArgumentList?
        get() = children.filter { it is PyArgumentList }.elementAtOrNull(0) as? PyArgumentList

    override fun acceptPyVisitor(pyVisitor: PyElementVisitor) {
        when (pyVisitor) {
            is SnakemakeAnnotator -> pyVisitor.visitSMKRuleParameterListStatement(this)
            is SnakemakeInspectionVisitor -> pyVisitor.visitSMKRuleParameterListStatement(this)
            else -> super.acceptPyVisitor(pyVisitor)
        }
    }

    fun getNameNode() = getIdentifierNode(node)
}