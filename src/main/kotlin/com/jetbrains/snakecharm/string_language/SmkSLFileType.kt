package com.jetbrains.snakecharm.string_language

import com.intellij.openapi.fileTypes.LanguageFileType
import com.jetbrains.snakecharm.SmkFileType
import javax.swing.Icon

object SmkSLFileType : LanguageFileType(SmkSL) {
    override fun getIcon(): Icon = SmkFileType.icon

    override fun getName() = "SmkSL"

    override fun getDefaultExtension() = "smkStringLanguage"

    override fun getDescription() = "Snakemake formatted string"
}
