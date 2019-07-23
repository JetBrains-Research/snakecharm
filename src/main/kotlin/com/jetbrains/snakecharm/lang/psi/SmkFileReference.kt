package com.jetbrains.snakecharm.lang.psi

import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.*
import com.jetbrains.python.psi.PyStringLiteralExpression
import com.jetbrains.snakecharm.lang.psi.impl.SmkWorkflowArgsSectionImpl

open class SmkFileReference(
        element: SmkWorkflowArgsSectionImpl,
        textRange: TextRange,
        val path: String
) : PsiReferenceBase<SmkWorkflowArgsSectionImpl>(element, textRange) {
    // Reference caching can be implemented with the 'ResolveCache' class if needed

    protected fun collectFilesLike(predicate: (file: PsiFileSystemItem) -> Boolean): Array<Any> {
        val parentDirVFile = getParendDirForCompletion()?.virtualFile ?: return emptyArray()
        val psiManager = PsiManager.getInstance(element.project)
        return VfsUtil.collectChildrenRecursively(parentDirVFile)
                .asSequence()
                .mapNotNull { psiManager.findFile(it) }
                .filter(predicate)
                .map {
                    LookupElementBuilder
                            .create(VfsUtil.getRelativeLocation(it.virtualFile, parentDirVFile)!!)
                            .withIcon(it.getIcon(0))
                }
                .toList()
                .toTypedArray()
    }

    override fun resolve(): PsiElement? {
        val stringLiteral = element.argumentList?.arguments?.firstOrNull {
            it is PyStringLiteralExpression && it.stringValue == path
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

    // This function is supposed to get parent dir of 'IntelliJIdeaRulezzz' fake element
    // when autocompletion is invoked
    protected fun getParendDirForCompletion() = (element.parent as? PsiFile)?.originalFile?.containingDirectory
}

class SmkIncludeReference(
        element: SmkWorkflowArgsSectionImpl,
        textRange: TextRange,
        path: String
) : SmkFileReference(element, textRange, path) {
    override fun getVariants() = collectFilesLike {
        it is SmkFile && it.name != element.containingFile.name
    }
}

class SmkConfigfileReference(
        element: SmkWorkflowArgsSectionImpl,
        textRange: TextRange,
        path: String
) : SmkFileReference(element, textRange, path) {
    override fun getVariants() = collectFilesLike {
        it.name.endsWith(".yaml") || it.name.endsWith(".yml")
    }
}

class SmkReportReference(
        element: SmkWorkflowArgsSectionImpl,
        textRange: TextRange,
        path: String
) : SmkFileReference(element, textRange, path) {
    override fun getVariants() = collectFilesLike {
        it.name.endsWith(".html")
    }
}

class SmkWorkDirReference(
        element: SmkWorkflowArgsSectionImpl,
        textRange: TextRange,
        path: String
) : SmkFileReference(element, textRange, path) {
    override fun getVariants(): Array<Any>  {
        val parentDir = getParendDirForCompletion() ?: return emptyArray()
        val psiManager = PsiManager.getInstance(element.project)
        return VfsUtil.collectChildrenRecursively(parentDir.virtualFile)
                .asSequence()
                .mapNotNull {
                    psiManager.findDirectory(it)
                }
                .toList()
                .toTypedArray()
    }
}