package com.jetbrains.snakecharm

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.psi.PsiFile
import com.intellij.psi.codeStyle.CodeStyleManager
import com.jetbrains.python.formatter.PyCodeStyleSettings
import com.jetbrains.snakecharm.lang.SnakemakeLanguageDialect


class SnakemakeFormatterTest : SnakemakeTestCase() {
    private fun getPythonCodeStyleSettings(): PyCodeStyleSettings {
        return getCodeStyleSettings().getCustomSettings(PyCodeStyleSettings::class.java)
    }

    fun testAlignSectionArgs() {
        doTest()
    }

    fun testAlignSectionArgs_AlignMultiline() {
        getCommonCodeStyleSettings().ALIGN_MULTILINE_PARAMETERS_IN_CALLS = true
        doTest()
    }

    fun testAlignSectionArgs_KeepLB() {
        doTest()
    }

    fun testAlignSectionArgs_NotKeepLB() {
        getCommonCodeStyleSettings().KEEP_LINE_BREAKS = false
        doTest()
    }

    fun testHardWrap20() {
        getCodeStyleSettings().setRightMargin(SnakemakeLanguageDialect, 20);
        // getCommonCodeStyleSettings().WRAP_LONG_LINES = true;
        doTest()
    }

    fun testHardWrap20_NoWrap() {
        // TODO: not working, let's use large margin workaround
        //getCodeStyleSettings().setRightMargin(SnakemakeLanguageDialect, 20);
        getCodeStyleSettings().setRightMargin(SnakemakeLanguageDialect, 400);
        getCommonCodeStyleSettings().WRAP_LONG_LINES = false
        doTest()
    }

    fun testAroundRuleLikeSections() {
        doTest()
    }

    private fun doTest() {
        doTest(false)
    }

    private fun doTest(reformatText: Boolean) {
        val testName = getTestName(true)
        fixture!!.configureByFile("formatter/$testName.smk")
        WriteCommandAction.runWriteCommandAction(null) {
            val codeStyleManager: CodeStyleManager = CodeStyleManager.getInstance(fixture!!.getProject())
            val file: PsiFile = fixture!!.getFile()
            if (reformatText) {
                codeStyleManager.reformatText(file, 0, file.textLength)
            } else {
                codeStyleManager.reformat(file)
            }
        }
        fixture!!.checkResultByFile("formatter/${testName}_after.smk")
    }

}