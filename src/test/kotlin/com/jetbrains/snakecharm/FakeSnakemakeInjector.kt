package com.jetbrains.snakecharm

import com.intellij.lang.injection.MultiHostRegistrar
import com.intellij.psi.PsiElement
import com.jetbrains.python.codeInsight.PyInjectionUtil
import com.jetbrains.snakecharm.lang.SnakemakeLanguageDialect
import com.jetbrains.snakecharm.stringLanguage.lang.SmkSLInjector

class FakeSnakemakeInjector(private val injectionOffset: Int) : SmkSLInjector() {
    override fun registerInjection(registrar: MultiHostRegistrar, host: PsiElement): PyInjectionUtil.InjectionResult {
        if (injectionOffset in host.textRange && SnakemakeLanguageDialect.isInsideSmkFile(host)) {
            val language = this.getInjectedLanguage(host)
            val element = PyInjectionUtil.getLargestStringLiteral(host)
            if (element != null) {
                return PyInjectionUtil.registerStringLiteralInjection(element, registrar, language)
            }
        }
        return PyInjectionUtil.InjectionResult.EMPTY
    }
}