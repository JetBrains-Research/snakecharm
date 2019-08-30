package com.jetbrains.snakecharm.stringLanguage

import com.intellij.extapi.psi.PsiFileBase
import com.intellij.psi.FileViewProvider

class SmkSLFile(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, SmkSLanguage) {
    override fun getFileType() = SmkSLFileType
}
