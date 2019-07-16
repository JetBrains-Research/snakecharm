package com.jetbrains.snakecharm.lang.parser

import com.jetbrains.python.parsing.ParsingScope

/**
 * @author Roman.Chernyatchik
 * @date 2018-12-31
 */
class SnakemakeParsingScope : ParsingScope() {
    var inRuleLikeSectionsList: Boolean = false
        private set

    // In rule or workflow params args list
    var inParamArgsList: Boolean = false
        private set

    // In rule or workflow params args list
    var inNoSmkKeywordsAllowed: Boolean = false
        private set

    override fun createInstance() = SnakemakeParsingScope()

    fun withRuleLike(): SnakemakeParsingScope {
        val result = copy()
        result.inRuleLikeSectionsList = true
        return result
    }

    fun withParamsArgsList(): SnakemakeParsingScope {
        val result = copy()
        result.inParamArgsList = true
        return result
    }

    fun withNoSmkKeywordsAllowed(): SnakemakeParsingScope {
        val result = copy()
        result.inNoSmkKeywordsAllowed = true
        return result
    }

    override fun copy(): SnakemakeParsingScope {
        val copy = super.copy() as SnakemakeParsingScope
        copy.inRuleLikeSectionsList = inRuleLikeSectionsList
        copy.inParamArgsList = inParamArgsList
        copy.inNoSmkKeywordsAllowed = inNoSmkKeywordsAllowed
        return copy
    }
}