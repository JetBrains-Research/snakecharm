package com.jetbrains.snakecharm.lang.psi

import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiReferenceBase
import com.jetbrains.python.psi.PyStringLiteralExpression

open class SmkFileReference(
        element: SMKWorkflowParameterListStatement,
        textRange: TextRange
) : PsiReferenceBase<SMKWorkflowParameterListStatement>(element, textRange) {
    private val key = element.text.substring(textRange.startOffset, textRange.endOffset)

    protected fun collectFilesLike(predicate: (file: PsiFile) -> Boolean): Array<Any> {
        val parentDir = (element.parent as? PsiFile)?.originalFile?.virtualFile?.parent
                ?: return emptyArray()
        val psiManager = PsiManager.getInstance(element.project)
        return VfsUtil.collectChildrenRecursively(parentDir)
                .asSequence()
                .mapNotNull { psiManager.findFile(it) }
                .filter(predicate)
                .map {
                    LookupElementBuilder
                            .create(VfsUtil.getRelativeLocation(it.virtualFile, parentDir)!!)
                            .withIcon(it.getIcon(0))
                }
                .toList()
                .toTypedArray()
    }

    override fun resolve(): PsiElement? {
        val stringLiteral = element.argumentList?.arguments?.firstOrNull {
            it is PyStringLiteralExpression && it.text.removeSurrounding("\"") == key
        } ?: return null

        val parentFolder = element.containingFile.virtualFile?.parent ?: return null

        val filePath = (stringLiteral as? PyStringLiteralExpression)?.stringValue
        val vfm = VirtualFileManager.getInstance()

        // Trying to find the file anywhere
        val virtualFile = parentFolder.findFileByRelativePath(filePath!!) ?:
        vfm.getFileSystem(LocalFileSystem.PROTOCOL).findFileByPath(filePath) ?:
        vfm.findFileByUrl(filePath)

        return virtualFile?.let { PsiManager.getInstance(stringLiteral.project).findFile(it) }
    }
}

class SmkIncludeReference(
        element: SMKWorkflowParameterListStatement, textRange: TextRange
) : SmkFileReference(element, textRange) {
    override fun getVariants() = collectFilesLike {
        it is SnakemakeFile && it.name != element.containingFile.name
    }
}

class SmkConfigfileReference(
        element: SMKWorkflowParameterListStatement, textRange: TextRange
) : SmkFileReference(element, textRange) {
    override fun getVariants() = collectFilesLike {
        it.name.endsWith(".yaml") || it.name.endsWith(".yml")
    }
}

class SmkReportReference(
        element: SMKWorkflowParameterListStatement, textRange: TextRange
) : SmkFileReference(element, textRange) {
    override fun getVariants() = collectFilesLike {
        it.name.endsWith(".html")
    }
}