package com.jetbrains.snakecharm.lang.psi

import com.intellij.psi.StubBasedPsiElement
import com.jetbrains.snakecharm.lang.psi.stubs.SmkCheckpointStub
import com.jetbrains.snakecharm.lang.psi.stubs.SmkRuleStub
import com.jetbrains.snakecharm.lang.psi.stubs.SmkSubworkflowStub

interface SMKRule: SmkRuleOrCheckpoint, StubBasedPsiElement<SmkRuleStub>

interface SMKCheckPoint: SmkRuleOrCheckpoint, StubBasedPsiElement<SmkCheckpointStub>

interface SmkSubworkflow: SmkRuleLike<SMKSubworkflowParameterListStatement>, StubBasedPsiElement<SmkSubworkflowStub>
