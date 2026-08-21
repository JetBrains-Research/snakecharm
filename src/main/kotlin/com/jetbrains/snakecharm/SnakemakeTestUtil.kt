package com.jetbrains.snakecharm

import com.intellij.openapi.application.PathManager
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

/**
 * @author Roman.Chernyatchik
 * @date 2019-02-03
 */
object SnakemakeTestUtil {
    private const val TEST_DATA_DIR = "testData"

    fun getTestDataPath(): Path {
        val homePath = projectHomePath(SnakemakeTestUtil::class.java)
        checkNotNull(homePath) { "Could not locate the project home (a directory containing '$TEST_DATA_DIR')." }
        return homePath.resolve(TEST_DATA_DIR)
    }

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
        // actually contains the 'testData' directory — the project home.
        var dir: Path? = File(rootPath).toPath().parent
        while (dir != null && !Files.isDirectory(dir.resolve(TEST_DATA_DIR))) {
            dir = dir.parent
        }
        return dir
    }
}