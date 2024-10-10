package com.jetbrains.snakecharm.inspections

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemsHolder
import com.jetbrains.python.psi.PyArgumentList
import com.jetbrains.python.psi.PyKeywordArgument
import com.jetbrains.snakecharm.SnakemakeBundle
import com.jetbrains.snakecharm.codeInsight.completion.wrapper.SmkWrapperStorage
import com.jetbrains.snakecharm.lang.SnakemakeNames
import com.jetbrains.snakecharm.lang.psi.SmkArgsSection
import com.jetbrains.snakecharm.lang.psi.SmkCheckPoint
import com.jetbrains.snakecharm.lang.psi.SmkRule
import com.jetbrains.snakecharm.lang.psi.SmkRuleOrCheckpoint

class SmkWrapperMissedArgumentsInspection : SnakemakeInspection() {
    override fun buildVisitor(
        holder: ProblemsHolder,
        isOnTheFly: Boolean,
        session: LocalInspectionToolSession,
    ) = object : SnakemakeInspectionVisitor(holder, getContext(session)) {

        override fun visitSmkRule(rule: SmkRule) {
            visitSmkRuleOrCheckpoint(rule)
        }

        override fun visitSmkCheckPoint(checkPoint: SmkCheckPoint) {
            visitSmkRuleOrCheckpoint(checkPoint)
        }

        fun visitSmkRuleOrCheckpoint(ruleOrCheckpoint: SmkRuleOrCheckpoint) {
            val wrapper = ruleOrCheckpoint.getSectionByName(SnakemakeNames.SECTION_WRAPPER) ?: return

            val wrappers = SmkWrapperStorage.getInstance(ruleOrCheckpoint.project).wrappers
            val wrapperName = wrapper.argumentList?.text ?: return
            val wrapperInfo = wrappers.find { wrapperName.contains(it.path) } ?: return

            wrapperInfo.args.keys.sorted().forEach { sectionName ->
                val expectedArgs = wrapperInfo.args[sectionName] ?: error("Cannot be null")
                val psiSection = ruleOrCheckpoint.getSectionByName(sectionName)
                if (psiSection != null) {
                    if (expectedArgs.isNotEmpty()) {
                        val psiArgList = psiSection.argumentList
                        if (psiArgList != null) {
                            checkArgumentList(psiArgList, psiSection, expectedArgs)
                        }
                    }
                } else {
                    val message = when {
                        expectedArgs.isEmpty() -> SnakemakeBundle.message(
                            "INSP.NAME.wrapper.args.section.missed.message",
                            sectionName,
                        )
                        else -> SnakemakeBundle.message(
                            "INSP.NAME.wrapper.args.section.with.args.missed.message",
                            sectionName,
                            expectedArgs.joinToString(", ")
                        )
                    }
                    registerProblem(
                        ruleOrCheckpoint.nameIdentifier?.originalElement,
                        message
                    )
                }
            }
        }

        private fun checkArgumentList(
            argumentList: PyArgumentList,
            section: SmkArgsSection,
            required: List<String>,
        ) {
            @Suppress("UnstableApiUsage")
            val usedKeywords = argumentList.arguments.filterIsInstance<PyKeywordArgument>().map { it.keyword }
            required.forEach { arg ->
                if (arg !in usedKeywords) {
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