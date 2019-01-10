package com.jetbrains.snakemake.lang.psi

import com.intellij.lang.ASTNode
import com.intellij.psi.TokenType
import com.jetbrains.python.PythonDialectsTokenSetProvider
import com.jetbrains.python.psi.PyElementVisitor
import com.jetbrains.python.psi.PyStatement
import com.jetbrains.python.psi.impl.PyElementImpl
import com.jetbrains.snakemake.lang.parser.SnakemakeTokenTypes
import com.jetbrains.snakemake.lang.validation.SnakemakeAnnotator

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

    override fun acceptPyVisitor(pyVisitor: PyElementVisitor) {
        if (pyVisitor is SnakemakeAnnotator) {
            pyVisitor.visitSMKRuleParameterListStatement(this)
        } else {
            super.acceptPyVisitor(pyVisitor)
        }
    }

    fun getNameNode(): ASTNode? {
        var id = node.findChildByType(SnakemakeTokenTypes.RULE_PARAM_IDENTIFIER_LIKE)
        if (id == null) {
            val error = node.findChildByType(TokenType.ERROR_ELEMENT)
            if (error != null) {
                // TODO: do we need this? it is like in PyFunction
                id = error.findChildByType(PythonDialectsTokenSetProvider.INSTANCE.keywordTokens)
            }
        }
        return id
    }

}