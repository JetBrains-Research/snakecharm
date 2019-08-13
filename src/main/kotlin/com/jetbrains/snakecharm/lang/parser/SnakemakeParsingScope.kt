package com.jetbrains.snakecharm.lang.parser

import com.jetbrains.python.parsing.ParsingScope

/**
 * @author Roman.Chernyatchik
 * @date 2018-12-31
 */
class SnakemakeParsingScope : ParsingScope() {
    // in onstart/onsuccess/onerror/run sections
    var inPythonicSection: Boolean = false
        private set

    override fun createInstance() = SnakemakeParsingScope()

    fun withPythonicSection(): SnakemakeParsingScope {
        val result = copy()
        result.inPythonicSection = true
        return result
    }

    override fun copy(): SnakemakeParsingScope {
        val copy = super.copy() as SnakemakeParsingScope
        copy.inPythonicSection = inPythonicSection
        return copy
    }
}