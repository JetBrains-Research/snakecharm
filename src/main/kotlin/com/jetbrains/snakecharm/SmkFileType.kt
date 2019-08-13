package com.jetbrains.snakecharm

import com.jetbrains.python.PythonFileType
import com.jetbrains.snakecharm.lang.SnakemakeLanguageDialect

/**
 * @author Roman.Chernyatchik
 * @date 2018-12-30
 */
object SmkFileType: PythonFileType(SnakemakeLanguageDialect) {
    override fun getIcon() = SnakemakeIcons.FILE
    override fun getName() = "Snakemake"
    override fun getDefaultExtension() = "smk"
    override fun getDescription() = "Snakemake Pipeline"
}