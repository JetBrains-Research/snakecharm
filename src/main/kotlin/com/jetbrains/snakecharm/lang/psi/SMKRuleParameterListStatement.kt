package com.jetbrains.snakecharm.lang.psi

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.jetbrains.python.psi.PyArgumentList
import com.jetbrains.python.psi.PyElementVisitor
import com.jetbrains.python.psi.PyStatement
import com.jetbrains.python.psi.impl.PyElementImpl
import com.jetbrains.snakecharm.inspections.SnakemakeInspectionVisitor
import com.jetbrains.snakecharm.lang.validation.SnakemakeAnnotator

class SMKRuleParameterListStatement(node: ASTNode): PyElementImpl(node), PyStatement, SMKRuleSection { // PyNamedElementContainer
    companion object {
        const val RESOURCES = "resources"
        const val PARAMS = "params"
        const val SHELL = "shell"
        const val SCRIPT = "script"
        const val WRAPPER = "wrapper"
        const val CWL = "cwl"
        const val SHADOW = "shadow"

        val EXECUTION_KEYWORDS = setOf(SHELL, SCRIPT, WRAPPER, CWL)

        val PARAMS_NAMES = setOf(
                "output", "input", PARAMS, "log", RESOURCES,
                "benchmark", "version", "message", SHELL, "threads", "singularity",
                "priority", "benchmark", "wildcard_constraints", "group", SHADOW,
                "conda", // >= 4.8
                SCRIPT, WRAPPER, CWL
        )
    }

    val section: PsiElement
        get() = firstChild

    val argumentList: PyArgumentList?
        get() = children.filter { it is PyArgumentList }.elementAtOrNull(0) as? PyArgumentList

    override fun getName() = getNameNode()?.text

    override fun acceptPyVisitor(pyVisitor: PyElementVisitor) {
        when (pyVisitor) {
            is SnakemakeAnnotator -> pyVisitor.visitSMKRuleParameterListStatement(this)
            is SnakemakeInspectionVisitor -> pyVisitor.visitSMKRuleParameterListStatement(this)
            else -> super.acceptPyVisitor(pyVisitor)
        }
    }

    fun getNameNode() = getIdentifierNode(node)
}