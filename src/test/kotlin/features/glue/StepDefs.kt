package features.glue

import com.intellij.codeInspection.LocalInspectionEP
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.extensions.ExtensionPoint
import com.intellij.openapi.extensions.impl.ExtensionsAreaImpl
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.util.Computable
import com.intellij.openapi.util.Disposer
import com.intellij.python.community.helpersLocator.PythonHelpersLocator
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
import com.jetbrains.snakecharm.SnakemakeTestCase.Companion.allowPythonRootsAccess
import com.jetbrains.snakecharm.SnakemakeTestUtil
import com.jetbrains.snakecharm.framework.SmkSupportProjectSettings
import com.jetbrains.snakecharm.framework.SnakemakeApiYamlAnnotationsService
import io.cucumber.java.en.Given
import java.nio.file.Path
import javax.swing.SwingUtilities
import kotlin.test.fail


/**
 * @author Roman.Chernyatchik
 * @date 2019-04-28
 */
class StepDefs {
    @Given("^a (snakemake|snakemake:.*|snakemake with disabled framework|python) project$")
    @Throws(Exception::class)
    fun configureSnakemakeProject(projectType: String) {
        // Launched from 'Test worker' thread
        val level = LanguageLevel.PYTHON37

        require(SnakemakeWorld.myFixture == null) {
            "fixture must be null here, looks like cleanup after prev test failed."
        }

        TestApplicationManager.getInstance()

        configurePythonHelpersLocator()

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
                "MockPackages3" // XXX: uses 'snakemake' symlink from 'MockPackages3' to attach snakemake libs
            }
            arrayOf(SnakemakeTestUtil.getTestDataPath().resolve(smkModuleRootName))
        } else {
            emptyArray()
        }

        // Write code here that turns the phrase above into concrete actions
        val testDataRoot = SnakemakeTestUtil.getTestDataPath().toString()

        // Reuse the descriptor (and therefore its SDK) across scenarios with the same roots. Each
        // descriptor builds a mock SDK named "Mock Python SDK <level>", and since 2026.2 SDKs are
        // workspace-model entities: adding a second one with the same symbolic id logs
        // "addEntity: symbolic id already exists", which TestLoggerFactory turns into a test failure.
        // A per-scenario descriptor therefore failed ~1070 otherwise-unrelated scenarios. Caching is
        // also the standard light-test pattern (a static LightProjectDescriptor), and lets the light
        // fixture reuse the project instead of rebuilding it per scenario.
        val descriptorKey = listOf(level.toString(), testDataRoot) + additionalRoots.map { it.toString() }
        val projectDescriptor = projectDescriptors.getOrPut(descriptorKey) {
            PyLightProjectDescriptor(level, testDataRoot, *additionalRoots)
        }

        SnakemakeWorld.myPythonOnlySdk = pythonOnlySdks.getOrPut(listOf(level.toString(), testDataRoot)) {
            PythonMockSdk.create(testDataRoot, level, sdkNameSuffix = "_wo_snakemake")
        }

        val factory = IdeaTestFixtureFactory.getFixtureFactory()
        allowPythonRootsAccess(SnakemakeWorld.myTestRootDisposable!!)

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

        // XXX: reset Snakemake API settings if smth was overridden in other tests
        ApplicationManager.getApplication().invokeAndWait {
            ApplicationManager.getApplication().runWriteAction {
                SnakemakeApiYamlAnnotationsService.getInstance().reinitializeInTests()
            }
        }
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
    @Given("^I set snakemake language version to \"(.+)\"")
    fun setLanguageSmkVersion(version: String) {
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

    /**
     * Make `com.jetbrains.python.pythonHelpersLocator` usable in the test JVM: prune the crashing Pro
     * locator when the EP exists (2026.1), and register the EP outright when it does not (2026.2).
     *
     * `PythonMockSdk.create` below triggers `PyTypeShed`'s lazy init, which calls
     * `PythonHelpersLocator.getHelpersRoots()` — that iterates every registered locator with no
     * exception guard. The obfuscated Pro locator's `getRoot()` calls `getPluginDistDirByClass`, which
     * throws `IllegalStateException: .../plugins/python/lib/modules should be lib directory` because the
     * unified 2026.1 Python plugin ships its code as v2 content modules under `lib/modules/` rather than
     * directly under `lib/`. That is purely a gradle-test-sandbox artifact (the flattened test classpath
     * means the plugin classes aren't under a `PluginAwareClassLoader`, so the safe branch of
     * `getPluginDistDirByClass` isn't taken; upstream
     * https://github.com/JetBrains/intellij-platform-gradle-plugin/issues/2070, unfixed). Unlike the
     * community locator it reads no `idea.python.helpers.path` property, so it can't be pointed at a
     * valid root. Removing just this one dynamic EP leaves the community locator (fed by the
     * `-Didea.python.helpers.path` jvmArg) and the rest of the Pro Python plugin intact, so Python
     * resolution still works. Idempotent — safe to call before every scenario. Runtime is unaffected.
     */
    private fun configurePythonHelpersLocator() {
        val area = ApplicationManager.getApplication().extensionArea

        val ep = area.getExtensionPointIfRegistered<Any>(PYTHON_HELPERS_LOCATOR_EP)
        if (ep != null) {
            ep.unregisterExtensions(
                { className, _ -> className != "com.jetbrains.python.PythonProHelpersLocator" },
                false,
            )
            return
        }

        // Since 2026.2 the EP itself is declared by the `intellij.python.community.helpersLocator`
        // content module, which the flat test classpath never loads (same #2070 cause as above), so
        // there is nothing to prune -- `PythonHelpersLocator.getHelpersRoots()` instead dies with
        // "Missing extension point: com.jetbrains.python.pythonHelpersLocator". Register the EP and a
        // locator pointing at the helpers directory the `-Didea.python.helpers.path` jvmArg already
        // supplies. We register our own rather than PythonHelpersLocatorDefault because the default
        // resolves through the plugin dist dir, which is exactly what #2070 breaks here.
        val helpersPath = requireNotNull(System.getProperty(PYTHON_HELPERS_PATH_PROPERTY)) {
            "'$PYTHON_HELPERS_PATH_PROPERTY' is not set; see the test task's jvmArgumentProviders in build.gradle.kts"
        }
        (area as ExtensionsAreaImpl).registerExtensionPoint(
            PYTHON_HELPERS_LOCATOR_EP,
            PythonHelpersLocator::class.java.name,
            ExtensionPoint.Kind.INTERFACE,
            true,
        )
        area.getExtensionPoint<PythonHelpersLocator>(PYTHON_HELPERS_LOCATOR_EP).registerExtension(
            object : PythonHelpersLocator {
                override fun getRoot(): Path = Path.of(helpersPath)
            },
            ApplicationManager.getApplication(),
        )
    }

    companion object {
        private const val PYTHON_HELPERS_LOCATOR_EP = "com.jetbrains.python.pythonHelpersLocator"
        private const val PYTHON_HELPERS_PATH_PROPERTY = "idea.python.helpers.path"

        /** Cached per JVM so the same mock SDK entity isn't added once per scenario -- see the use site. */
        private val projectDescriptors = HashMap<List<String>, PyLightProjectDescriptor>()
        private val pythonOnlySdks = HashMap<List<String>, com.intellij.openapi.projectRoots.Sdk>()

        fun waitEDTEventsDispatching() {
            ApplicationManager.getApplication().invokeAndWait() {
                // Do nothing, wait for events in EDT
            }
        }
    }
}

