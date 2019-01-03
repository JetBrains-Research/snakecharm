package com.jetbrains.snakemake.lang.parser

import com.jetbrains.python.parsing.ParsingScope

/**
 * @author Roman.Chernyatchik
 * @date 2018-12-31
 */
class SnakemakeParsingScope : ParsingScope() {
    //var isRule: Boolean = false
    //    private set

    override fun createInstance() = SnakemakeParsingScope()

//    fun withRule(flag: Boolean): SnakemakeParsingScope {
//        val result = copy()
//        result.isRule = flag
//        return result
//    }
//
//    override fun copy(): SnakemakeParsingScope {
//        val copy = super.copy() as SnakemakeParsingScope
//        copy.isRule = isRule
//        return copy
//    }
}