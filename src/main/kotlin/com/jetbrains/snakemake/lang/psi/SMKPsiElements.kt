package com.jetbrains.snakemake.lang.psi

import com.intellij.lang.ASTNode
import com.jetbrains.python.psi.impl.PyElementImpl

class SMKCheckPoint(node: ASTNode): SMKRule(node)

class SMKWorkflowLocalRulesStatement(node: ASTNode): PyElementImpl(node)

class SMKWorkflowRulesReorderStatement(node: ASTNode): PyElementImpl(node)
