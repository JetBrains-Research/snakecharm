package com.jetbrains.snakecharm.lang.psi.types

import com.jetbrains.python.psi.PyExpression
import com.jetbrains.python.psi.resolve.RatedResolveResult
import com.jetbrains.snakecharm.codeInsight.SnakemakeAPI.SMK_VARS_CHECKPOINTS
import com.jetbrains.snakecharm.lang.psi.SmkCheckPoint
import com.jetbrains.snakecharm.lang.psi.SmkFile
import com.jetbrains.snakecharm.lang.psi.stubs.SmkCheckpointNameIndex.Companion.KEY

class SmkCheckpointType(
    containingRule: SmkCheckPoint?,
    smkFile: SmkFile
) : AbstractSmkRuleOrCheckpointType<SmkCheckPoint>(
    containingRule, SMK_VARS_CHECKPOINTS, KEY, SmkCheckPoint::class.java
) {
    override val currentFileDeclarations: List<SmkCheckPoint> by lazy {
        smkFile.collectCheckPoints().map { it.second }
    }

    override fun getUseSections(name: String, location: PyExpression) = emptyList<RatedResolveResult>()
}
