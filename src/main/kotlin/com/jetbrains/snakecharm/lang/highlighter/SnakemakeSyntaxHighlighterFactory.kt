package com.jetbrains.snakecharm.lang.highlighter

import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.tree.IElementType
import com.intellij.util.containers.FactoryMap
import com.jetbrains.python.PyTokenTypes
import com.jetbrains.python.highlighting.PyHighlighter
import com.jetbrains.python.psi.LanguageLevel
import com.jetbrains.python.psi.impl.PythonLanguageLevelPusher

/**
 * @author Roman.Chernyatchik
 * @date 2018-12-31
 */
class SnakemakeSyntaxHighlighterFactory : SyntaxHighlighterFactory() {
    companion object {
        val SMK_KEYWORD = TextAttributesKey.createTextAttributesKey("SMK_KEYWORD",
            DefaultLanguageHighlighterColors.KEYWORD
        )
        val SMK_FUNC_DEFINITION = TextAttributesKey.createTextAttributesKey("SMK_FUNC_DEFINITION",
            DefaultLanguageHighlighterColors.FUNCTION_DECLARATION
        )
        val SMK_DECORATOR = TextAttributesKey.createTextAttributesKey("SMK_DECORATOR",
            DefaultLanguageHighlighterColors.METADATA
        )
        val SMK_PREDEFINED_DEFINITION: TextAttributesKey =
            PyHighlighter.PY_PREDEFINED_DEFINITION // IDK why, but explicit creating via '.createText...' works improperly

        val SMK_TEXT = TextAttributesKey.createTextAttributesKey("SMK_TEXT", DefaultLanguageHighlighterColors.STRING)
    }

    private val myMap = FactoryMap.create<LanguageLevel, PyHighlighter> { key ->
        object : PyHighlighter(key) {
            override fun getTokenHighlights(tokenType: IElementType?): Array<TextAttributesKey> {
                if (tokenType === PyTokenTypes.SINGLE_QUOTED_UNICODE) {
                    return arrayOf(SMK_TEXT)
                }
                return super.getTokenHighlights(tokenType)
            }

            override fun createHighlightingLexer(level: LanguageLevel) = SnakemakeHighlightingLexer(level)
        }
    }

    override fun getSyntaxHighlighter(project: Project?, virtualFile: VirtualFile?): SyntaxHighlighter {
        val level = when {
            project != null && virtualFile != null -> PythonLanguageLevelPusher.getLanguageLevelForVirtualFile(project, virtualFile)
            else -> LanguageLevel.getDefault()
        }

        return getSyntaxHighlighterForLanguageLevel(level)
    }

    fun getSyntaxHighlighterForLanguageLevel(level: LanguageLevel): SyntaxHighlighter = myMap[level]!!
}