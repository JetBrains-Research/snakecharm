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