package com.jetbrains.snakecharm.lang.highlighter

import com.jetbrains.python.PyTokenTypes
import com.jetbrains.python.highlighting.PyHighlighter
import com.jetbrains.python.highlighting.PyHighlighter.PY_FUNC_DEFINITION
import com.jetbrains.snakecharm.lang.parser.SmkTokenTypes
import com.jetbrains.snakecharm.lang.psi.*
import com.jetbrains.snakecharm.lang.psi.elementTypes.SmkElementTypes
import com.jetbrains.snakecharm.lang.validation.SmkAnnotator

object SmkSyntaxAnnotator : SmkAnnotator() {
    override fun visitSmkRule(rule: SmkRule) {
        highlightSMKRuleLike(rule)
    }

    override fun visitSmkCheckPoint(checkPoint: SmkCheckPoint) {
        highlightSMKRuleLike(checkPoint)
    }

    override fun visitSmkSubworkflow(subworkflow: SmkSubworkflow) {
        highlightSMKRuleLike(subworkflow)
    }

    override fun visitSmkModule(module: SmkModule) {
        highlightSMKRuleLike(module)
    }

    override fun visitSmkUse(use: SmkUse) {
        highlightWorkflowSection(use)

        val node = use.node.firstChildNode
        var next = node.treeNext
        var done = false
        while (!done && next != null) {
            when (next.elementType) {
                SmkTokenTypes.RULE_KEYWORD -> addHighlightingAnnotation(next, PyHighlighter.PY_KEYWORD)
                SmkElementTypes.USE_NAME_IDENTIFIER, PyTokenTypes.IDENTIFIER -> addHighlightingAnnotation(
                    next, PY_FUNC_DEFINITION
                )
                PyTokenTypes.COLON -> done = true
            }
            next = next.treeNext
        }
    }

    override fun visitSmkRuleOrCheckpointArgsSection(st: SmkRuleOrCheckpointArgsSection) {
        highlightRuleLikeSection(st)
    }

    override fun visitSmkSubworkflowArgsSection(st: SmkSubworkflowArgsSection) {
        highlightRuleLikeSection(st)
    }

    override fun visitSmkModuleArgsSection(st: SmkModuleArgsSection) {
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