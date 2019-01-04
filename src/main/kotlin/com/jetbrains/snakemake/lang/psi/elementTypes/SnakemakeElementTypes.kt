package com.jetbrains.snakemake.lang.psi.elementTypes

import com.jetbrains.python.psi.PyElementType
import com.jetbrains.snakemake.lang.psi.SMKRule
import com.jetbrains.snakemake.lang.psi.SMKRuleParameterListStatement

/**
 * @author Roman.Chernyatchik
 * @date 2018-12-31
 */
object SnakemakeElementTypes {
    val RULE_DECLARATION = PyElementType("SMK_RULE_DECLARATION", SMKRule::class.java)
    val RULE_PARAMETER_LIST_STATEMENT = PyElementType(
            "SMK_RULE_PARAMETER_LIST_STATEMENT",
            SMKRuleParameterListStatement::class.java
    )
}