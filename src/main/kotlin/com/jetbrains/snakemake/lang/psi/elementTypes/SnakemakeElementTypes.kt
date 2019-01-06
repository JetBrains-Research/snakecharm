package com.jetbrains.snakemake.lang.psi.elementTypes

import com.jetbrains.python.psi.PyElementType
import com.jetbrains.snakemake.lang.psi.SMKCheckPoint
import com.jetbrains.snakemake.lang.psi.SMKRule
import com.jetbrains.snakemake.lang.psi.SMKRuleParameterListStatement
import com.jetbrains.snakemake.lang.psi.SMKRuleRunParameter

/**
 * @author Roman.Chernyatchik
 * @date 2018-12-31
 */
object SnakemakeElementTypes {
    val RULE_DECLARATION = PyElementType("SMK_RULE_DECLARATION", SMKRule::class.java)
    val CHECKPOINT_DECLARATION = PyElementType("SMK_CHECKPOINT_DECLARATION", SMKCheckPoint::class.java)

    val RULE_PARAMETER_LIST_STATEMENT = PyElementType(
            "SMK_RULE_PARAMETER_LIST_STATEMENT",
            SMKRuleParameterListStatement::class.java
    )

    val RULE_RUN_STATEMENT = PyElementType(
            "SMK_RULE_RUN_STATEMENT",
            SMKRuleRunParameter::class.java
    )
}