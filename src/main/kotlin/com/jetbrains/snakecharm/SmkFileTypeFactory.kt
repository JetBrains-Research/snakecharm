package com.jetbrains.snakecharm

import com.intellij.openapi.fileTypes.ExactFileNameMatcher
import com.intellij.openapi.fileTypes.ExtensionFileNameMatcher
import com.intellij.openapi.fileTypes.FileTypeConsumer
import com.intellij.openapi.fileTypes.FileTypeFactory

/**
 * @author Roman.Chernyatchik
 * @date 2018-12-30
 */
class SmkFileTypeFactory: FileTypeFactory() {
    override fun createFileTypes(fileTypeConsumer: FileTypeConsumer) {
        fileTypeConsumer.consume(
                SmkFileType,
                ExactFileNameMatcher("Snakefile", true),
                ExtensionFileNameMatcher(SmkFileType.defaultExtension),
                ExtensionFileNameMatcher("rule"),
                ExtensionFileNameMatcher("rules"),
                ExtensionFileNameMatcher("Snakefile") // weird use-case from a real project
        )
    }
}