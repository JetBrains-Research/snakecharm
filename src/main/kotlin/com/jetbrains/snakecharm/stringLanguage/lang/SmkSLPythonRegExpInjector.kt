package com.jetbrains.snakecharm.stringLanguage.lang

import com.intellij.lang.injection.MultiHostRegistrar
import com.intellij.psi.PsiElement
import com.intellij.psi.util.elementType
import com.jetbrains.python.codeInsight.PyInjectionUtil
import com.jetbrains.python.codeInsight.PyInjectorBase
import com.jetbrains.python.codeInsight.regexp.PythonRegexpLanguage
import com.jetbrains.python.psi.PyStringLiteralExpression

open class SmkSLPythonRegExpInjector : PyInjectorBase() {
    override fun getInjectedLanguage(element: PsiElement) = PythonRegexpLanguage()

    override fun elementsToInjectIn() = listOf(PyStringLiteralExpression::class.java)

    override fun registerInjection(
            registrar: MultiHostRegistrar,
            host: PsiElement
    ): PyInjectionUtil.InjectionResult = when {
        host.isValidForInjection() -> super.registerInjection(registrar, host)
        else -> PyInjectionUtil.InjectionResult.EMPTY
    }

    private fun PsiElement.isValidForInjection(): Boolean {
        if (this.language == PythonRegexpLanguage()) {
            return true
        }
        return false
    }
}
