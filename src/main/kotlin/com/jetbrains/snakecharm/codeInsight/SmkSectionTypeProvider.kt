package com.jetbrains.snakecharm.codeInsight

import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.python.psi.PyReferenceExpression
import com.jetbrains.python.psi.types.PyType
import com.jetbrains.python.psi.types.PyTypeProviderBase
import com.jetbrains.python.psi.types.TypeEvalContext
import com.jetbrains.snakecharm.lang.SnakemakeLanguageDialect
import com.jetbrains.snakecharm.lang.SnakemakeNames.SMK_VARS_CHECKPOINTS
import com.jetbrains.snakecharm.lang.SnakemakeNames.SMK_VARS_RULES
import com.jetbrains.snakecharm.lang.SnakemakeNames.SMK_VARS_WILDCARDS
import com.jetbrains.snakecharm.lang.psi.SmkCheckPoint
import com.jetbrains.snakecharm.lang.psi.SmkFile
import com.jetbrains.snakecharm.lang.psi.SmkRule
import com.jetbrains.snakecharm.lang.psi.SmkRuleOrCheckpoint
import com.jetbrains.snakecharm.lang.psi.types.SmkCheckPointsType
import com.jetbrains.snakecharm.lang.psi.types.SmkRulesType
import com.jetbrains.snakecharm.lang.psi.types.SmkWildcardsType
import com.jetbrains.snakecharm.stringLanguage.SmkSL

class SmkSectionTypeProvider : PyTypeProviderBase() {

    override fun getReferenceExpressionType(
            referenceExpression: PyReferenceExpression,
            context: TypeEvalContext
    ): PyType? {
        val smkExpression = when {
            SnakemakeLanguageDialect.isInsideSmkFile(referenceExpression) -> referenceExpression
            SmkSL.isInsideSmkSLFile(referenceExpression) -> {
                val manager = InjectedLanguageManager.getInstance(referenceExpression.project)
                manager.getInjectionHost(referenceExpression)
            }
            else -> return null
        }

        val psiFile = smkExpression?.containingFile
        val parentDeclaration =
                PsiTreeUtil.getParentOfType(smkExpression, SmkRuleOrCheckpoint::class.java)

        if (referenceExpression.children.isNotEmpty() ||
                psiFile == null ||
                psiFile !is SmkFile) {
            return null
        }

        val referencedName = referenceExpression.referencedName

        // XXX: at the moment affects all "rules" variables in a *.smk file, better to
        // affect only "rules" which is resolved to appropriate place
        return when {
            referencedName == SMK_VARS_RULES -> SmkRulesType(
                    parentDeclaration as? SmkRule,
                    psiFile
            )
            referencedName == SMK_VARS_CHECKPOINTS -> SmkCheckPointsType(
                    parentDeclaration as? SmkCheckPoint,
                    psiFile
            )
            referencedName == SMK_VARS_WILDCARDS && parentDeclaration != null ->
                SmkWildcardsType(parentDeclaration)
            else -> null
        }
    }
}