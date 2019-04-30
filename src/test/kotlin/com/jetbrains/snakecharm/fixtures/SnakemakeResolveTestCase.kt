package com.jetbrains.snakecharm.fixtures

import com.intellij.openapi.vfs.ex.temp.TempFileSystem
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
import com.jetbrains.snakecharm.SnakemakeTestCase
import kotlin.test.assertNotEquals

abstract class SnakemakeResolveTestCase : SnakemakeTestCase() {
    companion object {
        fun <T : PsiElement> assertResolveResult(
                actualElement: PsiElement?,
                expectedClass: Class<T>,
                expectedElementName: String,
                expectedContainingFilePath: String?
        ): T {
            assertInstanceOf(actualElement, expectedClass)
            assertEquals(expectedElementName, (actualElement as PsiNamedElement).name)
            if (expectedContainingFilePath != null) {
                val virtualFile = actualElement.getContainingFile().virtualFile
                if (virtualFile.fileSystem is TempFileSystem) {
                    assertEquals(expectedContainingFilePath, virtualFile.path)
                } else {
                    assertEquals(expectedContainingFilePath, virtualFile.name)
                }

            }
            return actualElement as T
        }
    }

    private fun doResolve(fileExtension: String): PsiElement? = fixture
            ?.getReferenceAtCaretPosition("resolve/${getTestName(true)}$fileExtension")
            ?.resolve()

    protected fun <T : PsiElement> assertResolvesTo(
            expectedClass: Class<T>,
            expectedElementName: String,
            expectedContainingFilePath: String? = null,
            fileExtension: String = ".smk"
    ): T = assertResolveResult(doResolve(fileExtension), expectedClass, expectedElementName, expectedContainingFilePath)

    protected fun <T : PsiElement> assertResolveFail(
            expectedClass: Class<T>,
            expectedElementName: String,
            fileExtension: String
    ) {
        val resolveResult = doResolve(fileExtension)
        assertTrue(resolveResult?.javaClass != expectedClass)
        assertNotEquals(expectedElementName, (resolveResult as PsiNamedElement?)?.name)
    }
}