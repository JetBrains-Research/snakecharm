package com.jetbrains.snakecharm.cucumber

import com.intellij.codeInspection.LocalInspectionEP
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.util.Computable
import com.intellij.openapi.util.text.StringUtil
import com.intellij.testFramework.UsefulTestCase
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory
import com.intellij.testFramework.fixtures.impl.LightTempDirTestFixtureImpl
import com.jetbrains.python.PythonDialectsTokenSetProvider
import com.jetbrains.python.codeInsight.controlflow.ControlFlowCache
import com.jetbrains.python.fixtures.PyLightProjectDescriptor
import com.jetbrains.python.inspections.PyUnreachableCodeInspection
import com.jetbrains.python.psi.PyFile
import com.jetbrains.snakecharm.SnakemakeTestCase
import com.jetbrains.snakecharm.SnakemakeTestUtil
import com.jetbrains.snakecharm.inspections.*
import cucumber.api.java.en.Given
import junit.framework.Assert.fail


/**
 * @author Roman.Chernyatchik
 * @date 2019-04-28
 */
class StepDefs {
    @Given("^a (snakemake|python)? project$")
    @Throws(Exception::class)
    fun configureSnakemakeProject(projectType: String) {
        // XXX: Seems don't need to enable them, enabled via fixture.enableInspection()
        //if (enabledInspections != null) {
        //    InspectionProfileImpl.INIT_INSPECTIONS = true
        //}
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

    @Given("^I expect controlflow")
    fun iexpectControlflow(expectedCFG: String) {
        val actualCFG = ApplicationManager.getApplication().runReadAction(Computable<String> {
            val flow = ControlFlowCache.getControlFlow(SnakemakeWorld.fixture().file as PyFile)
            flow.instructions.joinToString(separator = "\n")
        })
        UsefulTestCase.assertSameLines(expectedCFG.trim(), actualCFG.trim())
    }

    @Given("^([^\\]]+) inspection is enabled$")
    fun inspectionIsEnabled(inspectionName: String) {
        val fixture = SnakemakeWorld.fixture()
        when (inspectionName) {
            "Shadow Settings" -> fixture.enableInspections(SmkShadowSettingsInspection::class.java)
            "Shadow Multiple Settings" -> fixture.enableInspections(SmkShadowMultipleSettingsInspection::class.java)
            "Resources Keyword Arguments" -> fixture.enableInspections(SmkResourcesKeywordArgsInspection::class.java)
            "Rule Redeclaration" -> fixture.enableInspections(SmkRuleRedeclarationInspection::class.java)
            "Rule Section After Execution Section" ->
                fixture.enableInspections(SmkRuleSectionAfterExecutionInspection::class.java)
            "Section Redeclaration" -> fixture.enableInspections(SmkSectionRedeclarationInspection::class.java)
            "Subworkflow Multiple Args" -> fixture.enableInspections(SmkSubworkflowMultipleArgsInspection::class.java)
            "Subworkflow Redeclaration" -> fixture.enableInspections(SmkSubworkflowRedeclarationInspection::class.java)
            "Unreachable Code" -> fixture.enableInspections(PyUnreachableCodeInspection::class.java)
            else -> {
                for (provider in LocalInspectionEP.LOCAL_INSPECTION.extensionList) {
                    val o = provider.instance
                    if (o is LocalInspectionTool && inspectionName == o.shortName) {
                        fixture.enableInspections(o)
                        return
                    }
                }
                fail("Unknown inspection:$inspectionName")
            }
        }
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