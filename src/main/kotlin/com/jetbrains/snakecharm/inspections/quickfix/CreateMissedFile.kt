package com.jetbrains.snakecharm.inspections.quickfix

import com.intellij.codeInspection.LocalQuickFixAndIntentionActionOnPsiElement
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.command.undo.*
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.jetbrains.snakecharm.SmkNotifier
import com.jetbrains.snakecharm.SnakemakeBundle
import com.jetbrains.snakecharm.lang.SnakemakeNames
import org.apache.commons.io.FileUtils
import java.io.IOException
import java.nio.file.Files
import java.nio.file.InvalidPathException
import java.nio.file.Paths
import kotlin.io.path.isDirectory
import kotlin.io.path.name
import kotlin.io.path.notExists

class CreateMissedFile(
    element: PsiElement,
    private val fileName: String,
    private val sectionName: String,
    private val searchRelativelyToCurrentFolder: Boolean
) : LocalQuickFixAndIntentionActionOnPsiElement(element) {
    companion object {
        private val condaDefaultContext = """
        channels:
        dependencies:
    """.trimIndent()

        // Update it if wee need default text
        val supportedSections = mapOf(
            SnakemakeNames.SECTION_CONDA to condaDefaultContext,
            SnakemakeNames.SECTION_NOTEBOOK to null,
            SnakemakeNames.SECTION_SCRIPT to null,
            SnakemakeNames.MODULE_SNAKEFILE_KEYWORD to null,

            SnakemakeNames.WORKFLOW_CONFIGFILE_KEYWORD to null,
            SnakemakeNames.WORKFLOW_PEPFILE_KEYWORD to null,
            SnakemakeNames.WORKFLOW_PEPSCHEMA_KEYWORD to null
        )
    }

    override fun getFamilyName() = SnakemakeBundle.message("INSP.NAME.conda.env.missing.fix", fileName)

    override fun getText() = familyName

    override fun invoke(
        project: Project,
        file: PsiFile,
        editor: Editor?,
        startElement: PsiElement,
        endElement: PsiElement
    ) {
        val relativeDirectory =
            if (searchRelativelyToCurrentFolder) file.virtualFile.parent else ProjectRootManager.getInstance(project).fileIndex.getContentRootForFile(
                file.virtualFile
            ) ?: return
        val targetFilePath = try {
            Paths.get(relativeDirectory.path, fileName)
        } catch (e: InvalidPathException) {
            SmkNotifier.notify(
                title = SnakemakeBundle.message("notifier.msg.create.env.file.title"),
                content = SnakemakeBundle.message("notifier.msg.create.env.file.name.invalid.file.exception", fileName),
                type = NotificationType.ERROR,
                project = project
            )
            return
        }
        var firstAffectedFile = targetFilePath
        while (firstAffectedFile.parent.notExists()) {
            firstAffectedFile = firstAffectedFile.parent ?: break
        }

        val undo = Runnable {
            firstAffectedFile ?: return@Runnable
            if (firstAffectedFile.isDirectory()) {
                FileUtils.deleteDirectory(firstAffectedFile.toFile())
            } else {
                Files.delete(firstAffectedFile)
            }
            VirtualFileManager.getInstance().asyncRefresh { }
        }
        val redo = Runnable {
            try {
                val directoryPath = Files.createDirectories(targetFilePath.parent)
                val directoryVirtualFile = VfsUtil.findFile(directoryPath, true) ?: return@Runnable
                LocalFileSystem.getInstance().createChildFile(this, directoryVirtualFile, targetFilePath.name)
                VirtualFileManager.getInstance().asyncRefresh { }
                val context = supportedSections[sectionName]
                if (context != null) {
                    // We don't use the result of 'createChildFile()' because it has inappropriate type (and throws UnsupportedOperationException)
                    targetFilePath.toFile().appendText(context)
                }
            } catch (e: SecurityException) {
                SmkNotifier.notify(
                    title = SnakemakeBundle.message("notifier.msg.create.env.file.title"),
                    content = SnakemakeBundle.message("notifier.msg.create.env.file.invalid.file.exception", fileName),
                    type = NotificationType.ERROR,
                    project = project
                )
            } catch (e: IOException) {
                SmkNotifier.notify(
                    title = SnakemakeBundle.message("notifier.msg.create.env.file.title"),
                    content = SnakemakeBundle.message("notifier.msg.create.env.file.io.exception", fileName),
                    type = NotificationType.ERROR,
                    project = project
                )
            }
        }
        val action = object : UndoableAction {
            override fun undo() {
                // invoke later because changing document inside undo/redo is not allowed (see ChangeFileEncodingAction)
                ApplicationManager.getApplication().invokeLater(undo, ModalityState.NON_MODAL, project.disposed)
            }

            override fun redo() {
                // invoke later because changing document inside undo/redo is not allowed (see ChangeFileEncodingAction)
                ApplicationManager.getApplication().invokeLater(redo, ModalityState.NON_MODAL, project.disposed)
            }

            override fun getAffectedDocuments(): Array<DocumentReference> =
            // We mark only the current file as affected to make feature convenient.
            // Otherwise, new file opening will be regarded as a new action
                // and 'undone' will be banned
                arrayOf(DocumentReferenceManager.getInstance().create(file.virtualFile))

            override fun isGlobal() = true
        }
        action.redo()
        UndoManager.getInstance(project).undoableActionPerformed(action)
    }
}