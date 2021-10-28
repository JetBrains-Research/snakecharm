package com.jetbrains.snakecharm.lang.highlighter

import com.jetbrains.python.PyTokenTypes
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
                SmkTokenTypes.RULE_KEYWORD, SmkTokenTypes.SMK_FROM_KEYWORD,
                SmkTokenTypes.SMK_AS_KEYWORD, SmkTokenTypes.SMK_WITH_KEYWORD -> addHighlightingAnnotation(
                    next,
                    SnakemakeSyntaxHighlighterFactory.SMK_KEYWORD
                )
                SmkElementTypes.USE_NAME_IDENTIFIER, PyTokenTypes.IDENTIFIER -> addHighlightingAnnotation(
                    next, SnakemakeSyntaxHighlighterFactory.SMK_FUNC_DEFINITION
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
            addHighlightingAnnotation(it, SnakemakeSyntaxHighlighterFactory.SMK_PREDEFINED_DEFINITION)
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
            addHighlightingAnnotation(nameElement, SnakemakeSyntaxHighlighterFactory.SMK_FUNC_DEFINITION)
        }
    }

    private fun highlightWorkflowSection(st: SmkSection) {
        st.getSectionKeywordNode()?.let {
            addHighlightingAnnotation(it, SnakemakeSyntaxHighlighterFactory.SMK_KEYWORD)
        }
    }

    private fun highlightRuleLikeSection(st: SmkSection) {
        st.getSectionKeywordNode()?.let {
            addHighlightingAnnotation(it, SnakemakeSyntaxHighlighterFactory.SMK_DECORATOR)
        }
    }
}