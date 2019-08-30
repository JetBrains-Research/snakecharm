package com.jetbrains.snakecharm.lang.parser

import com.jetbrains.python.parsing.ParsingScope

/**
 * @author Roman.Chernyatchik
 * @date 2018-12-31
 */
class SmkParsingScope : ParsingScope() {
    // in onstart/onsuccess/onerror/run sections
    var inPythonicSection: Boolean = false
        private set

    override fun createInstance() = SmkParsingScope()

    fun withPythonicSection(): SmkParsingScope {
        val result = copy()
        result.inPythonicSection = true
        return result
    }

    override fun copy(): SmkParsingScope {
        val copy = super.copy() as SmkParsingScope
        copy.inPythonicSection = inPythonicSection
        return copy
    }
}