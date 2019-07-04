package com.jetbrains.snakecharm.lang.psi

import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubElement
import com.jetbrains.python.psi.PyElementVisitor
import com.jetbrains.python.psi.impl.PyFileImpl
import com.jetbrains.snakecharm.SnakemakeFileType
import com.jetbrains.snakecharm.lang.SnakemakeLanguageDialect

/**
 * @author Roman.Chernyatchik
 * @date 2018-12-31
 */
class SnakemakeFile(viewProvider: FileViewProvider) : PyFileImpl(viewProvider, SnakemakeLanguageDialect) {
    // consider extend SnakemakeScopeOwner:ScopeOwner
    // e.g. CythonFile, CythonScopeOwner

    override fun getIcon(flags: Int) = SnakemakeFileType.icon

    override fun toString() = "SnakemakeFile: $name"

    override fun getStub(): StubElement<SnakemakeFile>? = null

    fun collectRules(): List<Pair<String, PsiElement>> {
        // TODO: add tests, this is simple impl for internship task practice
        val ruleNameAndPsi = arrayListOf<Pair<String, PsiElement>>()

        acceptChildren(object : PyElementVisitor(), SMKElementVisitor {
            override val pyElementVisitor: PyElementVisitor = this

            // TODO: collect checkpoints or not?
            override fun visitSMKRule(rule: SMKRule) {
                val element = rule.getNameNode()?.psi
                if (element != null) {
                    ruleNameAndPsi.add(rule.name!! to element)
                }
            }
        })
        return ruleNameAndPsi
    }
}