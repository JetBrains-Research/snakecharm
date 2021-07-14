package com.jetbrains.snakecharm.lang.psi

import com.jetbrains.python.psi.PyElementVisitor

/**
 * Implements Snakemake specific visit* methods and accepts [PyElementVisitor] for possible delegation to python
 * visitor. This impl is chosen because [PyElementVisitor] is abstract class, not interface. Also we'd like to
 * add snakemake visit* support into [com.jetbrains.snakecharm.lang.validation.SmkAnnotator]
 * and [com.jetbrains.snakecharm.inspections.SnakemakeInspectionVisitor] without copy-pasting implementation details
 * from [com.jetbrains.python.validation.PyAnnotator] and [com.jetbrains.python.inspections.PyInspectionVisitor]
 */
interface SmkElementVisitor {
    /**
     * Adapter instead of inheritance for to python specific methods
     */
    val pyElementVisitor: PyElementVisitor

    fun visitSmkRule(rule: SmkRule) {
        pyElementVisitor.visitPyElement(rule)
    }

    fun visitSmkCheckPoint(checkPoint: SmkCheckPoint) {
        pyElementVisitor.visitPyElement(checkPoint)
    }

    fun visitSmkSubworkflow(subworkflow: SmkSubworkflow) {
        pyElementVisitor.visitPyElement(subworkflow)
    }

    fun visitSmkModule(module: SmkModule){
        pyElementVisitor.visitPyElement(module)
    }

    fun visitSmkRuleOrCheckpointArgsSection(st: SmkRuleOrCheckpointArgsSection) {
        pyElementVisitor.visitPyStatement(st)
    }

    fun visitSmkRunSection(st: SmkRunSection) {
        pyElementVisitor.visitPyStatementList(st.statementList)
    }

    fun visitSmkSubworkflowArgsSection(st: SmkSubworkflowArgsSection) {
        pyElementVisitor.visitPyStatement(st)
    }

    fun visitSmkWorkflowArgsSection(st: SmkWorkflowArgsSection) {
        pyElementVisitor.visitPyStatement(st)
    }

    fun visitSmkWorkflowPythonBlockSection(st: SmkWorkflowPythonBlockSection) {
        pyElementVisitor.visitPyStatementList(st.statementList)
    }

    fun visitSmkWorkflowRuleorderSection(st: SmkWorkflowRuleorderSection) {
        pyElementVisitor.visitPyStatement(st)
    }

    fun visitSmkWorkflowLocalrulesSection(st: SmkWorkflowLocalrulesSection) {
        pyElementVisitor.visitPyStatement(st)
    }
}