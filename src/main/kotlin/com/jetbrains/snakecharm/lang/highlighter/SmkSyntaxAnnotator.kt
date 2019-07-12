package com.jetbrains.snakecharm.lang.highlighter

import com.jetbrains.python.highlighting.PyHighlighter
import com.jetbrains.python.highlighting.PyHighlighter.PY_FUNC_DEFINITION
import com.jetbrains.snakecharm.lang.psi.*
import com.jetbrains.snakecharm.lang.validation.SmkAnnotator

object SmkSyntaxAnnotator: SmkAnnotator() {
    override fun visitSmkRule(rule: SmkRule) {
        visitSMKRuleLike(rule)
    }

    override fun visitSmkCheckPoint(checkPoint: SmkCheckPoint) {
        visitSMKRuleLike(checkPoint)
    }


    private fun visitSMKRuleLike(ruleLike: SmkRuleLike<SmkArgsSection>) {
        ruleLike.nameIdentifier?.let { nameElement ->
            addHighlightingAnnotation(nameElement, PY_FUNC_DEFINITION)
        }
    }

    override fun visitSmkSubworkflow(subworkflow: SmkSubworkflow) {
        subworkflow.nameIdentifier?.let { nameElement ->
            addHighlightingAnnotation(nameElement, PY_FUNC_DEFINITION)
        }
    }

    override fun visitSmkRuleOrCheckpointArgsSection(st: SmkRuleOrCheckpointArgsSection) {
        val nameNode = st.getSectionKeywordNode()
        if (nameNode != null) {
            addHighlightingAnnotation(nameNode, PyHighlighter.PY_DECORATOR)
        }
    }

    override fun visitSmkSubworkflowArgsSection(st: SmkSubworkflowArgsSection) {
        val nameNode = st.getSectionKeywordNode()
        if (nameNode != null) {
            addHighlightingAnnotation(nameNode, PyHighlighter.PY_DECORATOR)
        }
    }

    override fun visitSmkRunSection(st: SmkRunSection) {
        val nameNode = st.getSectionKeywordNode()
        if (nameNode != null) {
            addHighlightingAnnotation(nameNode, PyHighlighter.PY_PREDEFINED_DEFINITION)
        }
    }

    override fun visitSmkWorkflowPythonBlockSection(st: SmkWorkflowPythonBlockSection) {
        val keywordNode = st.getSectionKeywordNode()
        if (keywordNode != null) {
            addHighlightingAnnotation(keywordNode, PyHighlighter.PY_KEYWORD)
        }
    }

    override fun visitSmkWorkflowArgsSection(st: SmkWorkflowArgsSection) {
        val keywordNode = st.getSectionKeywordNode()
        if (keywordNode != null) {
            addHighlightingAnnotation(keywordNode, PyHighlighter.PY_KEYWORD)
        }
    }

    override fun visitSmkWorkflowLocalrulesSection(st: SmkWorkflowLocalrulesSection) {
        val keywordNode = st.getSectionKeywordNode()
        if (keywordNode != null) {
            addHighlightingAnnotation(keywordNode, PyHighlighter.PY_KEYWORD)
        }
    }

    override fun visitSmkWorkflowRuleorderSection(st: SmkWorkflowRuleorderSection) {
        val keywordNode = st.getSectionKeywordNode()
        if (keywordNode != null) {
            addHighlightingAnnotation(keywordNode, PyHighlighter.PY_KEYWORD)
        }
    }
}