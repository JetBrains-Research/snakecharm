package com.jetbrains.snakecharm

import com.jetbrains.python.psi.PyFunction
import com.jetbrains.snakecharm.fixtures.SnakemakeResolveTestCase

class SnakemakeResolveTest : SnakemakeResolveTestCase() {
    fun testExpandRule() {
        assertResolvesTo(PyFunction::class.java, "expand")
    }

    fun testExpandNested() {
        assertResolvesTo(PyFunction::class.java, "expand")
    }

    fun testExpandRunSection() {
        assertResolvesTo(PyFunction::class.java, "expand")
    }

    fun testExpandTopLevel() {
        assertResolvesTo(PyFunction::class.java, "expand")
    }

    fun testExpandPythonFile() {
        // do not enable snakemake specific resolves in ordinary python files
        assertUnresolved(".py")
    }

    fun testExpandPythonDialect() {
        // do not enable snakemake specific resolves in python based other languages
        assertUnresolved(".pyi")
    }

}