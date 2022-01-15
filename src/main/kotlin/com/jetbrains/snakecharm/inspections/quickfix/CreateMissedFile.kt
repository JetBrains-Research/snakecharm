package com.jetbrains.snakecharm.inspections.quickfix

import com.intellij.codeInspection.LocalQuickFixAndIntentionActionOnPsiElement
import com.intellij.notification.NotificationType
import com.intellij.openapi.command.undo.DocumentReference
import com.intellij.openapi.command.undo.DocumentReferenceManager
import com.intellij.openapi.command.undo.UndoManager
import com.intellij.openapi.command.undo.UndoableAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.VirtualFile
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
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.*

class CreateMissedFile(
    element: PsiElement,
    private val fileName: String,
    private val sectionName: String,
    private val searchRelativelyToCurrentFolder: Boolean
) : LocalQuickFixAndIntentionActionOnPsiElement(element) {
    companion object {
        private val LOGGER = Logger.getInstance(CreateMissedFile::class.java)

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
        val vFile = file.virtualFile ?: return

        val fileIndex = ProjectRootManager.getInstance(project).fileIndex
        val relativeDirectory = when {
            searchRelativelyToCurrentFolder -> vFile.parent
            else -> fileIndex.getContentRootForFile(vFile)
        } ?: return

        val fileToCreate = try {
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
        val action = createdUndoableBackgroundAction(
            project, vFile, undoLambda(fileToCreate), redoLambda(fileToCreate, project)
        )
        action.redo()
        UndoManager.getInstance(project).undoableActionPerformed(action)
    }

    private fun createdUndoableBackgroundAction(
        project: Project,
        actionInvocationTarget: VirtualFile,
        undoStep: () -> Unit,
        redoStep: () -> Unit
    ) = object : UndoableAction {
        override fun undo() {
            // invoke in such way because:
            // (1) changing document inside undo/redo is not allowed (see ChangeFileEncodingAction)
            // (2) we don't want to use UI thread
            ProgressManager.getInstance().run(object : Task.Backgroundable(
                project,
                SnakemakeBundle.message("notifier.msg.deleting.env.file", fileName),
                false
            ) {
                override fun run(indicator: ProgressIndicator) = undoStep()

                override fun onSuccess() {
                    VirtualFileManager.getInstance().syncRefresh()
                }
            })
        }

        override fun redo() {
            // invoke in such way because:
            // (1) changing document inside undo/redo is not allowed (see ChangeFileEncodingAction)
            // (2) we don't want to use UI thread
            ProgressManager.getInstance().run(object : Task.Backgroundable(
                project,
                SnakemakeBundle.message("notifier.msg.creating.env.file", fileName),
                false
            ) {
                override fun run(indicator: ProgressIndicator) = redoStep()

                override fun onSuccess() {
                    VirtualFileManager.getInstance().syncRefresh()
                    // TODO: write action is required!
                }
            })
        }

        override fun getAffectedDocuments(): Array<DocumentReference> {
            // * If not affected files - action not in undo/redo
            // * If 'all' affected files - action undo doesn't work

            // So:mark only the current editor file as affected to make feature convenient.
            // Otherwise, new file opening will be regarded as a new action
            // and 'undone' will be banned
            return arrayOf(DocumentReferenceManager.getInstance().create(actionInvocationTarget))
        }

        override fun isGlobal() = true
    }

    private fun redoLambda(
        fileToCreate: Path,
        project: Project,
    ): () -> Unit = {
        try {
            Files.createDirectories(fileToCreate.parent)
            val targetFile = Files.createFile(fileToCreate)
            val context = supportedSections[sectionName]
            if (context != null) {
                // We don't use the result of 'createChildFile()' because it has inappropriate
                // type (and throws UnsupportedOperationException)
                targetFile.appendText(context)
            }
        } catch (e: SecurityException) {
            LOGGER.error(e)
            val message = e.message ?: "Error: ${e.javaClass.name}"
            SmkNotifier.notify(
                title = SnakemakeBundle.message("notifier.msg.create.env.file.title"),
                content = message,
                type = NotificationType.ERROR,
                project = project
            )
        } catch (e: IOException) {
            LOGGER.error(e)
            SmkNotifier.notify(
                title = SnakemakeBundle.message("notifier.msg.create.env.file.title"),
                content = SnakemakeBundle.message("notifier.msg.create.env.file.io.exception", fileName),
                type = NotificationType.ERROR,
                project = project
            )
        }
    }

    private fun undoLambda(fileToCreate: Path): () -> Unit {
        // Memorize first directory that will be created & delete it on "undo" step
        var firstCreatedFileOrDir = fileToCreate
        while (firstCreatedFileOrDir.parent.notExists()) {
            firstCreatedFileOrDir = firstCreatedFileOrDir.parent ?: break
        }

        val undo = {
            if (firstCreatedFileOrDir.exists()) {
                if (firstCreatedFileOrDir.isDirectory()) {
                    FileUtils.deleteDirectory(firstCreatedFileOrDir.toFile())
                } else {
                    Files.delete(firstCreatedFileOrDir)
                }
            }
        }
        return undo
    }
}