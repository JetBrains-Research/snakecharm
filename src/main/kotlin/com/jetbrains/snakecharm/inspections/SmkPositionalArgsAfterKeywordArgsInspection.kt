package com.jetbrains.snakecharm.inspections

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemsHolder
import com.jetbrains.python.psi.PyKeywordArgument
import com.jetbrains.snakecharm.SnakemakeBundle
import com.jetbrains.snakecharm.lang.SnakemakeLanguageDialect
import com.jetbrains.snakecharm.lang.psi.SMKRuleParameterListStatement

class SmkPositionalArgsAfterKeywordArgsInspection : SnakemakeInspection() {
    override fun buildVisitor(
            holder: ProblemsHolder,
            isOnTheFly: Boolean,
            session: LocalInspectionToolSession
    ) = object : SnakemakeInspectionVisitor(holder, session) {

        override fun visitSMKRuleParameterListStatement(st: SMKRuleParameterListStatement) {
            if (st.containingFile.language != SnakemakeLanguageDialect) {
                return
            }

            var encounteredKeywordArgument = false
            val arguments = st.argumentList?.arguments ?: return
            for (argument in arguments) {
                if (!encounteredKeywordArgument && argument is PyKeywordArgument) {
                    encounteredKeywordArgument = true
                }
                if (encounteredKeywordArgument && argument !is PyKeywordArgument) {
                    registerProblem(argument,
                            SnakemakeBundle.message("INSP.NAME.positional.args.after.keyword.args"))
                }
            }
        }
    }

    override fun getDisplayName(): String = SnakemakeBundle.message("INSP.NAME.positional.args.after.keyword.args")
}
