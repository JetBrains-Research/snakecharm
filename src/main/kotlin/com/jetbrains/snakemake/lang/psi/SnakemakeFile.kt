package com.jetbrains.snakemake.lang.psi

import com.intellij.psi.FileViewProvider
import com.jetbrains.python.psi.impl.PyFileImpl
import com.jetbrains.snakemake.SnakemakeFileType
import com.jetbrains.snakemake.lang.SnakemakeLanguageDialect

/**
 * @author Roman.Chernyatchik
 * @date 2018-12-31
 */
class SnakemakeFile(viewProvider: FileViewProvider): PyFileImpl(viewProvider, SnakemakeLanguageDialect) {
    override fun getIcon(flags: Int) = SnakemakeFileType.icon

    override fun toString() = "SnakemakeFile: $name"

    override fun getStub() = null
}