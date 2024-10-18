package com.jetbrains.snakecharm.inspections

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemsHolder
import com.jetbrains.python.psi.PyCallExpression
import com.jetbrains.python.psi.PyClass
import com.jetbrains.python.psi.PyFunction
import com.jetbrains.python.psi.PyReferenceExpression
import com.jetbrains.snakecharm.SnakemakeBundle
import com.jetbrains.snakecharm.codeInsight.SnakemakeApiService
import com.jetbrains.snakecharm.lang.psi.SmkFile
import com.jetbrains.snakecharm.lang.psi.SmkRuleOrCheckpointArgsSection

class SmkMisuseUsageIOFlagMethodsInspection : SnakemakeInspection() {
    override fun buildVisitor(
        holder: ProblemsHolder,
        isOnTheFly: Boolean,
        session: LocalInspectionToolSession,
    ) = object : SnakemakeInspectionVisitor(holder, getContext(session)) {

        override fun visitSmkRuleOrCheckpointArgsSection(st: SmkRuleOrCheckpointArgsSection) {

            if (st.containingFile !is SmkFile) {
                return
            }

            val argList = st.argumentList ?: return

            val apiService = SnakemakeApiService.getInstance(holder.project)

            argList.arguments
                .filterIsInstance<PyCallExpression>()
                .forEach { callExpr ->
                    val callee = callExpr.callee
                    if (callee is PyReferenceExpression) {
                        // We don't need qualified refs here (e.g. like `foo.boo.ancient`)
                        @Suppress("UnstableApiUsage")
                        val callName = when (callee.qualifier) {
                            null -> callee.referencedName
                            else -> null
                        }
                        if (callName != null) {
                            val declaration = callee.reference.resolve()

                            var sectionsList: Array<String> = emptyArray()
                            if (declaration != null) {
                                // resolved
                                val fqn = if (declaration is PyFunction) {
                                    declaration.qualifiedName
                                } else if (declaration is PyClass) {
                                    // e.g. 'input' is resolved into 'snakemake.io.InputFiles'
                                    declaration.qualifiedName
                                } else {
                                    null
                                }

                                if (fqn != null) {
                                    sectionsList = apiService.getFunctionSectionsRestrictionsByFqn(fqn)
                                }
                            } else {
                                // not resolved, workaround:
                                sectionsList = apiService.getFunctionSectionsRestrictionsByFqn("snakemake.io.$callName")
                            }

                            if (sectionsList.isNotEmpty() && (st.sectionKeyword !in sectionsList)) {
                                holder.registerProblem(
                                    callExpr,
                                    SnakemakeBundle.message(
                                        "INSP.NAME.misuse.usage.io.flag.methods.warning.message",
                                        callName,
                                        st.sectionKeyword!!,
                                        sectionsList.sorted().joinToString { "'$it'" }
                                    )
                                )
                            }
                        }
                    }
                }
        }
    }
}