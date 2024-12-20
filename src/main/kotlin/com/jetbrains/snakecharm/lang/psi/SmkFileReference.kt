package com.jetbrains.snakecharm.lang.psi

import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.*
import com.intellij.psi.impl.source.resolve.ResolveCache
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.python.psi.PsiReferenceEx
import com.jetbrains.python.psi.PyElementGenerator
import com.jetbrains.python.psi.PyFormattedStringElement
import com.jetbrains.python.psi.PyStringLiteralExpression
import com.jetbrains.python.psi.types.TypeEvalContext

/**
 * @param [searchRelativelyToCurrentFolder] If is true - the file sought relative to the current folder
 * else relative to the project root.
 */
open class SmkFileReference(
    element: SmkArgsSection,
    private val textRange: TextRange,
    private val stringLiteralExpression: PyStringLiteralExpression,
    val path: String,
    val searchRelativelyToCurrentFolder: Boolean = true,
) : PsiReferenceBase<SmkArgsSection>(element, textRange), PsiReferenceEx {
    // Reference caching can be implemented with the 'ResolveCache' class if needed

    companion object {
        // it is *not* final so that it can be changed in debug time. if set to false, caching is off
        private const val USE_CACHE = true
    }

    override fun handleElementRename(newElementName: String): PsiElement {
        val replacedElem = element.findElementAt(textRange.startOffset) ?: return element

        val stringLiteral = PsiTreeUtil.getParentOfType(replacedElem, PyStringLiteralExpression::class.java)!!
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
        predicate: (file: PsiFileSystemItem) -> Boolean = { true },
    ): Array<Any> {
        val project = element.project

        val parentDirVFile = getParendDirForCompletion()?.virtualFile ?: return emptyArray()

        val psiManager = PsiManager.getInstance(project)
        val folders = ProjectRootManager.getInstance(project).contentRoots

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
                        searchRelativelyToCurrentFolder -> parentDirVFile
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
        baseFile: VirtualFile,
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
        // Try to find using relative path
        val relativeFile = if (searchRelativelyToCurrentFolder) {
            element.containingFile.originalFile.virtualFile?.parent
                ?.findFileByRelativePath(path)
        } else {
            //search in all content roots
            ProjectRootManager.getInstance(element.project).contentRoots.firstNotNullOfOrNull { root ->
                root.findFileByRelativePath(path)
            }
        }
        if (relativeFile != null) {
            return relativeFile
        }

        // Trying to find the file anywhere
        val vfm = VirtualFileManager.getInstance()
        val localFS = vfm.getFileSystem(LocalFileSystem.PROTOCOL)
        return localFS.findFileByPath(path) ?: vfm.findFileByUrl(path)
    }

    override fun resolve(): PsiElement? = if (USE_CACHE) {
        val cache = ResolveCache.getInstance(element.project)
        cache.resolveWithCaching(this, MyResolver, true, false)
    } else {
        findPathToResolve()
    }

    open fun findPathToResolve(): PsiElement? {
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

    open fun hasAppropriateSuffix(): Boolean = false
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SmkFileReference) return false

        if (textRange != other.textRange) return false
        if (stringLiteralExpression != other.stringLiteralExpression) return false
        if (path != other.path) return false
        if (searchRelativelyToCurrentFolder != other.searchRelativelyToCurrentFolder) return false

        return true
    }

    override fun hashCode(): Int {
        var result = textRange.hashCode()
        result = 31 * result + stringLiteralExpression.hashCode()
        result = 31 * result + path.hashCode()
        result = 31 * result + searchRelativelyToCurrentFolder.hashCode()
        return result
    }
}

private object MyResolver : ResolveCache.AbstractResolver<SmkFileReference, PsiElement?> {
    override fun resolve(ref: SmkFileReference, incompleteCode: Boolean) = ref.findPathToResolve()
}

/**
 * Must be in subdirectory of makefile parent
 * version 6.5.1
 */
class SmkIncludeReference(
    element: SmkArgsSection,
    textRange: TextRange,
    stringLiteralExpression: PyStringLiteralExpression,
    path: String,
) : SmkFileReference(element, textRange, stringLiteralExpression, path) {
    override fun getVariants() = collectFileSystemItemLike {
        it is SmkFile && it.originalFile != element.containingFile.originalFile
    }

    override fun hasAppropriateSuffix() =
        (path.endsWith(".smk") || path == "Snakemake") && element.containingFile.virtualFile.path != path
}

/**
 * The path must built from working directory
 * We use contentRoots as working directory
 * version 6.5.1
 */
