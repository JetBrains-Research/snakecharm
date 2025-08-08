package com.jetbrains.snakecharm

import com.intellij.application.options.CodeStyle
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.roots.impl.FilePropertyPusher
import com.intellij.openapi.vfs.newvfs.impl.VfsRootAccess
import com.intellij.testFramework.TestApplicationManager
import com.intellij.testFramework.UsefulTestCase
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory
import com.intellij.testFramework.fixtures.TempDirTestFixture
import com.intellij.testFramework.fixtures.impl.LightTempDirTestFixtureImpl
import com.jetbrains.python.fixtures.PyLightProjectDescriptor
import com.jetbrains.python.psi.LanguageLevel
import com.jetbrains.python.psi.impl.PythonLanguageLevelPusher
import com.jetbrains.snakecharm.lang.SnakemakeLanguageDialect
import javax.swing.SwingUtilities


/**
 * @author Roman.Chernyatchik
 * @date 2019-02-03
 */
abstract class SnakemakeTestCase : UsefulTestCase() {
    // TODO: could be extend SnakemakeTestCase here?

    protected val ourPyDescriptor = PyLightProjectDescriptor(
            LanguageLevel.PYTHON27,
            SnakemakeTestUtil.getTestDataPath().toString()
    )

    protected val ourPy3Descriptor = PyLightProjectDescriptor(
            LanguageLevel.PYTHON37,
            SnakemakeTestUtil.getTestDataPath().toString(),
            SnakemakeTestUtil.getTestDataPath().resolve("MockPackages3")
    )

    open protected val projectDescriptor = ourPy3Descriptor

    var fixture: CodeInsightTestFixture? = null

    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()
        TestApplicationManager.getInstance()
        val factory = IdeaTestFixtureFactory.getFixtureFactory()

        // It is ok to access these pythons
        //  Fix for: ERROR: File accessed outside allowed roots: file:///usr/local/bin/python3;
        VfsRootAccess.allowRootAccess(
            testRootDisposable,
            "/usr/local/bin/python3.10",
            "/usr/local/bin/python3",
            "/usr/bin/python3"
        )

        val fixtureBuilder = factory.createLightFixtureBuilder(
            projectDescriptor, getTestName(false)
        )
        fixture = IdeaTestFixtureFactory.getFixtureFactory().createCodeInsightFixture(
                fixtureBuilder.fixture,
                createTempDirFixture()
        )
        fixture!!.testDataPath = SnakemakeTestUtil.getTestDataPath().toString()
        VfsRootAccess.allowRootAccess(testRootDisposable, fixture!!.testDataPath)

        // TODO() replace with: runInEdtAndWait
        if (SwingUtilities.isEventDispatchThread()) {
            fixture!!.setUp()
        } else {
            ApplicationManager.getApplication().invokeAndWait {
                try {
                    fixture!!.setUp()
                } catch (e: java.lang.Exception) {
                    throw RuntimeException("Error running setup", e)
                }
            }
        }
// TODO investigate is it required or not
//        if (SwingUtilities.isEventDispatchThread()) {
//            fixture!!.setUp()
//        } else {
//            ApplicationManager.getApplication().invokeAndWait {
//                try {
//                    fixture!!.setUp()
//                } catch (e: java.lang.Exception) {
//                    throw RuntimeException("Error running setup", e)
//                }
//            }
//        }
    }

    @Throws(Exception::class)
    override fun tearDown() {
        try {
// TODO investigate is it required or not
//            InspectionProfileImpl.INIT_INSPECTIONS = false
//
//            (LocalInspectionEP.LOCAL_INSPECTION.extensionList
//                    .first { it.shortName == "SmkUnrecognizedSectionInspection" }
//                    .instance as SmkUnrecognizedSectionInspection).ignoredItems.clear()

            setLanguageLevel(null)
            fixture!!.tearDown()
            fixture = null
            FilePropertyPusher.EP_NAME.findExtensionOrFail(PythonLanguageLevelPusher::class.java)
                .flushLanguageLevelCache()
        } catch (e: Throwable) {
            addSuppressedException(e)
        } finally {
            super.tearDown()
            clearFields(this)
        }
    }

    /**
     * @return fixture to be used as temporary dir.
     */
    protected fun createTempDirFixture(): TempDirTestFixture {
        return LightTempDirTestFixtureImpl(true) // "tmp://" dir by default
    }

    protected fun setLanguageLevel(languageLevel: LanguageLevel?) {
        PythonLanguageLevelPusher.setForcedLanguageLevel(fixture!!.project, languageLevel)
    }

    protected fun runWithLanguageLevel(languageLevel: LanguageLevel, runnable: Runnable) {
        setLanguageLevel(languageLevel)
        try {
            runnable.run()
        } finally {
            setLanguageLevel(null)
        }
    }

    protected open fun getCommonCodeStyleSettings() =
        getCodeStyleSettings().getCommonSettings(SnakemakeLanguageDialect)

    protected open fun getCodeStyleSettings() = CodeStyle.getSettings(fixture!!.project)

    protected open fun getIndentOptions() = getCommonCodeStyleSettings().indentOptions

}