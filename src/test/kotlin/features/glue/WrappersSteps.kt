package features.glue

import com.intellij.openapi.util.io.FileUtil
import com.intellij.util.io.exists
import com.intellij.util.io.isDirectory
import com.jetbrains.snakecharm.framework.SmkSupportProjectSettings
import io.cucumber.java.en.Given
import java.nio.file.Paths

class WrappersSteps {
    @Given("^wrapper repo path in test dir \"(.+)\"")
       fun wrapperRepoPath(folderRelativePathToTestData: String) {
        // val folder = SnakemakeWorld.fixture().tempDirFixture.findOrCreateDir(pathStr)
        // folder.url

        val path = Paths.get(SnakemakeWorld.fixture().testDataPath, folderRelativePathToTestData)
        require(path.exists() && path.isDirectory()) {
            "Should be an existing directory, but was: exist:${path.exists()}, dir:${path.isDirectory()}"
        }
        val module = SnakemakeWorld.fixture().module

        val newState = SmkSupportProjectSettings.State()
        newState.snakemakeSupportEnabled = true
        newState.useBundledWrappersInfo = false
        newState.wrappersCustomSourcesFolder = FileUtil.toSystemIndependentName(path.toString())

        SmkSupportProjectSettings.updateStateAndFireEvent(SnakemakeWorld.fixture().project, newState)
       }
}