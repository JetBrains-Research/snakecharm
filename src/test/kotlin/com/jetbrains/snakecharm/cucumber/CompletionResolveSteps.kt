package com.jetbrains.snakecharm.cucumber

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiPolyVariantReference
import com.intellij.psi.PsiReference
import cucumber.api.java.en.Then
import cucumber.api.java.en.When
import junit.framework.Assert
import junit.framework.TestCase
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * @author Roman.Chernyatchik
 * @date 2019-05-09
 */
class CompletionResolveSteps {
    @Then("^reference should not resolve$")
    fun referenceShouldNotResolve() {
        ApplicationManager.getApplication().invokeAndWait({
            val ref = getReferenceAtOffset()
            assertNotNull(ref)
            assertUnresolvedReference(ref)
        }, ModalityState.NON_MODAL)
    }


    @When("^I put the caret at (.+)$")
    fun iPutCaretAt(marker: String) {
        ApplicationManager.getApplication().invokeAndWait({
            val editor = SnakemakeWorld.fixture().editor
            val position = getPositionBySignature(editor, marker, false)
            editor.caretModel.moveToOffset(position)
        }, ModalityState.NON_MODAL)
    }


    @Then("^reference should resolve to \"(.+)\" in \"(.+)\"$")
    fun referenceShouldResolveToIn(marker: String, file: String) {
        ApplicationManager.getApplication().runReadAction {
            val ref = getReferenceAtOffset()
            assertNotNull(ref)
            val result = resolve(ref)
            assertNotNull(result)
            val containingFile = result!!.containingFile
            assertNotNull(containingFile)
            assertEquals(file, containingFile.name)

            val text = TextRange.from(result.textOffset, marker.length).substring(containingFile.text)
            assertEquals(marker, text)
        }
    }

    private fun resolve(ref: PsiReference) = when (ref) {
        is PsiPolyVariantReference -> {
            val results = ref.multiResolve(false)
            assertNotNull(results)
            assertEquals(1, results.size.toLong())
            results[0].element
        }
        else -> ref.resolve()
    }

    private fun assertUnresolvedReference(ref: PsiReference) {
        when (ref) {
            is PsiPolyVariantReference -> {
                val results = ref.multiResolve(false)
                assertNotNull(results)
                assertEquals(0, results.size.toLong())
            }
            else -> TestCase.assertNull(ref.resolve())
        }
    }

    private fun getReferenceAtOffset() = SnakemakeWorld.fixture()
            .file.findReferenceAt(getOffsetUnderCaret())

    private fun getPositionBySignature(editor: Editor, marker: String, after: Boolean): Int {
        val text = editor.document.text
        val pos = text.indexOf(marker)
        Assert.assertTrue(pos >= 0)
        return if (after) pos + marker.length else pos
    }

    companion object {
        fun getOffsetUnderCaret() = SnakemakeWorld.fixture().editor.caretModel.offset
    }
}
