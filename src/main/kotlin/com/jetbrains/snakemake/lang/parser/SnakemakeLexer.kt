package com.jetbrains.snakemake.lang.parser

import com.google.common.collect.ImmutableMap
import com.jetbrains.python.lexer.PythonIndentingLexer
import com.jetbrains.python.psi.PyElementType
import com.jetbrains.snakemake.lang.SnakemakeNames

/**
 * @author Roman.Chernyatchik
 * @date 2018-12-31
 */
class SnakemakeLexer : PythonIndentingLexer() {
    companion object {
        val KEYWORDS = ImmutableMap.Builder<String, PyElementType>()
                .put(SnakemakeNames.RULE_KEYWORD, SnakemakeTokenTypes.RULE_KEYWORD)
                .put(SnakemakeNames.CHECKPOINT_KEYWORD, SnakemakeTokenTypes.CHECKPOINT_KEYWORD)
                .put(SnakemakeNames.WORKFLOW_CONFIGFILE, SnakemakeTokenTypes.WORKFLOW_CONFIGFILE)
                .put(SnakemakeNames.WORKFLOW_REPORT, SnakemakeTokenTypes.WORKFLOW_REPORT)
                .put(SnakemakeNames.WORKFLOW_WILDCARD_CONSTRAINTS, SnakemakeTokenTypes.WORKFLOW_WILDCARD_CONSTRAINTS)
                .put(SnakemakeNames.WORKFLOW_SINGULARITY, SnakemakeTokenTypes.WORKFLOW_SINGULARITY)
                .put(SnakemakeNames.WORKFLOW_INCLUDE, SnakemakeTokenTypes.WORKFLOW_INCLUDE)
                .put(SnakemakeNames.WORKFLOW_WORKDIR, SnakemakeTokenTypes.WORKFLOW_WORKDIR)
                .put(SnakemakeNames.WORKFLOW_LOCALRULES, SnakemakeTokenTypes.WORKFLOW_LOCALRULES)
                .put(SnakemakeNames.WORKFLOW_RULEORDER, SnakemakeTokenTypes.WORKFLOW_RULEORDER)
                .put(SnakemakeNames.WORKFLOW_ONSUCCESS, SnakemakeTokenTypes.WORKFLOW_ONSUCCESS)
                .put(SnakemakeNames.WORKFLOW_ONERROR, SnakemakeTokenTypes.WORKFLOW_ONERROR)
                .put(SnakemakeNames.WORKFLOW_ONSTART, SnakemakeTokenTypes.WORKFLOW_ONSTART)
                .build()!!
    }

    override fun getTokenType() = KEYWORDS[tokenText] ?: super.getTokenType()
}