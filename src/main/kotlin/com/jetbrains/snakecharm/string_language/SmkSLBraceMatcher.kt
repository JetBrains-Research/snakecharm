package com.jetbrains.snakecharm.string_language

import com.intellij.lang.BracePair
import com.intellij.lang.PairedBraceMatcher
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IElementType

class SmkSLBraceMatcher : PairedBraceMatcher {
    companion object {
        val myBraces = BracePair(
                SmkSLTokenTypes.LBRACE,
                SmkSLTokenTypes.RBRACE,
                true)
    }

    override fun getCodeConstructStart(file: PsiFile?, openingBraceOffset: Int) = openingBraceOffset

    override fun getPairs() = arrayOf(myBraces)

    override fun isPairedBracesAllowedBeforeType(
            lbraceType: IElementType,
            contextType: IElementType?) = true
}