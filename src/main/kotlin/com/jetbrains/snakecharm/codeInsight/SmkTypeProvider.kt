package com.jetbrains.snakecharm.codeInsight

import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.QualifiedName
import com.jetbrains.python.psi.*
import com.jetbrains.python.psi.resolve.fromSdk
import com.jetbrains.python.psi.resolve.resolveQualifiedName
import com.jetbrains.python.psi.types.PyType
import com.jetbrains.python.psi.types.PyTypeProviderBase
import com.jetbrains.python.psi.types.TypeEvalContext
import com.jetbrains.snakecharm.codeInsight.SnakemakeApi.SECTION_ACCESSOR_CLASSES
import com.jetbrains.snakecharm.framework.SmkSupportProjectSettings
import com.jetbrains.snakecharm.lang.SnakemakeLanguageDialect
import com.jetbrains.snakecharm.lang.SnakemakeNames.SMK_VARS_CHECKPOINTS
import com.jetbrains.snakecharm.lang.SnakemakeNames.SMK_VARS_PEP
import com.jetbrains.snakecharm.lang.SnakemakeNames.SMK_VARS_RULES
import com.jetbrains.snakecharm.lang.SnakemakeNames.SMK_VARS_WILDCARDS
import com.jetbrains.snakecharm.lang.SnakemakeNames.WILDCARDS_ACCESSOR_CLASS
import com.jetbrains.snakecharm.lang.psi.*
import com.jetbrains.snakecharm.lang.psi.impl.SmkPsiUtil
import com.jetbrains.snakecharm.lang.psi.types.SmkCheckpointType
import com.jetbrains.snakecharm.lang.psi.types.SmkRuleLikeSectionArgsType
import com.jetbrains.snakecharm.lang.psi.types.SmkRulesType
import com.jetbrains.snakecharm.lang.psi.types.SmkWildcardsType
import com.jetbrains.snakecharm.stringLanguage.SmkSLanguage
import com.jetbrains.snakecharm.stringLanguage.lang.psi.SmkSLReferenceExpressionImpl

class SmkTypeProvider : PyTypeProviderBase() {
    // TODO: provide types for 'run:' : threads, version, wildcards, rule, jobid,...
    //  collectPyFiles("builtins", usedFiles).get(0).findTopLevelClass("str")

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
        if (!SmkPsiUtil.isInsideSnakemakeOrSmkSLFile(anchor)) {
            return null
        }

        if (anchor is PyReferenceExpression) {
            // lambdas params types
            if (referenceTarget is PyNamedParameter) {
                return getLambdaParamType(referenceTarget)
            }

            // XXX: Cannot assign SmkRulesType, SmkCheckPointsType here: anchor is null, only resolve
            // target is available, we need anchor for [SmkRulesType] at the moment

            // 'run:' section: input, output, wildcards, ..
            if (referenceTarget is PyClass) {
                return getSectionAccessorInRunSection(referenceTarget, anchor, context)
            }
        }
        return null
    }

    private fun getSectionAccessorInRunSection(
        referenceTarget: PyClass,
        anchor: PyReferenceExpression,
        context: TypeEvalContext
    ): Ref<PyType>? {

        val fqn = referenceTarget.qualifiedName

        val refTargetSection = SECTION_ACCESSOR_CLASSES[fqn]
        val type = when {
            refTargetSection != null -> {
                // check if in run section & rule
                val (_, ruleLike) = getParentSectionAndRuleLike(
                    anchor, SmkRunSection::class.java
                ) ?: return null

                ruleLike.getSectionByName(refTargetSection)?.let { SmkRuleLikeSectionArgsType(it) }

            }

            fqn == WILDCARDS_ACCESSOR_CLASS -> {
                val ruleLike = PsiTreeUtil.getParentOfType(
                    anchor, SmkRuleOrCheckpoint::class.java
                ) ?: return null

                context.getType(ruleLike.wildcardsElement)
            }

            else -> null
        }
        return type?.let { Ref.create(it) }
    }

    private fun getLambdaParamType(referenceTarget: PyNamedParameter): Ref<PyType>? {
        // in a lambda
        val lambda = PsiTreeUtil.getParentOfType(
            referenceTarget, PyLambdaExpression::class.java
        ) ?: return null

        // in a section, lambda not in call
        val (parentSection, ruleLike) = getParentSectionAndRuleLike(
            lambda, SmkRuleOrCheckpointArgsSection::class.java, PyCallExpression::class.java
        ) ?: return null

        val apiService = SnakemakeApiService.getInstance(lambda.project)
        val allowedArgs = apiService.getLambdaArgsForSubsection(
            parentSection.sectionKeyword, ruleLike.sectionKeyword
        )
        val paramName = referenceTarget.text

        @Suppress("UnstableApiUsage")
        val isFstPositionalParam = !referenceTarget.isKeywordOnly
                && lambda.parameterList.parameters.indexOf(referenceTarget) == 0

        var type: PyType? = null
        if (isFstPositionalParam) {
            // 1st positional parameter in a lambda is wildcard
            type = SmkWildcardsType(ruleLike)
        } else if (paramName in allowedArgs && paramName != SMK_VARS_WILDCARDS) {
            type = ruleLike.getSectionByName(paramName)?.let { SmkRuleLikeSectionArgsType(it) }
        }

        return type?.let { Ref.create(type) }
    }

    private fun <T : SmkSection> getParentSectionAndRuleLike(
        element: PsiElement,
        sectionClass: Class<T>,
        vararg sectionStopAt: Class<out PsiElement>,
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
        context: TypeEvalContext,
    ): PyType? {
        val smkExpression = when {
            SnakemakeLanguageDialect.isInsideSmkFile(referenceExpression) -> referenceExpression
            SmkSLanguage.isInsideSmkSLFile(referenceExpression) -> {
                val manager = InjectedLanguageManager.getInstance(referenceExpression.project)
                manager.getInjectionHost(referenceExpression)
            }

            else -> return null
        }

        val psiFile = smkExpression?.containingFile
        val parentDeclaration =
            PsiTreeUtil.getParentOfType(smkExpression, SmkRuleOrCheckpoint::class.java)
// TODO
//        if (referenceExpression.asQualifiedName()?.matches(SMK_VARS_PEP, SMK_VARS_CONFIG) == true) {
//            return SmkPepConfigType(psiFile as SmkFile)
//        }
        if (referenceExpression.children.isNotEmpty() ||
            psiFile == null ||
            psiFile !is SmkFile
        ) {
            return null
        }

        if (referenceExpression is SmkSLReferenceExpressionImpl && referenceExpression.isWildcard()) {
            // is just a wildcard here
            return null
        }

        // XXX: at the moment affects all "rules" variables in a *.smk file, better to
        // affect only "rules" which is resolved to appropriate place
        @Suppress("UnstableApiUsage")
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

            SMK_VARS_PEP -> {
                // Assign correct type to `pep` variable in order to get resolve/completion
                val project = referenceExpression.project
                val sdk = SmkSupportProjectSettings.getInstance(project).getActiveSdk()
                if (sdk == null) {
                    null
                } else {
                    val resolveContext = fromSdk(project, sdk)
                    val pepFile = resolveQualifiedName(
                        QualifiedName.fromDottedString("peppy.project"),
                        resolveContext
                    ).filterIsInstance<PyFile>().firstOrNull()

                    pepFile?.findTopLevelClass("Project")?.getType(TypeEvalContext.codeCompletion(project, pepFile))
                }
            }
            // TODO: TYPE FOR 'snakemake' var in run section & python scripts
            else -> null
        }
    }
}