package features.glue

import com.intellij.codeInspection.LocalInspectionEP
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.util.Computable
import com.intellij.openapi.util.Disposer
import com.intellij.testFramework.TestApplicationManager
import com.intellij.testFramework.UsefulTestCase
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory
import com.intellij.testFramework.fixtures.InjectionTestFixture
import com.intellij.testFramework.fixtures.impl.LightTempDirTestFixtureImpl
import com.jetbrains.python.PythonMockSdk
import com.jetbrains.python.codeInsight.controlflow.ControlFlowCache
import com.jetbrains.python.fixtures.PyLightProjectDescriptor
import com.jetbrains.python.psi.LanguageLevel
import com.jetbrains.python.psi.PyFile
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
    @Given("^a (snakemake|snakemake:5x|snakemake:6.1|snakemake:6.5|snakemake:7.32.4|snakemake with disabled framework|python) project$")
    @Throws(Exception::class)
    fun configureSnakemakeProject(projectType: String) {
        // Launched from 'Test worker' thread
        val level = LanguageLevel.PYTHON37

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
            val smkModuleRootName = if (projectType.startsWith("snakemake:")) {
                val suffix = projectType.replace("snakemake:", "")
                "MockPackages3_smk_${suffix}"
            }   else {
                "MockPackages3"
            }
            arrayOf(SnakemakeTestUtil.getTestDataPath().resolve(smkModuleRootName))
        } else {
            emptyArray()
        }

        // Write code here that turns the phrase above into concrete actions
        val testDataRoot = SnakemakeTestUtil.getTestDataPath().toString()
        val projectDescriptor = PyLightProjectDescriptor(level, testDataRoot, *additionalRoots)

        SnakemakeWorld.myPythonOnlySdk = PythonMockSdk.create(
            testDataRoot, level, sdkNameSuffix = "_wo_snakemake"
        )

        val factory = IdeaTestFixtureFactory.getFixtureFactory()
        val fixtureBuilder = factory.createLightFixtureBuilder(projectDescriptor, SnakemakeWorld.myScenarioName)
        val tmpDirFixture = LightTempDirTestFixtureImpl(true) // "tmp://" dir by default

//        val configureSdk = { fixture: CodeInsightTestFixture ->
//            // An alternative is to force set SDK in module settings.
//            // TODO: tests on that!!!!
//            ApplicationManager.getApplication().runWriteAction {
//                ProjectRootManager.getInstance(fixture.project).projectSdk = projectDescriptor.sdk
//            }
//        }

//        val setupFacetClosure = { fixture: CodeInsightTestFixture ->
//            val module = fixture.module
//            val config = createDefaultConfiguration(module.project)
//            if (projectType == "snakemake with facet") {
//                val storage = module.getService(SmkWrapperStorage::class.java)
//                storage.initFrom("\${TEST}", emptyList())
//            }
//            SmkFacetType.createAndAddFacet(module, config)
//        }
        SnakemakeWorld.myPythonSnakemakeSdk = projectDescriptor.sdk
        SnakemakeWorld.myFixture = factory.createCodeInsightFixture(
            fixtureBuilder.fixture, tmpDirFixture
        ).apply {
            testDataPath = testDataRoot

            if (SwingUtilities.isEventDispatchThread()) {
                setUp()
                //configureSdk(this)
            } else {
                ApplicationManager.getApplication().invokeAndWait {
                    try {
                        setUp()
                        //configureSdk(this)
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

        // XXX: Sometimes have to clean it manually:
        //  * When running as gradle task: see ~/.sandbox_pycharm/sytem-test
        //  * When running from *.feature file (IDE run conf): see smth like ~/.gradle/caches/modules-2/files-2.1/com.jetbrains.intellij.pycharm/pycharmPC/2021.1/168dc60fb44a67e1fdfa63e0376b79725178c2df/pycharmPC-2021.1
        // println("Caches: ${PathManager.getIndexRoot()}")

        // XXX: Post Startup activities should end before this if everything goes OK
        SnakemakeWorld.myInjectionFixture = InjectionTestFixture(SnakemakeWorld.fixture())

        setProjectSdk("python with snakemake")

        if (projectType != "snakemake with disabled framework") {
            withSnakemakeFacet("without")
        }
    }

    @Given("^set project sdk as (none|python with snakemake|python only) interpreter")
    fun setProjectSdk(mode: String) {
        ApplicationManager.getApplication().invokeAndWait {
            ApplicationManager.getApplication().runWriteAction {
                val sdk = when (mode) {
                    "none" -> null
                    "python with snakemake" -> SnakemakeWorld.myPythonSnakemakeSdk
                    "python only" -> SnakemakeWorld.myPythonOnlySdk
                    else -> fail("Unsupported mode: $mode")
                }
                val project = SnakemakeWorld.fixture().project
                ProjectRootManager.getInstance(project).projectSdk = sdk
            }
        }
        waitEDTEventsDispatching()
    }

    @Given("^set snakemake framework sdk to (python with snakemake|project|invalid) interpreter")
    fun setSnakemakeFrameworkSdk(mode: String) {
        val newState = SmkSupportProjectSettings.getInstance(SnakemakeWorld.fixture().project).stateSnapshot()
        when (mode) {
            "invalid" -> newState.pythonSdkName = "invalid sdk"
            "python with snakemake" -> newState.pythonSdkName = SnakemakeWorld.myPythonSnakemakeSdk!!.name
            "project" -> newState.pythonSdkName = ""
            else -> fail("Not expected: $mode")
        }
        ApplicationManager.getApplication().invokeAndWait {
            SmkSupportProjectSettings.updateStateAndFireEvent(SnakemakeWorld.fixture().project, newState)
        }

        waitEDTEventsDispatching()
    }
    @Given("^I set snakemake version to \"(.+)\"")
    fun setSmkVersion(version: String) {
        val newState = SmkSupportProjectSettings.getInstance(SnakemakeWorld.fixture().project).stateSnapshot()
        newState.snakemakeLanguageVersion = version
        ApplicationManager.getApplication().invokeAndWait {
            SmkSupportProjectSettings.updateStateAndFireEvent(SnakemakeWorld.fixture().project, newState)
        }
    }

    @Given("^add snakemake framework support (with|without) wrappers loaded")
    fun withSnakemakeFacet(withWrappersStr: String) {
        val project = SnakemakeWorld.fixture().project

        val state = SmkSupportProjectSettings.State()
        state.snakemakeSupportEnabled = true
        if (withWrappersStr != "with") {
            state.useBundledWrappersInfo = false
        }
        waitEDTEventsDispatching()
        ApplicationManager.getApplication().invokeAndWait {
            SmkSupportProjectSettings.updateStateAndFireEvent(project, state)
        }
        waitEDTEventsDispatching()
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

    companion object {
        fun waitEDTEventsDispatching() {
            ApplicationManager.getApplication().invokeAndWait() {
                // Do nothing, wait for events in EDT
            }
        }
    }
}