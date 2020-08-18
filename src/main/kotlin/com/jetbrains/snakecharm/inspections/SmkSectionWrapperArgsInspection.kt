package com.jetbrains.snakecharm.inspections

import com.intellij.codeInspection.*
import com.intellij.openapi.components.service
import com.jetbrains.python.psi.PyArgumentList
import com.jetbrains.snakecharm.SnakemakeBundle
import com.jetbrains.snakecharm.codeInsight.completion.wrapper.SmkWrapperStorage
import com.jetbrains.snakecharm.lang.psi.*

class SmkSectionWrapperArgsInspection : SnakemakeInspection() {
    override fun buildVisitor(
            holder: ProblemsHolder,
            isOnTheFly: Boolean,
            session: LocalInspectionToolSession
    ) = object : SnakemakeInspectionVisitor(holder, session) {
        override fun visitSmkRule(rule: SmkRule) {
            visitSmkRulelike(rule)
        }

        override fun visitSmkCheckPoint(checkPoint: SmkCheckPoint) {
            visitSmkRulelike(checkPoint)
        }

        fun visitSmkRulelike(rulelike: SmkRuleOrCheckpoint) {
            val wrapper = rulelike.getSectionByName("wrapper") ?: return
            val wrappers = rulelike.project.service<SmkWrapperStorage>().wrapperStorage
            val wrname = wrapper.argumentList?.text ?: return
            val ideal = wrappers.find { wrname.contains(it.path) } ?: return

            ideal.args.forEach { (key, value) ->
                val section = rulelike.getSectionByName(key)
                if (section != null) {
                    if (value.size == 1 && value[0].isBlank()) {
                        return
                    } else {
                        checkArgumentList(section.argumentList!!, section, value)
                    }
                } else {
                    registerProblem(
                        rulelike.nameIdentifier?.originalElement,
                        SnakemakeBundle.message(
                            "INSP.NAME.wrapper.args.section.missed.message",
                            key,
                            value.joinToString(", ")
                        )
                    )
                }
            }
        }

        private fun checkArgumentList(
                argumentList: PyArgumentList,
                section: SmkArgsSection,
                required: List<String>
        ) {
            required.forEach { arg ->
                if (argumentList.getKeywordArgument(arg) == null) {
                    registerProblem(
                        section.nameIdentifier?.originalElement,
                            SnakemakeBundle.message(
                                "INSP.NAME.wrapper.args.missed.message",
                                    arg,
                                    section.name!!
                        )
                    )
                }
            }
        }
    }
}