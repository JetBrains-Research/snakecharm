package com.jetbrains.snakecharm.lang.psi.impl.stubs

import com.intellij.psi.PsiNamedElement
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.stubs.NamedStub
import com.intellij.psi.stubs.StubBase
import com.intellij.psi.stubs.StubElement
import com.jetbrains.python.psi.PyElement
import com.jetbrains.snakecharm.lang.psi.SMKCheckPoint
import com.jetbrains.snakecharm.lang.psi.SMKRule
import com.jetbrains.snakecharm.lang.psi.SmkSubworkflow
import com.jetbrains.snakecharm.lang.psi.elementTypes.SmkStubElementTypes.CHECKPOINT_DECLARATION
import com.jetbrains.snakecharm.lang.psi.elementTypes.SmkStubElementTypes.RULE_DECLARATION
import com.jetbrains.snakecharm.lang.psi.elementTypes.SmkStubElementTypes.SUBWORKFLOW_DECLARATION
import com.jetbrains.snakecharm.lang.psi.stubs.SmkCheckpointStub
import com.jetbrains.snakecharm.lang.psi.stubs.SmkRuleStub
import com.jetbrains.snakecharm.lang.psi.stubs.SmkSubworkflowStub

abstract class SmkRuleLikeStubImpl<StubT: NamedStub<PsiT>, PsiT>(
        private val myName: String?,
        parent: StubElement<*>?,
        type: IStubElementType<StubT, PsiT>
) : StubBase<PsiT>(parent, type), NamedStub<PsiT> where PsiT : PsiNamedElement, PsiT: PyElement {
    override fun getName() = myName
}

class SmkCheckpointStubImpl(
        name: String?,
        parent: StubElement<*>?
): SmkRuleLikeStubImpl<SmkCheckpointStub, SMKCheckPoint>(name, parent, CHECKPOINT_DECLARATION), SmkCheckpointStub

class SmkRuleStubImpl(
        name: String?,
        parent: StubElement<*>?
): SmkRuleLikeStubImpl<SmkRuleStub, SMKRule>(name, parent, RULE_DECLARATION), SmkRuleStub

class SmkSubworkflowStubImpl(
        name: String?,
        parent: StubElement<*>?
): SmkRuleLikeStubImpl<SmkSubworkflowStub, SmkSubworkflow>(name, parent, SUBWORKFLOW_DECLARATION), SmkSubworkflowStub