package com.jetbrains.snakecharm.inspections

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemsHolder
import com.jetbrains.python.psi.PyStringLiteralExpression
import com.jetbrains.snakecharm.SnakemakeBundle
import com.jetbrains.snakecharm.codeInsight.completion.ShadowSectionSettingsProvider
import com.jetbrains.snakecharm.lang.SnakemakeLanguageDialect
import com.jetbrains.snakecharm.lang.psi.SMKRuleParameterListStatement

class SmkShadowSettingsInspection : SnakemakeInspection()  {
    override fun buildVisitor(
            holder: ProblemsHolder,
            isOnTheFly: Boolean,
            session: LocalInspectionToolSession
    ) = object : SnakemakeInspectionVisitor(holder, session) {

        override fun visitSMKRuleParameterListStatement(st: SMKRuleParameterListStatement) {
            if (!SnakemakeLanguageDialect.isInsideSmkFile(st) ||
                    st.name != SMKRuleParameterListStatement.SHADOW) {
                return
            }

            val argument = if (st.argumentList?.arguments?.size != 0) st.argumentList!!.arguments[0] else null

            if (argument != null &&
                    argument is PyStringLiteralExpression &&
                    argument.stringValue !in ShadowSectionSettingsProvider.SHADOW_SETTINGS) {
                registerProblem(argument.originalElement,
                        SnakemakeBundle.message("INSP.NAME.shadow.settings"))
            }
        }
    }

    override fun getDisplayName(): String = SnakemakeBundle.message("INSP.NAME.shadow.settings")
}