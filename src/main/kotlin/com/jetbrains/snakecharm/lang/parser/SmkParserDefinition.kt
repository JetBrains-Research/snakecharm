package com.jetbrains.snakecharm.lang.parser

import com.intellij.lang.PsiParser
import com.intellij.openapi.project.Project
import com.intellij.psi.FileViewProvider
import com.jetbrains.python.PythonParserDefinition
import com.jetbrains.snakecharm.lang.SnakemakeLanguageDialect
import com.jetbrains.snakecharm.lang.psi.SmkFile

/**
 * @author Roman.Chernyatchik
 * @date 2018-12-31
 */
class SmkParserDefinition: PythonParserDefinition() {
    override fun createLexer(project: Project) = SnakemakeLexer()

    override fun createParser(project: Project): PsiParser = SnakemakeParser(project)

    override fun getFileNodeType() = SnakemakeLanguageDialect.fileElementType

    override fun createFile(viewProvider: FileViewProvider) = SmkFile(viewProvider)
}