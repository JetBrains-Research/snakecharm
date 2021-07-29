package com.jetbrains.snakecharm.inspections

import com.intellij.codeInspection.*
import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.jetbrains.python.psi.*
import com.jetbrains.snakecharm.SnakemakeBundle
import com.jetbrains.snakecharm.lang.SnakemakeNames
import com.jetbrains.snakecharm.lang.psi.*
import com.jetbrains.snakecharm.stringLanguage.lang.psi.SmkSLFile

class SmkUnusedLogSectionInspection : SnakemakeInspection() {
    override fun buildVisitor(
        holder: ProblemsHolder,
        isOnTheFly: Boolean,
        session: LocalInspectionToolSession
    ) = object : SnakemakeInspectionVisitor(holder, session) {
        override fun visitSmkRule(rule: SmkRule) {
            visitSMKRuleLike(rule)
        }

        override fun visitSmkCheckPoint(checkPoint: SmkCheckPoint) {
            visitSMKRuleLike(checkPoint)
        }

        private fun visitSMKRuleLike(rule: SmkRuleLike<SmkArgsSection>) {
            var hasLog = false
            var logSection: SmkSection? = null
            var logUsed = false

            rule.getSections().forEach { section ->
                val name = section.sectionKeyword ?: return

                val collector = SmkSLSectionReferencesCollector("log")
                section.accept(collector)
                val sections = collector.getSections()
                val z = 22 + sections.size
//                when (name) {
//                    SnakemakeNames.SECTION_LOG -> {
//                        hasLog = true
//                        logSection = section
//                    }
//                    SnakemakeNames.SECTION_SHELL -> logUsed = logUsed or hasShellSectionLogReference(section)
//                    SnakemakeNames.SECTION_RUN -> logUsed = logUsed or hasRunSectionWildcard(section)
//                }
            }

//            if (hasLog && !logUsed) {
//                registerProblem(
//                    logSection ?: return,
//                    SnakemakeBundle.message("INSP.NAME.unused.section"),
//                    ProblemHighlightType.LIKE_UNUSED_SYMBOL,
//                    null,
//                    RemoveSectionQuickFix
//                )
//            }
        }

        private fun hasShellSectionLogReference(shellSection: SmkSection): Boolean {
            val argsSection = shellSection.children.firstOrNull { it is PyArgumentList } // Searches for argument list
            if (argsSection != null) {
                val stringExpression = // Searches for string expression
                    (argsSection as PyArgumentList).arguments.firstOrNull { it is StringLiteralExpression }
                if (stringExpression != null) { // If it's found, checks log reference
                    return expressionContainsLogRef(stringExpression as StringLiteralExpression)
                }
            }
            return false
        }

        private fun hasRunSectionWildcard(runSection: SmkSection): Boolean {
            val argsSection = runSection.children.firstOrNull { it is PyStatementList } ?: return false
            val statements = (argsSection as PyStatementList).statements // Run section statements
            statements.forEach { statement -> // Checks each statement in order to find 'shell' expression
                if (statement is PyExpressionStatement) {
                    val expression = statement.expression
                    if (expression.firstChild.text == SnakemakeNames.SECTION_SHELL) {
                        // If there is, checks its string param
                        val argList = expression.children.firstOrNull { it is PyArgumentList }
                        if (argList != null) {
                            val stringExpression = argList.children.firstOrNull { it is StringLiteralExpression }
                            if (stringExpression != null) {
                                return expressionContainsLogRef(stringExpression as StringLiteralExpression)
                            }
                        }
                    }

                }
            }
            return false
        }

        /**
         * Checks if [expression] contains log section reference
         */
        private fun expressionContainsLogRef(expression: StringLiteralExpression): Boolean {
            // Is there any options to access injection from here?
            val regex = "\\{log.*}".toRegex()
            return regex.findAll(expression.text).count() > 0
        }
    }

    private object RemoveSectionQuickFix : LocalQuickFix {
        override fun getFamilyName() = SnakemakeBundle.message("INSP.NAME.remove.unused.section")

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            WriteCommandAction.runWriteCommandAction(project) { descriptor.psiElement.delete() }
        }
    }
}