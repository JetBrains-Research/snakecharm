package com.jetbrains.snakecharm

import com.intellij.openapi.util.io.FileUtil
import com.intellij.testFramework.PlatformTestCase
import com.jetbrains.snakecharm.lang.psi.SmkFile
import junit.framework.TestCase

/**
 * @author Roman.Chernyatchik
 * @date 2019-02-02
 */
class SnakeFileTypeTest: PlatformTestCase() {
    fun testSnakefile() {
        doTest(prefix = "Snakefile", extension = null)
    }

    fun testSmk() {
        doTest(extension = ".smk")
    }

    fun testRule() {
        doTest(extension = ".rule")
    }

    private fun doTest(extension: String?, prefix: String="test") {
        val dir = createTempDirectory()
        val file = FileUtil.createTempFile(dir, prefix, extension, true)
        val virtualFile = PlatformTestCase.getVirtualFile(file)
        TestCase.assertNotNull(virtualFile)
        val psi = psiManager.findFile(virtualFile)
        TestCase.assertTrue(psi is SmkFile)
        assertEquals(SnakemakeFileType, virtualFile.fileType)
    }
}