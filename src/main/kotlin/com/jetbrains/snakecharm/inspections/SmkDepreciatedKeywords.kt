package com.jetbrains.snakecharm.inspections

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.psi.PsiElement
import com.jetbrains.python.psi.PyClass
import com.jetbrains.python.psi.PyFunction
import com.jetbrains.python.psi.PyReferenceExpression
import com.jetbrains.snakecharm.SnakemakeBundle
import com.jetbrains.snakecharm.framework.SmkFrameworkDeprecationProvider
import com.jetbrains.snakecharm.framework.SmkSupportProjectSettings
import com.jetbrains.snakecharm.framework.UpdateType
import com.jetbrains.snakecharm.lang.SmkVersion
import com.jetbrains.snakecharm.lang.SnakemakeNames.CHECKPOINT_KEYWORD
import com.jetbrains.snakecharm.lang.SnakemakeNames.MODULE_KEYWORD
import com.jetbrains.snakecharm.lang.SnakemakeNames.RULE_KEYWORD
import com.jetbrains.snakecharm.lang.SnakemakeNames.SUBWORKFLOW_KEYWORD
import com.jetbrains.snakecharm.lang.SnakemakeNames.USE_KEYWORD
import com.jetbrains.snakecharm.lang.SnakemakeNames.WORKFLOW_LOCALRULES_KEYWORD
import com.jetbrains.snakecharm.lang.SnakemakeNames.WORKFLOW_RULEORDER_KEYWORD
import com.jetbrains.snakecharm.lang.psi.*

class SmkDepreciatedKeywords : SnakemakeInspection() {
    override fun buildVisitor(
        holder: ProblemsHolder,
        isOnTheFly: Boolean,
        session: LocalInspectionToolSession
    ) = object : SnakemakeInspectionVisitor(holder, getContext(session)) {

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
            val parent = if (st.getParentRuleOrCheckPoint() is SmkRule) RULE_KEYWORD else CHECKPOINT_KEYWORD
            val name = st.name
            if (name != null) {
                checkSubSectionDefinition(st, name, parent)
            }
        }

        override fun visitSmkRunSection(st: SmkRunSection) {
            val parent = if (st.getParentRuleOrCheckPoint() is SmkRule) RULE_KEYWORD else CHECKPOINT_KEYWORD
            val name = st.name
            if (name != null) {
                checkSubSectionDefinition(st, name, parent)
            }
        }

        override fun visitSmkSubworkflowArgsSection(st: SmkSubworkflowArgsSection) {
            val name = st.name
            if (name != null) {
                checkSubSectionDefinition(st, name, SUBWORKFLOW_KEYWORD)
            }
        }

        override fun visitSmkModuleArgsSection(st: SmkModuleArgsSection) {
            val name = st.name
            if (name != null) {
                checkSubSectionDefinition(st, name, MODULE_KEYWORD)
            }
        }

        override fun visitSmkWorkflowArgsSection(st: SmkWorkflowArgsSection) {
            val name = st.name
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
            val declaration = node.reference.resolve()
            if (declaration is PyClass || declaration is PyFunction) {
                val settings = SmkSupportProjectSettings.getInstance(holder.project)
                val deprecationProvider = ApplicationManager.getApplication().service<SmkFrameworkDeprecationProvider>()
                val currentVersionString = settings.snakemakeVersion
                val currentVersion = if (currentVersionString == null) null else SmkVersion(currentVersionString)
                val name = node.name
                if (name != null && currentVersion != null) {
                    val issue = deprecationProvider.getFunctionCorrection(name, currentVersion)
                    if (issue != null) {
                        val versionWithAdvice = if (issue.second.first != null) {
                            issue.second.second.toString() + " - you should " + issue.second.first
                        } else {
                            issue.second.second.toString()
                        }
                        when (issue.first) {
                            UpdateType.REMOVED -> {
                                registerProblem(
                                    node,
                                    SnakemakeBundle.message(
                                        "INSP.NAME.deprecated.keywords.removed.func",
                                        name,
                                        versionWithAdvice
                                    ),
                                    ProblemHighlightType.GENERIC_ERROR
                                )
                            }
                            UpdateType.DEPRECATED -> {
                                registerProblem(
                                    node,
                                    SnakemakeBundle.message(
                                        "INSP.NAME.deprecated.keywords.deprecated.func",
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
        }


        private fun checkTopLevelDefinition(psiElement: PsiElement, name: String) {
            val settings = SmkSupportProjectSettings.getInstance(holder.project)
            val deprecationProvider = ApplicationManager.getApplication().service<SmkFrameworkDeprecationProvider>()
            val lowestVersion = deprecationProvider.getTopLevelIntroductionVersion(name)
            val currentVersionString = settings.snakemakeVersion
            val currentVersion = if (currentVersionString == null) null else SmkVersion(currentVersionString)
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
                val issue = deprecationProvider.getTopLevelCorrection(name, currentVersion)
                if (issue != null) {
                    val versionWithAdvice = if (issue.second.first != null) {
                        issue.second.second.toString() + " - you should " + issue.second.first
                    } else {
                        issue.second.second.toString()
                    }
                    when (issue.first) {
                        UpdateType.REMOVED -> {
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

                        UpdateType.DEPRECATED -> {
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
            val deprecationProvider = ApplicationManager.getApplication().service<SmkFrameworkDeprecationProvider>()
            val lowestVersion = deprecationProvider.getSubSectionIntroductionVersion(name, parentName)
            val currentVersionString = settings.snakemakeVersion
            val currentVersion = if (currentVersionString == null) null else SmkVersion(currentVersionString)
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
                val issue = deprecationProvider.getSubsectionCorrection(name, currentVersion, parentName)
                if (issue != null) {
                    val versionWithAdvice = if (issue.second.first != null) {
                        issue.second.second.toString() + " - you should " + issue.second.first
                    } else {
                        issue.second.second.toString()
                    }
                    when (issue.first) {
                        UpdateType.REMOVED -> {
                            registerProblem(
                                psiElement,
                                SnakemakeBundle.message(
                                    "INSP.NAME.deprecated.keywords.removed.sub",
                                    name,
                                    parentName,
                                    versionWithAdvice
                                ),
                                ProblemHighlightType.GENERIC_ERROR
                            )
                        }

                        UpdateType.DEPRECATED -> {
                            registerProblem(
                                psiElement,
                                SnakemakeBundle.message(
                                    "INSP.NAME.deprecated.keywords.deprecated.sub",
                                    name,
                                    parentName,
                                    versionWithAdvice
                                ),
                                ProblemHighlightType.WEAK_WARNING
                            )
                        }
                    }
                }
            }
        }


    }
}