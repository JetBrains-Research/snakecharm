package com.jetbrains.snakecharm.lang.psi.elementTypes

import com.jetbrains.python.psi.PyElementType
import com.jetbrains.snakecharm.lang.psi.impl.*

/**
 * @author Roman.Chernyatchik
 * @date 2018-12-31
 */
object SnakemakeElementTypes {
    val RULE_OR_CHECKPOINT_ARGS_SECTION_STATEMENT = PyElementType(
            "SMK_RULE_OR_CHECKPOINT_ARGS_SECTION_STATEMENT",
            SmkRuleOrCheckpointArgsSectionImpl::class.java
    )

    val SUBWORKFLOW_ARGS_SECTION_STATEMENT =  PyElementType(
            "SMK_SUBWORKFLOW_ARGS_SECTION_STATEMENT",
            SmkSubworkflowArgsSectionImpl::class.java
    )

    val WORKFLOW_ARGS_SECTION_STATEMENT = PyElementType(
            "SMK_WORKFLOW_ARGS_SECTION_STATEMENT",
            SmkWorkflowArgsSectionImpl::class.java
    )

    val RULE_OR_CHECKPOINT_RUN_SECTION_STATEMENT = PyElementType(
            "SMK_RULE_OR_CHECKPOINT_RUN_SECTION_STATEMENT",
            SmkRunSectionImpl::class.java
    )

    val WORKFLOW_PY_BLOCK_SECTION_STATEMENT = PyElementType(
            "SMK_WORKFLOW_PY_BLOCK_SECTION_STATEMENT",
            SmkWorkflowPythonBlockSectionImpl::class.java
    )

    val WORKFLOW_LOCALRULES_SECTION_STATEMENT = PyElementType(
            "SMK_WORKFLOW_LOCALRULES_SECTION_STATEMENT",
            SmkWorkflowLocalrulesSectionImpl::class.java
    )

    val WORKFLOW_RULEORDER_SECTION_STATEMENT = PyElementType(
            "SMK_WORKFLOW_RULEORDER_SECTION_STATEMENT",
            SmkWorkflowRuleorderSectionImpl::class.java
    )

    val REFERENCE_EXPRESSION = PyElementType(
            "SMK_REFERENCE_EXPRESSION",
            SmkReferenceExpressionImpl::class.java
    )
}