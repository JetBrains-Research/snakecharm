package com.jetbrains.snakecharm

import com.intellij.openapi.vfs.ex.temp.TempFileSystem
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
import junit.framework.TestCase

/**
 * We are using cucumber tests instead
 */
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

    private fun doResolve(fileExtension: String): PsiElement? {
        val path = "resolve/${getTestName(true)}$fileExtension"
        val ref = fixture!!.getReferenceAtCaretPosition(path)
        requireNotNull(ref) {
            "Precondition not met: no reference at caret was found"
        }
        return ref.resolve()
    }

    protected fun <T : PsiElement> assertResolvesTo(
            expectedClass: Class<T>,
            expectedElementName: String,
            expectedContainingFilePath: String? = null,
            fileExtension: String = ".smk"
    ) {
        assertResolveResult(
                doResolve(fileExtension),
                expectedClass, expectedElementName, expectedContainingFilePath
        )
    }

    protected fun assertUnresolved(fileExtension: String) {
        TestCase.assertNull(doResolve(fileExtension))
    }
}