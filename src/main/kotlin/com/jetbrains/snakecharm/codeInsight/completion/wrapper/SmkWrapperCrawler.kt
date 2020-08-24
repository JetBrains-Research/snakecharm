package com.jetbrains.snakecharm.codeInsight.completion.wrapper

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.intellij.platform.templates.github.ZipUtil
import com.intellij.util.io.URLUtil
import java.io.File
import java.net.URL

object SmkWrapperCrawler : StartupActivity {
    override fun runActivity(project: Project) {
        if (ApplicationManager.getApplication().isUnitTestMode) {
            return
        }

        ApplicationManager.getApplication().invokeLater {
            ProgressManager.getInstance().run(object : Task.Backgroundable(
                    project,
                    "Downloading wrapper repository data",
                    true
            ) {
                override fun run(indicator: ProgressIndicator) {
                    val folder = File(SmkWrapperCompletionProvider.WRAPPERS_PATH)
                    if (!folder.exists()) {
                        val file = URLUtil.urlToFile(URL("https://github.com/snakemake/snakemake-wrappers/archive/master.zip"))
                        ZipUtil.unzip(null, folder, file, null, null, true)
                    }
                    localWrapperParser(project)
                }
            })
        }
    }

    fun localWrapperParser(project: Project) {
        val storage = project.service<SmkWrapperStorage>()
        val wrappers = mutableListOf<SmkWrapperStorage.Wrapper>()
        var mainFolder = File(SmkWrapperCompletionProvider.WRAPPERS_PATH)
        while (mainFolder.isDirectory && !mainFolder.list()!!.contains("bio")) {
            mainFolder = mainFolder.listFiles()!![0]
        }
        mainFolder.walkTopDown()
                .filter { it.path.endsWith("meta.yaml") }
                .forEach { metafile ->
                    val wrapperfile: File
                    val args: Map<String, List<String>>
                    if (metafile.resolveSibling("wrapper.py").exists()) {
                        wrapperfile = metafile.resolveSibling("wrapper.py")
                        args = parseArgsPython(wrapperfile.readText())
                    } else {
                        wrapperfile = metafile.resolveSibling("wrapper.R")
                        args = parseArgsR(wrapperfile.readText())
                    }
                    val path = metafile.parentFile.toRelativeString(mainFolder)

                    val versions = "0.64.0"
                    val metatext = metafile.readText()
                    val name = metatext.substringAfter("name:").substringBefore("description").trim('"', '\n', ' ')
                    val description = metatext.substringAfter("description:").substringBefore("authors").trim('"', '\n', ' ')
                    val authors = metatext.substringAfter("authors:").trim('"', '\n', ' ')
                    wrappers.add(
                            SmkWrapperStorage.Wrapper(
                                    name = name,
                                    path = path,
                                    firstTag = versions,
                                    args = args,
                                    description = description,
                                    author = authors
                            )
                    )
                }
        storage.wrapperStorage = wrappers.toList()
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
