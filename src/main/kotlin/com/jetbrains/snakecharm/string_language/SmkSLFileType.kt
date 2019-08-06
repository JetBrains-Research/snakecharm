package com.jetbrains.snakecharm.string_language

import com.intellij.openapi.fileTypes.LanguageFileType
import javax.swing.Icon

object SmkSLFileType : LanguageFileType(SmkStringLanguage) {
    override fun getIcon(): Icon? = null

    override fun getName() = "SmkSL"

    override fun getDefaultExtension() = "smkStringLanguage"

    override fun getDescription() = "Snakemake string language file"
}
