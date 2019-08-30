package com.jetbrains.snakecharm.codeInsight.resolve

import com.jetbrains.python.psi.resolve.RatedResolveResult
import com.jetbrains.snakecharm.lang.psi.SmkFile

class SmkResolveUtil {
    companion object {
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
    }
}