class SmkConfigfileReference(
    element: SmkArgsSection,
    textRange: TextRange,
    stringLiteralExpression: PyStringLiteralExpression,
    path: String,
) : SmkFileReference(
    element,
    textRange,
    stringLiteralExpression,
    path,
    searchRelativelyToCurrentFolder = false
) {
    override fun getVariants() = collectFileSystemItemLike {
        isYamlFile(it.name)
    }

    override fun hasAppropriateSuffix() = isYamlFile(path)
}

/**
 * The path must built from working directory
 * We use contentRoots as working directory
 * version 6.5.1
 */
class SmkPepfileReference(
    element: SmkArgsSection,
    textRange: TextRange,
    stringLiteralExpression: PyStringLiteralExpression,
    path: String,
) : SmkFileReference(
    element,
    textRange,
    stringLiteralExpression,
    path,
    searchRelativelyToCurrentFolder = false
) {
    override fun getVariants() = collectFileSystemItemLike {
        isYamlFile(it.name)
    }

    override fun hasAppropriateSuffix() = isYamlFile(path)
}

/**
 * The path must built from directory with current snakefile
 * version 6.5.1
 */
class SmkPepschemaReference(
    element: SmkArgsSection,
    textRange: TextRange,
    stringLiteralExpression: PyStringLiteralExpression,
    path: String,
) : SmkFileReference(element, textRange, stringLiteralExpression, path) {
    override fun getVariants() = collectFileSystemItemLike {
        isYamlFile(it.name)
    }

    override fun hasAppropriateSuffix() = isYamlFile(path)
}

/**
 * The path must built from directory with current snakefile
 * version 6.5.1 (rule) + 8.0.0 (workfklow)
 */
class SmkCondaEnvReference(
    element: SmkArgsSection,
    textRange: TextRange,
    stringLiteralExpression: PyStringLiteralExpression,
    path: String,
) : SmkFileReference(element, textRange, stringLiteralExpression, path) {
    override fun getVariants() = collectFileSystemItemLike {
        isYamlFile(it.name)
    }

    /**
     * W/o proper suffix is just local conda env name and cannot be verified => do not show
     * unresolved reference error
     */
    override fun isSoft() = !hasAppropriateSuffix() // W/o suffix just conda env name

    override fun hasAppropriateSuffix() = isYamlFile(path.lowercase())
}

private fun isYamlFile(it: String) = it.endsWith(".yaml") || it.endsWith(".yml")

/**
 * The path must built from directory with current snakefile
 * version 6.5.1
 */
class SmkNotebookReference(
    element: SmkArgsSection,
    textRange: TextRange,
    stringLiteralExpression: PyStringLiteralExpression,
    path: String,
) : SmkFileReference(element, textRange, stringLiteralExpression, path) {
    override fun getVariants() = collectFileSystemItemLike {
        val name = it.name.lowercase()
        name.endsWith(".ipynb")
    }

    override fun hasAppropriateSuffix() = path.endsWith(".ipynb")
}

/**
 * The path must built from directory with current snakefile
 * version 6.5.1
 */
class SmkScriptReference(
    element: SmkArgsSection,
    textRange: TextRange,
    stringLiteralExpression: PyStringLiteralExpression,
    path: String,
) : SmkFileReference(element, textRange, stringLiteralExpression, path) {
    override fun getVariants() = collectFileSystemItemLike {
        val name = it.name.lowercase()
        hasCorrectEnding(name)
    }

    override fun hasAppropriateSuffix() = hasCorrectEnding(path.lowercase())

    private fun hasCorrectEnding(name: String) =
        name.endsWith(".py") or name.endsWith(".r") or name.endsWith(".rmd") or name.endsWith(".jl") or name.endsWith(".rs")
}

/**
 * The path must built from directory with current snakefile
 * version 6.5.1
 */
class SmkReportReference(
    element: SmkArgsSection,
    textRange: TextRange,
    stringLiteralExpression: PyStringLiteralExpression,
    path: String,
) : SmkFileReference(element, textRange, stringLiteralExpression, path) {
    override fun getVariants() = collectFileSystemItemLike {
        it.name.endsWith(".html")
    }

    override fun hasAppropriateSuffix() = path.endsWith(".html")
}

/**
 * The path must built from directory with current snakefile
 * version 6.5.1
 */
class SmkWorkDirReference(
    element: SmkArgsSection,
    textRange: TextRange,
    stringLiteralExpression: PyStringLiteralExpression,
    path: String,
) : SmkFileReference(element, textRange, stringLiteralExpression, path) {
    override fun getVariants() = collectFileSystemItemLike(collectFiles = false)
}
