package com.jetbrains.snakecharm.lang.psi.impl.stubs

import com.intellij.psi.stubs.*
import com.jetbrains.python.psi.PyElement
import com.jetbrains.python.psi.PyStubElementType
import com.jetbrains.snakecharm.lang.psi.SmkRuleLike

abstract class SmkRuleLikeElementType<StubT: NamedStub<*>, PsiT: PyElement>(
        debugName: String,
        private val nameIndexKey: StubIndexKey<String, out SmkRuleLike<*>>?
): PyStubElementType<StubT, PsiT>(debugName) {

    abstract fun createStub(name: String?, parentStub: StubElement<*>?): StubT

    override fun serialize(stub: StubT, dataStream: StubOutputStream) {
        dataStream.writeName(stub.name)
    }

    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?): StubT =
            createStub(dataStream.readNameString(), parentStub)

    override fun indexStub(stub: StubT, sink: IndexSink) {
        if (nameIndexKey != null) {
            stub.name?.let { name ->
                sink.occurrence(nameIndexKey, name)
            }
        }

        super.indexStub(stub, sink)
    }
}