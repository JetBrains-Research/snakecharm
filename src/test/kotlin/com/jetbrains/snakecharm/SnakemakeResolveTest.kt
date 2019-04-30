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
        assertResolveFail(PyFunction::class.java, "expand", ".py")
    }

    // resolve for docstrings is not supported, thus resolve fail is expected
    fun testExpandDocstring() {
        assertResolveFail(PyFunction::class.java, "expand",".smk")
    }

}