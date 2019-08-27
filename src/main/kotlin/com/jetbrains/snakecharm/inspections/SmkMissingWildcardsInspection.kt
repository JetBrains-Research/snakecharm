package com.jetbrains.snakecharm.inspections

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.snakecharm.SnakemakeBundle
import com.jetbrains.snakecharm.lang.SnakemakeNames
import com.jetbrains.snakecharm.lang.psi.SmkRuleOrCheckpoint
import com.jetbrains.snakecharm.lang.psi.SmkRuleOrCheckpointArgsSection
import com.jetbrains.snakecharm.lang.psi.SmkWildcardsCollector

class SmkMissingWildcardsInspection : SnakemakeInspection() {
    override fun buildVisitor(
            holder: ProblemsHolder,
            isOnTheFly: Boolean,
            session: LocalInspectionToolSession
    ) = object : AbstractSmkWildcardsInspectionVisitor<SmkRuleOrCheckpointArgsSection>(holder, session) {
        override fun visitSmkRuleOrCheckpointArgsSection(st: SmkRuleOrCheckpointArgsSection) {
            if (st.name == SnakemakeNames.SECTION_OUTPUT ||
                    st.name !in SmkRuleOrCheckpointArgsSection.SECTIONS_DEFINING_WILDCARDS ||
                    st.argumentList == null ||
                    st.argumentList!!.arguments.isEmpty()) {
                return
            }

            updateDeclarationAndWildcards(
                    st,
                    getDeclaration = { PsiTreeUtil.getParentOfType(st, SmkRuleOrCheckpoint::class.java) }
            )

            val stWildcards = SmkWildcardsCollector()
                    .also { it.visitSmkRuleOrCheckpointArgsSection(st) }
                    .getWildcardsFirstMentions()
                    .map { it.second }

            val wildcardsSetDif = currentGeneratedWildcards.filter { it !in stWildcards }
            if (wildcardsSetDif.isNotEmpty()) {
                registerProblem(st,
                        SnakemakeBundle.message("INSP.NAME.missing.wildcards",
                                wildcardsSetDif.joinToString()
                        )
                )
            }
        }
    }

    override fun getDisplayName(): String = SnakemakeBundle.message("INSP.NAME.missing.wildcards", "")
}
