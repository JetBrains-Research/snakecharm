package com.jetbrains.snakecharm.codeInsight.completion.wrapper

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.util.io.exists
import com.intellij.util.io.readBytes
import com.jetbrains.snakecharm.SnakemakeBundle
import com.jetbrains.snakecharm.SnakemakeTestUtil
import com.jetbrains.snakecharm.framework.SmkSupportProjectSettings
import com.jetbrains.snakecharm.framework.SmkSupportProjectSettingsListener
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray
import java.net.URL
import java.nio.file.Path
import java.util.*
import kotlin.system.exitProcess

class SmkWrapperStorage(val project: Project) : Disposable {
    var version = ""
        private set

    var wrappers: List<WrapperInfo> = emptyList()
        private set

    fun initFrom(
        version: String,
        wrappers: List<WrapperInfo>
    ) {
        this.version = version
        this.wrappers = Collections.unmodifiableList(wrappers)
    }

    fun cleanVersion() = version.removePrefix("refs/tags/").removePrefix("refs/heads/")

    fun initOnStartup() {
        subscribeOnEvents()

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
                    if (SmkSupportProjectSettings.getInstance(project).snakemakeSupportEnabled) {
                        loadOrCollectLocalWrappers(project)
                    }
                }
            })
        }
    }

    private fun subscribeOnEvents() {
        val connection = project.messageBus.connect()
        connection.subscribe(SmkSupportProjectSettings.TOPIC, object : SmkSupportProjectSettingsListener {
            override fun stateChanged(
                newSettings: SmkSupportProjectSettings,
                oldState: SmkSupportProjectSettings.State,
                sdkRenamed: Boolean,
                sdkRemoved: Boolean
            ) {
                val wrappersChanged = if (newSettings.useBundledWrappersInfo) {
                    !oldState.useBundledWrappersInfo
                } else {
                    newSettings.wrappersCustomSourcesFolder != oldState.wrappersCustomSourcesFolder
                }

                if (wrappersChanged) {
                    stateChanged(newSettings)
                }
            }

            override fun enabled(newSettings: SmkSupportProjectSettings) = stateChanged(newSettings)

            override fun disabled(newSettings: SmkSupportProjectSettings) = stateChanged(newSettings)

            fun stateChanged(newSettings: SmkSupportProjectSettings) {
                if (!newSettings.snakemakeSupportEnabled) {
                    // clean wrappers
                    getInstance(project).initFrom(
                        "", emptyList()
                    )
                    return
                }

                val forceLoadOrCollectWrappersAction = {
                    loadOrCollectLocalWrappers(project, true)
                }

                if (ApplicationManager.getApplication().isUnitTestMode) {
                    // Do now, in in BG
                    forceLoadOrCollectWrappersAction()
                    return
                }
                ApplicationManager.getApplication().invokeLater {
                    ProgressManager.getInstance().run(object : Task.Backgroundable(
                        project,
                        SnakemakeBundle.message("wrappers.parsing.progress.collecting.data"),
                        true
                    ) {
                        override fun run(indicator: ProgressIndicator) {
                            forceLoadOrCollectWrappersAction()
                        }
                    })
                }
            }

        })
        Disposer.register(this, connection)
    }

    @Suppress("PROVIDED_RUNTIME_TOO_LOW")
    @Serializable
    data class WrapperInfo(
        val path: String = "", // system independent separators
        val args: Map<String, List<String>> = emptyMap(),
        val description: String = ""
    )

    companion object {
        fun getInstance(project: Project) = project.getService(SmkWrapperStorage::class.java)

        fun loadOrCollectLocalWrappers(project: Project, forced: Boolean = false) {
            // TODO: scan 'custom' wrappers repo if used and decide force reparse wrappers or used serialized *.cbor representation,
            //       stored in .idea folder

            val storage = getInstance(project)

            val config = SmkSupportProjectSettings.getInstance(project)
            if (!config.snakemakeSupportEnabled) {
                // remove wrappers
                storage.initFrom("", emptyList())
                return
            }
            require(config.snakemakeSupportEnabled)

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
                if (unitTestMode && config.stateSnapshot().wrappersCustomSourcesFolder == null) {
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

            val (repoVersion, wrappers) = deserializeWrappers(wrappersInfoBundlerForTests)
            storage.initFrom(repoVersion, wrappers)
        }

        @ExperimentalSerializationApi
        private fun loadBundledWrappers(storage: SmkWrapperStorage) {
            val resourceName = "/smk-wrapper-storage-bundled.cbor"
            val resource = SmkWrapperStorage::class.java.getResource(resourceName)
            requireNotNull(resource) {
                "Missing '$resourceName' wrappers bundle"
            }
            val (repoVersion, wrappers) = deserializeWrappers(resource)
            storage.initFrom(repoVersion, wrappers)
        }

        @ExperimentalSerializationApi
        private fun deserializeWrappers(storagePath: Path) =
            Cbor.decodeFromByteArray<Pair<String, List<SmkWrapperStorage.WrapperInfo>>>(
                storagePath.readBytes()
            )

        @ExperimentalSerializationApi
        private fun deserializeWrappers(url: URL): Pair<String, List<SmkWrapperStorage.WrapperInfo>> =
            Cbor.decodeFromByteArray(url.readBytes())
    }

    override fun dispose() {
        // do nothing
    }
}
