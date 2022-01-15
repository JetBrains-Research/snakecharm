package com.jetbrains.snakecharm.inspections.quickfix

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.undo.DocumentReference
import com.intellij.openapi.command.undo.DocumentReferenceManager
import com.intellij.openapi.command.undo.UndoableAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiFile
import com.jetbrains.snakecharm.SmkNotifier
import com.jetbrains.snakecharm.SnakemakeBundle
import com.jetbrains.snakecharm.lang.SnakemakeNames
import org.apache.commons.io.FileUtils
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.appendText
import kotlin.io.path.isDirectory
import kotlin.io.path.name
import kotlin.io.path.notExists

class CreateMissedFileUndoableAction(
    private val actionInvocationTarget: PsiFile,
    private val fileToCreatePath: Path,
    private val sectionName: String,
) : UndoableAction {

    companion object {
        private val LOGGER = Logger.getInstance(CreateMissedFileUndoableAction::class.java)
        private val condaDefaultContent = """
        channels:
        dependencies:
    """.trimIndent()

        // Update it if wee need default text
        val sectionToDefaultFileContent = mapOf(
            SnakemakeNames.SECTION_CONDA to condaDefaultContent,
            SnakemakeNames.SECTION_NOTEBOOK to null,
            SnakemakeNames.SECTION_SCRIPT to null,
            SnakemakeNames.MODULE_SNAKEFILE_KEYWORD to null,

            SnakemakeNames.WORKFLOW_CONFIGFILE_KEYWORD to null,
            SnakemakeNames.WORKFLOW_PEPFILE_KEYWORD to null,
            SnakemakeNames.WORKFLOW_PEPSCHEMA_KEYWORD to null
        )
    }

    private val actionInvocationTargetVFile = actionInvocationTarget.virtualFile!!
    private val project = actionInvocationTarget.project
    private val firstCreatedFileOrDir: Path
    private val firstCreatedFileOrDirParent: VirtualFile?

    init {
        // Memorize first directory that will be created & delete it on "undo" step
        var fileOrDirToCreate = fileToCreatePath
        while (fileOrDirToCreate.parent.notExists()) {
            fileOrDirToCreate = fileOrDirToCreate.parent ?: break
        }
        firstCreatedFileOrDir = fileOrDirToCreate

        val parent = fileOrDirToCreate.parent
        firstCreatedFileOrDirParent = when (parent) {
            null -> null
            else -> VirtualFileManager.getInstance().findFileByNioPath(parent)
        }
    }

    override fun undo() {
        // invoke in such way because:
        // (1) changing document inside undo/redo is not allowed (see ChangeFileEncodingAction)
        // (2) we don't want to use UI thread, e.g. if disk IO operation will be slow due to NFS, or etc.
        ProgressManager.getInstance().run(object : Task.Backgroundable(
            project,
            SnakemakeBundle.message("notifier.msg.deleting.env.file", fileToCreatePath.name),
            false
        ) {
            override fun run(indicator: ProgressIndicator) {
                doUndo()
            }

            override fun onSuccess() {
                refreshFS()
            }
        })
    }

    override fun redo() {
        // invoke in such way because:
        // (1) changing document inside undo/redo is not allowed (see ChangeFileEncodingAction)
        // (2) we don't want to use UI thread, e.g. if disk IO operation will be slow due to NFS, or etc.
        ProgressManager.getInstance().run(object : Task.Backgroundable(
            project,
            SnakemakeBundle.message("notifier.msg.creating.env.file", fileToCreatePath.name),
            false
        ) {
            override fun run(indicator: ProgressIndicator) {
                doRedo(fileToCreatePath, project)
            }

            override fun onSuccess() {
                refreshFS()
            }
        })
    }

    private fun Task.Backgroundable.refreshFS() {
        firstCreatedFileOrDirParent?.refresh(true, true) {
            ApplicationManager.getApplication().invokeLater(
                {
                    DaemonCodeAnalyzer.getInstance(project).restart(actionInvocationTarget)
                },
                project.disposed
            )
        }
    }

    override fun getAffectedDocuments(): Array<DocumentReference> {
        // * If not affected files - action not in undo/redo
        // * If 'all' affected files - action undo doesn't work

        // So:mark only the current editor file as affected to make feature convenient.
        // Otherwise, new file opening will be regarded as a new action
        // and 'undone' will be banned
        return arrayOf(DocumentReferenceManager.getInstance().create(actionInvocationTargetVFile))
    }

    override fun isGlobal() = true

    private fun doRedo(
        fileToCreate: Path,
        project: Project,
    ) {
        try {
            // file could be created in background by someone else or if action triggred twice
            if (fileToCreate.notExists()) {
                Files.createDirectories(fileToCreate.parent)
                val targetFile = Files.createFile(fileToCreate)

                val context = sectionToDefaultFileContent[sectionName]
                if (context != null) {
                    // We don't use the result of 'createChildFile()' because it has inappropriate
                    // type (and throws UnsupportedOperationException)
                    targetFile.appendText(context)
                }
            }
        } catch (e: SecurityException) {
            val message = e.message ?: "Error: ${e.javaClass.name}"
            SmkNotifier.notify(
                title = SnakemakeBundle.message("notifier.msg.create.env.file.title"),
                content = message,
                type = NotificationType.ERROR,
                project = project
            )
        } catch (e: IOException) {
            LOGGER.warn(e)
            SmkNotifier.notify(
                title = SnakemakeBundle.message("notifier.msg.create.env.file.title"),
                content = SnakemakeBundle.message(
                    "notifier.msg.create.env.file.io.exception", fileToCreate.name
                ),
                type = NotificationType.ERROR,
                project = project
            )
        }
    }

    private fun doUndo() {
        if (firstCreatedFileOrDir.notExists()) {
            return
        }

        when {
            firstCreatedFileOrDir.isDirectory() -> FileUtils.deleteDirectory(
                firstCreatedFileOrDir.toFile()
            )
            else -> Files.delete(firstCreatedFileOrDir)
        }
    }
}