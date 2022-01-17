package com.jetbrains.snakecharm.inspections.quickfix

import com.intellij.codeInspection.LocalQuickFixAndIntentionActionOnPsiElement
import com.intellij.notification.NotificationType
import com.intellij.openapi.command.undo.UndoManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.jetbrains.snakecharm.SmkNotifier
import com.jetbrains.snakecharm.SnakemakeBundle
import java.nio.file.InvalidPathException
import java.nio.file.Paths
import kotlin.io.path.Path

class CreateMissedFileQuickFix(
    element: PsiElement,
    private val fileName: String,
    private val sectionName: String,
    private val searchRelativelyToCurrentFolder: Boolean,
) : LocalQuickFixAndIntentionActionOnPsiElement(element) {
    companion object {
        private val LOGGER = Logger.getInstance(CreateMissedFileQuickFix::class.java)
    }

    override fun getFamilyName() = SnakemakeBundle.message("INSP.NAME.conda.env.missing.fix", fileName)

    override fun getText() = familyName

    override fun invoke(
        project: Project,
        file: PsiFile,
        editor: Editor?,
        startElement: PsiElement,
        endElement: PsiElement,
    ) {
        val vFile = file.virtualFile ?: return

        val fileIndex = ProjectRootManager.getInstance(project).fileIndex
        val relativeDirectory = when {
            searchRelativelyToCurrentFolder -> vFile.parent
            else -> fileIndex.getContentRootForFile(vFile)
        } ?: return

        val fileToCreatePath = try {
            when {
                Path(fileName).isAbsolute -> Path(fileName)
                else -> Paths.get(relativeDirectory.path, fileName)
            }
        } catch (e: InvalidPathException) {
            LOGGER.error(e)
            SmkNotifier.notify(
                title = SnakemakeBundle.message("notifier.msg.create.env.file.title"),
                content = SnakemakeBundle.message("notifier.msg.create.env.file.name.invalid.file.exception", fileName),
                type = NotificationType.ERROR,
                project = project
            )
            return
        }
        val action = CreateMissedFileUndoableAction(file, fileToCreatePath, sectionName)
        action.redo()
        UndoManager.getInstance(project).undoableActionPerformed(action)
    }
}