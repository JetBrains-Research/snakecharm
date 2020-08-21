package com.jetbrains.snakecharm.inspections

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemsHolder
import com.jetbrains.python.psi.PyCallExpression
import com.jetbrains.python.psi.PyReferenceExpression
import com.jetbrains.snakecharm.SnakemakeBundle
import com.jetbrains.snakecharm.codeInsight.SnakemakeAPI.IO_FLAG_2_SUPPORTED_SECTION
import com.jetbrains.snakecharm.lang.psi.SmkFile
import com.jetbrains.snakecharm.lang.psi.SmkRuleOrCheckpointArgsSection

class SmkMisuseUsageIOFlagMethodsInspection : SnakemakeInspection() {
    override fun buildVisitor(
        holder: ProblemsHolder,
        isOnTheFly: Boolean,
        session: LocalInspectionToolSession
    ) = object : SnakemakeInspectionVisitor(holder, session) {

        private fun getSupportedSectionIfMisuse(
            flagCallName: String,
            section: SmkRuleOrCheckpointArgsSection
        ): List<String> {
            val supportedSections = IO_FLAG_2_SUPPORTED_SECTION[flagCallName]
            if (supportedSections != null) {
                if (section.sectionKeyword !in supportedSections) {
                    return supportedSections
                }
            }
            // check N/A here
            return emptyList()
        }

        override fun visitSmkRuleOrCheckpointArgsSection(st: SmkRuleOrCheckpointArgsSection) {

            if (st.containingFile !is SmkFile) {
                return
            }

            val argList = st.argumentList ?: return
            argList.arguments
                .filterIsInstance<PyCallExpression>()
                .forEach { callExpr -> 
                    val callee = callExpr.callee
                    if (callee is PyReferenceExpression) {
                        // We don't need qualified refs here (e.g. like `foo.boo.ancient`)
                        val callName = when (callee.qualifier) {
                            null -> callee.referencedName
                            else -> null
                        }

                        if (callName != null) {
                            val supportedSectionIfMisuse = getSupportedSectionIfMisuse(callName, st)
                            if (supportedSectionIfMisuse.isNotEmpty()) {
                                holder.registerProblem(
                                    callExpr,
                                    SnakemakeBundle.message(
                                        "INSP.NAME.misuse.usage.io.flag.methods.warning.message",
                                        callName,
                                        st.sectionKeyword!!,
                                        supportedSectionIfMisuse.sorted().joinToString { "'$it'"}
                                    )
                                )
                            }
                        }
                    }
                }
        }
    }
}