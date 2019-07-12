package com.jetbrains.snakecharm.lang

import com.intellij.lang.Language
import com.jetbrains.python.psi.PyFileElementType

/**
 * @author Roman.Chernyatchik
 * @date 2018-12-31
 */
class SmkFileElementType(language: Language) : PyFileElementType(language) {
    override fun getExternalId() = "snakemake.FILE"
}