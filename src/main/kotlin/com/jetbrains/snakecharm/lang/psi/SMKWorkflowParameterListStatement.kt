package com.jetbrains.snakecharm.lang.psi

import com.intellij.lang.ASTNode
import com.jetbrains.python.psi.PyElementVisitor
import com.jetbrains.python.psi.PyStatement
import com.jetbrains.python.psi.impl.PyElementImpl
import com.jetbrains.snakecharm.lang.parser.SnakemakeTokenTypes
import com.jetbrains.snakecharm.lang.validation.SnakemakeAnnotator

/**
 * @author Roman.Chernyatchik
 * @date 2019-02-03
 */
class SMKWorkflowParameterListStatement(node: ASTNode) : PyElementImpl(node), PyStatement { // PyNamedElementContainer
    override fun acceptPyVisitor(pyVisitor: PyElementVisitor) {
        if (pyVisitor is SnakemakeAnnotator) {
            pyVisitor.visitSMKWorkflowParameterListStatement(this)
        } else {
            super.acceptPyVisitor(pyVisitor)
        }
    }

    fun getNameNode(): ASTNode? {
        val id = node.findChildByType(SnakemakeTokenTypes.WORKFLOW_TOPLEVEL_PARAMLISTS_DECORATORS)
//        if (id == null) {
//            val error = node.findChildByType(TokenType.ERROR_ELEMENT)
//            if (error != null) {
//                // TODO: do we need this? it is like in PyFunction
//                id = error.findChildByType(PythonDialectsTokenSetProvider.INSTANCE.keywordTokens)
//            }
//        }
        return id
    }
}