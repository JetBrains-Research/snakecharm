package com.jetbrains.snakecharm.lang.highlighter

import com.jetbrains.python.highlighting.PyHighlighter
import com.jetbrains.python.highlighting.PyHighlighter.PY_FUNC_DEFINITION
import com.jetbrains.snakecharm.lang.psi.*
import com.jetbrains.snakecharm.lang.validation.SnakemakeAnnotator

object SnakemakeSyntaxAnnotator: SnakemakeAnnotator() {
    override fun visitSMKRule(rule: SMKRule) {
        visitSMKRuleLike(rule)
    }

    override fun visitSMKCheckPoint(checkPoint: SMKCheckPoint) {
        visitSMKRuleLike(checkPoint)
    }

    private fun visitSMKRuleLike(ruleLike: SmkRuleLike) {
        val nameNode = ruleLike.getNameNode()
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

    override fun visitSMKWorkflowPythonBlockParameter(st: SMKWorkflowPythonBlockParameter) {
        val keywordNode = st.getKeywordNode()
        if (keywordNode != null) {
            addHighlightingAnnotation(keywordNode, PyHighlighter.PY_KEYWORD)
        }
    }

    override fun visitSMKWorkflowParameterListStatement(st: SMKWorkflowParameterListStatement) {
        val keywordNode = st.getKeywordNode()
        if (keywordNode != null) {
            addHighlightingAnnotation(keywordNode, PyHighlighter.PY_KEYWORD)
        }
    }

    override fun visitSMKWorkflowLocalRulesStatement(st: SMKWorkflowLocalRulesStatement) {
        val keywordNode = st.getKeywordNode()
        if (keywordNode != null) {
            addHighlightingAnnotation(keywordNode, PyHighlighter.PY_KEYWORD)
        }
    }

    override fun visitSMKWorkflowRuleOrderStatement(st: SMKWorkflowRulesOrderStatement) {
        val keywordNode = st.getKeywordNode()
        if (keywordNode != null) {
            addHighlightingAnnotation(keywordNode, PyHighlighter.PY_KEYWORD)
        }
    }
}