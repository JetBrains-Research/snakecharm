package com.jetbrains.snakecharm.lang.psi.impl.stubs

import com.intellij.psi.stubs.*
import com.jetbrains.python.psi.PyElement
import com.jetbrains.python.psi.PyStubElementType
import com.jetbrains.snakecharm.lang.psi.SmkRuleLike
import com.jetbrains.snakecharm.lang.psi.stubs.RuleDescendantStub
import com.jetbrains.snakecharm.lang.psi.stubs.SmkUseInheritedRulesIndex

abstract class SmkRuleDescendantElementType<StubT : RuleDescendantStub<*>, PsiT : PyElement>(
    debugName: String,
    private val nameIndexKey: StubIndexKey<String, out SmkRuleLike<*>>?
) :
    PyStubElementType<StubT, PsiT>(debugName) {
    abstract fun createStub(name: String?, inheritedRules: List<String?>, parentStub: StubElement<*>?): StubT

    override fun serialize(stub: StubT, dataStream: StubOutputStream) {
        dataStream.writeName(stub.name)
        dataStream.writeInt(stub.getInheritedRulesNames().size)
        stub.getInheritedRulesNames().forEach { name ->
            dataStream.writeName(name)
        }
    }

    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?): StubT {
        val name = dataStream.readNameString()
        val size = dataStream.readInt()
        val inheritedRules = mutableListOf<String?>()
        for (x in 0 until size) {
            inheritedRules.add(dataStream.readNameString())
        }
        return createStub(name, inheritedRules, parentStub)
    }

    override fun indexStub(stub: StubT, sink: IndexSink) {
        if (nameIndexKey != null) {
            stub.name?.let { name ->
                sink.occurrence(nameIndexKey, name)
            }
        }
        stub.getInheritedRulesNames().forEach { inheritedName ->
            if (inheritedName != null) {
                sink.occurrence(SmkUseInheritedRulesIndex.KEY, inheritedName)
            }
        }
        super.indexStub(stub, sink)
    }
}