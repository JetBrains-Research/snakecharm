package com.jetbrains.snakecharm.string_language.lang.parser

import com.intellij.lang.ASTNode
import com.intellij.lang.ParserDefinition
import com.intellij.openapi.project.Project
import com.intellij.psi.FileViewProvider
import com.intellij.psi.tree.IFileElementType
import com.intellij.psi.tree.TokenSet
import com.jetbrains.snakecharm.string_language.SmkSLFile
import com.jetbrains.snakecharm.string_language.SmkSL
import com.jetbrains.snakecharm.string_language.lang.psi.SmkSLElement

class SmkSLParserDefinition : ParserDefinition {
    companion object {
        val FILE = IFileElementType(SmkSL)
    }

    override fun createElement(node: ASTNode) = SmkSLElement(node)

    override fun getStringLiteralElements()= TokenSet.EMPTY!!

    override fun getCommentTokens() = TokenSet.EMPTY!!

    override fun getFileNodeType() = FILE

    override fun createLexer(project: Project) = SmkSLLexerAdapter()

    override fun createParser(project: Project) = SmkSLParser()

    override fun createFile(viewProvider: FileViewProvider) = SmkSLFile(viewProvider)
}
