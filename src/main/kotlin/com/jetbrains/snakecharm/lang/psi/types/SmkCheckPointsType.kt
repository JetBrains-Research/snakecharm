package com.jetbrains.snakecharm.lang.psi.types

import com.jetbrains.snakecharm.lang.SnakemakeNames.SMK_VARS_CHECKPOINTS
import com.jetbrains.snakecharm.lang.psi.SMKCheckPoint
import com.jetbrains.snakecharm.lang.psi.SnakemakeFile
import com.jetbrains.snakecharm.lang.psi.stubs.SmkCheckpointNameIndex.Companion.KEY

class SmkCheckPointsType(
        containingRule: SMKCheckPoint?,
        smkFile: SnakemakeFile
) : AbstractSmkRuleOrCheckpointType<SMKCheckPoint>(
        containingRule, SMK_VARS_CHECKPOINTS, KEY, SMKCheckPoint::class.java
) {
    override val currentFileDeclarations: List<SMKCheckPoint> by lazy {
        smkFile.collectCheckPoints().map { it.second }
    }

}
