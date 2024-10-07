package com.jetbrains.snakecharm.inspections.smksl

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.LocalQuickFixOnPsiElement
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.python.psi.PyLambdaExpression
import com.jetbrains.python.psi.types.TypeEvalContext
import com.jetbrains.snakecharm.SnakemakeBundle
import com.jetbrains.snakecharm.inspections.SnakemakeInspection
import com.jetbrains.snakecharm.lang.psi.types.SmkWildcardsType
import com.jetbrains.snakecharm.stringLanguage.lang.SmkSLInjector.Companion.isInExpandCallExpression
import com.jetbrains.snakecharm.stringLanguage.lang.psi.SmkSLReferenceExpression

class SmkSLMissingWildcardsAccessorPrefixInspection : SnakemakeInspection() {
    override fun buildVisitor(
        holder: ProblemsHolder,
        isOnTheFly: Boolean,
        session: LocalInspectionToolSession,
    ) = object : SmkSLInspectionVisitor(holder, getContext(session)) {

        override fun visitSmkSLReferenceExpression(expr: SmkSLReferenceExpression) {
            @Suppress("UnstableApiUsage")
            if (expr.isQualified) {
                return
            }
            if (expr.isWildcard()) {
                return
            }

            val section = expr.containingRuleOrCheckpointSection()
            val ruleLike = section?.getParentRuleOrCheckPoint() ?: return

            val host = expr.injectionHost()
            if (host == null || isInExpandCallExpression(host)) {
                return
            }

            if (PsiTreeUtil.getParentOfType(host, PyLambdaExpression::class.java) != null) {
                return
            }

            @Suppress("UnstableApiUsage")
            val referencedName = expr.referencedName
            val typeEvalContext = TypeEvalContext.codeAnalysis(host.project, host.containingFile)
            val type = typeEvalContext.getType(ruleLike.wildcardsElement)
            if (type is SmkWildcardsType) {
                val wildcards = type.wildcards
                if (wildcards != null) {
                    // check if our expr has a name as one of wildcards:
                    if (referencedName in wildcards) {
                        // ensure that reference isn't resolved to some other element
                        if (expr.reference.resolve() == null) {
                            registerProblem(
                                expr,
                                SnakemakeBundle.message("INSP.NAME.wildcards.prefix.missing.message"),
                                InsertWildcardsQuickFix(expr)
                            )
                        }
                    }
                }
            }
        }
    }
}

class InsertWildcardsQuickFix(expr: SmkSLReferenceExpression) : LocalQuickFixOnPsiElement(expr) {
    override fun getFamilyName() = SnakemakeBundle.message("INSP.INTN.add.wildcards.prefix")

    override fun getText() = familyName

    override fun invoke(project: Project, file: PsiFile, startElement: PsiElement, endElement: PsiElement) {
        val doc = PsiDocumentManager.getInstance(project).getDocument(file)
        doc!!.insertString(startElement.textOffset, "wildcards.")
        PsiDocumentManager.getInstance(project).commitDocument(doc)
    }
}