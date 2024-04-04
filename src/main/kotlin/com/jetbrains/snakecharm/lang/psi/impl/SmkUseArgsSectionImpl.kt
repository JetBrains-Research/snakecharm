package com.jetbrains.snakecharm.lang.psi.impl

import com.intellij.lang.ASTNode
import com.jetbrains.python.psi.PyElementVisitor
import com.jetbrains.python.psi.types.PyType
import com.jetbrains.python.psi.types.TypeEvalContext
import com.jetbrains.snakecharm.lang.psi.SmkElementVisitor
import com.jetbrains.snakecharm.lang.psi.SmkRuleOrCheckpointArgsSection
import com.jetbrains.snakecharm.lang.psi.types.SmkRuleLikeSectionArgsType

class SmkUseArgsSectionImpl(node: ASTNode) : SmkArgsSectionImpl(node), SmkRuleOrCheckpointArgsSection {
    override fun acceptPyVisitor(pyVisitor: PyElementVisitor?) {
        when (pyVisitor) {
            is SmkElementVisitor -> pyVisitor.visitSmkRuleOrCheckpointArgsSection(this)
            else -> super<SmkArgsSectionImpl>.acceptPyVisitor(pyVisitor)
        }
    }

    override fun multilineSectionDefinition(): Boolean =
        SmkRuleOrCheckpointArgsSectionImpl.multilineSectionDefinition(this)


    override fun getType(context: TypeEvalContext, key: TypeEvalContext.Key): PyType =
        SmkRuleLikeSectionArgsType(this)
}