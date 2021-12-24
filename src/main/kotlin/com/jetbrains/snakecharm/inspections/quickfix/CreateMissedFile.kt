package com.jetbrains.snakecharm.inspections.quickfix

import com.intellij.codeInspection.LocalQuickFixAndIntentionActionOnPsiElement
import com.intellij.notification.NotificationType
import com.intellij.openapi.command.undo.*
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
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
import kotlin.io.path.Path
import kotlin.io.path.isDirectory
import kotlin.io.path.notExists

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
        val relativeDirectory =
            if (searchRelativelyToCurrentFolder) file.virtualFile.parent else ProjectRootManager.getInstance(project).fileIndex.getContentRootForFile(
                file.virtualFile
            ) ?: return
        val targetFilePath = try {
            if (Path(fileName).isAbsolute) {
                Path(fileName)
            } else {
                Paths.get(relativeDirectory.path, fileName)
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
        var firstAffectedFile = targetFilePath
        while (firstAffectedFile.parent.notExists()) {
            firstAffectedFile = firstAffectedFile.parent ?: break
        }

        val undo = Runnable {
            if (firstAffectedFile == null || firstAffectedFile.notExists()) {
                return@Runnable
            }
            if (firstAffectedFile.isDirectory()) {
                FileUtils.deleteDirectory(firstAffectedFile.toFile())
            } else {
                Files.delete(firstAffectedFile)
            }
        }
        val redo = Runnable {
            try {
                Files.createDirectories(targetFilePath.parent)
                Files.createFile(targetFilePath)
                val context = supportedSections[sectionName]
                if (context != null) {
                    // We don't use the result of 'createChildFile()' because it has inappropriate type (and throws UnsupportedOperationException)
                    targetFilePath.toFile().appendText(context)
                }
            } catch (e: SecurityException) {
                LOGGER.error(e)
                val message = e.message ?: return@Runnable
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
        val action = object : UndoableAction {
            override fun undo() {
                // invoke in such way because:
                // (1) changing document inside undo/redo is not allowed (see ChangeFileEncodingAction)
                // (2) we don't want to use UI thread
                ProgressManager.getInstance().run(object : Task.Backgroundable(
                    project,
                    SnakemakeBundle.message("notifier.msg.deleting.env.file", fileName),
                    false
                ) {
                    override fun run(indicator: ProgressIndicator) {
                        undo.run()
                    }

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
                    override fun run(indicator: ProgressIndicator) {
                        redo.run()
                    }

                    override fun onSuccess() {
                        VirtualFileManager.getInstance().syncRefresh()
                    }
                })
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