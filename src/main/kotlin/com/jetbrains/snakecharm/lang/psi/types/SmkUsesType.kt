package com.jetbrains.snakecharm.lang.psi.types

import com.jetbrains.snakecharm.codeInsight.SnakemakeAPI
import com.jetbrains.snakecharm.lang.psi.SmkFile
import com.jetbrains.snakecharm.lang.psi.SmkUse
import com.jetbrains.snakecharm.lang.psi.stubs.SmkUseNameIndex

class SmkUsesType(
    containingUseSection: SmkUse?,
    smkFile: SmkFile
) : AbstractSmkRuleOrCheckpointType<SmkUse>(
    containingUseSection, SnakemakeAPI.SMK_VARS_RULES, SmkUseNameIndex.KEY, SmkUse::class.java
) {
    override val currentFileDeclarations: List<SmkUse> by lazy {
        smkFile.collectUses().map { it.second }
    }
}