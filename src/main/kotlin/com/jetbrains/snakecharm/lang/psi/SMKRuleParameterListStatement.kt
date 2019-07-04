package com.jetbrains.snakecharm.lang.psi

import com.intellij.lang.ASTNode
import com.jetbrains.python.psi.PyElementVisitor

class SMKRuleParameterListStatement(node: ASTNode): SmkSectionStatement(node) { // PyNamedElementContainer
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

    override fun acceptPyVisitor(pyVisitor: PyElementVisitor) = when (pyVisitor) {
        is SMKElementVisitor -> pyVisitor.visitSMKRuleParameterListStatement(this)
        else -> super.acceptPyVisitor(pyVisitor)
    }
}