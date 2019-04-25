package com.jetbrains.snakecharm

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiPolyVariantReference
import com.intellij.psi.PsiReference
import com.intellij.psi.ResolveResult
import com.jetbrains.python.psi.PyFunction
import com.jetbrains.snakecharm.fixtures.SnakemakeResolveTestCase

class SnakemakeResolveTest : SnakemakeResolveTestCase() {
    fun testExpandTopLevel() {
        assertResolvesTo(PyFunction::class.java, "expand")
    }

    fun testExpandNested() {
        assertResolvesTo(PyFunction::class.java, "expand")
    }

}