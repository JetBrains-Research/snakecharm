package com.jetbrains.snakecharm.lang.parser

import com.jetbrains.python.parsing.ParsingScope

/**
 * @author Roman.Chernyatchik
 * @date 2018-12-31
 */
class SnakemakeParsingScope : ParsingScope() {
    var inRule: Boolean = false
        private set

    // In rule or workflow params args list
    var inParamArgsList: Boolean = false
        private set

    override fun createInstance() = SnakemakeParsingScope()

    fun withRule(): SnakemakeParsingScope {
        val result = copy()
        result.inRule = true
        return result
    }

    fun withParamsArgsList(): SnakemakeParsingScope {
        val result = copy()
        result.inParamArgsList = true
        return result
    }

    override fun copy(): SnakemakeParsingScope {
        val copy = super.copy() as SnakemakeParsingScope
        copy.inRule = inRule
        copy.inParamArgsList = inParamArgsList
        return copy
    }
}