package com.jetbrains.snakecharm.lang.parser

import com.intellij.lang.SyntaxTreeBuilder
import com.intellij.openapi.project.Project
import com.jetbrains.python.parsing.ParsingContext
import com.jetbrains.python.parsing.PyParser
import com.jetbrains.python.psi.LanguageLevel

/**
 * @author Roman.Chernyatchik
 * @date 2018-12-31
 */
class SnakemakeParser(val project: Project) : PyParser() {
    override fun createParsingContext(
        builder: SyntaxTreeBuilder?,
        languageLevel: LanguageLevel?
    ): ParsingContext = SmkParserContext(builder!!, languageLevel!!, project)
}