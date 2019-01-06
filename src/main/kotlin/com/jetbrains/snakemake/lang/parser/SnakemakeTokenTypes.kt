package com.jetbrains.snakemake.lang.parser

import com.intellij.psi.tree.TokenSet
import com.jetbrains.python.PyTokenTypes
import com.jetbrains.python.psi.PyElementType

/**
 * @author Roman.Chernyatchik
 * @date 2018-12-31
 */
object SnakemakeTokenTypes {
    val RULE_KEYWORD = PyElementType("RULE_KEYWORD") // rule
    val CHECKPOINT_KEYWORD = PyElementType("CHECKPOINT_KEYWORD") // rule

    val WORKFLOW_CONFIGFILE = PyElementType("WORKFLOW_CONFIGFILE")
    val WORKFLOW_REPORT = PyElementType("WORKFLOW_REPORT")
    val WORKFLOW_WILDCARD_CONSTRAINTS = PyElementType("WORKFLOW_WILDCARD_CONSTRAINTS")
    val WORKFLOW_SINGULARITY = PyElementType("WORKFLOW_SINGULARITY")
    val WORKFLOW_INCLUDE = PyElementType("WORKFLOW_INCLUDE")
    val WORKFLOW_WORKDIR = PyElementType("WORKFLOW_WORKDIR")

    val WORKFLOW_TOPLEVEL_PARAMLISTS_DECORATORS = TokenSet.create(
            WORKFLOW_CONFIGFILE, WORKFLOW_REPORT, WORKFLOW_WILDCARD_CONSTRAINTS, WORKFLOW_SINGULARITY,
            WORKFLOW_INCLUDE, WORKFLOW_WORKDIR
    )

    val WORKFLOW_LOCALRULES = PyElementType("WORKFLOW_LOCALRULES")
    val WORKFLOW_RULEORDER = PyElementType("WORKFLOW_RULEORDER")

    val WORKFLOW_ONSUCCESS = PyElementType("WORKFLOW_ONSUCCESS")
    val WORKFLOW_ONERROR = PyElementType("WORKFLOW_ONERROR")
    val WORKFLOW_ONSTART = PyElementType("WORKFLOW_ONSTART")
    val WORKFLOW_TOPLEVEL_PYTHON_BLOCK_PARAMETER = TokenSet.create(
            WORKFLOW_ONSUCCESS, WORKFLOW_ONERROR, WORKFLOW_ONSTART
    )

    val WORKFLOW_TOPLEVEL_DECORATORS = TokenSet.orSet(
            WORKFLOW_TOPLEVEL_PARAMLISTS_DECORATORS,
            WORKFLOW_TOPLEVEL_PYTHON_BLOCK_PARAMETER,
            TokenSet.create(
                    RULE_KEYWORD, CHECKPOINT_KEYWORD,
                    WORKFLOW_LOCALRULES, WORKFLOW_RULEORDER
            ))

    val RULE_PARAM_IDENTIFIER_LIKE = TokenSet.create(
            WORKFLOW_SINGULARITY, WORKFLOW_WILDCARD_CONSTRAINTS, PyTokenTypes.IDENTIFIER
    )
}