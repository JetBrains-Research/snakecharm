package com.jetbrains.snakecharm.codeInsight

import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.python.psi.*
import com.jetbrains.python.psi.types.PyType
import com.jetbrains.python.psi.types.PyTypeProviderBase
import com.jetbrains.python.psi.types.TypeEvalContext
import com.jetbrains.snakecharm.codeInsight.SnakemakeAPI.SECTION_ACCESSOR_CLASSES
import com.jetbrains.snakecharm.codeInsight.SnakemakeAPI.SMK_VARS_CHECKPOINTS
import com.jetbrains.snakecharm.codeInsight.SnakemakeAPI.SMK_VARS_RULES
import com.jetbrains.snakecharm.codeInsight.SnakemakeAPI.SMK_VARS_WILDCARDS
import com.jetbrains.snakecharm.codeInsight.SnakemakeAPI.WILDCARDS_ACCESSOR_CLASS
import com.jetbrains.snakecharm.inspections.SmkLambdaRuleParamsInspection.Companion.ALLOWED_LAMBDA_ARGS
import com.jetbrains.snakecharm.lang.SnakemakeLanguageDialect
import com.jetbrains.snakecharm.lang.SnakemakeNames.SECTION_INPUT
import com.jetbrains.snakecharm.lang.SnakemakeNames.SECTION_OUTPUT
import com.jetbrains.snakecharm.lang.SnakemakeNames.SECTION_RESOURCES
import com.jetbrains.snakecharm.lang.psi.*
import com.jetbrains.snakecharm.lang.psi.types.SmkCheckpointType
import com.jetbrains.snakecharm.lang.psi.types.SmkRulesType
import com.jetbrains.snakecharm.lang.psi.types.SmkSectionType
import com.jetbrains.snakecharm.lang.psi.types.SmkWildcardsType
import com.jetbrains.snakecharm.stringLanguage.SmkSL
import com.jetbrains.snakecharm.stringLanguage.lang.psi.references.SmkSLWildcardReference

class SmkTypeProvider : PyTypeProviderBase() {
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
        if (anchor is PyReferenceExpression) {
            // lambdas params types
            if (referenceTarget is PyNamedParameter) {
                return getLambdaParamType(referenceTarget)
            }

            // XXX: Cannot assign SmkRulesType, SmkCheckPointsType here: anchor is null, only resolve
            // target is available, we need anchor for [SmkRulesType] at the moment

            // 'run:' section: input, output, wildcards, ..
            if (referenceTarget is PyClass) {
                return getSectionAccessorInRunSection(referenceTarget, anchor)
            }
        }
        return null
    }

    private fun getSectionAccessorInRunSection(
            referenceTarget: PyClass,
            anchor: PyReferenceExpression
    ): Ref<PyType>? {

        val fqn = referenceTarget.qualifiedName

        val refTargetSection = SECTION_ACCESSOR_CLASSES[fqn]
        val type = when {
            refTargetSection != null -> {
                // check if in run section & rule
                val (_, ruleLike) = getParentSectionAndRuleLike(
                        anchor, SmkRunSection::class.java
                ) ?: return null

                ruleLike.getSectionByName(refTargetSection)?.let { SmkSectionType(it) }

            }
            fqn == WILDCARDS_ACCESSOR_CLASS -> {
                val ruleLike = PsiTreeUtil.getParentOfType(
                        anchor, SmkRuleOrCheckpoint::class.java
                ) ?: return null

                SmkWildcardsType(ruleLike)
            }
            else -> null
        }
        return type?.let { Ref.create(it) }
    }

    private fun getLambdaParamType(referenceTarget: PyNamedParameter): Ref<PyType>? {
        // in lambda
        val lambda = PsiTreeUtil.getParentOfType(
                referenceTarget, PyLambdaExpression::class.java
        ) ?: return null

        // in section, lambda not in call
        val (parentSection, ruleLike) = getParentSectionAndRuleLike(
                lambda, SmkRuleOrCheckpointArgsSection::class.java, PyCallExpression::class.java
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

    private fun <T: SmkSection> getParentSectionAndRuleLike(
            element: PsiElement,
            sectionClass: Class<T>,
            vararg sectionStopAt: Class<out PsiElement>
    ): Pair<T, SmkRuleOrCheckpoint>? {
        val section = PsiTreeUtil.getParentOfType(
                element, sectionClass, true, *sectionStopAt
        ) ?: return null

        val ruleLike = PsiTreeUtil.getParentOfType(
                section, SmkRuleOrCheckpoint::class.java
        ) ?: return null

        return section to ruleLike
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

        if (referenceExpression is SmkSLReferenceExpression) {
            if (referenceExpression.getReference() is SmkSLWildcardReference) {
                // is just a wildcard here
                return null
            }
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