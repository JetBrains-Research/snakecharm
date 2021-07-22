package com.jetbrains.snakecharm.lang.psi.elementTypes;

import com.intellij.psi.stubs.IStubElementType;
import com.jetbrains.snakecharm.lang.psi.*;
import com.jetbrains.snakecharm.lang.psi.impl.stubs.*;
import com.jetbrains.snakecharm.lang.psi.stubs.*;

public interface SmkStubElementTypes {
    IStubElementType<SmkRuleStub, SmkRule>  RULE_DECLARATION_STATEMENT = new SmkRuleElementType();
    IStubElementType<SmkCheckpointStub, SmkCheckPoint> CHECKPOINT_DECLARATION_STATEMENT = new SmkCheckpointElementType();
    IStubElementType<SmkSubworkflowStub, SmkSubworkflow> SUBWORKFLOW_DECLARATION_STATEMENT = new SmkSubworkflowElementType();
    IStubElementType<SmkModuleStub, SmkModule> MODULE_DECLARATION_STATEMENT = new SmkModuleElementType();
    IStubElementType<SmkUseStub, SmkUse> USE_DECLARATION_STATEMENT = new SmkUseElementType();
}
