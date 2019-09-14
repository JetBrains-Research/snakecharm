package com.jetbrains.snakecharm.inspections.smksl

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.LocalQuickFixOnPsiElement
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.jetbrains.python.psi.types.TypeEvalContext
import com.jetbrains.snakecharm.SnakemakeBundle
import com.jetbrains.snakecharm.inspections.SnakemakeInspection
import com.jetbrains.snakecharm.lang.psi.types.SmkWildcardsType
import com.jetbrains.snakecharm.stringLanguage.lang.psi.SmkSLReferenceExpressionImpl
import com.jetbrains.snakecharm.stringLanguage.lang.psi.references.SmkSLWildcardReference

class SmkSLMissingWildcardsAccessorPrefixInspection : SnakemakeInspection() {
    override fun buildVisitor(
            holder: ProblemsHolder,
            isOnTheFly: Boolean,
            session: LocalInspectionToolSession
    ) = object : SmkSLInspectionVisitor(holder, session) {

        override fun visitSmkSLReferenceExpression(expr: SmkSLReferenceExpressionImpl) {
            if (expr.isQualified) {
                return
            }
            val ref = expr.reference
            if (ref is SmkSLWildcardReference) {
                return
            }
            val referencedName = expr.referencedName

            val section = expr.containingRuleOrCheckpointSection()
            val ruleLike = section?.getParentRuleOrCheckPoint()
            if (ruleLike != null) {
                val typeEvalContext = TypeEvalContext.codeAnalysis(expr.project, expr.containingFile)
                val type = typeEvalContext.getType(ruleLike.wildcardsElement)
                if (type is SmkWildcardsType) {
                    val wildcards = type.getWildcards()
                    if (wildcards != null) {
                        // check if our expr has a name as one of wildcards:
                        if (referencedName in wildcards) {
                            // ensure that reference isn't resolved to some other element
                            if (ref.resolve() == null) {
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
}

class InsertWildcardsQuickFix(expr: SmkSLReferenceExpressionImpl) : LocalQuickFixOnPsiElement(expr) {
    override fun getFamilyName() = SnakemakeBundle.message("INSP.INTN.add.wildcards.prefix")

    override fun getText() = familyName

    override fun invoke(project: Project, file: PsiFile, startElement: PsiElement, endElement: PsiElement) {
        val doc = PsiDocumentManager.getInstance(project).getDocument(file)
        doc!!.insertString(startElement.textOffset, "wildcards.")
        PsiDocumentManager.getInstance(project).commitDocument(doc)
    }
}