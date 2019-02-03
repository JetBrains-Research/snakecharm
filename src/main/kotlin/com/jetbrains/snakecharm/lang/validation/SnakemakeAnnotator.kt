package com.jetbrains.snakecharm.lang.validation

import com.jetbrains.python.validation.PyAnnotator
import com.jetbrains.snakecharm.lang.psi.*

/**
 * @author Roman.Chernyatchik
 * @date 2019-01-09
 */
abstract class SnakemakeAnnotator: PyAnnotator() {
    open fun visitSMKRule(smkRule: SMKRule) {
        super.visitPyElement(smkRule)
    }

    open fun visitSMKRuleParameterListStatement(st: SMKRuleParameterListStatement) {
        super.visitPyStatement(st)
    }

    open fun visitSMKRuleRunParameter(st: SMKRuleRunParameter) {
        super.visitPyStatementList(st.statementList)
    }

    open fun visitSMKWorkflowParameterListStatement(st: SMKWorkflowParameterListStatement) {
        super.visitPyStatement(st)
    }

    open fun visitSMKWorkflowPythonBlockParameter(st: SMKWorkflowPythonBlockParameter) {
        super.visitPyStatementList(st.statementList)
    }

    open fun visitSMKWorkflowRulesReorderStatement(st: SMKWorkflowRulesReorderStatement) {
        super.visitPyStatement(st)
    }

    open fun visitSMKWorkflowLocalRulesStatement(st: SMKWorkflowLocalRulesStatement) {
        super.visitPyStatement(st)
    }


}

