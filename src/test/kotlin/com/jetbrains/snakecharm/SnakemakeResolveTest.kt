package com.jetbrains.snakecharm

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiPolyVariantReference
import com.intellij.psi.PsiReference
import com.intellij.psi.ResolveResult
import com.jetbrains.python.psi.PyFunction
import com.jetbrains.snakecharm.fixtures.SnakemakeResolveTestCase

class SnakemakeResolveTest : SnakemakeResolveTestCase() {
    override fun doResolve(): PsiElement? {
        val ref = findReferenceByMarker()
        return ref?.resolve()
    }

    private fun findReferenceByMarker(): PsiReference? {
        fixture?.configureByFile("resolve/" + getTestName(false) + ".smk")
        return SnakemakeResolveTestCase.findReferenceByMarker(fixture?.file)
    }

    protected fun resolve(): PsiElement? {
        val ref = configureByFile("resolve/" + getTestName(false) + ".smk")
        //  if need be: PythonLanguageLevelPusher.setForcedLanguageLevel(project, LanguageLevel.PYTHON26);
        return ref?.resolve()
    }

    private fun multiResolve(): Array<ResolveResult> {
        val ref = findReferenceByMarker()
        assertTrue(ref is PsiPolyVariantReference)
        return (ref as PsiPolyVariantReference).multiResolve(false)
    }

    fun testExpandTopLevel() {
        //val result = resolve()
        assertResolvesTo(PyFunction::class.java, "expand")
    }


}