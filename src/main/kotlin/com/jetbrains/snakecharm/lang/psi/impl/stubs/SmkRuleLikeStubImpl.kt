package com.jetbrains.snakecharm.lang.psi.impl.stubs

import com.intellij.psi.PsiNamedElement
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.stubs.NamedStub
import com.intellij.psi.stubs.StubBase
import com.intellij.psi.stubs.StubElement
import com.jetbrains.python.psi.PyElement
import com.jetbrains.snakecharm.lang.psi.*
import com.jetbrains.snakecharm.lang.psi.elementTypes.SmkStubElementTypes.*
import com.jetbrains.snakecharm.lang.psi.stubs.*

abstract class SmkRuleLikeStubImpl<StubT : NamedStub<PsiT>, PsiT>(
    private val myName: String?,
    parent: StubElement<*>?,
    type: IStubElementType<StubT, PsiT>
) : StubBase<PsiT>(parent, type), NamedStub<PsiT> where PsiT : PsiNamedElement, PsiT : PyElement {
    override fun getName() = myName
}

class SmkCheckpointStubImpl(
    name: String?,
    parent: StubElement<*>?
) : SmkRuleLikeStubImpl<SmkCheckpointStub, SmkCheckPoint>(name, parent, CHECKPOINT_DECLARATION_STATEMENT),
    SmkCheckpointStub

class SmkRuleStubImpl(
    name: String?,
    parent: StubElement<*>?
) : SmkRuleLikeStubImpl<SmkRuleStub, SmkRule>(name, parent, RULE_DECLARATION_STATEMENT), SmkRuleStub

class SmkSubworkflowStubImpl(
    name: String?,
    parent: StubElement<*>?
) : SmkRuleLikeStubImpl<SmkSubworkflowStub, SmkSubworkflow>(name, parent, SUBWORKFLOW_DECLARATION_STATEMENT),
    SmkSubworkflowStub

class SmkModuleStubImpl(
    name: String?,
    parent: StubElement<*>?
) : SmkRuleLikeStubImpl<SmkModuleStub, SmkModule>(name, parent, MODULE_DECLARATION_STATEMENT), SmkModuleStub

class SmkUseStubImpl(
    name: String?,
    parent: StubElement<*>?
) : SmkRuleLikeStubImpl<SmkUseStub, SmkUse>(name, parent, USE_DECLARATION_STATEMENT), SmkUseStub