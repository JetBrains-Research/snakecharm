package com.jetbrains.snakecharm.lang.parser

import com.intellij.psi.tree.TokenSet
import com.jetbrains.python.PyTokenTypes
import com.jetbrains.python.psi.PyElementType

/**
 * @author Roman.Chernyatchik
 * @date 2018-12-31
 */
object SmkTokenTypes {
    val RULE_KEYWORD = PyElementType("RULE_KEYWORD") // rule
    val SUBWORKFLOW_KEYWORD = PyElementType("SUBWORKFLOW_KEYWORD")
    val CHECKPOINT_KEYWORD = PyElementType("CHECKPOINT_KEYWORD") // rule
    val MODULE_KEYWORD = PyElementType("MODULE_KEYWORD")
    val USE_KEYWORD = PyElementType("USE_KEYWORD")

    val SMK_FROM_KEYWORD = PyElementType("SMK_FROM_KEYWORD")
    val SMK_AS_KEYWORD = PyElementType("SMK_AS_KEYWORD")
    val SMK_WITH_KEYWORD = PyElementType("SMK_WITH_KEYWORD")

    val WORKFLOW_TOPLEVEL_ARGS_SECTION_STATEMENT = PyElementType("WORKFLOW_TOPLEVEL_ARGS_SECTION_STATEMENT")

    val WORKFLOW_LOCALRULES_KEYWORD = PyElementType("WORKFLOW_LOCALRULES_KEYWORD")
    val WORKFLOW_RULEORDER_KEYWORD = PyElementType("WORKFLOW_RULEORDER_KEYWORD")

    val WORKFLOW_ONSUCCESS_KEYWORD = PyElementType("WORKFLOW_ONSUCCESS_KEYWORD")
    val WORKFLOW_ONERROR_KEYWORD = PyElementType("WORKFLOW_ONERROR_KEYWORD")
    val WORKFLOW_ONSTART_KEYWORD = PyElementType("WORKFLOW_ONSTART_KEYWORD")
    val WORKFLOW_TOPLEVEL_PYTHON_BLOCK_PARAMETER_KEYWORDS = TokenSet.create(
        WORKFLOW_ONSUCCESS_KEYWORD, WORKFLOW_ONERROR_KEYWORD, WORKFLOW_ONSTART_KEYWORD
    )

    val WORKFLOW_TOPLEVEL_DECORATORS_WO_RULE_LIKE = TokenSet.orSet(
        WORKFLOW_TOPLEVEL_PYTHON_BLOCK_PARAMETER_KEYWORDS,
        TokenSet.create(
            WORKFLOW_LOCALRULES_KEYWORD, WORKFLOW_RULEORDER_KEYWORD
        )
    )

    val RULE_OR_CHECKPOINT = TokenSet.create(
        RULE_KEYWORD, CHECKPOINT_KEYWORD
    )

    val RULE_LIKE = TokenSet.orSet(
        RULE_OR_CHECKPOINT, TokenSet.create(SUBWORKFLOW_KEYWORD, MODULE_KEYWORD, USE_KEYWORD)
    )

    val WORKFLOW_TOPLEVEL_DECORATORS = TokenSet.orSet(
        TokenSet.create(WORKFLOW_TOPLEVEL_ARGS_SECTION_STATEMENT),
        WORKFLOW_TOPLEVEL_DECORATORS_WO_RULE_LIKE,
        RULE_LIKE
    )

    /**
     * Parser converts all [WORKFLOW_TOPLEVEL_DECORATORS] tokens to identifiers if token doesn't
     * start expression mentioned here.
     *
     * P.S: This behaviour differs from snakemake runtime and not all valid smk code will be parsed
     * by our parser, but difference seems only in some `synthetic` cases which likely don't not happen
     * in real life examples.
     *
     * Alternative is to do lookup and look for ':' after most of keywords + some workaround for rule/checkpoint/
     * subworkflow/etc. Or look how it is made in snakemake
     */
    val PY_EXPRESSIONS_ALLOWING_SNAKEMAKE_KEYWORDS = TokenSet.create(
        // in if .. elif .. else ..
        PyTokenTypes.IF_KEYWORD, PyTokenTypes.ELIF_KEYWORD, PyTokenTypes.ELSE_KEYWORD,
        // in try .. except ..
        PyTokenTypes.TRY_KEYWORD, PyTokenTypes.EXCEPT_KEYWORD,
        // in method body
        PyTokenTypes.DEF_KEYWORD
    )
}