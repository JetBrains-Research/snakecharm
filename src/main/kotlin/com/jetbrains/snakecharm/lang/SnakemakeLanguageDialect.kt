package com.jetbrains.snakecharm.lang

import com.intellij.lang.Language
import com.intellij.psi.PsiElement
import com.jetbrains.python.PythonLanguage
import com.jetbrains.snakecharm.lang.psi.impl.SmkPsiUtil

/**
 * @author Roman.Chernyatchik
 * @date 2018-12-31
 */
object SnakemakeLanguageDialect : Language(PythonLanguage.getInstance(), "Snakemake") {
     val fileElementType = SmkFileElementType(this)

     /**
      * Used as a check to find all the code that knows about Snake.
      *
      * @param foothold Some psi element from some file
      * @return `true` if the `foothold` element is inside a SnakeMake file.
      *
      */
    fun isInsideSmkFile(foothold: PsiElement?) =
             SmkPsiUtil.isInsideFileWithLanguage(foothold, SnakemakeLanguageDialect)
}