package com.jetbrains.snakecharm.lang.psi.types

import com.jetbrains.snakecharm.lang.SnakemakeNames
import com.jetbrains.snakecharm.lang.psi.SMKRule
import com.jetbrains.snakecharm.lang.psi.SnakemakeFile

class SmkRulesType(
        containingRule: SMKRule?,
        smkFile: SnakemakeFile
) : AbstractSmkRuleOrCheckpointType<SMKRule>(
        containingRule, smkFile.collectRules(), SnakemakeNames.SMK_VARS_RULES
)