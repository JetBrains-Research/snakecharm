package com.jetbrains.snakecharm.lang.psi.types

import com.jetbrains.snakecharm.codeInsight.SnakemakeApi.SMK_VARS_RULES
import com.jetbrains.snakecharm.lang.psi.SmkFile
import com.jetbrains.snakecharm.lang.psi.SmkRule
import com.jetbrains.snakecharm.lang.psi.stubs.SmkRuleNameIndexCompanion.KEY

class SmkRulesType(
        containingRule: SmkRule?,
        smkFile: SmkFile
) : AbstractSmkRuleOrCheckpointType<SmkRule>(
        containingRule, SMK_VARS_RULES, KEY, SmkRule::class.java
) {
    override val currentFileDeclarations: List<SmkRule> by lazy {
        smkFile.filterRulesPsi().map { it.second }
    }
}