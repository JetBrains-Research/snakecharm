package com.jetbrains.snakecharm.lang.highlighter

import com.jetbrains.python.highlighting.PyHighlighter
import com.jetbrains.python.highlighting.PyHighlighter.PY_FUNC_DEFINITION
import com.jetbrains.snakecharm.lang.psi.*
import com.jetbrains.snakecharm.lang.validation.SnakemakeAnnotator

object SnakemakeSyntaxAnnotator: SnakemakeAnnotator() {
    override fun visitSMKRule(rule: SmkRule) {
        visitSMKRuleLike(rule)
    }

    override fun visitSMKCheckPoint(checkPoint: SmkCheckPoint) {
        visitSMKRuleLike(checkPoint)
    }


    private fun visitSMKRuleLike(ruleLike: SmkRuleLike<SmkArgsSection>) {
        ruleLike.nameIdentifier?.let { nameElement ->
            addHighlightingAnnotation(nameElement, PY_FUNC_DEFINITION)
        }
    }

    override fun visitSMKSubworkflow(subworkflow: SmkSubworkflow) {
        subworkflow.nameIdentifier?.let { nameElement ->
            addHighlightingAnnotation(nameElement, PY_FUNC_DEFINITION)
        }
    }

    override fun visitSMKRuleParameterListStatement(st: SmkRuleArgsSection) {
        val nameNode = st.getSectionKeywordNode()
        if (nameNode != null) {
            addHighlightingAnnotation(nameNode, PyHighlighter.PY_DECORATOR)
        }
    }

    override fun visitSMKSubworkflowParameterListStatement(st: SmkSubworkflowArgsSection) {
        val nameNode = st.getSectionKeywordNode()
        if (nameNode != null) {
            addHighlightingAnnotation(nameNode, PyHighlighter.PY_DECORATOR)
        }
    }

    override fun visitSMKRuleRunParameter(st: SmkRunSection) {
        val nameNode = st.getSectionKeywordNode()
        if (nameNode != null) {
            addHighlightingAnnotation(nameNode, PyHighlighter.PY_PREDEFINED_DEFINITION)
        }
    }

    override fun visitSMKWorkflowPythonBlockParameter(st: SMKWorkflowPythonBlockParameter) {
        val keywordNode = st.getSectionKeywordNode()
        if (keywordNode != null) {
            addHighlightingAnnotation(keywordNode, PyHighlighter.PY_KEYWORD)
        }
    }

    override fun visitSMKWorkflowParameterListStatement(st: SmkWorkflowArgsSection) {
        val keywordNode = st.getSectionKeywordNode()
        if (keywordNode != null) {
            addHighlightingAnnotation(keywordNode, PyHighlighter.PY_KEYWORD)
        }
    }

    override fun visitSMKWorkflowLocalRulesStatement(st: SmkWorkflowLocalrulesSection) {
        val keywordNode = st.getSectionKeywordNode()
        if (keywordNode != null) {
            addHighlightingAnnotation(keywordNode, PyHighlighter.PY_KEYWORD)
        }
    }

    override fun visitSMKWorkflowRuleOrderStatement(st: SmkWorkflowRuleorderSection) {
        val keywordNode = st.getSectionKeywordNode()
        if (keywordNode != null) {
            addHighlightingAnnotation(keywordNode, PyHighlighter.PY_KEYWORD)
        }
    }
}