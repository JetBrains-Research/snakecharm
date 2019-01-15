package com.jetbrains.snakecharm.lang.validation

import com.jetbrains.python.validation.PyAnnotator
import com.jetbrains.snakecharm.lang.psi.SMKRule
import com.jetbrains.snakecharm.lang.psi.SMKRuleParameterListStatement
import com.jetbrains.snakecharm.lang.psi.SMKRuleRunParameter

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
}

