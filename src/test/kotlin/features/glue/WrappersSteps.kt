package features.glue

import com.intellij.openapi.util.io.FileUtil
import com.intellij.util.io.exists
import com.intellij.util.io.isDirectory
import com.jetbrains.snakecharm.facet.SmkFacetConfiguration
import com.jetbrains.snakecharm.facet.SnakemakeFacet
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

        val newState = SmkFacetConfiguration.State()
        newState.useBundledWrappersInfo = false
        newState.wrappersCustomSourcesFolder = FileUtil.toSystemIndependentName(path.toString())

        val snakemakeFacet = SnakemakeFacet.getInstance(module)!!
        SmkFacetConfiguration.setStateAndFireEvent(snakemakeFacet, newState)
       }
}