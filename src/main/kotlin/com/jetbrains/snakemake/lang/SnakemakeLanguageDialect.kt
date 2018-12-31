package com.jetbrains.snakemake.lang

import com.intellij.lang.Language
import com.jetbrains.python.PythonLanguage

/**
 * @author Roman.Chernyatchik
 * @date 2018-12-31
 */
object SnakemakeLanguageDialect : Language(PythonLanguage.getInstance(), "Snakemake") {
     val fileElementType = SnakemakeFileElementType(this)
}