package com.jetbrains.snakecharm.codeInsight

import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.python.psi.PyCallExpression
import com.jetbrains.python.psi.PyLambdaExpression
import com.jetbrains.python.psi.PyNamedParameter
import com.jetbrains.python.psi.PyReferenceExpression
import com.jetbrains.python.psi.types.PyType
import com.jetbrains.python.psi.types.PyTypeProviderBase
import com.jetbrains.python.psi.types.TypeEvalContext
import com.jetbrains.snakecharm.inspections.SmkLambdaRuleParamsInspection.Companion.ALLOWED_LAMBDA_ARGS
import com.jetbrains.snakecharm.lang.SnakemakeLanguageDialect
import com.jetbrains.snakecharm.lang.SnakemakeNames.SECTION_INPUT
import com.jetbrains.snakecharm.lang.SnakemakeNames.SECTION_OUTPUT
import com.jetbrains.snakecharm.lang.SnakemakeNames.SECTION_RESOURCES
import com.jetbrains.snakecharm.lang.SnakemakeNames.SMK_VARS_CHECKPOINTS
import com.jetbrains.snakecharm.lang.SnakemakeNames.SMK_VARS_RULES
import com.jetbrains.snakecharm.lang.SnakemakeNames.SMK_VARS_WILDCARDS
import com.jetbrains.snakecharm.lang.psi.*
import com.jetbrains.snakecharm.lang.psi.types.SmkCheckpointType
import com.jetbrains.snakecharm.lang.psi.types.SmkRulesType
import com.jetbrains.snakecharm.lang.psi.types.SmkSectionType
import com.jetbrains.snakecharm.lang.psi.types.SmkWildcardsType
import com.jetbrains.snakecharm.stringLanguage.SmkSL

class SmkSectionTypeProvider : PyTypeProviderBase() {
    // getParameterType(param, function, context) // only for function declarations, not lambdas
    // registerReturnType(classQualifiedName, methods, callback)
    // getReturnType(callable, context)
    // getCallType(function, callSite, context)
    // getGenericType(cls, context)
    // getGenericSubstitutions(cls, context)
    // getCallableType(callable, context)  // e.g. method calls

    override fun getReferenceType(
            referenceTarget: PsiElement,
            context: TypeEvalContext,
            anchor: PsiElement?
    ): Ref<PyType>? {
        // lambdas params types
        if (referenceTarget is PyNamedParameter && anchor is PyReferenceExpression) {
            return getLambdaParamType(referenceTarget)
        }

        // cannot assign SmkRulesType, SmkCheckPointsType here: anchor is null, only resolve
        // target is available

        return null
    }

    private fun getLambdaParamType(referenceTarget: PyNamedParameter): Ref<PyType>? {
        // in lambda
        val lambda = PsiTreeUtil.getParentOfType(
                referenceTarget, PyLambdaExpression::class.java
        ) ?: return null

        // in section, lambda not in call
        val parentSection = PsiTreeUtil.getParentOfType(
                lambda,
                SmkRuleOrCheckpointArgsSection::class.java,
                true,
                PyCallExpression::class.java
        ) ?: return null

        val ruleLike = PsiTreeUtil.getParentOfType(
                parentSection, SmkRuleOrCheckpoint::class.java
        ) ?: return null

        val allowedArgs = ALLOWED_LAMBDA_ARGS[parentSection.sectionKeyword] ?: emptyArray()
        val paramName = referenceTarget.text

        val isFstPositionalParam = !referenceTarget.isKeywordOnly
                && lambda.parameterList.parameters.indexOf(referenceTarget) == 0

        if (isFstPositionalParam || paramName in allowedArgs) {
            val type = when (paramName) {
                SECTION_INPUT, SECTION_OUTPUT, SECTION_RESOURCES -> {
                    ruleLike.getSectionByName(paramName)?.let { SmkSectionType(it) }
                }
                else -> {
                    // 1st pos parameter in lambda is wildcard
                    if (isFstPositionalParam) SmkWildcardsType(ruleLike) else null
                }
            }
            return type?.let { Ref.create(it) }
        }
        return null
    }
    
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

        // XXX: at the moment affects all "rules" variables in a *.smk file, better to
        // affect only "rules" which is resolved to appropriate place
        return when (referenceExpression.referencedName) {
            SMK_VARS_RULES -> SmkRulesType(
                    parentDeclaration as? SmkRule,
                    psiFile
            )
            SMK_VARS_CHECKPOINTS -> SmkCheckpointType(
                    parentDeclaration as? SmkCheckPoint,
                    psiFile
            )

            SMK_VARS_WILDCARDS -> parentDeclaration?.let {
                SmkWildcardsType(parentDeclaration)
            }
            else -> null
        }
    }
}