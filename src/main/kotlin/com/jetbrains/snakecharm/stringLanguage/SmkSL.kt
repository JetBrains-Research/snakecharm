package com.jetbrains.snakecharm.stringLanguage

import com.intellij.lang.Language
import com.intellij.psi.PsiElement
import com.jetbrains.snakecharm.lang.psi.impl.SmkPsiUtil

object SmkSL : Language("SnakemakeSL") {
    fun isInsideSmkSLFile(foothold: PsiElement?) =
            SmkPsiUtil.isInsideFileWithLanguage(foothold, SmkSL)
}
