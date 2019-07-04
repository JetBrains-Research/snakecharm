package com.jetbrains.snakecharm.lang.validation

import com.intellij.psi.PsiIdentifier
import com.jetbrains.python.psi.PyArgumentList
import com.jetbrains.python.psi.PyKeywordArgument
import com.jetbrains.snakecharm.SnakemakeBundle
import com.jetbrains.snakecharm.lang.SnakemakeLanguageDialect
import com.jetbrains.snakecharm.lang.psi.*

object SnakemakeSyntaxErrorAnnotator : SnakemakeAnnotator() {
    override fun visitSMKRuleParameterListStatement(st: SMKRuleParameterListStatement) {
        if (!SnakemakeLanguageDialect.isInsideSmkFile(st)) {
            return
        }

        val seenKeywords = HashSet<String>()
        var encounteredKeywordArgument = false

        st.argumentList?.arguments?.forEach {arg ->
            if (arg is PyKeywordArgument) {
                arg.keyword?.let { keyword ->
                    if (!seenKeywords.add(keyword)) {
                        holder.createErrorAnnotation(
                                arg.textRange,
                                SnakemakeBundle.message("ANN.keyword.argument.repeated")
                        )
                    }
                }
                encounteredKeywordArgument = true
            } else {
                if (encounteredKeywordArgument) {
                    holder.createErrorAnnotation(
                            arg,
                            SnakemakeBundle.message("ANN.positional.argument.after.keyword.argument")
                    )
                }
            }
        }
    }

    override fun visitSMKRule(rule: SMKRule) {
        var executionSectionOccurred = false

        val sections = rule.getSections()
        for (st in sections) {
            when (st) {
                is SMKRuleParameterListStatement -> {
                    val sectionName = st.section.text
                    val isExecutionSection = sectionName in SMKRuleParameterListStatement.EXECUTION_KEYWORDS

                    if (executionSectionOccurred && isExecutionSection) {
                        holder.createErrorAnnotation(st, SnakemakeBundle.message("ANN.multiple.execution.sections"))
                    }

                    if (isExecutionSection) {
                        executionSectionOccurred = true
                    }
                }
                is SMKRuleRunParameter -> {
                    if (executionSectionOccurred) {
                        holder.createErrorAnnotation(st, SnakemakeBundle.message("ANN.multiple.execution.sections"))
                    }
                    executionSectionOccurred = true
                }
            }
        }
    }

    override fun visitSMKWorkflowLocalRulesStatement(st: SMKWorkflowLocalRulesStatement) {
        placeErrorOnNonIdentifiers(st.argumentList, SnakemakeBundle.message("ANN.expressions.in.localrules"))
    }

    override fun visitSMKWorkflowRuleOrderStatement(st: SMKWorkflowRulesOrderStatement) {
        placeErrorOnNonIdentifiers(st.argumentList, SnakemakeBundle.message("ANN.expressions.in.ruleorder"))
    }

    private fun placeErrorOnNonIdentifiers(
            argumentList: PyArgumentList?,
            errorMessage: String
    ) {
        argumentList
                ?.arguments
                ?.filter { it !is PsiIdentifier }
                ?.forEach {
                    holder.createErrorAnnotation(it, errorMessage)
                }
    }
}