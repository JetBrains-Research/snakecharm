package com.jetbrains.snakecharm.inspections

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElement
import com.jetbrains.python.psi.PyClass
import com.jetbrains.python.psi.PyFunction
import com.jetbrains.python.psi.PyReferenceExpression
import com.jetbrains.snakecharm.SnakemakeBundle
import com.jetbrains.snakecharm.codeInsight.SnakemakeAPIProjectService
import com.jetbrains.snakecharm.framework.SmkSupportProjectSettings
import com.jetbrains.snakecharm.framework.SnakemakeFrameworkAPIProvider
import com.jetbrains.snakecharm.framework.snakemakeAPIAnnotations.SmkKeywordDeprecationParams
import com.jetbrains.snakecharm.lang.SmkLanguageVersion
import com.jetbrains.snakecharm.lang.SnakemakeNames.CHECKPOINT_KEYWORD
import com.jetbrains.snakecharm.lang.SnakemakeNames.MODULE_KEYWORD
import com.jetbrains.snakecharm.lang.SnakemakeNames.RULE_KEYWORD
import com.jetbrains.snakecharm.lang.SnakemakeNames.SUBWORKFLOW_KEYWORD
import com.jetbrains.snakecharm.lang.SnakemakeNames.USE_KEYWORD
import com.jetbrains.snakecharm.lang.SnakemakeNames.WORKFLOW_LOCALRULES_KEYWORD
import com.jetbrains.snakecharm.lang.SnakemakeNames.WORKFLOW_RULEORDER_KEYWORD
import com.jetbrains.snakecharm.lang.psi.*

