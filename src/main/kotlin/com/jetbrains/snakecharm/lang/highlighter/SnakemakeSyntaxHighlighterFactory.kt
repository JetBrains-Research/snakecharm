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
import com.jetbrains.snakecharm.lang.highlighter.SnakemakeSyntaxHighlighterAttributes.SMK_TEXT
import com.jetbrains.snakecharm.lang.highlighter.SnakemakeSyntaxHighlighterAttributes.SMK_TRIPLE_QUOTED_STRING

/**
 * @author Roman.Chernyatchik
 * @date 2018-12-31
 */
class SnakemakeSyntaxHighlighterFactory : SyntaxHighlighterFactory() {
    private val myMap = FactoryMap.create<LanguageLevel, PyHighlighter> { key ->
        object : PyHighlighter(key) {
            override fun getTokenHighlights(tokenType: IElementType?): Array<TextAttributesKey> {
                return when (tokenType) {
                    PyTokenTypes.SINGLE_QUOTED_UNICODE -> arrayOf(SMK_TEXT)
                    PyTokenTypes.TRIPLE_QUOTED_UNICODE -> arrayOf(SMK_TRIPLE_QUOTED_STRING)
                    else -> super.getTokenHighlights(tokenType)
                }
            }

            override fun createHighlightingLexer(level: LanguageLevel) = SnakemakeHighlightingLexer(level)
        }
    }

    override fun getSyntaxHighlighter(project: Project?, virtualFile: VirtualFile?): SyntaxHighlighter {
        val level = when {
            project != null && virtualFile != null -> PythonLanguageLevelPusher.getLanguageLevelForVirtualFile(
                project,
                virtualFile
            )
            else -> LanguageLevel.getDefault()
        }

        return getSyntaxHighlighterForLanguageLevel(level)
    }

    fun getSyntaxHighlighterForLanguageLevel(level: LanguageLevel): SyntaxHighlighter = myMap[level]!!
}

object SnakemakeSyntaxHighlighterAttributes {
    val SMK_KEYWORD = TextAttributesKey.createTextAttributesKey(
        "SMK_KEYWORD",
        DefaultLanguageHighlighterColors.KEYWORD
    )

    val SMK_FUNC_DEFINITION = TextAttributesKey.createTextAttributesKey(
        "SMK_FUNC_DEFINITION",
        DefaultLanguageHighlighterColors.FUNCTION_DECLARATION
    )

    val SMK_DECORATOR = TextAttributesKey.createTextAttributesKey(
        "SMK_DECORATOR",
        DefaultLanguageHighlighterColors.METADATA
    )

    val SMK_PREDEFINED_DEFINITION: TextAttributesKey =
        PyHighlighter.PY_PREDEFINED_DEFINITION // IDK why, but explicit creating via '.createText...' works improperly

    val SMK_KEYWORD_ARGUMENT: TextAttributesKey =
        PyHighlighter.PY_KEYWORD_ARGUMENT // The same to SMK_PREDEFINED_DEFINITION case

    val SMK_TEXT = TextAttributesKey.createTextAttributesKey("SMK_TEXT", DefaultLanguageHighlighterColors.STRING)

    val SMK_TRIPLE_QUOTED_STRING = TextAttributesKey.createTextAttributesKey(
        "SMK_TRIPLE_QUOTED_STRING",
        DefaultLanguageHighlighterColors.STRING
    )
}