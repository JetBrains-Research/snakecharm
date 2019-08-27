package com.jetbrains.snakecharm.cucumber

import com.intellij.codeInspection.LocalInspectionEP
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.Computable
import com.intellij.testFramework.UsefulTestCase
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory
import com.intellij.testFramework.fixtures.InjectionTestFixture
import com.intellij.testFramework.fixtures.impl.LightTempDirTestFixtureImpl
import com.jetbrains.python.PythonDialectsTokenSetProvider
import com.jetbrains.python.codeInsight.controlflow.ControlFlowCache
import com.jetbrains.python.fixtures.PyLightProjectDescriptor
import com.jetbrains.python.inspections.PyUnreachableCodeInspection
import com.jetbrains.python.inspections.unresolvedReference.PyUnresolvedReferencesInspection
import com.jetbrains.python.psi.PyFile
import com.jetbrains.snakecharm.SnakemakeTestCase
import com.jetbrains.snakecharm.SnakemakeTestUtil
import com.jetbrains.snakecharm.inspections.*
import cucumber.api.java.en.Given
import kotlin.test.fail


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

        SnakemakeWorld.myInjectionFixture = InjectionTestFixture(SnakemakeWorld.fixture())

        PythonDialectsTokenSetProvider.reset()
    }

    @Given("^I expect controlflow")
    fun iexpectControlflow(expectedCFG: String) {
        val actualCFG = ApplicationManager.getApplication().runReadAction(Computable<String> {
            val flow = ControlFlowCache.getControlFlow(SnakemakeWorld.fixture().file as PyFile)
            flow.instructions.joinToString(separator = "\n")
        })
        UsefulTestCase.assertSameLines(expectedCFG.replace("\r", "").trim(), actualCFG.trim())
    }

    @Given("^([^\\]]+) inspection is enabled$")
    fun inspectionIsEnabled(inspectionName: String) {
        val fixture = SnakemakeWorld.fixture()
        when (inspectionName) {
            "Shadow Settings" -> fixture.enableInspections(SmkShadowSettingsInspection::class.java)
            "Resources Keyword Arguments" -> fixture.enableInspections(SmkResourcesKeywordArgsInspection::class.java)
            "Rule Redeclaration" -> fixture.enableInspections(SmkRuleRedeclarationInspection::class.java)
            "Rule Section After Execution Section" ->
                fixture.enableInspections(SmkRuleSectionAfterExecutionInspection::class.java)
            "Section Redeclaration" -> fixture.enableInspections(SmkSectionRedeclarationInspection::class.java)
            "Section Multiple Args" -> fixture.enableInspections(SmkSectionMultipleArgsInspection::class.java)
            "Subworkflow Redeclaration" -> fixture.enableInspections(SmkSubworkflowRedeclarationInspection::class.java)
            "Unreachable Code" -> fixture.enableInspections(PyUnreachableCodeInspection::class.java)
            "Rule or Checkpoint Name yet undefined" ->
                fixture.enableInspections(SmkRuleOrCheckpointNameYetUndefinedInspection::class.java)
            "Unresolved reference" -> fixture.enableInspections(PyUnresolvedReferencesInspection::class.java)
            "Repeated Rule in Localrules or Ruleorder" ->
                fixture.enableInspections(SmkLocalrulesRuleorderRepeatedRuleInspection::class.java)
            "Lambda Functions in Rule Sections" -> fixture.enableInspections(SmkLambdaRuleParamsInspection::class.java)
            "Wildcard not defined" -> fixture.enableInspections(SmkWildcardNotDefinedInspection::class.java)
            "Not same wildcards set" -> fixture.enableInspections(SmkNotSameWildcardsSetInspection::class.java)
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
}