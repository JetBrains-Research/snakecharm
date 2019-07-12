package com.jetbrains.snakecharm.lang.psi

import com.jetbrains.python.psi.PyElementVisitor

/**
 * Implements Snakemake specific visit* methods and accepts [PyElementVisitor] for possible delegation to python
 * visitor. This impl is chosen because [PyElementVisitor] is abstract class, not interface. Also we'd like to
 * add snakemake visit* support into [com.jetbrains.snakecharm.lang.validation.SnakemakeAnnotator]
 * and [com.jetbrains.snakecharm.inspections.SnakemakeInspectionVisitor] without copy-pasting implementation details
 * from [com.jetbrains.python.validation.PyAnnotator] and [com.jetbrains.python.inspections.PyInspectionVisitor]
 */
interface SMKElementVisitor {
    /**
     * Adapter instead of inheritance for to python specific methods
     */
    val pyElementVisitor: PyElementVisitor

    fun visitSMKRule(rule: SmkRule) {
        pyElementVisitor.visitPyElement(rule)
    }

    fun visitSMKCheckPoint(checkPoint: SmkCheckPoint) {
        pyElementVisitor.visitPyElement(checkPoint)
    }

    fun visitSMKSubworkflow(subworkflow: SmkSubworkflow) {
        pyElementVisitor.visitPyElement(subworkflow)
    }

    fun visitSMKRuleParameterListStatement(st: SmkRuleOrCheckpointArgsSection) {
        pyElementVisitor.visitPyStatement(st)
    }

    fun visitSMKRuleRunParameter(st: SmkRunSection) {
        pyElementVisitor.visitPyStatementList(st.statementList)
    }

    fun visitSMKSubworkflowParameterListStatement(st: SmkSubworkflowArgsSection) {
        pyElementVisitor.visitPyStatement(st)
    }

    fun visitSMKWorkflowParameterListStatement(st: SmkWorkflowArgsSection) {
        pyElementVisitor.visitPyStatement(st)
    }

    fun visitSMKWorkflowPythonBlockParameter(st: SMKWorkflowPythonBlockParameter) {
        pyElementVisitor.visitPyStatementList(st.statementList)
    }

    fun visitSMKWorkflowRuleOrderStatement(st: SmkWorkflowRuleorderSection) {
        pyElementVisitor.visitPyStatement(st)
    }

    fun visitSMKWorkflowLocalRulesStatement(st: SmkWorkflowLocalrulesSection) {
        pyElementVisitor.visitPyStatement(st)
    }
}