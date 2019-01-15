package com.jetbrains.snakecharm.lang

import com.intellij.lang.Language
import com.jetbrains.python.PythonLanguage

/**
 * @author Roman.Chernyatchik
 * @date 2018-12-31
 */
object SnakemakeLanguageDialect : Language(PythonLanguage.getInstance(), "Snakemake") {
     val fileElementType = SnakemakeFileElementType(this)

// CythonLanguageDialect
//    /**
//     * Returns `true` if the `foothold` element is inside a Cython file.
//     *
//     * Used as a check to find all the code that knows about Cython.
//     */
//    fun isInsideCythonFile(foothold: PsiElement?): Boolean {
//        if (foothold != null && foothold.isValid) {
//            val file = foothold.containingFile
//            if (file != null) {
//                return file.language is SnakemakeLanguageDialect
//            }
//        }
//        return false
//    }

}