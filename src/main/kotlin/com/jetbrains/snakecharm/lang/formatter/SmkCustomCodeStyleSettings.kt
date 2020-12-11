package com.jetbrains.snakecharm.lang.formatter

import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.codeStyle.CustomCodeStyleSettings
import com.jetbrains.snakecharm.lang.SnakemakeLanguageDialect

class SmkCustomCodeStyleSettings(settings: CodeStyleSettings):
    CustomCodeStyleSettings(SnakemakeLanguageDialect.id, settings) {

    // TODO: add useful options and implement in formatter, use PythonCodeStyleSettings
    //  or MarkdownCodeStyleSettings as Example
    // @JvmField
    //  var MAX_LINES_AROUND_HEADER: Int = 1
}