package com.jetbrains.snakecharm.stringLanguage.lang.parser

import com.intellij.lang.ASTNode
import com.intellij.openapi.project.Project
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.IFileElementType
import com.intellij.psi.tree.TokenSet
import com.jetbrains.python.PythonParserDefinition
import com.jetbrains.snakecharm.stringLanguage.SmkSLanguage
import com.jetbrains.snakecharm.stringLanguage.lang.psi.*
import com.jetbrains.snakecharm.stringLanguage.lang.psi.elementTypes.SmkSLElementTypes

class SmkSLParserDefinition : PythonParserDefinition() {
    private val FILE = IFileElementType(SmkSLanguage)

    override fun createElement(node: ASTNode): PsiElement =
        when (node.elementType) {
            SmkSLElementTypes.REFERENCE_EXPRESSION -> SmkSLReferenceExpressionImpl(node) // e.g. config in '{config}', {config[foo]}, {a.config.b}
            SmkSLElementTypes.SUBSCRIPTION_EXPRESSION -> SmkSLSubscriptionExpressionImpl(node)
            SmkSLElementTypes.KEY_EXPRESSION -> SmkSLSubscriptionIndexKeyExpressionImpl(node)
            else -> SmkSLElementImpl(node)
        }

    override fun getStringLiteralElements() = TokenSet.EMPTY!!

    override fun getCommentTokens() = TokenSet.EMPTY!!

    override fun getFileNodeType() = FILE

    override fun createLexer(project: Project) = SmkSLLexerAdapter()

    override fun createParser(project: Project) = SmkSLParser()

    override fun createFile(viewProvider: FileViewProvider) = SmkSLFile(viewProvider)
}
