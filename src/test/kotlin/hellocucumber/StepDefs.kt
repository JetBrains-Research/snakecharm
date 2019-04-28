package hellocucumber

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiPolyVariantReference
import cucumber.api.PendingException
import cucumber.api.java.en.Given
import cucumber.api.java.en.Then
import cucumber.api.java.en.When
import junit.framework.Assert.*

/**
 * @author Roman.Chernyatchik
 * @date 2019-04-28
 */
class StepDefs {
    @Given("^a snakemake project$")
    @Throws(Exception::class)
    fun configureSnakemakeProject() {
        // SnakemakeWorld.myFixture = ...
        // Write code here that turns the phrase above into concrete actions
        throw PendingException()
    }

    @Given("^a file \"(.+)\" with text$")
    fun aFileWithText(name: String, text: String) {
        ApplicationManager.getApplication().runWriteAction {
            SnakemakeWorld.myFixture.addFileToProject(name, text)
        }
    }

    @When("^I put the caret at (.+)$")
    fun iPutCaretAt(marker: String) {
        val editor = SnakemakeWorld.myFixture.editor
        val position = getPositionBySignature(editor, marker, false)
        editor.caretModel.moveToOffset(position)
        throw PendingException()
    }


    @Then("^reference should resolve to \"(.+)\" in \"(.+)\"$")
    fun referenceShouldResolveToIn(marker: String, file: String) {
        val ref = getReferenceAtOffset()
        assertNotNull(ref)
        val result = if (ref is PsiPolyVariantReference) {
            val results = ref.multiResolve(false)
            assertNotNull(results)
            assertEquals(1, results.size.toLong())
            results[0].element
        } else {
            ref!!.resolve()
        }
        assertNotNull(result)
        val containingFile = result!!.containingFile
        assertNotNull(containingFile)
        assertEquals(file, containingFile.name)

        val text = TextRange.from(result.textOffset, marker.length).substring(containingFile.text)
        assertEquals(marker, text)
    }

    private fun getReferenceAtOffset() = SnakemakeWorld.myFixture
            .file.findReferenceAt(getOffsetUnderCaret())

    fun getOffsetUnderCaret() = SnakemakeWorld.myFixture.editor.caretModel.offset


    private fun getPositionBySignature(editor: Editor, marker: String, after: Boolean): Int {
        val text = editor.document.text
        val pos = text.indexOf(marker)
        assertTrue(pos >= 0)
        return if (after) pos + marker.length else pos
    }

}

//import cucumber.api.PendingException
//import cucumber.api.java8.En
//
//class StepDefs2 : En {
//    init {
//        Given("^today is Sunday\$") {
//            throw PendingException()
//        }
//
//        Given("foooooo") {
//
//        }
//    }
//}