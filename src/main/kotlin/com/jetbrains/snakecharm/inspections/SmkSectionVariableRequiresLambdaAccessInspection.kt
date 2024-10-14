package com.jetbrains.snakecharm.inspections

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.util.parentOfType
import com.intellij.psi.util.parentOfTypes
import com.jetbrains.python.psi.PyLambdaExpression
import com.jetbrains.python.psi.PyReferenceExpression
import com.jetbrains.snakecharm.SnakemakeBundle
import com.jetbrains.snakecharm.codeInsight.SnakemakeAPI.SMK_VARS_WILDCARDS
import com.jetbrains.snakecharm.codeInsight.SnakemakeAPIProjectService
import com.jetbrains.snakecharm.codeInsight.SnakemakeAPIService
import com.jetbrains.snakecharm.lang.psi.SmkArgsSection

class SmkSectionVariableRequiresLambdaAccessInspection : SnakemakeInspection() {
    override fun buildVisitor(
        holder: ProblemsHolder,
        isOnTheFly: Boolean,
        session: LocalInspectionToolSession,
    ) = object : SnakemakeInspectionVisitor(holder, getContext(session)) {
        val apiService = SnakemakeAPIService.getInstance()
        val apiProjectService = SnakemakeAPIProjectService.getInstance(holder.project)

        override fun visitPyReferenceExpression(node: PyReferenceExpression) {
            @Suppress("UnstableApiUsage")
            if (node.isQualified) {
                // Not suitable case
                return
            }
            @Suppress("UnstableApiUsage")
            val varName = node.referencedName
            // Not allowed arg (e.g. wildcards, ..) and not section keyword (e.g. 'threads', 'version' etc)
            if (varName == null || (
                        (varName !in apiProjectService.getPossibleLambdaParamNames()) &&
                                ( varName !in apiService.RULE_OR_CHECKPOINT_ARGS_SECTION_KEYWORDS))
                ) {
                // Not suitable case
                return
            }

            val lambdaOrSectionExpr = node.parentOfTypes(PyLambdaExpression::class, SmkArgsSection::class)
                ?: return  // Not suitable case

            val (lambdaElem, containingArgsSection) = if (lambdaOrSectionExpr is SmkArgsSection) {
                null to lambdaOrSectionExpr
            } else {
                val sectionArg = lambdaOrSectionExpr.parentOfType<SmkArgsSection>()
                    ?: return // Shouldn't happen
                (lambdaOrSectionExpr as PyLambdaExpression) to sectionArg
            }

            val supportedVarNames = apiProjectService.getLambdaArgsFor(containingArgsSection.sectionKeyword)
            val varNameIsSupportedByLambda = supportedVarNames?.contains(varName) == true

            val resolve = node.reference.resolve()
            if (resolve == null) {
                val message = when {
                    varNameIsSupportedByLambda -> getMissingLambdaMsg(varName)
                    else ->
                        SnakemakeBundle.message(
                            "INSP.NAME.section.var.requires.lambda.unsupported.var.message",
                            varName, containingArgsSection.sectionKeyword ?: ""
                        )
                }
                registerProblem(node, message)
            } else {
                if (lambdaElem != null && resolve in lambdaElem.parameterList.parameters) {
                    // var is lambda arg
                    return
                }

                if (!varNameIsSupportedByLambda) {
                    // looks like legal usage
                    return
                }

                val msg = getPossibleOuterAccessMsg(varName)
                val ruleLike = containingArgsSection.getParentRuleOrCheckPoint()
                when {
                    ruleLike?.getSectionByName(varName) == null -> registerProblem(
                        node, msg, ProblemHighlightType.WEAK_WARNING
                    )
                    // Doesn't occur on runtime at the moment
                    else -> registerProblem(node, msg)
                }
            }
        }
    }

    private fun getMissingLambdaMsg(varName: String) = when (varName) {
        SMK_VARS_WILDCARDS -> SnakemakeBundle.message(
            "INSP.NAME.section.var.requires.lambda.missing.for.wildcard"
        )
        else -> SnakemakeBundle.message(
            "INSP.NAME.section.var.requires.lambda.missing.for.var", varName
        )
    }

    private fun getPossibleOuterAccessMsg(varName: String) = when (varName) {
        SMK_VARS_WILDCARDS -> SnakemakeBundle.message(
            "INSP.NAME.section.var.requires.lambda.possible.outer.wildcards.access"
        )
        else -> SnakemakeBundle.message(
            "INSP.NAME.section.var.requires.lambda.possible.outer.var.access", varName
        )
    }
}