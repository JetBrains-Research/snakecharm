package com.jetbrains.snakecharm.inspections

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.jetbrains.python.psi.PyCallExpression
import com.jetbrains.python.psi.PyFunction
import com.jetbrains.python.psi.PyStringLiteralExpression
import com.jetbrains.snakecharm.SnakemakeBundle
import com.jetbrains.snakecharm.codeInsight.SnakemakeAPI.SNAKEMAKE_MODULE_NAME_UTILS_PY
import com.jetbrains.snakecharm.framework.SmkSupportProjectSettings
import com.jetbrains.snakecharm.lang.SmkLanguageVersion

class SmkMinVersionWarningInspection : SnakemakeInspection() {
    override fun buildVisitor(
        holder: ProblemsHolder,
        isOnTheFly: Boolean,
        session: LocalInspectionToolSession,
    ) = object : SnakemakeInspectionVisitor(holder, getContext(session)) {

        @Suppress("UnstableApiUsage")
        override fun visitPyCallExpression(node: PyCallExpression) {
            val callee = node.callee
            if (callee?.name != "min_version") {
                return
            }

            val reference = callee.reference
            val resolveResult = reference?.resolve()
            val function = resolveResult as? PyFunction ?: return
            if (function.name != "min_version" || function.containingFile.name != SNAKEMAKE_MODULE_NAME_UTILS_PY) {
                return
            }

            val args = node.argumentList?.children
            if (args == null || args.size != 1 || args[0] !is PyStringLiteralExpression) {
                return
            }
            val versionArg = args[0] as? PyStringLiteralExpression ?: return

            val version = try {
                SmkLanguageVersion(versionArg.stringValue)
            } catch (e: Exception) {
                return //we ignore function inputs that are not correct versions
            }
            val setVersion = SmkSupportProjectSettings.getInstance(holder.project).snakemakeLanguageVersion
            if (setVersion != null && SmkLanguageVersion(setVersion) < version) {
                registerProblem(
                    node,
                    SnakemakeBundle.message("INSP.NAME.min.version.too.early", setVersion),
                    ProblemHighlightType.WEAK_WARNING
                )
            }
        }
    }
}