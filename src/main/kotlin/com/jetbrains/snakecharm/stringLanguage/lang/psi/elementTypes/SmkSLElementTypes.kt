package com.jetbrains.snakecharm.stringLanguage.lang.psi.elementTypes

import com.intellij.psi.tree.TokenSet
import com.jetbrains.snakecharm.stringLanguage.lang.psi.SmkSLElementType

object SmkSLElementTypes {
    val REFERENCE_EXPRESSION = SmkSLElementType("REFERENCE_EXPRESSION")
    val SUBSCRIPTION_EXPRESSION = SmkSLElementType("SUBSCRIPTION_EXPRESSION")
    val KEY_EXPRESSION = SmkSLElementType("KEY_EXPRESSION")

    val EXPRESSION_TOKENS = TokenSet.create(REFERENCE_EXPRESSION, SUBSCRIPTION_EXPRESSION)
}