package com.jetbrains.snakecharm.cucumber

import com.intellij.psi.PsiReference
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import com.intellij.testFramework.fixtures.InjectionTestFixture

/**
 * @author Roman.Chernyatchik
 * @date 2019-04-28
 */
object SnakemakeWorld {
    var myInjectionFixture: InjectionTestFixture? = null
    var myFixture: CodeInsightTestFixture? = null
    var myCompletionList: List<String>? = null
    var myGeneratedDocPopupText: String? = null
    var myFoundRefs: List<PsiReference> = emptyList()

    fun injectionFixture() = myInjectionFixture!!
    fun fixture()= myFixture!!
    fun completionList()= myCompletionList!!

    fun getOffsetUnderCaret() = fixture().editor.caretModel.offset
    fun findPsiElementUnderCaret() = fixture().file.findElementAt(getOffsetUnderCaret())
}