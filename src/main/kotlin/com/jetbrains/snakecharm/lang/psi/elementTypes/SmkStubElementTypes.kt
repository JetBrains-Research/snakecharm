package com.jetbrains.snakecharm.lang.psi.elementTypes

import com.intellij.psi.stubs.IStubElementType
import com.jetbrains.snakecharm.lang.psi.SMKCheckPoint
import com.jetbrains.snakecharm.lang.psi.SMKRule
import com.jetbrains.snakecharm.lang.psi.SmkSubworkflow
import com.jetbrains.snakecharm.lang.psi.impl.stubs.SmkCheckpointElementType
import com.jetbrains.snakecharm.lang.psi.impl.stubs.SmkRuleElementType
import com.jetbrains.snakecharm.lang.psi.impl.stubs.SmkSubworkflowElementType
import com.jetbrains.snakecharm.lang.psi.stubs.SmkCheckpointStub
import com.jetbrains.snakecharm.lang.psi.stubs.SmkRuleStub
import com.jetbrains.snakecharm.lang.psi.stubs.SmkSubworkflowStub

object SmkStubElementTypes {
    val RULE_DECLARATION: IStubElementType<SmkRuleStub, SMKRule> = SmkRuleElementType()
    val CHECKPOINT_DECLARATION: IStubElementType<SmkCheckpointStub, SMKCheckPoint> = SmkCheckpointElementType()
     val SUBWORKFLOW_DECLARATION: IStubElementType<SmkSubworkflowStub, SmkSubworkflow> = SmkSubworkflowElementType()
}