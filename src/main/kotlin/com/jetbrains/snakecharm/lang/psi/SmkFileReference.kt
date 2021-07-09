package com.jetbrains.snakecharm.lang.psi

import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.*
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.python.psi.PsiReferenceEx
import com.jetbrains.python.psi.PyElementGenerator
import com.jetbrains.python.psi.PyFormattedStringElement
import com.jetbrains.python.psi.PyStringLiteralExpression
import com.jetbrains.python.psi.types.TypeEvalContext

/**
 * If [searchRelativelyToCurrentFolder] is true - the file sought relative to the current folder
 * else relative to the project root
 * If [makePathRelativelyCurrentFolder] is true - the path built relative to the current folder
 * else relative to the project root
 */
open class SmkFileReference(
        element: SmkArgsSection,
        private val textRange: TextRange,
        private val stringLiteralExpression: PyStringLiteralExpression,
        private val path: String,
        private val searchRelativelyToCurrentFolder: Boolean = true,
        private val makePathRelativelyCurrentFolder: Boolean = true
) : PsiReferenceBase<SmkArgsSection>(element, textRange), PsiReferenceEx {
    // Reference caching can be implemented with the 'ResolveCache' class if needed

    override fun handleElementRename(newElementName: String): PsiElement {
        val replacedElem = element.findElementAt(textRange.startOffset) ?: return element

        val stringLiteral =  PsiTreeUtil.getParentOfType(replacedElem, PyStringLiteralExpression::class.java)!!
        val relativePathToSelf = "(.+)/".toRegex().find(stringLiteral.stringValue)?.value ?: ""

        val elementGenerator = PyElementGenerator.getInstance(element.project)
        val newStringLiteral =
                elementGenerator.createStringLiteral(stringLiteral, relativePathToSelf + newElementName)

        stringLiteral.replace(newStringLiteral)

        return element
    }

    // XXX: I thing better is a completion contributor with support of user-entered prefixes
    protected fun collectFileSystemItemLike(
            collectFiles: Boolean = true,
            predicate: (file: PsiFileSystemItem) -> Boolean = { true }): Array<Any> {
        val project = element.project

        val parentDirVFile = getParendDirForCompletion()?.virtualFile ?: return emptyArray()

        val psiManager = PsiManager.getInstance(project)
        val folders = when {
            searchRelativelyToCurrentFolder -> arrayOf(parentDirVFile)
            else -> ProjectRootManager.getInstance(project).contentRoots
        }

        return folders.flatMap { root ->
            VfsUtil.collectChildrenRecursively(root)
                .asSequence()
                .mapNotNull {
                    if (collectFiles) psiManager.findFile(it) else psiManager.findDirectory(it)
                }
                .filter(predicate)
                .map {
                    val vFile = it.virtualFile
                    val baseFile = when {
                        makePathRelativelyCurrentFolder -> parentDirVFile
                        else -> root
                    }
                    LookupElementBuilder
                        .create(relativePath(vFile, baseFile) ?: vFile.path)
                        .withIcon(it.getIcon(0))
                }
                .toList()
        }.toTypedArray()
    }

    /**
     * Build relative path even if baseFile isn't ancestor of targetFile
     */
    private fun relativePath(
        targetFile: VirtualFile,
        baseFile: VirtualFile
    ): String? {

        val relativePath = VfsUtil.getRelativePath(targetFile, baseFile)
        if (relativePath != null) {
            return relativePath
        }

        val ancestor = VfsUtil.getCommonAncestor(targetFile, baseFile)
        if (ancestor != null) {
            val buff = StringBuilder()
            var file = baseFile
            while (ancestor != file) {
                buff.append("../")
                file = file.parent!! // parent cannot be null here by design
            }
            buff.append(VfsUtil.getRelativePath(targetFile, ancestor)!!)
            return buff.toString()
        }

        return null
    }

    private fun findVirtualFile(): VirtualFile? {
        val relativeFile = if (!makePathRelativelyCurrentFolder) {
            //search in all content roots
            ProjectRootManager.getInstance(element.project).contentRoots.firstNotNullOfOrNull { root ->
                root.findFileByRelativePath(path)
            }
        } else {
            element.containingFile.originalFile.virtualFile?.parent
                ?.findFileByRelativePath(path)
        }
        if (relativeFile != null) {
            return relativeFile
        }
        // Trying to find the file anywhere
        val vfm = VirtualFileManager.getInstance()
        val localFS = vfm.getFileSystem(LocalFileSystem.PROTOCOL)
        return localFS.findFileByPath(path) ?: vfm.findFileByUrl(path)
    }

    override fun resolve(): PsiElement? = findPathToResolve()

    private fun findPathToResolve(): PsiElement?  {
        if (!couldBeParsed()) {
            return null
        }
        return findVirtualFile()?.let {
            if (it.isDirectory) {
                PsiManager.getInstance(element.project).findDirectory(it)
            } else {
                PsiManager.getInstance(element.project).findFile(it)
            }
        }
    }

    private fun couldBeParsed() = stringLiteralExpression.children.all { it !is PyFormattedStringElement }

    /**
     * This function is supposed to get parent dir of 'IntelliJIdeaRulezzz' fake element
     * when autocompletion is invoked
     */
    private fun getParendDirForCompletion() =
            PsiTreeUtil.getParentOfType(element, PsiFile::class.java)?.originalFile?.containingDirectory

    override fun getUnresolvedHighlightSeverity(typeEvalContext: TypeEvalContext?): HighlightSeverity =
            if (couldBeParsed()) {
                HighlightSeverity.ERROR
            } else {
                HighlightSeverity.WEAK_WARNING
            }

    override fun getUnresolvedDescription(): String? = null
}

