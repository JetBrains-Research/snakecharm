package com.jetbrains.snakecharm.stringLanguage.lang.parser

import com.intellij.lang.ASTNode
import com.intellij.openapi.project.Project
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.IFileElementType
import com.intellij.psi.tree.TokenSet
import com.jetbrains.python.PythonParserDefinition
import com.jetbrains.snakecharm.stringLanguage.SmkSLanguage
import com.jetbrains.snakecharm.stringLanguage.SmkSLFile
import com.jetbrains.snakecharm.stringLanguage.SmkSLTokenTypes
import com.jetbrains.snakecharm.stringLanguage.lang.psi.elementTypes.*

class SmkSLParserDefinition : PythonParserDefinition() {
    companion object {
        val FILE = IFileElementType(SmkSLanguage)
    }

    override fun createElement(node: ASTNode): PsiElement =
            when(node.elementType) {
                SmkSLTokenTypes.REFERENCE_EXPRESSION -> SmkSLReferenceExpressionImpl(node)
                SmkSLTokenTypes.SUBSCRIPTION_EXPRESSION -> SmkSLSubscriptionExpression(node)
                SmkSLTokenTypes.LANGUAGE -> SmkSLLanguageElement(node)
                SmkSLTokenTypes.KEY_EXPRESSION -> SmkSLKeyExpression(node)
                else -> SmkSLElementImpl(node)
            }

    override fun getStringLiteralElements()= TokenSet.EMPTY!!

    override fun getCommentTokens() = TokenSet.EMPTY!!

    override fun getFileNodeType() = FILE

    override fun createLexer(project: Project) = SmkSLLexerAdapter()

    override fun createParser(project: Project) = SmkSLParser()

    override fun createFile(viewProvider: FileViewProvider) = SmkSLFile(viewProvider)
}
