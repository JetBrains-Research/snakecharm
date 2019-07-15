package com.jetbrains.snakecharm.lang.highlighter

import com.jetbrains.python.highlighting.PyHighlighter
import com.jetbrains.python.highlighting.PyHighlighter.PY_FUNC_DEFINITION
import com.jetbrains.snakecharm.lang.psi.*
import com.jetbrains.snakecharm.lang.validation.SmkAnnotator

object SmkSyntaxAnnotator: SmkAnnotator() {
    override fun visitSmkRule(rule: SmkRule) {
        highlightSMKRuleLike(rule)
    }

    override fun visitSmkCheckPoint(checkPoint: SmkCheckPoint) {
        highlightSMKRuleLike(checkPoint)
    }

    override fun visitSmkSubworkflow(subworkflow: SmkSubworkflow) {
        highlightSMKRuleLike(subworkflow)
    }

    override fun visitSmkRuleOrCheckpointArgsSection(st: SmkRuleOrCheckpointArgsSection) {
        highlightRuleLikeSection(st)
    }

    override fun visitSmkSubworkflowArgsSection(st: SmkSubworkflowArgsSection) {
        highlightRuleLikeSection(st)
    }

    override fun visitSmkRunSection(st: SmkRunSection) {
        st.getSectionKeywordNode()?.let {
            addHighlightingAnnotation(it, PyHighlighter.PY_PREDEFINED_DEFINITION)
        }
    }

    override fun visitSmkWorkflowPythonBlockSection(st: SmkWorkflowPythonBlockSection) {
        highlightWorkflowSection(st)
    }

    override fun visitSmkWorkflowArgsSection(st: SmkWorkflowArgsSection) {
        highlightWorkflowSection(st)
    }

    override fun visitSmkWorkflowLocalrulesSection(st: SmkWorkflowLocalrulesSection) {
        highlightWorkflowSection(st)
    }

    override fun visitSmkWorkflowRuleorderSection(st: SmkWorkflowRuleorderSection) {
        highlightWorkflowSection(st)
    }

    private fun highlightSMKRuleLike(ruleLike: SmkRuleLike<SmkArgsSection>) {
        highlightWorkflowSection(ruleLike)

        ruleLike.nameIdentifier?.let { nameElement ->
            addHighlightingAnnotation(nameElement, PY_FUNC_DEFINITION)
        }
    }

    private fun highlightWorkflowSection(st: SmkSection) {
        st.getSectionKeywordNode()?.let {
            addHighlightingAnnotation(it, PyHighlighter.PY_KEYWORD)
        }
    }

    private fun highlightRuleLikeSection(st: SmkSection) {
        st.getSectionKeywordNode()?.let {
            addHighlightingAnnotation(it, PyHighlighter.PY_DECORATOR)
        }
    }
}