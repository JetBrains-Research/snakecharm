package com.jetbrains.snakecharm.lang.parser

import com.jetbrains.python.parsing.ParsingScope

/**
 * @author Roman.Chernyatchik
 * @date 2018-12-31
 */
class SnakemakeParsingScope : ParsingScope() {
    var inRule: Boolean = false
        private set

    override fun createInstance() = SnakemakeParsingScope()

    fun withRule(flag: Boolean): SnakemakeParsingScope {
        val result = copy()
        result.inRule = flag
        return result
    }

    override fun copy(): SnakemakeParsingScope {
        val copy = super.copy() as SnakemakeParsingScope
        copy.inRule = inRule
        return copy
    }
}