package com.jetbrains.snakecharm.inspections.quickfix

import com.intellij.codeInspection.LocalQuickFixOnPsiElement
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.InvalidVirtualFileAccessException
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.jetbrains.snakecharm.SmkNotifier
import com.jetbrains.snakecharm.SnakemakeBundle
import java.io.IOException

class CreateEnvFile(expr: PsiElement, private val fileName: String) : LocalQuickFixOnPsiElement(expr) {
    private val defaultContext = """
        channels:
        dependencies:
    """.trimIndent()

    override fun getFamilyName() = SnakemakeBundle.message("INSP.NAME.conda.env.missing.fix", fileName)

    override fun getText() = familyName

    override fun invoke(project: Project, file: PsiFile, startElement: PsiElement, endElement: PsiElement) {
        val tokens = fileName.split('/')
        val targetName = tokens.lastOrNull() ?: return
        var currentFile = file.virtualFile.parent
        try {
            tokens.forEach {
                if (it == targetName) {
                    val resultVirtualFile = currentFile.createChildData(this, targetName)
                    val resultPsiFile = PsiManager.getInstance(project).findFile(resultVirtualFile) ?: return
                    val doc = PsiDocumentManager.getInstance(project).getDocument(resultPsiFile) ?: return
                    doc.insertString(0, defaultContext)
                    return
                }
                currentFile = if (it == "..") {
                    currentFile.parent ?: return
                } else {
                    currentFile.findChild(it) ?: currentFile.createChildDirectory(this, it)
                }
            }
        } catch (e: InvalidVirtualFileAccessException) {
            SmkNotifier.notifyTargetFileIsInvalid(currentFile.name, project)
        } catch (e: IOException) {
            SmkNotifier.notifyImpossibleToCreateFileOrDirectory(currentFile.name, project)
        }
    }

}