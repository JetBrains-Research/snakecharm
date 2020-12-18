package com.jetbrains.snakecharm.codeInsight.completion.wrapper

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.intellij.util.io.exists
import com.intellij.util.io.readBytes
import com.jetbrains.snakecharm.SnakemakeBundle
import com.jetbrains.snakecharm.SnakemakeTestUtil
import com.jetbrains.snakecharm.codeInsight.SnakemakeAPI.SMK_WRAPPERS_BUNDLED_REPO
import com.jetbrains.snakecharm.facet.SnakemakeFacet
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray
import java.net.URL
import java.nio.file.Path
import kotlin.system.exitProcess

class SmkWrapperLoaderStartupActivity : StartupActivity {
    @ExperimentalSerializationApi
    override fun runActivity(project: Project) {
        // TODO: scan 'custom' wrappers repo if used and decide force reparse wrappers or used serialized *.cbor representation,
        //       stored in .idea folder

        if (ApplicationManager.getApplication().isUnitTestMode) {
            // In tests mode we load wrappers only if requested and not in background
            return
        }

        ApplicationManager.getApplication().invokeLater {
            ProgressManager.getInstance().run(object : Task.Backgroundable(
                project,
                SnakemakeBundle.message("wrappers.parsing.progress.collecting.data"),
                true
            ) {
                override fun run(indicator: ProgressIndicator) {
                    ModuleManager
                        .getInstance(project)
                        .modules
                        .forEach { loadOrCollectLocalWrappers(it) }
                }
            })
        }
    }

    companion object {
        @ExperimentalSerializationApi
        fun loadOrCollectLocalWrappers(module: Module, forced: Boolean = false) {
            val config = SnakemakeFacet.getInstance(module)?.configuration ?: return

            val storage = module.getService(SmkWrapperStorage::class.java)
            if (!forced && storage.wrappers.isNotEmpty()) {
                // Do nothing
                return
            }

            val unitTestMode = ApplicationManager.getApplication().isUnitTestMode

            if (config.useBundledWrappersInfo) {
                when {
                    unitTestMode -> loadBundledWrappersTestsMode(storage)
                    else -> loadBundledWrappers(storage)
                }
            } else {
                if (unitTestMode && config.state.wrappersCustomSourcesFolder == null) {
                    // Special mode when do not load wrappers, here [SmkFacetConfiguration.wrappersCustomSourcesFolder] is
                    // an empty string, but SmkFacetConfiguration.State treats it as NULL
                    return
                }
                storage.initFrom(
                    "file://${config.wrappersCustomSourcesFolder}",
                    SmkWrapperCrawler.localWrapperParser(config.wrappersCustomSourcesFolder)
                )
            }
        }

        @ExperimentalSerializationApi
        private fun loadBundledWrappersTestsMode(storage: SmkWrapperStorage) {
            val wrappersInfoBundlerForTests = SnakemakeTestUtil.getTestDataPath().parent
                .resolve("build/bundledWrappers/smk-wrapper-storage.test.cbor")

            if (!wrappersInfoBundlerForTests.exists()) {
                System.err.println(
                    "Generate test data 'build/bundledWrappers/smk-wrapper-storage.test.cbor'" +
                            " using `buildTestWrappersBundle` gradle task"
                )
                exitProcess(1)
            }

            val vers = "test" // todo, test repo - fixed version?
            val wrappers = deserializeWrappers(wrappersInfoBundlerForTests)

            storage.initFrom(vers, wrappers)
        }

        @ExperimentalSerializationApi
        private fun loadBundledWrappers(storage: SmkWrapperStorage) {
            // TODO: version specific repo!!
            val wrappersRepoVersion = SMK_WRAPPERS_BUNDLED_REPO

            val resource = SmkWrapperStorage::class.java.getResource(
                "/smk-wrapper-storage-$wrappersRepoVersion.cbor"
            )
            requireNotNull(resource) {
                "Missing '${wrappersRepoVersion}' wrappers bundle"
            }
            storage.initFrom(
                wrappersRepoVersion,
                deserializeWrappers(resource)
            )
        }

        @ExperimentalSerializationApi
        private fun deserializeWrappers(storagePath: Path) = Cbor.decodeFromByteArray<List<SmkWrapperStorage.WrapperInfo>>(
            storagePath.readBytes()
        )

        @ExperimentalSerializationApi
        private fun deserializeWrappers(url: URL): List<SmkWrapperStorage.WrapperInfo> =
            Cbor.decodeFromByteArray(url.readBytes())
    }
}