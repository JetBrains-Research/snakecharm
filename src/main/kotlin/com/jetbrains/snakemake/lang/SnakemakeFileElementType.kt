package com.jetbrains.snakemake.lang

import com.intellij.lang.Language
import com.jetbrains.python.psi.PyFileElementType

/**
 * @author Roman.Chernyatchik
 * @date 2018-12-31
 */
class SnakemakeFileElementType(language: Language) : PyFileElementType(language) {
    override fun getExternalId() = "snakemake.FILE"
}