class SmkDepreciatedKeywordsInspection : SnakemakeInspection() {
    override fun buildVisitor(
        holder: ProblemsHolder,
        isOnTheFly: Boolean,
        session: LocalInspectionToolSession
    ) = object : SnakemakeInspectionVisitor(holder, getContext(session)) {
        val apiService = SnakemakeAPIProjectService.getInstance(holder.project)
        val deprecationProvider = SnakemakeFrameworkAPIProvider.getInstance()
        val snakemakeSettings = SmkSupportProjectSettings.getInstance(holder.project)

        override fun visitSmkRule(rule: SmkRule) {
            checkTopLevelDefinition(rule, RULE_KEYWORD)
        }

        override fun visitSmkCheckPoint(checkPoint: SmkCheckPoint) {
            checkTopLevelDefinition(checkPoint, CHECKPOINT_KEYWORD)
        }

        override fun visitSmkSubworkflow(subworkflow: SmkSubworkflow) {
            checkTopLevelDefinition(subworkflow, SUBWORKFLOW_KEYWORD)
        }

        override fun visitSmkModule(module: SmkModule) {
            checkTopLevelDefinition(module, MODULE_KEYWORD)
        }

        override fun visitSmkUse(use: SmkUse) {
            checkTopLevelDefinition(use, USE_KEYWORD)
        }

        override fun visitSmkRuleOrCheckpointArgsSection(st: SmkRuleOrCheckpointArgsSection) {
            val parent = st.getParentRuleOrCheckPoint().sectionKeyword!!
            val name = st.sectionKeyword
            if (name != null) {
                checkSubSectionDefinition(st.firstChild, name, parent)
            }
        }

        override fun visitSmkRunSection(st: SmkRunSection) {
            val parent = st.getParentRuleOrCheckPoint().sectionKeyword!!
            val name = st.sectionKeyword
            if (name != null) {
                checkSubSectionDefinition(st.firstChild, name, parent)
            }
        }

        override fun visitSmkSubworkflowArgsSection(st: SmkSubworkflowArgsSection) {
            val name = st.sectionKeyword
            if (name != null) {
                checkSubSectionDefinition(st.firstChild, name, SUBWORKFLOW_KEYWORD)
            }
        }

        override fun visitSmkModuleArgsSection(st: SmkModuleArgsSection) {
            val name = st.sectionKeyword
            if (name != null) {
                checkSubSectionDefinition(st.firstChild, name, MODULE_KEYWORD)
            }
        }

        override fun visitSmkWorkflowArgsSection(st: SmkWorkflowArgsSection) {
            val name = st.sectionKeyword
            if (name != null) {
                checkTopLevelDefinition(st, name)
            }
        }

        override fun visitSmkWorkflowRuleorderSection(st: SmkWorkflowRuleorderSection) {
            checkTopLevelDefinition(st, WORKFLOW_RULEORDER_KEYWORD)
        }

        override fun visitSmkWorkflowLocalrulesSection(st: SmkWorkflowLocalrulesSection) {
            checkTopLevelDefinition(st, WORKFLOW_LOCALRULES_KEYWORD)
        }

        override fun visitPyReferenceExpression(node: PyReferenceExpression) {
            @Suppress("UnstableApiUsage") val callName = node.name ?: return
            val declaration = node.reference.resolve()

            // * Resolved
            if (declaration != null) {
                // resolved
                val fqn = if (declaration is PyFunction) {
                    declaration.qualifiedName
                } else if (declaration is PyClass) {
                    // e.g. 'input' is resolved into 'snakemake.io.InputFiles'
                    declaration.qualifiedName
                }  else {
                    null
                }
                if (fqn != null) {

                    // everything resolved OK
                    val deprecationEntry = apiService.getFunctionDeprecationByFqn(fqn)
                    if (deprecationEntry != null) {
                        showPropblem(deprecationEntry, fqn, node, true)
                    }
                }
                return
            }

            // * Unresolved
            val deprecationEntry = apiService.getFunctionDeprecationByShortName(callName)
            if (deprecationEntry != null && deprecationEntry.second.itemRemoved) {
                // XXX: only if removed we unexpected unresolved reference, let's ignore 'deprecated' case here
                // to reduce false-positive errors
                showPropblem(deprecationEntry, callName, node, false)
            }
        }

        fun showPropblem(
            deprecationEntry: Pair<SmkLanguageVersion, SmkKeywordDeprecationParams>,
            name: String,
            node: PsiElement,
            basedOnFqn: Boolean
        ) {
            val (version, deprecationDetails) = deprecationEntry

            val versionWithAdvice = if (deprecationDetails.advice != null) {
                SnakemakeBundle.message(
                    "INSP.NAME.deprecated.keywords.version.and.advice.join",
                    version,
                    deprecationDetails.advice
                )
            } else {
                version
            }
            when {
                deprecationDetails.itemRemoved -> if (basedOnFqn) {
                    // use default 'error' highlighting'
                    registerProblem(
                        node,
                        SnakemakeBundle.message(
                            "INSP.NAME.deprecated.keywords.removed.func",
                            name,
                            versionWithAdvice
                        )
                    )
                } else {
                    registerProblem(
                        node,
                        SnakemakeBundle.message(
                            "INSP.NAME.deprecated.keywords.removed.unresolved.func",
                            name,
                            deprecationDetails.name,
                            versionWithAdvice
                        ),
                    )
                }

                else -> {
                    require(basedOnFqn)
                    val message = SnakemakeBundle.message(
                        "INSP.NAME.deprecated.keywords.deprecated.func",
                        name,
                        versionWithAdvice
                    )

                    registerProblem(node, message, ProblemHighlightType.WEAK_WARNING)
                }
            }
        }

        private fun checkTopLevelDefinition(smkSection: SmkSection, name: String) {
            val psiElement= smkSection.getSectionKeywordNode()?.psi
            if (psiElement == null) {
                return
            }

            val lowestVersion = deprecationProvider.getTopLevelIntroductionVersion(name)
            val currentVersionString = snakemakeSettings.snakemakeLanguageVersion
            val currentVersion = if (currentVersionString == null) null else SmkLanguageVersion(currentVersionString)
            if (lowestVersion != null
                && currentVersion != null
                && lowestVersion > currentVersion
            ) {
                registerProblem(
                    psiElement,
                    SnakemakeBundle.message(
                        "INSP.NAME.deprecated.keywords.introduced.top",
                        name,
                        lowestVersion,
                        currentVersion
                    ),
                    ProblemHighlightType.GENERIC_ERROR
                )
                return
            }
            if (currentVersion != null) {
                val deprecationEntry = deprecationProvider.getTopLevelDeprecation(name, currentVersion)
                if (deprecationEntry != null) {
                    val (version, deprecationDetails) = deprecationEntry
                    val versionWithAdvice = if (deprecationDetails.advice != null) {
                        SnakemakeBundle.message(
                            "INSP.NAME.deprecated.keywords.version.and.advice.join",
                            version,
                            deprecationDetails.advice
                        )
                    } else {
                        version
                    }
                    when  {
                        deprecationDetails.itemRemoved -> {
                            registerProblem(
                                psiElement,
                                SnakemakeBundle.message(
                                    "INSP.NAME.deprecated.keywords.removed.top",
                                    name,
                                    versionWithAdvice
                                ),
                                ProblemHighlightType.GENERIC_ERROR
                            )
                        }

                        else -> {
                            registerProblem(
                                psiElement,
                                SnakemakeBundle.message(
                                    "INSP.NAME.deprecated.keywords.deprecated.top",
                                    name,
                                    versionWithAdvice
                                ),
                                ProblemHighlightType.WEAK_WARNING
                            )
                        }
                    }
                }
            }
        }


        private fun checkSubSectionDefinition(psiElement: PsiElement, name: String, parentName: String) {
            val settings = SmkSupportProjectSettings.getInstance(holder.project)
            val deprecationProvider = SnakemakeFrameworkAPIProvider.getInstance()
            val lowestVersion = deprecationProvider.getSubSectionIntroductionVersion(name, parentName)
            val currentVersionString = settings.snakemakeLanguageVersion
            val currentVersion = if (currentVersionString == null) null else SmkLanguageVersion(currentVersionString)
            if (lowestVersion != null
                && currentVersion != null
                && lowestVersion > currentVersion
            ) {
                registerProblem(
                    psiElement,
                    SnakemakeBundle.message(
                        "INSP.NAME.deprecated.keywords.introduced.sub",
                        name,
                        parentName,
                        lowestVersion,
                        currentVersion
                    ),
                    ProblemHighlightType.GENERIC_ERROR
                )
                return
            }
            if (currentVersion != null) {
                val versAndParams = deprecationProvider.getSubsectionDeprecation(name, currentVersion, parentName)
                if (versAndParams != null) {
                    val (version, deprecationDetails) = versAndParams
                    val versionWithAdvice = if (deprecationDetails.advice != null) {
                        SnakemakeBundle.message(
                            "INSP.NAME.deprecated.keywords.version.and.advice.join",
                            version,
                            deprecationDetails.advice
                        )
                    } else {
                        version
                    }
                    when  {
                        deprecationDetails.itemRemoved -> {
                            val message = SnakemakeBundle.message(
                                    "INSP.NAME.deprecated.keywords.removed.sub",
                                    name,
                                    parentName,
                                    versionWithAdvice
                                )
                            registerProblem(psiElement, message, ProblemHighlightType.GENERIC_ERROR)
                        }

                        else -> {
                            val message =SnakemakeBundle.message(
                                    "INSP.NAME.deprecated.keywords.deprecated.sub",
                                    name,
                                    parentName,
                                    versionWithAdvice
                                )
                            registerProblem(psiElement, message, ProblemHighlightType.WEAK_WARNING)
                        }
                    }
                }
            }
        }
    }
}