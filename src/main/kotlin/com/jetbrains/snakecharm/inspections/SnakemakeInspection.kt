package com.jetbrains.snakecharm.inspections

import com.intellij.codeInspection.InspectionProfileEntry
import com.intellij.codeInspection.LocalInspectionTool
import com.jetbrains.snakecharm.SnakemakeBundle
import org.intellij.lang.annotations.Pattern
import org.jetbrains.annotations.Nls

abstract class SnakemakeInspection : LocalInspectionTool() {
    @Pattern(VALID_ID_PATTERN)
    override fun getID(): String {

        return InspectionProfileEntry.getShortName(super.getID())
    }

    @Nls
    override fun getGroupDisplayName(): String {
        return SnakemakeBundle.message("INSP.GROUP.snakemake")
    }

    override fun getShortName(): String {
        return javaClass.simpleName
    }

    override fun isEnabledByDefault(): Boolean {
        return true
    }
}