package com.jetbrains.snakemake.lang.parser

import com.jetbrains.python.psi.PyElementType

/**
 * @author Roman.Chernyatchik
 * @date 2018-12-31
 */
object SnakemakeTokenTypes {
    val RULE_KEYWORD = PyElementType("RULE_KEYWORD") // rule
    val CHECKPOINT_KEYWORD = PyElementType("CHECKPOINT_KEYWORD") // rule

}