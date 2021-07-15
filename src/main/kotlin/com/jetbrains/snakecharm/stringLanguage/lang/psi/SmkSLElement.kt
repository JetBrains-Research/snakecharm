package com.jetbrains.snakecharm.stringLanguage.lang.psi

import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.psi.PsiElement

interface SmkSLElement: PsiElement {
    fun injectionHost() = InjectedLanguageManager.getInstance(project).getInjectionHost(this)
}
