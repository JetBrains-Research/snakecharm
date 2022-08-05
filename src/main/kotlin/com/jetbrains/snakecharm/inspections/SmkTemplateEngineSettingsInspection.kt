package com.jetbrains.snakecharm.inspections

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemsHolder
import com.jetbrains.python.psi.PyStringLiteralExpression
import com.jetbrains.snakecharm.SnakemakeBundle
import com.jetbrains.snakecharm.codeInsight.completion.TemplateEngineSettingsProvider
import com.jetbrains.snakecharm.lang.SnakemakeNames
import com.jetbrains.snakecharm.lang.psi.SmkRuleOrCheckpointArgsSection

class SmkTemplateEngineSettingsInspection : SnakemakeInspection() {
    override fun buildVisitor(
        holder: ProblemsHolder,
        isOnTheFly: Boolean,
        session: LocalInspectionToolSession,
    ) = object : SnakemakeInspectionVisitor(holder, getContext(session)) {

        override fun visitSmkRuleOrCheckpointArgsSection(st: SmkRuleOrCheckpointArgsSection) {
            if (st.name != SnakemakeNames.SECTION_TEMPLATE_ENGINE) {
                return
            }

            val argument = st.argumentList?.arguments?.firstOrNull()
            if (argument is PyStringLiteralExpression &&
                argument.stringValue !in TemplateEngineSettingsProvider.OPTIONS
            ) {
                registerProblem(
                    argument.originalElement,
                    SnakemakeBundle.message("INSP.NAME.template.engine.settings.msg")
                )
            }
        }
    }

    override fun getDisplayName(): String = SnakemakeBundle.message("INSP.NAME.template.engine.settings.title")
}