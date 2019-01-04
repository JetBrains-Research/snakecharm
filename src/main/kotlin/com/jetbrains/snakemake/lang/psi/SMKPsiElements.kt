package com.jetbrains.snakemake.lang.psi

import com.intellij.lang.ASTNode
import com.jetbrains.python.psi.PyStatement
import com.jetbrains.python.psi.impl.PyElementImpl

/**
 * @author Roman.Chernyatchik
 * @date 2018-12-31
 */
class SMKRule(node: ASTNode): PyElementImpl(node) //TODO: PyNamedElementContainer; PyStubElementType<SMKRuleStub, SMKRule>

class SMKRuleParameterListStatement(node: ASTNode): PyElementImpl(node), PyStatement  // PyNamedElementContainer