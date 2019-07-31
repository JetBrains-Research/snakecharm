package com.jetbrains.snakecharm.inspections

import com.intellij.codeInspection.*
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.jetbrains.python.psi.PyFile
import com.jetbrains.python.psi.PyStringLiteralExpression
import com.jetbrains.python.psi.PyTargetExpression
import com.jetbrains.snakecharm.SnakemakeBundle
import com.jetbrains.snakecharm.lang.SnakemakeNames
import com.jetbrains.snakecharm.lang.psi.SmkFile
import com.jetbrains.snakecharm.lang.psi.SmkRuleOrCheckpointArgsSection
import com.jetbrains.snakecharm.lang.psi.SmkWorkflowArgsSection
import java.util.regex.Pattern

class SmkWildcardsInShellSectionInspection : SnakemakeInspection() {
    private val wildcardsPrefix = "wildcards."

    override fun buildVisitor(
            holder: ProblemsHolder,
            isOnTheFly: Boolean,
            session: LocalInspectionToolSession
    ) = object : SnakemakeInspectionVisitor(holder, session) {
        private val wildcardPattern = Pattern.compile("\\{(.+?)}")

        override fun visitSmkRuleOrCheckpointArgsSection(st: SmkRuleOrCheckpointArgsSection) {
            if (st.sectionKeyword != SnakemakeNames.SECTION_SHELL) {
                return
            }

            val availableVariables = mutableListOf<PyTargetExpression>()
            availableVariables.addAll((st.containingFile as PyFile).topLevelAttributes)

            val includeStatements = (st.containingFile as SmkFile)
                    .findChildrenByClass(SmkWorkflowArgsSection::class.java)
                    .filter { it.sectionKeyword == SnakemakeNames.WORKFLOW_INCLUDE_KEYWORD }
            for (statement in includeStatements) {
                for (reference in statement.references) {
                    val resolvedFile = reference.resolve() as PyFile
                    availableVariables.addAll(resolvedFile.topLevelAttributes)
                }
            }

            val shellCommand = st.argumentList?.arguments?.firstOrNull { it is PyStringLiteralExpression } ?: return
            val wildcardMatcher = wildcardPattern.matcher(shellCommand.text)
            while (wildcardMatcher.find()) {
                val content = wildcardMatcher.group(1)
                if (!SmkRuleOrCheckpointArgsSection.PARAMS_NAMES.any { content.startsWith(it) } &&
                        !content.startsWith(wildcardsPrefix) && content !in availableVariables.map { it.name }) {
                    registerProblem(
                            shellCommand,
                            SnakemakeBundle.message("INSP.NAME.wildcards.in.shell.section"),
                            ProblemHighlightType.ERROR,
                            null,
                            TextRange(wildcardMatcher.start(1), wildcardMatcher.end(1)),
                            InsertWildcardsPrefixQuickFix()
                    )
                }
            }
        }
    }

    private inner class InsertWildcardsPrefixQuickFix : LocalQuickFix {
        override fun getFamilyName() = SnakemakeBundle.message("INSP.INTN.insert.wildcards.prefix")

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val offset = descriptor.textRangeInElement.startOffset
            val editor = FileEditorManager.getInstance(project).selectedTextEditor
            val document = editor?.document ?: return
            WriteCommandAction.runWriteCommandAction(project) {
                document.insertString(descriptor.psiElement.textOffset + offset, wildcardsPrefix)
            }
        }
    }
}