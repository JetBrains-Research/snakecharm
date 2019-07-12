package com.jetbrains.snakecharm.codeInsight

import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.python.psi.PyReferenceExpression
import com.jetbrains.python.psi.types.PyType
import com.jetbrains.python.psi.types.PyTypeProviderBase
import com.jetbrains.python.psi.types.TypeEvalContext
import com.jetbrains.snakecharm.lang.SnakemakeLanguageDialect
import com.jetbrains.snakecharm.lang.SnakemakeNames.SMK_VARS_CHECKPOINTS
import com.jetbrains.snakecharm.lang.SnakemakeNames.SMK_VARS_RULES
import com.jetbrains.snakecharm.lang.psi.SmkCheckPoint
import com.jetbrains.snakecharm.lang.psi.SmkRule
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
            SMK_VARS_RULES -> SmkRulesType(
                    PsiTreeUtil.getParentOfType(referenceExpression, SmkRule::class.java),
                    psiFile as SnakemakeFile
            )
            SMK_VARS_CHECKPOINTS -> SmkCheckPointsType(
                    PsiTreeUtil.getParentOfType(referenceExpression, SmkCheckPoint::class.java),
                    psiFile as SnakemakeFile
            )
            else -> null
        }
    }
}