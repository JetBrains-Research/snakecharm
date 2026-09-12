package com.jetbrains.snakecharm

import com.intellij.openapi.application.PathManager
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile

/**
 * @author Roman.Chernyatchik
 * @date 2019-02-03
 */
object SnakemakeTestUtil {
    private const val TEST_DATA_DIR = "testData"

    /**
     * Marker file that, together with [TEST_DATA_DIR], identifies the project home. A lone 'testData'
     * directory is not enough: the walk below runs all the way to the filesystem root, and an
     * unrelated ancestor that happens to own one (a CI workspace, ~/testData, an enclosing monorepo)
     * would be accepted silently — every test would then read fixtures from the wrong tree and fail
     * as a pile of "file not found" errors rather than as a locator problem.
     */
    private const val PROJECT_HOME_MARKER = "snakemake_api.yaml"

    /**
     * Resolved once: the project home cannot move while the test JVM runs, and this is called several
     * times per scenario (~10k times over the suite), each call otherwise costing a resource-root
     * lookup plus two stat() per ancestor level.
     */
    private val projectTestDataPath: Path by lazy {
        val homePath = projectHomePath(SnakemakeTestUtil::class.java)
        checkNotNull(homePath) {
            "Could not locate the project home (a directory containing both '$TEST_DATA_DIR' and '$PROJECT_HOME_MARKER')."
        }
        homePath.resolve(TEST_DATA_DIR)
    }

    fun getTestDataPath(): Path = projectTestDataPath

    private fun projectHomePath(aClass: Class<*>): Path? {
        val rootPath = PathManager.getResourceRoot(
                aClass,
                "/" + aClass.name.replace('.', '/') + ".class"
        ) ?: return null

        // The class is loaded either from the plugin jar inside the Gradle test sandbox
        // (e.g. <home>/.sandbox_pycharm/<projectName>/PY-2026.1.3/plugins-test/snakecharm/lib/snakecharm-*.jar)
        // or from a build output directory. The exact depth of the sandbox layout has changed across
        // platform / IntelliJ Platform Gradle Plugin versions (2026.1 added an extra <projectName> level),
        // so instead of counting a fixed number of parents we walk up to the nearest ancestor that
        // looks like the project home.
        return generateSequence(Path(rootPath).parent) { it.parent }.firstOrNull(::isProjectHome)
    }

    private fun isProjectHome(dir: Path) =
            dir.resolve(TEST_DATA_DIR).isDirectory() && dir.resolve(PROJECT_HOME_MARKER).isRegularFile()
}
