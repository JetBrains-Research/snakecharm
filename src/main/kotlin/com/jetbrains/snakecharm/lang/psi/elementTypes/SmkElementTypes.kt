package com.jetbrains.snakecharm.lang.psi.elementTypes

import com.intellij.psi.tree.TokenSet
import com.jetbrains.python.psi.PyElementType
import com.jetbrains.snakecharm.lang.psi.SmkImportedRulesNamesList
import com.jetbrains.snakecharm.lang.psi.impl.*

/**
 * @author Roman.Chernyatchik
 * @date 2018-12-31
 */
object SmkElementTypes {
    val RULE_OR_CHECKPOINT_ARGS_SECTION_STATEMENT = PyElementType(
        "SMK_RULE_OR_CHECKPOINT_ARGS_SECTION_STATEMENT"
    ) {
        SmkRuleOrCheckpointArgsSectionImpl(it)
    }

    val SUBWORKFLOW_ARGS_SECTION_STATEMENT = PyElementType(
        "SMK_SUBWORKFLOW_ARGS_SECTION_STATEMENT"
    ) {
        SmkSubworkflowArgsSectionImpl(it)
    }

    val MODULE_ARGS_SECTION_STATEMENT = PyElementType(
        "SMK_MODULE_ARGS_SECTION_STATEMENT"
    ) {
        SmkModuleArgsSectionImpl(it)
    }

    val USE_ARGS_SECTION_STATEMENT = PyElementType(
        "SMK_USE_ARGS_SECTION_STATEMENT"
    ) {
        SmkUseArgsSectionImpl(it)
    }

    val USE_NEW_NAME_PATTERN = PyElementType(
        "SMK_USE_NEW_NAME_PATTERN"
    ) {
        SmkUseNewNamePatternImpl(it)
    }

    val USE_IMPORTED_RULES_NAMES = PyElementType(
        "USE_IMPORTED_RULES_NAMES"
    ) {
        SmkImportedRulesNamesList(it)
    }

    val USE_EXCLUDE_RULES_NAMES_STATEMENT = PyElementType(
        "USE_EXCLUDE_RULES_NAMES_STATEMENT"
    ) {
        SmkExcludedRulesNamesListImpl(it)
    }

    val WORKFLOW_ARGS_SECTION_STATEMENT = PyElementType(
        "SMK_WORKFLOW_ARGS_SECTION_STATEMENT"
    ) {
        SmkWorkflowArgsSectionImpl(it)
    }

    val RULE_OR_CHECKPOINT_RUN_SECTION_STATEMENT = PyElementType(
        "SMK_RULE_OR_CHECKPOINT_RUN_SECTION_STATEMENT"
    ) {
        SmkRunSectionImpl(it)
    }

    val WORKFLOW_PY_BLOCK_SECTION_STATEMENT = PyElementType(
        "SMK_WORKFLOW_PY_BLOCK_SECTION_STATEMENT"
    ) {
        SmkWorkflowPythonBlockSectionImpl(it)
    }

    val WORKFLOW_LOCALRULES_SECTION_STATEMENT = PyElementType(
        "SMK_WORKFLOW_LOCALRULES_SECTION_STATEMENT"
    ) {
        SmkWorkflowLocalrulesSectionImpl(it)
    }

    val WORKFLOW_RULEORDER_SECTION_STATEMENT = PyElementType(
        "SMK_WORKFLOW_RULEORDER_SECTION_STATEMENT"
    ) {
        SmkWorkflowRuleorderSectionImpl(it)
    }

    val REFERENCE_EXPRESSION = PyElementType(
        "SMK_REFERENCE_EXPRESSION"
    ) {
        SmkReferenceExpressionImpl(it)
    }

    val SMK_PY_REFERENCE_EXPRESSION = PyElementType(
        "SMK_PY_REFERENCE_EXPRESSION"
    ) {
        SmkPyReferenceExpressionImpl(it)
    }

    val RULE_LIKE_STATEMENTS = TokenSet.create(
        SmkStubElementTypes.RULE_DECLARATION_STATEMENT,
        SmkStubElementTypes.CHECKPOINT_DECLARATION_STATEMENT,
        SmkStubElementTypes.SUBWORKFLOW_DECLARATION_STATEMENT,
        SmkStubElementTypes.MODULE_DECLARATION_STATEMENT,
        SmkStubElementTypes.USE_DECLARATION_STATEMENT
    )
}