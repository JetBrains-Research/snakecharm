package com.jetbrains.snakecharm.inspections.quickfix

import com.intellij.codeInspection.LocalQuickFixAndIntentionActionOnPsiElement
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.command.undo.*
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
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
import java.nio.file.Paths
import kotlin.io.path.isDirectory
import kotlin.io.path.name
import kotlin.io.path.notExists

class CreateMissedFile(
    element: PsiElement,
    private val fileName: String,
    private val sectionName: String,
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
        val targetFilePath = Paths.get(file.virtualFile.parent.path, fileName)
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
            VirtualFileManager.getInstance().syncRefresh()
        }
        val redo = Runnable {
            if (!supportedSections.containsKey(sectionName)) {
                return@Runnable
            }
            try {
                val directoryPath = Files.createDirectories(targetFilePath.parent)
                val directoryVirtualFile = VfsUtil.findFile(directoryPath, true) ?: return@Runnable
                LocalFileSystem.getInstance().createChildFile(this, directoryVirtualFile, targetFilePath.name)
                VirtualFileManager.getInstance().syncRefresh()
                val context = supportedSections[sectionName]
                if (context != null) {
                    // We don't use the result of 'createChildFile()' because it has inappropriate type (and throw UnsupportedOperationException)
                    targetFilePath.toFile().appendText(context)
                }
            } catch (e: SecurityException) {
                SmkNotifier.notifyTargetFileIsInvalid(fileName, project)
            } catch (e: IOException) {
                SmkNotifier.notifyImpossibleToCreateFileOrDirectory(fileName, project)
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
                arrayOf(DocumentReferenceManager.getInstance().create(file.virtualFile))

            override fun isGlobal() = true
        }
        action.redo()
        UndoManager.getInstance(project).undoableActionPerformed(action)
    }
}