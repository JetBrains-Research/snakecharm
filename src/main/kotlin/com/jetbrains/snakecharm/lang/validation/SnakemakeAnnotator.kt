package com.jetbrains.snakecharm.lang.validation

import com.jetbrains.python.validation.PyAnnotator
import com.jetbrains.snakecharm.lang.psi.*

/**
 * @author Roman.Chernyatchik
 * @date 2019-01-09
 */
abstract class SnakemakeAnnotator: PyAnnotator(), SnakemakeVisitor {
    override fun visitSMKRule(smkRule: SMKRule) {
        super.visitPyElement(smkRule)
    }

    override fun visitSMKRuleParameterListStatement(st: SMKRuleParameterListStatement) {
        super.visitPyStatement(st)
    }

    override fun visitSMKRuleRunParameter(st: SMKRuleRunParameter) {
        super.visitPyStatementList(st.statementList)
    }

    override fun visitSMKWorkflowParameterListStatement(st: SMKWorkflowParameterListStatement) {
        super.visitPyStatement(st)
    }

    override fun visitSMKWorkflowPythonBlockParameter(st: SMKWorkflowPythonBlockParameter) {
        super.visitPyStatementList(st.statementList)
    }

    override fun visitSMKWorkflowRulesReorderStatement(st: SMKWorkflowRulesReorderStatement) {
        super.visitPyStatement(st)
    }

    override fun visitSMKWorkflowLocalRulesStatement(st: SMKWorkflowLocalRulesStatement) {
        super.visitPyStatement(st)
    }
}

