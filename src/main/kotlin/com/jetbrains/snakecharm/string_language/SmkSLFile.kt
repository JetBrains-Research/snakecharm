package com.jetbrains.snakecharm.string_language

import com.intellij.extapi.psi.PsiFileBase
import com.intellij.psi.FileViewProvider

class SmkSLFile(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, SmkStringLanguage) {
    override fun getFileType() = SmkSLFileType
}