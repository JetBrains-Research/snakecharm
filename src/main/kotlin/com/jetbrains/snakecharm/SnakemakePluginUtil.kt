package com.jetbrains.snakecharm

import com.intellij.openapi.application.PathManager
import java.nio.file.Path
import kotlin.io.path.Path

object SnakemakePluginUtil {
    fun getPluginSandboxPath(klass: Class<*>): Path {
        val resourceName = "/${klass.name.replace('.', '/')}.class"
        val resourceRootPath = PathManager.getResourceRoot(klass, resourceName)
        requireNotNull(resourceRootPath) {
            "Missing '$resourceName' in plugin bundle"
        }
        val jarPath = Path(resourceRootPath)
        val libsPath = jarPath.parent
        val pluginSandboxPath = libsPath.parent
        return pluginSandboxPath
    }
}