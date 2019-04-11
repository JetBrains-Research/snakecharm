package com.jetbrains.snakecharm.fixtures

import com.intellij.openapi.util.Ref
import com.intellij.openapi.vfs.ex.temp.TempFileSystem
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
import com.jetbrains.python.psi.LanguageLevel
import com.jetbrains.python.psi.impl.PyBuiltinCache
import com.jetbrains.snakecharm.SnakemakeTestCase

abstract class SnakemakeResolveTestCase : SnakemakeTestCase() {
    companion object {
        fun <T : PsiElement> assertResolveResult(element: PsiElement?,
                                                 aClass: Class<T>,
                                                 name: String): T {
            return assertResolveResult(element, aClass, name, null)
        }

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

        protected fun assertIsBuiltin(element: PsiElement?) {
            assertNotNull(element)
            assertTrue(PyBuiltinCache.getInstance(element).isBuiltin(element))
        }
    }

    protected abstract fun doResolve(): PsiElement?

    protected fun <T : PsiElement> assertResolvesTo(langLevel: LanguageLevel,
                                                    aClass: Class<T>,
                                                    name: String): T {
        val result = Ref<T>()

        runWithLanguageLevel(
                langLevel,
                Runnable { result.set(assertResolvesTo(aClass, name, null)) }
        )

        return result.get()
    }

    protected fun <T : PsiElement> assertResolvesTo(aClass: Class<T>, name: String): T {
        return assertResolvesTo(aClass, name, null)
    }

    protected fun <T : PsiElement> assertResolvesTo(aClass: Class<T>,
                                                    name: String,
                                                    containingFilePath: String?): T {
        val element: PsiElement?
        try {
            element = doResolve()
        } catch (e: Exception) {
            throw RuntimeException(e)
        }

        return assertResolveResult(element, aClass, name, containingFilePath)
    }

    protected fun assertUnresolved() {
        val element: PsiElement?
        try {
            element = doResolve()
        } catch (e: Exception) {
            throw RuntimeException(e)
        }

        assertNull(element)
    }
}