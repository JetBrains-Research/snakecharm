package com.jetbrains.snakecharm.lang.parser

import com.jetbrains.python.parsing.ParsingScope

/**
 * @author Roman.Chernyatchik
 * @date 2018-12-31
 */
class SnakemakeParsingScope : ParsingScope() {
    var inRuleSectionsList: Boolean = false
        private set

    // In rule or workflow params args list
    var inParamArgsList: Boolean = false
        private set

    override fun createInstance() = SnakemakeParsingScope()

    fun withRule(): SnakemakeParsingScope {
        val result = copy()
        result.inRuleSectionsList = true
        return result
    }

    fun withParamsArgsList(): SnakemakeParsingScope {
        val result = copy()
        result.inParamArgsList = true
        return result
    }

    override fun copy(): SnakemakeParsingScope {
        val copy = super.copy() as SnakemakeParsingScope
        copy.inRuleSectionsList = inRuleSectionsList
        copy.inParamArgsList = inParamArgsList
        return copy
    }
}