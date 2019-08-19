package com.jetbrains.snakecharm.codeInsight

import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.python.psi.PyReferenceExpression
import com.jetbrains.python.psi.types.PyType
import com.jetbrains.python.psi.types.PyTypeProviderBase
import com.jetbrains.python.psi.types.TypeEvalContext
import com.jetbrains.snakecharm.lang.SnakemakeLanguageDialect
import com.jetbrains.snakecharm.lang.SnakemakeNames.SMK_VARS_CHECKPOINTS
import com.jetbrains.snakecharm.lang.SnakemakeNames.SMK_VARS_RULES
import com.jetbrains.snakecharm.lang.psi.SmkCheckPoint
import com.jetbrains.snakecharm.lang.psi.SmkFile
import com.jetbrains.snakecharm.lang.psi.SmkRule
import com.jetbrains.snakecharm.lang.psi.SmkRuleOrCheckpoint
import com.jetbrains.snakecharm.lang.psi.types.SmkCheckPointsType
import com.jetbrains.snakecharm.lang.psi.types.SmkRulesType
import com.jetbrains.snakecharm.string_language.SmkSL

class SmkSectionTypeProvider : PyTypeProviderBase() {

    override fun getReferenceExpressionType(
            referenceExpression: PyReferenceExpression,
            context: TypeEvalContext
    ): PyType? {
        val psiFile: PsiFile?
        val parentDeclaration: SmkRuleOrCheckpoint?
        when {
            SnakemakeLanguageDialect.isInsideSmkFile(referenceExpression) -> {
                psiFile = referenceExpression.containingFile
                parentDeclaration = PsiTreeUtil.getParentOfType(referenceExpression, SmkRuleOrCheckpoint::class.java)
            }
            SmkSL.isInsideSmkSLFile(referenceExpression) -> {
                val languageManager =
                        InjectedLanguageManager.getInstance(referenceExpression.project)
                psiFile = languageManager.getTopLevelFile(referenceExpression)

                val host = languageManager.getInjectionHost(referenceExpression)
                parentDeclaration = PsiTreeUtil.getParentOfType(host, SmkRuleOrCheckpoint::class.java)
            }
            else -> return null
        }

        if (referenceExpression.children.isNotEmpty() || psiFile == null) {
            return null
        }

        // XXX: at the moment affects all "rules" variables in a *.smk file, better to
        // affect only "rules" which is resolved to appropriate place
        return when (referenceExpression.referencedName) {
            SMK_VARS_RULES -> SmkRulesType(
                    parentDeclaration as? SmkRule,
                    psiFile as SmkFile
            )
            SMK_VARS_CHECKPOINTS -> SmkCheckPointsType(
                    parentDeclaration as? SmkCheckPoint,
                    psiFile as SmkFile
            )
            else -> null
        }
    }
}