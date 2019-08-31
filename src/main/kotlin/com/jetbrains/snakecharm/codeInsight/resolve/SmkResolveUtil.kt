package com.jetbrains.snakecharm.codeInsight.resolve

import com.intellij.lang.ASTNode
import com.jetbrains.python.PyNames
import com.jetbrains.python.psi.PyElement
import com.jetbrains.python.psi.PyUtil
import com.jetbrains.python.psi.resolve.RatedResolveResult
import com.jetbrains.snakecharm.lang.psi.SmkFile

object SmkResolveUtil {
    const val RATE_IMPLICIT_SYMBOLS = RatedResolveResult.RATE_HIGH
    const val RATE_NORMAL = RatedResolveResult.RATE_NORMAL

    fun getIncludedFiles(file: SmkFile): List<SmkFile> {
        val includedFiles = mutableListOf<SmkFile>()
        getIncludedFilesForFile(file, includedFiles, mutableSetOf())
        return includedFiles
    }

    private fun getIncludedFilesForFile(
            file: SmkFile,
            includedFiles: MutableList<SmkFile>,
            visitedFiles: MutableSet<SmkFile>
    ) {
        visitedFiles.add(file)
        val currentIncludes = file.collectIncludes()
                .flatMap { it.references.toList() }
                .map { it.resolve() }
                .filterIsInstance<SmkFile>()
        includedFiles.addAll(currentIncludes)
        currentIncludes.forEach {
            if (it !in visitedFiles) {
                getIncludedFilesForFile(it, includedFiles, visitedFiles)
            }
        }
    }

    fun <T : PyElement> renameNameNode(newElementName: String, nameElement: ASTNode?, element: T): T {
        val newElementName = newElementName
        //            val newElementName = newElementName.trim()
        if (nameElement != null && PyNames.isIdentifier(newElementName)) {
            val newNameElement = PyUtil.createNewName(element, newElementName)
            element.node.replaceChild(nameElement, newNameElement);
        }
        return element
    }
}