/**
 * Must be in subdirectory of makefile parent
 * version 6.5.1
 */
class SmkIncludeReference(
        element: SmkArgsSection,
        textRange: TextRange,
        stringLiteralExpression: PyStringLiteralExpression,
        path: String
) : SmkFileReference(element, textRange, stringLiteralExpression, path) {
    override fun getVariants() =  collectFileSystemItemLike() {
        it is SmkFile && it.name != element.containingFile.name
    }
}

/**
 *Can be in any directory
 *version 6.5.1
 */
class SmkConfigfileReference(
        element: SmkArgsSection,
        textRange: TextRange,
        stringLiteralExpression: PyStringLiteralExpression,
        path: String
) : SmkFileReference(
            element,
            textRange,
            stringLiteralExpression,
            path,
            searchRelativelyToCurrentFolder = false,
            makePathRelativelyCurrentFolder = false
    ) {
    override fun getVariants() =  collectFileSystemItemLike() {
        isYamlFile(it)
    }
}

/**
 * Must be in subdirectory of makefile parent
 * version 6.5.1
 */
class SmkCondaEnvReference(
        element: SmkArgsSection,
        textRange: TextRange,
        stringLiteralExpression: PyStringLiteralExpression,
        path: String
) : SmkFileReference(
            element,
            textRange,
            stringLiteralExpression,
            path,
            searchRelativelyToCurrentFolder = false
    ) {
    override fun getVariants() =  collectFileSystemItemLike() {
        isYamlFile(it)
    }
}
private fun isYamlFile(it: PsiFileSystemItem) = it.name.endsWith(".yaml") || it.name.endsWith(".yml")

/**
 * Must be in subdirectory of makefile parent
 * version 6.5.1
 */
class SmkNotebookReference(
        element: SmkArgsSection,
        textRange: TextRange,
        stringLiteralExpression: PyStringLiteralExpression,
        path: String
) : SmkFileReference(
            element,
            textRange,
            stringLiteralExpression,
            path,
            searchRelativelyToCurrentFolder = false
    ) {
    override fun getVariants() =  collectFileSystemItemLike() {
        val name = it.name.lowercase()
        name.endsWith(".py.ipynb") or name.endsWith(".r.ipynb")
    }
}

/**
 * Must be in subdirectory of makefile parent
 * version 6.5.1
 */
class SmkReportReference(
        element: SmkArgsSection,
        textRange: TextRange,
        stringLiteralExpression: PyStringLiteralExpression,
        path: String
) : SmkFileReference(element, textRange, stringLiteralExpression, path) {
    override fun getVariants() = collectFileSystemItemLike() {
        it.name.endsWith(".html")
    }
}

/**
 * Must be in subdirectory of makefile parent
 * version 6.5.1
 */
class SmkWorkDirReference(
        element: SmkArgsSection,
        textRange: TextRange,
        stringLiteralExpression: PyStringLiteralExpression,
        path: String
) : SmkFileReference(element, textRange, stringLiteralExpression, path) {
    override fun getVariants() = collectFileSystemItemLike(collectFiles = false)
}
