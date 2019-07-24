package com.jetbrains.snakecharm.lang.parser

import com.google.common.collect.ImmutableMap
import com.jetbrains.python.lexer.PythonIndentingLexer
import com.jetbrains.python.psi.PyElementType
import com.jetbrains.snakecharm.lang.SnakemakeNames

/**
 * @author Roman.Chernyatchik
 * @date 2018-12-31
 */
class SnakemakeLexer : PythonIndentingLexer() {
    companion object {
        val KEYWORDS = ImmutableMap.Builder<String, PyElementType>()
                .put(SnakemakeNames.RULE_KEYWORD, SnakemakeTokenTypes.RULE_KEYWORD)
                .put(SnakemakeNames.CHECKPOINT_KEYWORD, SnakemakeTokenTypes.CHECKPOINT_KEYWORD)
                .put(SnakemakeNames.WORKFLOW_CONFIGFILE_KEYWORD, SnakemakeTokenTypes.WORKFLOW_CONFIGFILE_KEYWORD)
                .put(SnakemakeNames.WORKFLOW_REPORT_KEYWORD, SnakemakeTokenTypes.WORKFLOW_REPORT_KEYWORD)
                .put(SnakemakeNames.WORKFLOW_WILDCARD_CONSTRAINTS_KEYWORD, SnakemakeTokenTypes.WORKFLOW_WILDCARD_CONSTRAINTS_KEYWORD)
                .put(SnakemakeNames.WORKFLOW_SINGULARITY_KEYWORD, SnakemakeTokenTypes.WORKFLOW_SINGULARITY_KEYWORD)
                .put(SnakemakeNames.WORKFLOW_INCLUDE_KEYWORD, SnakemakeTokenTypes.WORKFLOW_INCLUDE_KEYWORD)
                .put(SnakemakeNames.WORKFLOW_WORKDIR_KEYWORD, SnakemakeTokenTypes.WORKFLOW_WORKDIR_KEYWORD)
                .put(SnakemakeNames.WORKFLOW_LOCALRULES_KEYWORD, SnakemakeTokenTypes.WORKFLOW_LOCALRULES_KEYWORD)
                .put(SnakemakeNames.WORKFLOW_RULEORDER_KEYWORD, SnakemakeTokenTypes.WORKFLOW_RULEORDER_KEYWORD)
                .put(SnakemakeNames.WORKFLOW_ONSUCCESS_KEYWORD, SnakemakeTokenTypes.WORKFLOW_ONSUCCESS_KEYWORD)
                .put(SnakemakeNames.WORKFLOW_ONERROR_KEYWORD, SnakemakeTokenTypes.WORKFLOW_ONERROR_KEYWORD)
                .put(SnakemakeNames.WORKFLOW_ONSTART_KEYWORD, SnakemakeTokenTypes.WORKFLOW_ONSTART_KEYWORD)
                .put(SnakemakeNames.SUBWORKFLOW_KEYWORD, SnakemakeTokenTypes.SUBWORKFLOW_KEYWORD)
                .build()!!

        val KEYWORDS_2_TEXT = KEYWORDS.map { it.value to it.key }.toMap()
    }

    //override fun getTokenType() = KEYWORDS[tokenText] ?: super.getTokenType()
}