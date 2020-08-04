package com.jetbrains.snakecharm.codeInsight.completion.wrapper

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.intellij.platform.templates.github.ZipUtil
import com.intellij.util.io.URLUtil
import java.io.File
import java.net.URL

class SmkWrapperCrawler : StartupActivity {
    override fun runActivity(project: Project) {
        if (ApplicationManager.getApplication().isUnitTestMode) {
            return
        }

        ApplicationManager.getApplication().invokeLater {
            ProgressManager.getInstance().run(object: Task.Backgroundable(
                    project,
                    "Downloading wrapper repository data",
                    false
            ) {
                override fun run(indicator: ProgressIndicator) {
                    val folder = File(SmkWrapperCompletionProvider.WRAPPERS_PATH)
                    if (!folder.exists()) {
                        val file = URLUtil.urlToFile(URL("https://github.com/snakemake/snakemake-wrappers/archive/master.zip"))
                        ZipUtil.unzip(null, folder, file, null, null, true)
                    }
                    localWrapperParser()
                }
            })
        }
    }
}

fun localWrapperParser() {
    val storage = SmkWrapperStorage.getInstance()
    val wrappers = mutableListOf<SmkWrapperStorage.Wrapper>()
    var mainFolder = File(SmkWrapperCompletionProvider.WRAPPERS_PATH)
    while (mainFolder.isDirectory && !mainFolder.list()!!.contains("bio")) {
        mainFolder = mainFolder.listFiles()!![0]
    }
    mainFolder.walkTopDown()
            .filter { it.path.endsWith("meta.yaml") }
            .forEach { metafile ->
                var wrapperfile = metafile.resolveSibling("wrapper.py")
                if (!wrapperfile.exists()) {
                    wrapperfile = metafile.resolveSibling("wrapper.R")
                }
                val path = metafile.parentFile.toRelativeString(mainFolder)
                val args = Regex("\\{snakemake\\.\\S*}")
                        .findAll(wrapperfile.readText()).map { str ->
                            str.value
                                    .drop(11)
                                    .dropLast(1)
                        }
                        .toSortedSet()
                        .toList()
                        .joinToString(",")
                val versions = "0.64.0"
                val metatext = metafile.readText()
                val name = metatext.substringAfter("name:").substringBefore("description")
                val description = metatext.substringAfter("description:").substringBefore("authors")
                val authors = metatext.substringAfter("authors:")
                wrappers.add(
                    SmkWrapperStorage.Wrapper(
                        name=name,
                        path=path,
                        firstTag=versions,
                        args=args,
                        description=description,
                        author=authors
                    )
                )
            }
    storage.setStorage(wrappers.toList())
}
