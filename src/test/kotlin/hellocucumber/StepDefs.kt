package hellocucumber

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiPolyVariantReference
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory
import com.intellij.testFramework.fixtures.impl.LightTempDirTestFixtureImpl
import com.jetbrains.python.PythonDialectsTokenSetProvider
import com.jetbrains.python.fixtures.PyLightProjectDescriptor
import com.jetbrains.snakecharm.SnakemakeTestUtil
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
        val projectDescriptor = PyLightProjectDescriptor(
                "3.7", SnakemakeTestUtil.getTestDataPath().toString()
        )

        val fixtureBuilder = IdeaTestFixtureFactory
                .getFixtureFactory()
                .createLightFixtureBuilder(projectDescriptor)

        val tmpDirFixture = LightTempDirTestFixtureImpl(true) // "tmp://" dir by default

        SnakemakeWorld.myFixture = IdeaTestFixtureFactory.getFixtureFactory().createCodeInsightFixture(
                fixtureBuilder.fixture,
                tmpDirFixture
        )

        SnakemakeWorld.fixture().setUp()
        SnakemakeWorld.fixture().testDataPath = SnakemakeTestUtil.getTestDataPath().toString()
        PythonDialectsTokenSetProvider.reset()
    }

    @Given("^a file \"(.+)\" with text$")
    fun aFileWithText(name: String, text: String) {
        ApplicationManager.getApplication().invokeAndWait({
            ApplicationManager.getApplication().runWriteAction {
                SnakemakeWorld.fixture().addFileToProject(name, text)
            }
        }, ModalityState.NON_MODAL)
    }

    @Given("^I open a file \"(.+)\" with text$")
    fun iOpenAFile(name: String, text: String) {
        createAndAddFile(name, text)
    }

    fun createAndAddFile(name: String, text: String) {
        ApplicationManager.getApplication().invokeAndWait({
            ApplicationManager.getApplication().runWriteAction {
                val file = SnakemakeWorld.fixture().addFileToProject(
                        name, StringUtil.convertLineSeparators(text)
                )
                SnakemakeWorld.fixture().configureFromExistingVirtualFile(file.virtualFile)
            }
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
    }

    private fun getReferenceAtOffset() = SnakemakeWorld.fixture()
            .file.findReferenceAt(getOffsetUnderCaret())

    fun getOffsetUnderCaret() = SnakemakeWorld.fixture().editor.caretModel.offset


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