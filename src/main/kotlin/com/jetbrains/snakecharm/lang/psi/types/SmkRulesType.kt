package com.jetbrains.snakecharm.lang.psi.types

import com.jetbrains.snakecharm.lang.SnakemakeNames.SMK_VARS_RULES
import com.jetbrains.snakecharm.lang.psi.SMKRule
import com.jetbrains.snakecharm.lang.psi.SnakemakeFile
import com.jetbrains.snakecharm.lang.psi.stubs.SmkRuleNameIndex.Companion.KEY

class SmkRulesType(
        containingRule: SMKRule?,
        smkFile: SnakemakeFile
) : AbstractSmkRuleOrCheckpointType<SMKRule>(
        containingRule, SMK_VARS_RULES, KEY, SMKRule::class.java
) {
    override val currentFileDeclarations: List<SMKRule> by lazy {
        smkFile.collectRules().map { it.second }
    }
}