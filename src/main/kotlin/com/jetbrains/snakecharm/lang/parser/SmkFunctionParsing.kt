package com.jetbrains.snakecharm.lang.parser

import com.jetbrains.python.parsing.FunctionParsing
import com.jetbrains.snakecharm.lang.psi.elementTypes.SmkElementTypes

class SmkFunctionParsing(context: SmkParserContext) : FunctionParsing(context) {
    override fun getReferenceType() = SmkElementTypes.SMK_PY_REFERENCE_EXPRESSION
}