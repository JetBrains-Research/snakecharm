package com.jetbrains.snakecharm.inspections

import com.intellij.codeInspection.LocalInspectionTool
import com.jetbrains.snakecharm.SnakemakeBundle
import org.intellij.lang.annotations.Pattern
import org.jetbrains.annotations.Nls

abstract class SnakemakeInspection : LocalInspectionTool() {
    @Pattern(VALID_ID_PATTERN)
    override fun getID(): String = getShortName(super.getID())

    @Nls
    override fun getGroupDisplayName(): String = SnakemakeBundle.message("INSP.GROUP.snakemake")

    override fun getShortName(): String = javaClass.simpleName

    override fun isEnabledByDefault(): Boolean = true
}
