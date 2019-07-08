package com.jetbrains.snakecharm.lang.psi

import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiReferenceBase
import com.jetbrains.python.psi.PyStringLiteralExpression

class SmkFileReference(
        element: PsiElement, textRange: TextRange
) : PsiReferenceBase<PsiElement>(element, textRange) {
    override fun resolve(): PsiElement? {
        if (element !is PyStringLiteralExpression) {
            return null
        }

        val parentFolder = element.containingFile.virtualFile?.parent ?: return null

        val filePath = (element as PyStringLiteralExpression).stringValue
        val vfm = VirtualFileManager.getInstance()

        // Trying to find the file anywhere
        val virtualFile = parentFolder.findFileByRelativePath(filePath) ?:
        vfm.getFileSystem(LocalFileSystem.PROTOCOL).findFileByPath(filePath) ?:
        vfm.findFileByUrl(filePath)

        return virtualFile?.let { PsiManager.getInstance(element.project).findFile(it) }
    }
}