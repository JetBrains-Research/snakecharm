package com.jetbrains.snakecharm.lang.psi.elementTypes

import com.jetbrains.python.psi.PyElementType
import com.jetbrains.snakecharm.lang.psi.*
import com.jetbrains.snakecharm.lang.psi.impl.SmkReferenceExpressionImpl

/**
 * @author Roman.Chernyatchik
 * @date 2018-12-31
 */
object SnakemakeElementTypes {
    val RULE_PARAMETER_LIST_STATEMENT = PyElementType(
            "SMK_RULE_PARAMETER_LIST_STATEMENT",
            SmkRuleArgsSectionImpl::class.java
    )

    val SUBWORKFLOW_PARAMETER_LIST_STATEMENT =  PyElementType(
            "SUBWORKFLOW_PARAMETER_LIST_STATEMENT",
            SmkSubworkflowArgsSectionImpl::class.java
    )

    val WORKFLOW_PARAMETER_LIST_STATEMENT = PyElementType(
            "SMK_WORKFLOW_PARAMETER_LIST_STATEMENT",
            SmkWorkflowArgsSectionImpl::class.java
    )

    val RULE_RUN_STATEMENT = PyElementType(
            "SMK_RULE_RUN_STATEMENT",
            SmkRunSectionImpl::class.java
    )

    val WORKFLOW_PYTHON_BLOCK_PARAMETER = PyElementType(
            "SMK_WORKFLOW_PYTHON_BLOCK_PARAMETER",
            SMKWorkflowPythonBlockParameterImpl::class.java
    )

    val WORKFLOW_LOCALRULES_STATEMENT = PyElementType(
            "SMK_WORKFLOW_PYTHON_BLOCK_PARAMETER",
            SmkWorkflowLocalrulesSectionImpl::class.java
    )

    val WORKFLOW_RULEORDER_STATEMENT = PyElementType(
            "SMK_WORKFLOW_RULESREORDER_STATEMENT",
            SmkWorkflowRuleorderSectionImpl::class.java
    )

    val REFERENCE_EXPRESSION = PyElementType(
            "SMK_REFERENCE_EXPRESSION",
            SmkReferenceExpressionImpl::class.java
    )

}