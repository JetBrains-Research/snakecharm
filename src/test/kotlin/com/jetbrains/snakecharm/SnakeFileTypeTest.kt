package com.jetbrains.snakecharm

import com.jetbrains.snakecharm.lang.psi.SmkFile
import junit.framework.TestCase

/**
 * @author Roman.Chernyatchik
 * @date 2019-02-02
 */
class SnakeFileTypeTest : SnakemakeTestCase() {
    fun testSnakefile() {
        doTest("Snakefile")
    }

    fun testSmk() {
        doTest("file.smk")
    }

    fun testRule() {
        doTest("file.rule")
    }

    private fun doTest(fileName: String) {
        fixture!!.addFileToProject(fileName, "")
        fixture!!.configureByFile(fileName)
        val psiFile = fixture!!.file
        requireNotNull(psiFile)
        TestCase.assertTrue(psiFile is SmkFile)

        val virtualFile = psiFile.virtualFile
        assertEquals(SmkFileType, virtualFile.fileType)
    }
}