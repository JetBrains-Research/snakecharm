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

    val RULE_PARAMETER_LIST_STATEMENT = PyElementType(
            "SMK_RULE_PARAMETER_LIST_STATEMENT",
            SMKRuleParameterListStatement::class.java
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

    val WORKFLOW_RULESREORDER_STATEMENT = PyElementType(
            "SMK_WORKFLOW_RULESREORDER_STATEMENT",
            SMKWorkflowRulesReorderStatement::class.java
    )
}