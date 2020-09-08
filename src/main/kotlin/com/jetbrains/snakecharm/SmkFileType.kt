package com.jetbrains.snakecharm

import com.jetbrains.python.PythonFileType
import com.jetbrains.snakecharm.lang.SnakemakeLanguageDialect

/**
 * @author Roman.Chernyatchik
 * @date 2018-12-30
 */
class SmkFileType : PythonFileType(SnakemakeLanguageDialect) {
    companion object {
        // XXX: IntelliJ platform requirement: instance static field
        @JvmStatic
        val INSTANCE = SmkFileType()
    }

    override fun getIcon() = SnakemakeIcons.FILE
    override fun getName() = "Snakemake"
    override fun getDefaultExtension() = "smk"
    override fun getDescription() = "Snakemake Pipeline"
}