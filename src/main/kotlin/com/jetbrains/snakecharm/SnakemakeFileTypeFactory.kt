package com.jetbrains.snakecharm

import com.intellij.openapi.fileTypes.ExactFileNameMatcher
import com.intellij.openapi.fileTypes.ExtensionFileNameMatcher
import com.intellij.openapi.fileTypes.FileTypeConsumer
import com.intellij.openapi.fileTypes.FileTypeFactory

/**
 * @author Roman.Chernyatchik
 * @date 2018-12-30
 */
class SnakemakeFileTypeFactory: FileTypeFactory() {
    override fun createFileTypes(fileTypeConsumer: FileTypeConsumer) {
        fileTypeConsumer.consume(
                SnakemakeFileType,
                ExactFileNameMatcher("Snakefile", true),
                ExtensionFileNameMatcher(SnakemakeFileType.defaultExtension),
                ExtensionFileNameMatcher("rule")
        )
    }
}