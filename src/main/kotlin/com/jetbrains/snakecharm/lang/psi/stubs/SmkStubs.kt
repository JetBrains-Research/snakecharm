package com.jetbrains.snakecharm.lang.psi.stubs

import com.intellij.psi.stubs.NamedStub
import com.jetbrains.snakecharm.lang.psi.*

interface SmkRuleStub : NamedStub<SmkRule>
interface SmkCheckpointStub : NamedStub<SmkCheckPoint>
interface SmkSubworkflowStub : NamedStub<SmkSubworkflow>
interface SmkModuleStub : NamedStub<SmkModule>
interface SmkUseStub : NamedStub<SmkUse>