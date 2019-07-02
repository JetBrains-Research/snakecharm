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

        val argumentNamesSet = HashSet<String>()
        var encounteredKeywordArgument = false

        st.argumentList?.arguments?.forEach {
            // check argument name
            val name = it.name
            if (argumentNamesSet.contains(name)) {
                holder.createErrorAnnotation(it.textRange, SnakemakeBundle.message("ANN.keyword.argument.repeated"))
            } else if (name != null) {
                argumentNamesSet.add(name)
            }

            // check if positional or keyword argument
            if (!encounteredKeywordArgument && it is PyKeywordArgument) {
                encounteredKeywordArgument = true
            }
            if (encounteredKeywordArgument && it !is PyKeywordArgument) {
                holder.createErrorAnnotation(
                        it,
                        SnakemakeBundle.message("ANN.positional.argument.after.keyword.argument")
                )
            }
        }
    }

    override fun visitSMKRule(smkRule: SMKRule) {
        var executionSectionOccurred = false

        val sections = smkRule.getSections()
        for (st in sections) {
            if (st is SMKRuleParameterListStatement) {
                val sectionName = st.section.text ?: return
                val isExecutionSection = sectionName in SMKRuleParameterListStatement.EXECUTION_KEYWORDS

                if (executionSectionOccurred && isExecutionSection) {
                    holder.createErrorAnnotation(st, SnakemakeBundle.message("ANN.multiple.execution.sections"))
                }

                if (isExecutionSection) {
                    executionSectionOccurred = true
                }
            } else if (st is SMKRuleRunParameter) {
                if (executionSectionOccurred) {
                    holder.createErrorAnnotation(st, SnakemakeBundle.message("ANN.multiple.execution.sections"))
                }
                executionSectionOccurred = true
            }
        }
    }
}