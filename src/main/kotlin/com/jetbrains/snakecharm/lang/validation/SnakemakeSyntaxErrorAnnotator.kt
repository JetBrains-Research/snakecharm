package com.jetbrains.snakecharm.lang.validation

import com.jetbrains.python.psi.PyKeywordArgument
import com.jetbrains.snakecharm.SnakemakeBundle
import com.jetbrains.snakecharm.lang.SnakemakeLanguageDialect
import com.jetbrains.snakecharm.lang.psi.SMKRule
import com.jetbrains.snakecharm.lang.psi.SMKRuleParameterListStatement
import com.jetbrains.snakecharm.lang.psi.SMKRuleRunParameter

object SnakemakeSyntaxErrorAnnotator : SnakemakeAnnotator() {
    override fun visitSMKRuleParameterListStatement(st: SMKRuleParameterListStatement) {
        if (!SnakemakeLanguageDialect.isInsideSmkFile(st)) {
            return
        }

        val seenKeywords2Value = HashMap<String, String>()
        var encounteredKeywordArgument = false

        st.argumentList?.arguments?.forEach { arg ->
            when (arg) {
                is PyKeywordArgument -> {
                    arg.keyword?.let { keyword ->
                        val keywordValue = seenKeywords2Value[keyword]
                        if (keywordValue == null) {
                            // arg value is nullable, let's take arg text which isn't null
                            seenKeywords2Value[keyword] = arg.text
                        } else {
                            holder.createErrorAnnotation(
                                    arg.textRange,
                                    SnakemakeBundle.message("ANN.keyword.argument.already.provided", keywordValue)
                            )
                        }
                    }
                    encounteredKeywordArgument = true
                }
                else -> if (encounteredKeywordArgument) {
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
}