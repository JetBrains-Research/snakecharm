package features.glue

import com.intellij.codeInspection.LocalInspectionEP
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.Computable
import com.intellij.openapi.util.Disposer
import com.intellij.testFramework.TestApplicationManager
import com.intellij.testFramework.UsefulTestCase
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory
import com.intellij.testFramework.fixtures.InjectionTestFixture
import com.intellij.testFramework.fixtures.impl.LightTempDirTestFixtureImpl
import com.jetbrains.python.codeInsight.controlflow.ControlFlowCache
import com.jetbrains.python.fixtures.PyLightProjectDescriptor
import com.jetbrains.python.psi.PyFile
import com.jetbrains.snakecharm.SnakemakeTestCase
import com.jetbrains.snakecharm.SnakemakeTestUtil
import com.jetbrains.snakecharm.framework.SmkSupportProjectSettings
import io.cucumber.java.en.Given
import javax.swing.SwingUtilities
import kotlin.test.fail


/**
 * @author Roman.Chernyatchik
 * @date 2019-04-28
 */
class StepDefs {
    @Given("^a (snakemake|snakemake with wrappers|python) project$")
    @Throws(Exception::class)
    fun configureSnakemakeProject(projectType: String) {
        require(SnakemakeWorld.myFixture == null) {
            "fixture must be null here, looks like cleanup after prev test failed."
        }

        TestApplicationManager.getInstance()

        // From UsefulTestCase
        Disposer.setDebugMode(true)

        SnakemakeWorld.myTestRootDisposable = TestDisposable()
        SnakemakeWorld.myFoundRefs

        // XXX: Seems don't need to enable them, enabled via fixture.enableInspection()
        //if (enabledInspections != null) {
        //    InspectionProfileImpl.INIT_INSPECTIONS = true
        //}
        val additionalRoots = if (projectType.startsWith("snakemake")) {
            arrayOf(SnakemakeTestUtil.getTestDataPath().resolve("MockPackages3"))
        } else {
            emptyArray()
        }

        // Write code here that turns the phrase above into concrete actions
        val projectDescriptor = PyLightProjectDescriptor(
            SnakemakeTestCase.PYTHON_3_MOCK_SDK,
            SnakemakeTestUtil.getTestDataPath().toString(),
            *additionalRoots
        )

        val factory = IdeaTestFixtureFactory.getFixtureFactory()
        val fixtureBuilder = factory.createLightFixtureBuilder(projectDescriptor)
        val tmpDirFixture = LightTempDirTestFixtureImpl(true) // "tmp://" dir by default

//        val setupFacetClosure = { fixture: CodeInsightTestFixture ->
//            val module = fixture.module
//            val config = createDefaultConfiguration(module.project)
//            if (projectType == "snakemake with facet") {
//                val storage = module.getService(SmkWrapperStorage::class.java)
//                storage.initFrom("\${TEST}", emptyList())
//            }
//            SmkFacetType.createAndAddFacet(module, config)
//        }

        SnakemakeWorld.myFixture = factory.createCodeInsightFixture(
            fixtureBuilder.fixture, tmpDirFixture
        ).apply {
            testDataPath = SnakemakeTestUtil.getTestDataPath().toString()

            if (SwingUtilities.isEventDispatchThread()) {
                // todo: add facet?
                setUp()
//                setupFacetClosure(this)
            } else {
                ApplicationManager.getApplication().invokeAndWait {
                    try {
                        // todo: add facet?
                        setUp()
//                        setupFacetClosure(this)
                    } catch (e: java.lang.Exception) {
                        throw RuntimeException("Error running setup", e)
                    }
                }
            }
        }

        // XXX: optional: Ensure than language extensions are loaded, e.g. if `SnakemakeVisitorFilter` isn't available at
        //   runtime in test suite. This workaround is used in SnakemakeParsingTests and seems resolves all issues,
        //   but you could re-enable it here if something goes wrong
        // We have to force clean language extensions cache here, because these parser tests don't use real
        // test application and don't load all required extensions.
        // E.g. PythonId.visitorFilter EP will not load `SnakemakeVisitorFilter` and as a result other tests in test suite will fail
        // val languageExtension = PythonVisitorFilter.INSTANCE
        // languageExtension.clearCache(SnakemakeLanguageDialect)
        // languageExtension.clearCache(PythonLanguage.INSTANCE)

        SnakemakeWorld.myInjectionFixture = InjectionTestFixture(SnakemakeWorld.fixture())
    }

    @Given("^add snakemake framework support (with|without) wrappers loaded")
    fun withSnakemakeFacet(withWrappersStr: String) {
        ApplicationManager.getApplication().invokeAndWait {
            val project = SnakemakeWorld.fixture().project

            val state = SmkSupportProjectSettings.State()
            state.snakemakeSupportEnabled = true
            if (withWrappersStr != "with") {
                state.useBundledWrappersInfo = false
            }
            SmkSupportProjectSettings.updateStateAndFireEvent(project, state)
        }
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

        for (provider in LocalInspectionEP.LOCAL_INSPECTION.extensionList) {
            val o = provider.instance
            if (o is LocalInspectionTool && inspectionName == o.shortName) {
                fixture.enableInspections(o)
                return
            }
        }
        fail("Unknown inspection:$inspectionName")
    }

    @Given("^TODO")
    fun todo() {
        TODO()
    }
}