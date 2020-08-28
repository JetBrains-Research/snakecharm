package com.jetbrains.snakecharm.codeInsight.completion.wrapper

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.jetbrains.snakecharm.SnakemakeBundle
import com.jetbrains.snakecharm.facet.SnakemakeFacet
import kotlinx.serialization.*
import kotlinx.serialization.cbor.Cbor
import java.io.File

object SmkWrapperCrawler : StartupActivity {
    @ExperimentalSerializationApi
    override fun runActivity(project: Project) {
        if (ApplicationManager.getApplication().isUnitTestMode) {
            val storage = project.service<SmkWrapperStorage>()
            storage.version = SnakemakeBundle.message("wrapper.bundled.storage.version")
            storage.wrappers = Cbor
                    .decodeFromByteArray(
                            SmkWrapperStorage::class.java
                                    .getResourceAsStream("/smk-wrapper-storage.cbor")
                                    .readBytes()
                    )
        }

        ApplicationManager.getApplication().invokeLater {
            ProgressManager.getInstance().run(object : Task.Backgroundable(
                    project,
                    "Preparing wrapper data",
                    true
            ) {
                override fun run(indicator: ProgressIndicator) {
                    ModuleManager
                            .getInstance(project)
                            .modules
                            .forEach { runActivityModule(it) }
                }
            })
        }
    }

    @ExperimentalSerializationApi
    fun runActivityModule(module: Module, forced: Boolean = false) {
        val mod = SnakemakeFacet.getInstance(module)?.configuration?.state ?: return
        val storage = module.getService(SmkWrapperStorage::class.java)
        if (forced || storage.wrappers.isEmpty() || storage.wrappers.any {it.path == ""}) {
            if (mod.useBundledWrappersInfo) {
                storage.version = SnakemakeBundle.message("wrapper.bundled.storage.version")
                storage.wrappers = Cbor
                        .decodeFromByteArray(
                                SmkWrapperStorage::class.java
                                        .getResourceAsStream("/smk-wrapper-storage.cbor")
                                        .readBytes()
                        )
            } else {
                storage.version = "file://"
                storage.wrappers = localWrapperParser(mod.wrappersCustomSourcesFolder)

                /*
                 * To update bundled wrapper storage uncomment code below
                 * and launch "runIde" with facet setting wrapperCustomSourcesFolder="snakemake-wrappers-master".
                 * Generated "smk-wrapper-storage.cbor" move to "/snakecharm/src/main/resources".
                 * Then change wrapper.bundled.storage.version in "SnakemakeBundle.properties".
                 */

                /*
                val serialized = Cbor.encodeToByteArray(localWrapperParser(mod.wrappersCustomSourcesFolder, true))
                File(module.project.basePath + "/smk-wrapper-storage.cbor").writeBytes(serialized)
                */
            }
        }
    }

    private fun localWrapperParser(folder: String, relative: Boolean = false): List<SmkWrapperStorage.Wrapper> {
        val wrappers = mutableListOf<SmkWrapperStorage.Wrapper>()
        val mainFolder = File(folder)

        mainFolder.walkTopDown()
                .filter { it.isFile && it.name.startsWith("wrapper") }
                .forEach { wrapperFile ->

                    val path = if (relative) {
                        wrapperFile.parentFile.absolutePath
                    } else {
                        wrapperFile.parentFile.toRelativeString(mainFolder)
                    }

                    val args = when (wrapperFile.extension) {
                        "py" -> parseArgsPython(wrapperFile.readText())
                        "R" -> parseArgsR(wrapperFile.readText())
                        else -> emptyMap()
                    }

                    val description = wrapperFile
                            .resolveSibling("meta.yaml")
                            .readText()

                    wrappers.add(
                            SmkWrapperStorage.Wrapper(
                                    path = path,
                                    args = args,
                                    description = description
                            )
                    )
                }

        return wrappers.toList()
    }

    fun parseArgsPython(text: String): Map<String, List<String>> {
        return Regex("snakemake\\.\\w*(\\.(get\\(\"\\w*\"|[^get]\\w*)|\\[\\d+\\])?")
                .findAll(text).map { str ->
                    str.value
                            .substringAfter("snakemake.")
                }
                .toSortedSet()
                .toList()
                .map {
                    val splitted = it.split('.', ignoreCase = false, limit = 2)
                    when {
                        splitted.size != 2
                        -> splitted[0].substringBefore('[') to ""
                        splitted[1].startsWith("get")
                        -> splitted[0] to splitted[1].removeSurrounding("get(\"", "\"")
                        else
                        -> splitted[0] to splitted[1]
                    }
                }
                .groupBy({ it.first }, { it.second })
    }

    fun parseArgsR(text: String): Map<String, List<String>> {
        return Regex("snakemake@\\w*(\\[\\[\"\\w*\"\\]\\])?")
                .findAll(text).map { str ->
                    str.value
                            .substringAfter("snakemake@")
                }
                .toSortedSet()
                .toList()
                .map {
                    val splitted = it.split('[', ignoreCase = false, limit = 2)
                    if (splitted.size == 2) {
                        splitted[0] to splitted[1].substringAfter('"').substringBefore('"')
                    } else {
                        splitted[0] to ""
                    }
                }
                .groupBy({ it.first }, { it.second })
    }
}
