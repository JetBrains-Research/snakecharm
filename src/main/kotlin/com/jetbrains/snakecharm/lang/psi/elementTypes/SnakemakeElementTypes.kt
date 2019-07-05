package com.jetbrains.snakecharm.lang.psi.elementTypes

import com.jetbrains.python.psi.PyElementType
import com.jetbrains.snakecharm.lang.psi.*

/**
 * @author Roman.Chernyatchik
 * @date 2018-12-31
 */
object SnakemakeElementTypes {
    val RULE_DECLARATION = PyElementType("SMK_RULE_DECLARATION", SMKRule::class.java)
    val CHECKPOINT_DECLARATION = PyElementType("SMK_CHECKPOINT_DECLARATION", SMKCheckPoint::class.java)
    val SUBWORKFLOW_DECLARATION = PyElementType("SMK_SUBWORKFLOW_DECLARATION", SmkSubworkflow::class.java)

    val RULE_PARAMETER_LIST_STATEMENT = PyElementType(
            "SMK_RULE_PARAMETER_LIST_STATEMENT",
            SMKRuleParameterListStatement::class.java
    )

    val SUBWORKFLOW_PARAMETER_LIST_STATEMENT =  PyElementType(
            "SUBWORKFLOW_PARAMETER_LIST_STATEMENT",
            SMKSubworkflowParameterListStatement::class.java
    )

    val WORKFLOW_PARAMETER_LIST_STATEMENT = PyElementType(
            "SMK_WORKFLOW_PARAMETER_LIST_STATEMENT",
            SMKWorkflowParameterListStatement::class.java
    )

    val RULE_RUN_STATEMENT = PyElementType(
            "SMK_RULE_RUN_STATEMENT",
            SMKRuleRunParameter::class.java
    )

    val WORKFLOW_PYTHON_BLOCK_PARAMETER = PyElementType(
            "SMK_WORKFLOW_PYTHON_BLOCK_PARAMETER",
            SMKWorkflowPythonBlockParameter::class.java
    )

    val WORKFLOW_LOCALRULES_STATEMENT = PyElementType(
            "SMK_WORKFLOW_PYTHON_BLOCK_PARAMETER",
            SMKWorkflowLocalRulesStatement::class.java
    )

    val WORKFLOW_RULEORDER_STATEMENT = PyElementType(
            "SMK_WORKFLOW_RULESREORDER_STATEMENT",
            SMKWorkflowRuleOrderStatement::class.java
    )

    val RULE_ARGUMENT_IDENTIFIER = PyElementType("SMK_RULE_ARGUMENT_IDENTIFIER", SMKRuleIdentifierArgument::class.java)
}