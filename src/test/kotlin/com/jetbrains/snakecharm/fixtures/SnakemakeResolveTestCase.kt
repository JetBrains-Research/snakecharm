package com.jetbrains.snakecharm.fixtures

import com.intellij.openapi.vfs.ex.temp.TempFileSystem
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
import com.jetbrains.snakecharm.SnakemakeTestCase

abstract class SnakemakeResolveTestCase : SnakemakeTestCase() {
    companion object {
        fun <T : PsiElement> assertResolveResult(element: PsiElement?,
                                                 aClass: Class<T>,
                                                 name: String,
                                                 containingFilePath: String?): T {
            assertInstanceOf(element, aClass)
            assertEquals(name, (element as PsiNamedElement).name)
            if (containingFilePath != null) {
                val virtualFile = element.getContainingFile().virtualFile
                if (virtualFile.fileSystem is TempFileSystem) {
                    assertEquals(containingFilePath, virtualFile.path)
                } else {
                    assertEquals(containingFilePath, virtualFile.name)
                }

            }
            return element as T
        }
    }

    private fun doResolve(): PsiElement? {
        val ref = fixture?.getReferenceAtCaretPosition("resolve/" +
                getTestName(false) + ".smk")
        return ref?.resolve()
    }

    protected fun <T : PsiElement> assertResolvesTo(aClass: Class<T>,
                                                    name: String,
                                                    containingFilePath: String? = null): T {
        val element: PsiElement?
        try {
            element = doResolve()
        } catch (e: Exception) {
            throw RuntimeException(e)
        }

        return assertResolveResult(element, aClass, name, containingFilePath)
    }
}