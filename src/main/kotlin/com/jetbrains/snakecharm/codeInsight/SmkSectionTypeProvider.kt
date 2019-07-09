package com.jetbrains.snakecharm.codeInsight

import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.python.psi.PyReferenceExpression
import com.jetbrains.python.psi.types.PyType
import com.jetbrains.python.psi.types.PyTypeProviderBase
import com.jetbrains.python.psi.types.TypeEvalContext
import com.jetbrains.snakecharm.lang.SnakemakeLanguageDialect
import com.jetbrains.snakecharm.lang.psi.SMKCheckPoint
import com.jetbrains.snakecharm.lang.psi.SMKRule
import com.jetbrains.snakecharm.lang.psi.SnakemakeFile
import com.jetbrains.snakecharm.lang.psi.types.SmkCheckPointsType
import com.jetbrains.snakecharm.lang.psi.types.SmkRulesType

class SmkSectionTypeProvider : PyTypeProviderBase() {

    override fun getReferenceExpressionType(
            referenceExpression: PyReferenceExpression,
            context: TypeEvalContext
    ): PyType? {

        if (!SnakemakeLanguageDialect.isInsideSmkFile(referenceExpression)) {
            return null
        }

        val psiFile = referenceExpression.containingFile

        // XXX: at the moment affects all "rules" variables in a *.smk file, better to
        // affect only "rules" which is resolved to appropriate place
        return when (referenceExpression.referencedName) {
            "rules" -> SmkRulesType(
                    PsiTreeUtil.getParentOfType(referenceExpression, SMKRule::class.java),
                    psiFile as SnakemakeFile
            )
            "checkpoints" -> SmkCheckPointsType(
                    PsiTreeUtil.getParentOfType(referenceExpression, SMKCheckPoint::class.java),
                    psiFile as SnakemakeFile
            )
            else -> null
        }
    }
}