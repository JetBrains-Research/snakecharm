package com.jetbrains.snakecharm.lang.psi

import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.jetbrains.python.psi.PyElementVisitor
import com.jetbrains.python.psi.impl.PyFileImpl
import com.jetbrains.snakecharm.SnakemakeFileType
import com.jetbrains.snakecharm.lang.SnakemakeLanguageDialect

/**
 * @author Roman.Chernyatchik
 * @date 2018-12-31
 */
class SnakemakeFile(viewProvider: FileViewProvider) : PyFileImpl(viewProvider, SnakemakeLanguageDialect) { // SnakemakeScopeOwner:ScopeOwner
    // CythonFile, CythonScopeOwner

    override fun getIcon(flags: Int) = SnakemakeFileType.icon

    override fun toString() = "SnakemakeFile: $name"

    override fun getStub() = null

    fun collectRules(): List<Pair<String, PsiElement>> {
        // TODO: add tests, this is simple impl for internship task practice
        val ruleNameAndPsi = arrayListOf<Pair<String, PsiElement>>()

        // TODO[romeo]: refactor code: replace SnakemakeAnnotator with SMKElementVisitor
        acceptChildren(object : PyElementVisitor(), SMKElementVisitor {
            override val pyElementVisitor: PyElementVisitor = this

            override fun visitSMKRule(smkRule: SMKRule) {
                val element = smkRule.getNameNode()?.psi
                if (element != null) {
                    ruleNameAndPsi.add(smkRule.name!! to element)
                }
            }
        })
        return ruleNameAndPsi
    }
}