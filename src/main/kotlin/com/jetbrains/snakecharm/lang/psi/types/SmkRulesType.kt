package com.jetbrains.snakecharm.lang.psi.types

import com.jetbrains.snakecharm.lang.SnakemakeNames.SMK_VARS_RULES
import com.jetbrains.snakecharm.lang.psi.SmkRule
import com.jetbrains.snakecharm.lang.psi.SnakemakeFile
import com.jetbrains.snakecharm.lang.psi.stubs.SmkRuleNameIndex.Companion.KEY

class SmkRulesType(
        containingRule: SmkRule?,
        smkFile: SnakemakeFile
) : AbstractSmkRuleOrCheckpointType<SmkRule>(
        containingRule, SMK_VARS_RULES, KEY, SmkRule::class.java
) {
    override val currentFileDeclarations: List<SmkRule> by lazy {
        smkFile.collectRules().map { it.second }
    }
}