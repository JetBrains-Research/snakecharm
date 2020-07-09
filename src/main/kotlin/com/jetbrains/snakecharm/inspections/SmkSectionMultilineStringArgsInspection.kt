package com.jetbrains.snakecharm.inspections

import com.intellij.codeInspection.*
import com.jetbrains.python.psi.PyArgumentList
import com.jetbrains.python.psi.PyStringLiteralExpression
import com.jetbrains.snakecharm.SnakemakeBundle
import com.jetbrains.snakecharm.lang.SnakemakeNames.SECTION_INPUT
import com.jetbrains.snakecharm.lang.SnakemakeNames.SECTION_OUTPUT
import com.jetbrains.snakecharm.lang.SnakemakeNames.SECTION_LOG
import com.jetbrains.snakecharm.lang.psi.SmkRuleOrCheckpointArgsSection

class SmkSectionMultilineStringArgsInspection : SnakemakeInspection() {
    override fun buildVisitor(
            holder: ProblemsHolder,
            isOnTheFly: Boolean,
            session: LocalInspectionToolSession
    ) = object : SnakemakeInspectionVisitor(holder, session) {

        override fun visitSmkRuleOrCheckpointArgsSection(st: SmkRuleOrCheckpointArgsSection) {
            if (st.sectionKeyword in setOf(SECTION_INPUT, SECTION_OUTPUT, SECTION_LOG)) {
                checkArgumentList(st.argumentList, st)
            }
        }

        private fun checkArgumentList(
                argumentList: PyArgumentList?,
                section: SmkRuleOrCheckpointArgsSection
        ) {

            val args = argumentList?.arguments ?: emptyArray()
            args.forEach { arg ->
                if (arg is PyStringLiteralExpression && arg.decodedFragments.size > 1) {

                    registerProblem(
                            arg,
                            SnakemakeBundle.message("INSP.NAME.section.multiline.string.args.message",
                                    section.sectionKeyword!!)
                    )
                }
            }
        }
    }

    override fun getDisplayName(): String = SnakemakeBundle.message("INSP.NAME.section.multiline.string.args")
}