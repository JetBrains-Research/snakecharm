package com.jetbrains.snakecharm.lang.validation

import com.jetbrains.python.psi.PyKeywordArgument
import com.jetbrains.snakecharm.SnakemakeBundle
import com.jetbrains.snakecharm.lang.SnakemakeLanguageDialect
import com.jetbrains.snakecharm.lang.psi.*

object SnakemakeSyntaxErrorAnnotator : SnakemakeAnnotator() {
    override fun visitSMKRuleParameterListStatement(st: SmkRuleArgsSection) {
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

    override fun visitSMKRule(rule: SmkRule) {
        checkMultipleExecutionSections(rule)
    }

    override fun visitSMKCheckPoint(checkPoint: SmkCheckPoint) {
        checkMultipleExecutionSections(checkPoint)
    }

    private fun checkMultipleExecutionSections(ruleOrCheckpoint: SmkRuleOrCheckpoint) {
        var executionSectionOccurred = false

        val sections = ruleOrCheckpoint.getSections()
        for (st in sections) {
            when (st) {
                is SmkRuleArgsSection -> {
                    val sectionName = st.sectionKeyword
                    val isExecutionSection = sectionName in SmkRuleArgsSection.EXECUTION_KEYWORDS

                    if (executionSectionOccurred && isExecutionSection) {
                        holder.createErrorAnnotation(st, SnakemakeBundle.message("ANN.multiple.execution.sections"))
                    }

                    if (isExecutionSection) {
                        executionSectionOccurred = true
                    }
                }
                is SmkRunSection -> {
                    if (executionSectionOccurred) {
                        holder.createErrorAnnotation(st, SnakemakeBundle.message("ANN.multiple.execution.sections"))
                    }
                    executionSectionOccurred = true
                }
            }
        }
    }
}