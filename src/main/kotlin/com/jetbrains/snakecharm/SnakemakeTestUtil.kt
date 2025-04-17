package com.jetbrains.snakecharm

import com.intellij.openapi.application.PathManager
import java.io.File
import java.nio.file.Path

/**
 * @author Roman.Chernyatchik
 * @date 2019-02-03
 */
object SnakemakeTestUtil {
    fun getTestDataPath(): Path {
        val homePath = projectHomePath(SnakemakeTestUtil::class.java)
        checkNotNull(homePath)
        return homePath.resolve("testData")
    }

    private fun projectHomePath(aClass: Class<*>): Path? {
        val rootPath = PathManager.getResourceRoot(
                aClass,
                "/" + aClass.name.replace('.', '/') + ".class"
        )

        return when (rootPath) {
            null -> null
            else -> {
                // XXX: Starting from 2025.x:
                if (rootPath.endsWith(".jar")) {
                    // E.g.: ~/snakecharm/.sandbox_pycharm/PC-2025.1/plugins-test/snakecharm/lib/snakecharm-2025.1.1-eap.SNAPSHOT.jar
                    return File(rootPath).toPath().parent.parent.parent.parent.parent.parent
                }

                // XXX Before 2025.x :
                val subDir = File(rootPath).toPath().parent.parent
                when {
                    subDir.fileName.toString() == "out" -> subDir.parent
                    subDir.fileName.toString() == "build" -> subDir.parent
                    else -> null
                }
            }
        }
    }
}