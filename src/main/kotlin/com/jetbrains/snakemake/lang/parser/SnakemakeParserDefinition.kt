package com.jetbrains.snakemake.lang.parser

import com.intellij.lang.PsiParser
import com.intellij.openapi.project.Project
import com.intellij.psi.FileViewProvider
import com.intellij.psi.tree.TokenSet
import com.jetbrains.python.PythonParserDefinition
import com.jetbrains.snakemake.lang.SnakemakeLanguageDialect
import com.jetbrains.snakemake.lang.psi.SnakemakeFile

/**
 * @author Roman.Chernyatchik
 * @date 2018-12-31
 */
class SnakemakeParserDefinition: PythonParserDefinition() {
    override fun createLexer(project: Project) = SnakemakeLexer()

    override fun createParser(project: Project): PsiParser = SnakemakeParser()

    override fun getFileNodeType() = SnakemakeLanguageDialect.fileElementType

    override fun createFile(viewProvider: FileViewProvider) = SnakemakeFile(viewProvider)
}