package com.jetbrains.snakecharm.lang.psi

import com.intellij.lang.ASTNode

abstract class SmkRuleOrCheckpoint(node: ASTNode) : SmkRuleLike<SMKRuleParameterListStatement>(node)