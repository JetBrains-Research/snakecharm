package com.jetbrains.snakemake.lang.psi

import com.intellij.lang.ASTNode
import com.jetbrains.python.psi.impl.PyElementImpl

/**
 * @author Roman.Chernyatchik
 * @date 2018-12-31
 */
class SMKRule(node: ASTNode): PyElementImpl(node)

class SMKRuleParameterList(node: ASTNode): PyElementImpl(node)