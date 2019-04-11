package com.jetbrains.snakecharm

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiPolyVariantReference
import com.intellij.psi.PsiReference
import com.intellij.psi.ResolveResult
import com.jetbrains.python.psi.PyFunction
import com.jetbrains.snakecharm.fixtures.SnakemakeResolveTestCase

class SnakemakeResolveTest : SnakemakeResolveTestCase() {
    override fun doResolve(): PsiElement? {
        val ref = getReference()
        return ref?.resolve()
    }

    private fun multiResolve(): Array<ResolveResult> {
        val ref = getReference()
        assertTrue(ref is PsiPolyVariantReference)
        return (ref as PsiPolyVariantReference).multiResolve(false)
    }

    private fun getReference(): PsiReference? = fixture?.getReferenceAtCaretPosition("resolve/" +
            getTestName(false) + ".smk")

    fun testExpandTopLevel() {
        assertResolvesTo(PyFunction::class.java, "expand")
    }

    fun testExpandNested() {
        assertResolvesTo(PyFunction::class.java, "expand")
    }

}