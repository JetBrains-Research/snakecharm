package com.jetbrains.snakecharm.inspections.smksl

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.LocalQuickFixOnPsiElement
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.jetbrains.snakecharm.SnakemakeBundle
import com.jetbrains.snakecharm.inspections.SnakemakeInspection
import com.jetbrains.snakecharm.stringLanguage.lang.psi.SmkSLExpression
import com.jetbrains.snakecharm.stringLanguage.lang.psi.SmkSLSubscriptionIndexKeyExpression

class SmkSLQuotingMisuseInGetAccessorInspection : SnakemakeInspection() {
    override fun buildVisitor(
        holder: ProblemsHolder,
        isOnTheFly: Boolean,
        session: LocalInspectionToolSession,
    ) = object : SmkSLInspectionVisitor(holder, getContext(session)) {

        override fun visitSmkSLSubscriptionExpressionKey(expr: SmkSLSubscriptionIndexKeyExpression) {
            val keyStr = expr.text
            if (keyStr.startsWith('"') || keyStr.startsWith('\'')
                || keyStr.startsWith("\\'") || keyStr.startsWith("\\\"")
                || keyStr.endsWith('"') || keyStr.endsWith('\'')
            ) {
                registerProblem(
                    expr,
                    SnakemakeBundle.message("INSP.NAME.quoting.misuse.in.get.message"),
                    UnquoteQuickFix(expr))
            }
        }
    }
}

class UnquoteQuickFix(expr: SmkSLExpression) : LocalQuickFixOnPsiElement(expr) {
    override fun getFamilyName() = SnakemakeBundle.message("INSP.NAME.quoting.misuse.in.get.fix")

    override fun getText() = familyName

    override fun invoke(project: Project, file: PsiFile, startElement: PsiElement, endElement: PsiElement) {
        val doc = PsiDocumentManager.getInstance(project).getDocument(file)

        val endText = endElement.text
        val endOffsetOffset = endElement.textOffset + endElement.textLength - 1
        if (endText.endsWith("\\'") || endText.endsWith("\\\"")) {
            doc!!.deleteString(endOffsetOffset - 1, endOffsetOffset + 1)
        } else if (endText.endsWith('"') || endText.endsWith('\'')) {
            doc!!.deleteString(endOffsetOffset, endOffsetOffset + 1)
        }


        val startText = endElement.text
        val startOffset = startElement.textOffset
        if (startText.startsWith("\\'") || startText.startsWith("\\\"")) {
            doc!!.deleteString(startOffset, startOffset + 2)
        } else if (startText.startsWith('"') || startText.startsWith('\'')) {
            doc!!.deleteString(startOffset, startOffset + 1)
        }

        PsiDocumentManager.getInstance(project).commitDocument(doc!!)
    }
}