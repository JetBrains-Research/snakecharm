package com.jetbrains.snakecharm.stringLanguage.lang

import com.intellij.lang.BracePair
import com.intellij.lang.PairedBraceMatcher
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IElementType
import com.jetbrains.snakecharm.stringLanguage.lang.parser.SmkSLTokenTypes

class SmkSLBraceMatcher : PairedBraceMatcher {
    override fun getCodeConstructStart(file: PsiFile?, openingBraceOffset: Int) = openingBraceOffset

    override fun getPairs() = myBraces

    override fun isPairedBracesAllowedBeforeType(
            lbraceType: IElementType,
            contextType: IElementType?) = true

    private val myBraces = arrayOf(
        BracePair(SmkSLTokenTypes.LBRACE, SmkSLTokenTypes.RBRACE, true),
        BracePair(SmkSLTokenTypes.LBRACKET, SmkSLTokenTypes.RBRACKET, true)
    )

}