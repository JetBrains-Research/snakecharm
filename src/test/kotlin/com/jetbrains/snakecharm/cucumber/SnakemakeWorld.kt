package com.jetbrains.snakecharm.cucumber

import com.intellij.testFramework.fixtures.CodeInsightTestFixture

/**
 * @author Roman.Chernyatchik
 * @date 2019-04-28
 */
object SnakemakeWorld {
    var myFixture: CodeInsightTestFixture? = null
    var myCompletionList: List<String>? = null
    var myGeneratedDocPopupText: String? = null

    fun fixture()= myFixture!!
    fun completionList()= myCompletionList!!

    fun getOffsetUnderCaret() = fixture().editor.caretModel.offset
    fun findPsiElementUnderCaret() = fixture().file.findElementAt(getOffsetUnderCaret())
}