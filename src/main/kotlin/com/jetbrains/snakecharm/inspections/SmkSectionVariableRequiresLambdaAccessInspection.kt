package com.jetbrains.snakecharm.inspections

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.util.parentOfType
import com.intellij.psi.util.parentOfTypes
import com.jetbrains.python.psi.PyLambdaExpression
import com.jetbrains.python.psi.PyReferenceExpression
import com.jetbrains.snakecharm.SnakemakeBundle
import com.jetbrains.snakecharm.codeInsight.SnakemakeAPI.ALLOWED_LAMBDA_OR_CALLABLE_ARGS
import com.jetbrains.snakecharm.codeInsight.SnakemakeAPI.SECTION_LAMBDA_ARG_POSSIBLE_PARAMS
import com.jetbrains.snakecharm.lang.psi.SmkArgsSection

class SmkSectionVariableRequiresLambdaAccessInspection : SnakemakeInspection() {
    override fun buildVisitor(
        holder: ProblemsHolder,
        isOnTheFly: Boolean,
        session: LocalInspectionToolSession
    ) = object : SnakemakeInspectionVisitor(holder, session) {
        override fun visitPyReferenceExpression(node: PyReferenceExpression) {
            if (node.isQualified) {
                // N/a
                return
            }
            val varName = node.referencedName
            if (varName == null || varName !in SECTION_LAMBDA_ARG_POSSIBLE_PARAMS) {
                // N/a
                return
            }

            val lambdaOrSectionExpr = node.parentOfTypes(PyLambdaExpression::class, SmkArgsSection::class)
                ?: return // N/A

            val (lambdaElem, containingArgsSection) = if (lambdaOrSectionExpr is SmkArgsSection) {
                null to lambdaOrSectionExpr
            } else {
                val sectionArg = lambdaOrSectionExpr.parentOfType<SmkArgsSection>()
                    ?: return // N/A
                (lambdaOrSectionExpr as PyLambdaExpression) to sectionArg
            }

            val supportedVarNames = ALLOWED_LAMBDA_OR_CALLABLE_ARGS[containingArgsSection.sectionKeyword]
            val varNameIsSupportedByLambda = supportedVarNames?.contains(varName) ?: false

            val resolve = node.reference.resolve()
            if (resolve == null) {
                    val message = when {
                        varNameIsSupportedByLambda ->
                            SnakemakeBundle.message("INSP.NAME.section.var.requires.lambda.warning.message", varName)
                        else ->
                            SnakemakeBundle.message(
                                "INSP.NAME.section.var.requires.lambda.warning.unsupported.lambda.var.message",
                                varName, containingArgsSection.sectionKeyword ?: ""
                            )
                    }
                    registerProblem(node, message)
            } else {
                if (lambdaElem != null && resolve in lambdaElem.parameterList.parameters)  {
                    return
                }
                if (varNameIsSupportedByLambda) {
                    val ruleLike = containingArgsSection.getParentRuleOrCheckPoint()
                    val msg = SnakemakeBundle.message(
                        "INSP.NAME.section.var.requires.lambda.possible.access.message", varName
                    )
                    when {
                        ruleLike?.getSectionByName(varName) == null -> registerProblem(node, msg, ProblemHighlightType.WEAK_WARNING)
                        // Doesn't occur on runtime at the moment
                        else -> registerProblem(node, msg)
                    }
                }
            }

//            val lambdaExpr =  if (lambdaOrSectionExpr is PyLambdaExpression) lambdaOrSectionExpr else null
//            val smkSection = if () {
//
//            }
//
//            val qualified = node.isQualified
//            val qualifier = node.qualifier
//            println("!")
//            // 1. if not qualifed, e.g. 'left' part of qualified expre & node.text == "input" or node.reference == "input"
//            // 2. node.parentOfType<SmkArgsSection>().sectionKeyword
//            // if also in lambda?
//            //      -> node.parentOfTypes(PyLambdaExpression::class, SmkArgsSection::class)
//            //      -> check lambda args!
//            // 3. get parent rule & check section, if it exist there
//            // 4. node.reference.resolve()
        }

    }
}