package com.jetbrains.snakecharm.cucumber

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.util.text.StringUtil
import cucumber.api.java.en.Given
import cucumber.api.java.en.Then
import junit.framework.TestCase.assertEquals


class FilesSteps {
    @Given("^a file \"(.+)\" with text$")
    fun aFileWithText(name: String, text: String) {
        ApplicationManager.getApplication().invokeAndWait({
            ApplicationManager.getApplication().runWriteAction {
                SnakemakeWorld.fixture().addFileToProject(name, text)
            }
        }, ModalityState.NON_MODAL)
    }

    @Given("^a directory \"(.+)\"")
    fun aDirectory(name: String) {
        ApplicationManager.getApplication().invokeAndWait({
            ApplicationManager.getApplication().runWriteAction {
                SnakemakeWorld.fixture().tempDirFixture.findOrCreateDir(name)
            }
        }, ModalityState.NON_MODAL)
    }

    @Given("^I open a file \"(.+)\" with text$")
    fun iOpenAFile(name: String, text: String) {
        createAndAddFile(name, text)
    }

    @Then("^the file \"(.+)\" should have text$")
    fun theFileShouldHaveText(path: String, text: String) {
        ApplicationManager.getApplication().runReadAction {
            val file = SnakemakeWorld.fixture().findFileInTempDir(path)
            requireNotNull(file)
            val document = FileDocumentManager.getInstance().getDocument(file)
            assertEquals(StringUtil.convertLineSeparators(text), document!!.text)
        }
    }

    private fun createAndAddFile(name: String, text: String) {
        ApplicationManager.getApplication().invokeAndWait({
            ApplicationManager.getApplication().runWriteAction {
                val file = SnakemakeWorld.fixture().addFileToProject(
                        name, StringUtil.convertLineSeparators(text)
                )
                SnakemakeWorld.fixture().configureFromExistingVirtualFile(file.virtualFile)
            }
        }, ModalityState.NON_MODAL)
    }

}