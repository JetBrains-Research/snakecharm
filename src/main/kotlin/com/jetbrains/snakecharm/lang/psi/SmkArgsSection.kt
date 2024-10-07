package com.jetbrains.snakecharm.lang.psi

import com.jetbrains.python.psi.PyArgumentList
import com.jetbrains.python.psi.PyKeywordArgument

interface SmkArgsSection: SmkSection {
    val argumentList: PyArgumentList?
        get() = children.firstOrNull { it is PyArgumentList } as PyArgumentList?

    @Suppress("unused")
    val keywordArguments: List<PyKeywordArgument>?
        get() = argumentList?.arguments?.filterIsInstance<PyKeywordArgument>()
}