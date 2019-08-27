package com.jetbrains.snakecharm.inspections

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.snakecharm.SnakemakeBundle
import com.jetbrains.snakecharm.lang.SnakemakeNames
import com.jetbrains.snakecharm.lang.psi.SmkRuleOrCheckpoint
import com.jetbrains.snakecharm.lang.psi.SmkRuleOrCheckpointArgsSection
import com.jetbrains.snakecharm.lang.psi.SmkWildcardsCollector

class SmkNotSameWildcardsSetInspection : SnakemakeInspection() {
    override fun buildVisitor(
            holder: ProblemsHolder,
            isOnTheFly: Boolean,
            session: LocalInspectionToolSession
    ) = object : AbstractSmkWildcardsInspectionVisitor<SmkRuleOrCheckpointArgsSection>(holder, session) {
        override fun visitSmkRuleOrCheckpointArgsSection(st: SmkRuleOrCheckpointArgsSection) {
            val sectionKeyword = st.sectionKeyword

            // consider non-output wildcards defining sections:
            if (sectionKeyword == SnakemakeNames.SECTION_OUTPUT ||
                    sectionKeyword !in SmkRuleOrCheckpointArgsSection.SECTIONS_DEFINING_WILDCARDS) {
                return
            }

            val allWildcards = collectWildcards {
                PsiTreeUtil.getParentOfType(st, SmkRuleOrCheckpoint::class.java)
            }.second

            val sectionArgs = st.argumentList?.arguments ?: return
            val canContainWildcards = sectionKeyword in SmkRuleOrCheckpointArgsSection.KEYWORDS_CONTAINING_WILDCARDS
            sectionArgs.forEach { arg ->
                val argWildcards = when {
                    !canContainWildcards -> emptyList()
                    else -> {
                        SmkWildcardsCollector().also {
                            arg.accept(it)
                        }.getWildcards().asSequence().map { it.second }.distinct().toList()
                    }
                }

                val missingWildcards = allWildcards.filter { it !in argWildcards }
                if (missingWildcards.isNotEmpty()) {
                    registerProblem(
                            arg,
                            SnakemakeBundle.message(
                                    "INSP.NAME.not.same.wildcards.set", missingWildcards.joinToString()
                            )
                    )
                }
            }
        }
    }

    override fun getDisplayName(): String = SnakemakeBundle.message("INSP.NAME.not.same.wildcards.set", "")
}
