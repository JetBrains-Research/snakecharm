package com.jetbrains.snakecharm.stringLanguage.lang.psi

import com.intellij.extapi.psi.PsiFileBase
import com.intellij.psi.FileViewProvider
import com.jetbrains.snakecharm.stringLanguage.SmkSLFileType
import com.jetbrains.snakecharm.stringLanguage.SmkSLanguage

class SmkSLFile(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, SmkSLanguage) {
    override fun getFileType() = SmkSLFileType
}
