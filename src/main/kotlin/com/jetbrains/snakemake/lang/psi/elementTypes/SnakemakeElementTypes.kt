package com.jetbrains.snakemake.lang.psi.elementTypes

import com.jetbrains.python.psi.PyElementType
import com.jetbrains.snakemake.lang.psi.SMKRule
import com.jetbrains.snakemake.lang.psi.SMKRuleParameterList

/**
 * @author Roman.Chernyatchik
 * @date 2018-12-31
 */
object SnakemakeElementTypes {
    val RULE = PyElementType("SMK_RULE", SMKRule::class.java)
    val RULE_PARAMETER_LIST = PyElementType("SMK_RULE_PARAMETER_LIST", SMKRuleParameterList::class.java)
}