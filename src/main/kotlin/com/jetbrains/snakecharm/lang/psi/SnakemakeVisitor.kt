package com.jetbrains.snakecharm.lang.psi

interface SnakemakeVisitor {
    fun visitSMKRule(smkRule: SMKRule) {}

    fun visitSMKRuleParameterListStatement(st: SMKRuleParameterListStatement) {}

    fun visitSMKRuleRunParameter(st: SMKRuleRunParameter) {}

    fun visitSMKWorkflowParameterListStatement(st: SMKWorkflowParameterListStatement) {}

    fun visitSMKWorkflowPythonBlockParameter(st: SMKWorkflowPythonBlockParameter) {}

    fun visitSMKWorkflowRulesReorderStatement(st: SMKWorkflowRulesReorderStatement) {}

    fun visitSMKWorkflowLocalRulesStatement(st: SMKWorkflowLocalRulesStatement) {}
}