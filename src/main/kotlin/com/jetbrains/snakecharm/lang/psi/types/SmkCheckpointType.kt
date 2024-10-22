package com.jetbrains.snakecharm.lang.psi.types

import com.jetbrains.python.psi.PyExpression
import com.jetbrains.python.psi.resolve.RatedResolveResult
import com.jetbrains.snakecharm.lang.SnakemakeNames.SMK_VARS_CHECKPOINTS
import com.jetbrains.snakecharm.lang.psi.SmkCheckPoint
import com.jetbrains.snakecharm.lang.psi.SmkFile
import com.jetbrains.snakecharm.lang.psi.stubs.SmkCheckpointNameIndexCompanion.KEY

class SmkCheckpointType(
    containingRule: SmkCheckPoint?,
    smkFile: SmkFile
) : AbstractSmkRuleOrCheckpointType<SmkCheckPoint>(
    containingRule, SMK_VARS_CHECKPOINTS, KEY, SmkCheckPoint::class.java
) {
    override val currentFileDeclarations: List<SmkCheckPoint> by lazy {
        smkFile.filterCheckPointsPsi().map { it.second }
    }

    override fun getUseSections(name: String, location: PyExpression) = emptyList<RatedResolveResult>()
}
