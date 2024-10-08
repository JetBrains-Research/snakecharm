package com.jetbrains.snakecharm.lang.highlighter

import com.jetbrains.python.PyTokenTypes
import com.jetbrains.snakecharm.lang.parser.SmkTokenTypes.KEYWORD_LIKE_TOKENS_FOR_ANNOTATOR
import com.jetbrains.snakecharm.lang.psi.*
import com.jetbrains.snakecharm.lang.psi.elementTypes.SmkElementTypes
import com.jetbrains.snakecharm.lang.validation.SmkAnnotator

object SmkSyntaxAnnotator : SmkAnnotator() {
    override fun visitSmkRule(rule: SmkRule) {
        highlightSmkRuleLike(rule)
    }

    override fun visitSmkCheckPoint(checkPoint: SmkCheckPoint) {
        highlightSmkRuleLike(checkPoint)
    }

    override fun visitSmkSubworkflow(subworkflow: SmkSubworkflow) {
        highlightSmkRuleLike(subworkflow)
    }

    override fun visitSmkModule(module: SmkModule) {
        highlightSmkRuleLike(module)
    }

    override fun visitSmkUse(use: SmkUse) {
        highlightWorkflowSection(use)

        val node = use.node.firstChildNode
        var next = node.treeNext
        var done = false
        while (!done && next != null) {
            val elementType = next.elementType
            @Suppress("UnstableApiUsage")
            when (elementType) {
                in KEYWORD_LIKE_TOKENS_FOR_ANNOTATOR -> addHighlightingAnnotation(
                    next,
                    SnakemakeSyntaxHighlighterAttributes.SMK_KEYWORD
                )

                SmkElementTypes.USE_NEW_NAME_PATTERN, PyTokenTypes.IDENTIFIER -> addHighlightingAnnotation(
                    next, SnakemakeSyntaxHighlighterAttributes.SMK_FUNC_DEFINITION
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
            @Suppress("UnstableApiUsage")
            addHighlightingAnnotation(it, SnakemakeSyntaxHighlighterAttributes.SMK_PREDEFINED_DEFINITION)
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

    private fun highlightSmkRuleLike(ruleLike: SmkRuleLike<SmkArgsSection>) {
        highlightWorkflowSection(ruleLike)

        ruleLike.nameIdentifier?.let { nameElement ->
            @Suppress("UnstableApiUsage")
            addHighlightingAnnotation(nameElement, SnakemakeSyntaxHighlighterAttributes.SMK_FUNC_DEFINITION)
        }
    }

    private fun highlightWorkflowSection(st: SmkSection) {
        st.getSectionKeywordNode()?.let {
            @Suppress("UnstableApiUsage")
            addHighlightingAnnotation(it, SnakemakeSyntaxHighlighterAttributes.SMK_KEYWORD)
        }
    }

    private fun highlightRuleLikeSection(st: SmkSection) {
        st.getSectionKeywordNode()?.let {
            @Suppress("UnstableApiUsage")
            addHighlightingAnnotation(it, SnakemakeSyntaxHighlighterAttributes.SMK_DECORATOR)
        }
    }
}