package com.jetbrains.snakecharm.lang.psi.elementTypes;

import com.intellij.psi.stubs.IStubElementType;
import com.jetbrains.snakecharm.lang.psi.SmkCheckPoint;
import com.jetbrains.snakecharm.lang.psi.SmkRule;
import com.jetbrains.snakecharm.lang.psi.SmkSubworkflow;
import com.jetbrains.snakecharm.lang.psi.impl.stubs.SmkCheckpointElementType;
import com.jetbrains.snakecharm.lang.psi.impl.stubs.SmkRuleElementType;
import com.jetbrains.snakecharm.lang.psi.impl.stubs.SmkSubworkflowElementType;
import com.jetbrains.snakecharm.lang.psi.stubs.SmkCheckpointStub;
import com.jetbrains.snakecharm.lang.psi.stubs.SmkRuleStub;
import com.jetbrains.snakecharm.lang.psi.stubs.SmkSubworkflowStub;

public interface SmkStubElementTypes {
    IStubElementType<SmkRuleStub, SmkRule>  RULE_DECLARATION_STATEMENT = new SmkRuleElementType();
    IStubElementType<SmkCheckpointStub, SmkCheckPoint> CHECKPOINT_DECLARATION_STATEMENT = new SmkCheckpointElementType();
    IStubElementType<SmkSubworkflowStub, SmkSubworkflow> SUBWORKFLOW_DECLARATION_STATEMENT = new SmkSubworkflowElementType();
}
