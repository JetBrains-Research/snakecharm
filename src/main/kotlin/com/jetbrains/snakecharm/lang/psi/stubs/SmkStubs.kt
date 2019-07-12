package com.jetbrains.snakecharm.lang.psi.stubs

import com.intellij.psi.stubs.NamedStub
import com.jetbrains.snakecharm.lang.psi.SMKCheckPoint
import com.jetbrains.snakecharm.lang.psi.SMKRule
import com.jetbrains.snakecharm.lang.psi.SmkSubworkflow

interface SmkRuleStub: NamedStub<SMKRule>
interface SmkCheckpointStub: NamedStub<SMKCheckPoint>
interface SmkSubworkflowStub: NamedStub<SmkSubworkflow>
