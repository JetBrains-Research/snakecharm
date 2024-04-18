package features.glue

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.io.FileUtil
import com.jetbrains.snakecharm.framework.SmkSupportProjectSettings
import features.glue.StepDefs.Companion.waitEDTEventsDispatching
import io.cucumber.java.en.Given
import java.nio.file.Paths
import kotlin.io.path.exists
import kotlin.io.path.isDirectory

class WrappersSteps {
    @Given("^wrapper repo path in test dir \"(.+)\"")
    fun wrapperRepoPath(folderRelativePathToTestData: String) {
        // val folder = SnakemakeWorld.fixture().tempDirFixture.findOrCreateDir(pathStr)
        // folder.url

        val path = Paths.get(SnakemakeWorld.fixture().testDataPath, folderRelativePathToTestData)
        require(path.exists() && path.isDirectory()) {
            "Should be an existing directory, but was: exist:${path.exists()}, dir:${path.isDirectory()}"
        }
        val newState = SmkSupportProjectSettings.State()
        newState.snakemakeSupportEnabled = true
        newState.useBundledWrappersInfo = false
        newState.wrappersCustomSourcesFolder = FileUtil.toSystemIndependentName(path.toString())

        ApplicationManager.getApplication().invokeAndWait {
            SmkSupportProjectSettings.updateStateAndFireEvent(SnakemakeWorld.fixture().project, newState)
        }

        waitEDTEventsDispatching()
    }
}