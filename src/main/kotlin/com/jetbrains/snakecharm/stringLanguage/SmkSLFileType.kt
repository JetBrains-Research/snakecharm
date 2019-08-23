package com.jetbrains.snakecharm.stringLanguage

import com.intellij.openapi.fileTypes.LanguageFileType
import com.jetbrains.snakecharm.SmkFileType

object SmkSLFileType : LanguageFileType(SmkSL) {
    override fun getIcon()= SmkFileType.icon

    override fun getName() = "SmkSL"

    override fun getDefaultExtension() = "smkStringLanguage"

    override fun getDescription() = "Snakemake formatted string"
}
