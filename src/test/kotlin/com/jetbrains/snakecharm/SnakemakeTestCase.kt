package com.jetbrains.snakecharm

import com.intellij.lang.java.parser.JavaParserUtil.setLanguageLevel
import com.intellij.openapi.roots.impl.FilePropertyPusher
import com.intellij.testFramework.LightProjectDescriptor
import com.intellij.testFramework.UsefulTestCase
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory
import com.intellij.testFramework.fixtures.TempDirTestFixture
import com.intellij.testFramework.fixtures.impl.LightTempDirTestFixtureImpl
import com.jetbrains.python.PythonDialectsTokenSetProvider
import com.jetbrains.python.psi.LanguageLevel
import com.jetbrains.python.psi.impl.PythonLanguageLevelPusher

/**
 * @author Roman.Chernyatchik
 * @date 2019-02-03
 */
abstract class SnakemakeTestCase : UsefulTestCase() {
    // TODO: could be extend SnakemakeTestCase here?

    private val projectDescriptor = LightProjectDescriptor()

    var fixture: CodeInsightTestFixture? = null

    @Throws(Exception::class)
    override fun setUp() {


        super.setUp()
        val factory = IdeaTestFixtureFactory.getFixtureFactory()
        val fixtureBuilder = factory.createLightFixtureBuilder(projectDescriptor)
        fixture = IdeaTestFixtureFactory.getFixtureFactory().createCodeInsightFixture(
                fixtureBuilder.fixture,
                createTempDirFixture()
        )
        fixture!!.setUp()

        fixture!!.testDataPath = SnakemakeTestUtil.getTestDataPath().toString()
        PythonDialectsTokenSetProvider.reset()
    }

    @Throws(Exception::class)
    override fun tearDown() {
        try {
            //setLanguageLevel(null)
            fixture!!.tearDown()
            fixture = null
            FilePropertyPusher.EP_NAME.findExtensionOrFail(PythonLanguageLevelPusher::class.java).flushLanguageLevelCache()
        // TODO: this will be available in 2018.3 eap
        // } catch (e: Throwable) {
        //     addSuppressedException(e)
        } finally {
            super.tearDown()
            UsefulTestCase.clearFields(this)
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
}