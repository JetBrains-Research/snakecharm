package com.jetbrains.snakemake.lang.highlighter

import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.containers.FactoryMap
import com.jetbrains.python.highlighting.PyHighlighter
import com.jetbrains.python.psi.LanguageLevel
import com.jetbrains.python.psi.PyUtil

/**
 * @author Roman.Chernyatchik
 * @date 2018-12-31
 */
class SnakemakeSyntaxHighlighterFactory : SyntaxHighlighterFactory() {
    private val myMap = FactoryMap.create<LanguageLevel, PyHighlighter> { key ->
        object : PyHighlighter(key) {
            override fun createHighlightingLexer(level: LanguageLevel) = SnakemakeHighlightingLexer(level)
        }
    }

    override fun getSyntaxHighlighter(project: Project?, virtualFile: VirtualFile?): SyntaxHighlighter {
        val level = when {
            project != null && virtualFile != null -> PyUtil.getLanguageLevelForVirtualFile(project, virtualFile)
            else -> LanguageLevel.getDefault()
        }

        return myMap[level]!!
    }
}