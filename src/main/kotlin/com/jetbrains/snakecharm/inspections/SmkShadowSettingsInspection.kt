package com.jetbrains.snakecharm.inspections

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemsHolder
import com.jetbrains.python.psi.PyStringLiteralExpression
import com.jetbrains.snakecharm.SnakemakeBundle
import com.jetbrains.snakecharm.codeInsight.completion.ShadowSectionSettingsProvider
import com.jetbrains.snakecharm.lang.SnakemakeNames
import com.jetbrains.snakecharm.lang.psi.SmkRuleOrCheckpointArgsSection

class SmkShadowSettingsInspection : SnakemakeInspection() {
    override fun buildVisitor(
        holder: ProblemsHolder,
        isOnTheFly: Boolean,
        session: LocalInspectionToolSession,
    ) = object : SnakemakeInspectionVisitor(holder, getContext(session)) {

        override fun visitSmkRuleOrCheckpointArgsSection(st: SmkRuleOrCheckpointArgsSection) {
            if (st.name != SnakemakeNames.SECTION_SHADOW) {
                return
            }

            val argument = st.argumentList?.arguments?.firstOrNull()
            if (argument is PyStringLiteralExpression &&
                argument.stringValue !in ShadowSectionSettingsProvider.SHADOW_SETTINGS
            ) {
                registerProblem(argument.originalElement,
                    SnakemakeBundle.message("INSP.NAME.shadow.settings"))
            }
        }
    }

    override fun getDisplayName(): String = SnakemakeBundle.message("INSP.NAME.shadow.settings")
}