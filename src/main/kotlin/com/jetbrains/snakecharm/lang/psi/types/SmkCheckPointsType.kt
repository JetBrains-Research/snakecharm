package com.jetbrains.snakecharm.lang.psi.types

import com.jetbrains.snakecharm.lang.SnakemakeNames
import com.jetbrains.snakecharm.lang.psi.SMKCheckPoint
import com.jetbrains.snakecharm.lang.psi.SnakemakeFile

class SmkCheckPointsType(
        containingRule: SMKCheckPoint?,
        smkFile: SnakemakeFile
) : AbstractSmkRuleOrCheckpointType<SMKCheckPoint>(
        containingRule, smkFile.collectCheckPoints(), SnakemakeNames.SMK_VARS_CHECKPOINTS
)
