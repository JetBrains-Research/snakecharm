package com.jetbrains.snakecharm.cucumber

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.util.text.StringUtil
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory
import com.intellij.testFramework.fixtures.impl.LightTempDirTestFixtureImpl
import com.jetbrains.python.PythonDialectsTokenSetProvider
import com.jetbrains.python.fixtures.PyLightProjectDescriptor
import com.jetbrains.snakecharm.SnakemakeTestCase
import com.jetbrains.snakecharm.SnakemakeTestUtil
import cucumber.api.java.en.Given

/**
 * @author Roman.Chernyatchik
 * @date 2019-04-28
 */
class StepDefs {
    @Given("^a (snakemake|python)? project$")
    @Throws(Exception::class)
    fun configureSnakemakeProject(projectType: String) {
        val additionalRoots = if (projectType == "snakemake") {
           arrayOf(SnakemakeTestUtil.getTestDataPath().resolve("MockPackages3"))
        } else {
            emptyArray()
        }

        // SnakemakeWorld.myFixture = ...
        // Write code here that turns the phrase above into concrete actions
        val projectDescriptor = PyLightProjectDescriptor(
                SnakemakeTestCase.PYTHON_3_MOCK_SDK,
                SnakemakeTestUtil.getTestDataPath().toString(),
                *additionalRoots
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
}