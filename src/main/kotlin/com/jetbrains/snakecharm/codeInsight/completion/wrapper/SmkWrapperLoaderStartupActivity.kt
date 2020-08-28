package com.jetbrains.snakecharm.codeInsight.completion.wrapper

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.jetbrains.python.statistics.modules
import com.jetbrains.snakecharm.SnakemakeBundle
import com.jetbrains.snakecharm.facet.SnakemakeFacet
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray

class SmkWrapperLoaderStartupActivity : StartupActivity {
    @ExperimentalSerializationApi
    override fun runActivity(project: Project) {
        if (ApplicationManager.getApplication().isUnitTestMode) {
            project.modules.forEach {
                val storage = it.getService(SmkWrapperStorage::class.java)
                storage.version = SnakemakeBundle.message("wrapper.bundled.storage.version")
                storage.wrappers = Cbor
                    .decodeFromByteArray(
                        SmkWrapperStorage::class.java
                            .getResourceAsStream("/smk-wrapper-storage.cbor")
                            .readBytes()
                    )
            }
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

    companion object {
        @ExperimentalSerializationApi
        fun runActivityModule(module: Module, forced: Boolean = false) {
            val mod = SnakemakeFacet.getInstance(module)?.configuration?.state ?: return
            val storage = module.getService(SmkWrapperStorage::class.java)
            if (forced || storage.wrappers.isEmpty() || storage.wrappers.any { it.path == "" }) {
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
                    storage.wrappers = SmkWrapperCrawler.localWrapperParser(mod.wrappersCustomSourcesFolder)

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
    }
}