package com.jetbrains.snakemake

import com.intellij.openapi.fileTypes.ExactFileNameMatcher
import com.intellij.openapi.fileTypes.FileTypeConsumer
import com.intellij.openapi.fileTypes.FileTypeFactory

/**
 * @author Roman.Chernyatchik
 * @date 2018-12-30
 */
class SnakemakeFileTypeFactory: FileTypeFactory() {
    override fun createFileTypes(fileTypeConsumer: FileTypeConsumer) {
        fileTypeConsumer.consume(SnakemakeFileType)
        fileTypeConsumer.consume(
                SnakemakeFileType,
                ExactFileNameMatcher("Snakefile", true)
        )
    }
}