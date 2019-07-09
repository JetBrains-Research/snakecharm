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
    private val key: String = element.text.substring(textRange.startOffset, textRange.endOffset)

    override fun resolve(): PsiElement? {
        val stringLiteral = (element as? SMKWorkflowParameterListStatement)?.
                argumentList?.children?.firstOrNull {
            it is PyStringLiteralExpression && it.text == key
        } ?: return null

        val parentFolder = stringLiteral.containingFile.virtualFile?.parent ?: return null

        val filePath = (stringLiteral as PyStringLiteralExpression).stringValue
        val vfm = VirtualFileManager.getInstance()

        // Trying to find the file anywhere
        val virtualFile = parentFolder.findFileByRelativePath(filePath) ?:
        vfm.getFileSystem(LocalFileSystem.PROTOCOL).findFileByPath(filePath) ?:
        vfm.findFileByUrl(filePath)

        return virtualFile?.let { PsiManager.getInstance(stringLiteral.project).findFile(it) }
    }
}