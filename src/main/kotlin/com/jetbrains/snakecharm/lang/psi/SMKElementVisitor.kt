package com.jetbrains.snakecharm.lang.psi

import com.jetbrains.python.psi.PyElementVisitor

interface SMKElementVisitor {
    val pyElementVisitor: PyElementVisitor

    fun visitSMKRule(smkRule: SMKRule) {
        pyElementVisitor.visitPyElement(smkRule)
    }

    fun visitSMKRuleParameterListStatement(st: SMKRuleParameterListStatement) {
        pyElementVisitor.visitPyStatement(st)
    }

    fun visitSMKRuleRunParameter(st: SMKRuleRunParameter) {
        pyElementVisitor.visitPyStatementList(st.statementList)
    }

    fun visitSMKWorkflowParameterListStatement(st: SMKWorkflowParameterListStatement) {
        pyElementVisitor.visitPyStatement(st)
    }

    fun visitSMKWorkflowPythonBlockParameter(st: SMKWorkflowPythonBlockParameter) {
        pyElementVisitor.visitPyStatementList(st.statementList)
    }

    fun visitSMKWorkflowRulesReorderStatement(st: SMKWorkflowRulesReorderStatement) {
        pyElementVisitor.visitPyStatement(st)
    }

    fun visitSMKWorkflowLocalRulesStatement(st: SMKWorkflowLocalRulesStatement) {
        pyElementVisitor.visitPyStatement(st)
    }
}