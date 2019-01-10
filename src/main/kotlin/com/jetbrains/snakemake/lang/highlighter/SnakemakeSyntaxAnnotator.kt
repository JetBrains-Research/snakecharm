package com.jetbrains.snakemake.lang.highlighter

import com.jetbrains.python.highlighting.PyHighlighter
import com.jetbrains.python.highlighting.PyHighlighter.PY_FUNC_DEFINITION
import com.jetbrains.snakemake.lang.psi.SMKRule
import com.jetbrains.snakemake.lang.psi.SMKRuleParameterListStatement
import com.jetbrains.snakemake.lang.psi.SMKRuleRunParameter
import com.jetbrains.snakemake.lang.validation.SnakemakeAnnotator

object SnakemakeSyntaxAnnotator: SnakemakeAnnotator() {
    override fun visitSMKRule(smkRule: SMKRule) {
        val nameNode = smkRule.getNameNode()
        if (nameNode != null) {
            addHighlightingAnnotation(nameNode, PY_FUNC_DEFINITION)
        }
    }

    override fun visitSMKRuleParameterListStatement(st: SMKRuleParameterListStatement) {
        val nameNode = st.getNameNode()
        if (nameNode != null) {
            addHighlightingAnnotation(nameNode, PyHighlighter.PY_DECORATOR)
        }
    }

    override fun visitSMKRuleRunParameter(st: SMKRuleRunParameter) {
        val nameNode = st.getNameNode()
        if (nameNode != null) {
            addHighlightingAnnotation(nameNode, PyHighlighter.PY_PREDEFINED_DEFINITION)
        }
    }
}