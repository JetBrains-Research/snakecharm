package com.jetbrains.snakecharm.fixtures

import com.intellij.openapi.util.Ref
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.ex.temp.TempFileSystem
import com.intellij.psi.*
import com.intellij.testFramework.TestDataFile
import com.jetbrains.python.psi.LanguageLevel
import com.jetbrains.python.psi.impl.PyBuiltinCache
import com.jetbrains.snakecharm.SnakemakeTestCase
import com.jetbrains.snakecharm.SnakemakeTestUtil.getTestDataPath
import java.io.File
import java.io.IOException

abstract class SnakemakeResolveTestCase : SnakemakeTestCase() {
    companion object {
        protected const val MARKER = "<ref>"

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

        fun findMarkerOffset(psiFile: PsiFile): Int {
            // TODO: harmonize with CythonResolveTest synax
            // TODO: check and fix work with single letter identifiers
            val document = PsiDocumentManager.getInstance(psiFile.project).getDocument(psiFile)!!
            var offset = -1
            for (i in 1 until document.lineCount) {
                val lineStart = document.getLineStartOffset(i)
                val lineEnd = document.getLineEndOffset(i)
                val index = document.charsSequence.subSequence(lineStart, lineEnd).toString().indexOf("<ref>")
                if (index > 0) {
                    offset = document.getLineStartOffset(i - 1) + index
                }
            }
            assertTrue("<ref> in test file not found", offset >= 0)
            return offset
        }

        fun findReferenceByMarker(psiFile: PsiFile?): PsiReference? {
            val ref = psiFile?.findReferenceAt(findMarkerOffset(psiFile))
            assertNotNull("No reference found at <ref> position", ref)
            return ref
        }

        protected fun assertIsBuiltin(element: PsiElement?) {
            assertNotNull(element)
            assertTrue(PyBuiltinCache.getInstance(element).isBuiltin(element))
        }
    }

    protected fun configureByFile(@TestDataFile filePath: String): PsiReference? {
        val testDataRoot = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(getTestDataPath().toFile())
        val file = testDataRoot!!.findFileByRelativePath(filePath)
        assertNotNull(file)

        var fileText: String
        try {
            fileText = StringUtil.convertLineSeparators(VfsUtil.loadText(file!!))
        } catch (e: IOException) {
            throw RuntimeException(e)
        }

        val offset = fileText.indexOf(MARKER)
        assertTrue(offset >= 0)
        fileText = fileText.substring(0, offset) + fileText.substring(offset + MARKER.length)
        fixture?.configureByText(File(filePath).name, fileText)
        return fixture?.file?.findReferenceAt(offset)
    }

    protected abstract fun doResolve(): PsiElement?

    protected fun <T : PsiElement> assertResolvesTo(langLevel: LanguageLevel, aClass: Class<T>, name: String): T {
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