package com.jetbrains.snakecharm.stringLanguage

import com.intellij.openapi.fileTypes.LanguageFileType
import com.jetbrains.snakecharm.SmkFileType

class SmkSLFileType : LanguageFileType(SmkSLanguage) {
    companion object {
        // XXX: IntelliJ platform requirement: instance static field
        @JvmStatic
        val INSTANCE = SmkSLFileType()
    }

    override fun getIcon() = SmkFileType.INSTANCE.icon

    override fun getName() = "SmkSL"

    override fun getDefaultExtension() = "smkStringLanguage"

    override fun getDescription() = "Snakemake formatted string"
}
