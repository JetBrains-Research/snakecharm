package com.jetbrains.snakecharm.framework

import com.intellij.framework.detection.*
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModifiableModelsProvider
import com.intellij.openapi.roots.ui.configuration.ModulesProvider
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.containers.MultiMap
import com.jetbrains.snakecharm.SmkFileType
import com.jetbrains.snakecharm.SnakemakeBundle
import java.util.*

/**
 * For [IDEA]
 * Detects 'Snakemake' in project by snakemake related files
 * For some reason works in IDEA, not in PyCharm
 */
class SmkFrameworkDetector : FrameworkDetector("snakemake") {
    override fun createSuitableFilePattern() = FileContentPattern.fileContent()!!
    override fun getFileType() = SmkFileType.INSTANCE

    override fun detect(
        newFiles: MutableCollection<out VirtualFile>,
        context: FrameworkDetectionContext,
    ): MutableList<out DetectedFrameworkDescription> {
        val project = context.project!!

        val filesByModule = MultiMap.createSet<Module, VirtualFile>()
        for (file in newFiles) {
            val module = ModuleUtilCore.findModuleForFile(file, project)
            if (module != null) {
                filesByModule.putValue(module, file);
            }
        }

        val supportedFiles = ArrayList<VirtualFile>();
        for (module in filesByModule.keySet()) {
            if (!SmkFrameworkType.isSuitableModuleType(module)) {
                continue
            }

            supportedFiles.addAll(filesByModule.get(module))
        }

        if (supportedFiles.isEmpty()) {
            // N/A
            return mutableListOf()
        }

        if (SmkSupportProjectSettings.getInstance(project).snakemakeSupportEnabled) {
            // [DetectedFrameworksData.updateFrameworksList] expects mutable list here
            return mutableListOf()
        }

        return mutableListOf(FrameworkDescription(supportedFiles, context, project, this));
    }

    class FrameworkDescription(
        private val supportedFiles: ArrayList<VirtualFile>,
        private val context: FrameworkDetectionContext,
        private val project: Project,
        private val frameworkDetector: SmkFrameworkDetector
    ) : DetectedFrameworkDescription() {

        override fun equals(other: Any?) = supportedFiles == other

        override fun hashCode() = supportedFiles.hashCode()

        override fun getRelatedFiles(): MutableCollection<out VirtualFile> = supportedFiles

        override fun getSetupText() = SnakemakeBundle.message("smk.framework.detector.added.to.module")

        override fun getDetector() = frameworkDetector

        override fun setupFramework(
            modifiableModelsProvider: ModifiableModelsProvider,
            modulesProvider: ModulesProvider
        ) {
            // Enable
            SmkSupportProjectSettings.addSupport(context.project!!)

            // Show settings
            ApplicationManager.getApplication().invokeLater() {
                ShowSettingsUtil.getInstance().showSettingsDialog(
                    project,
                    SmkFrameworkConfigurableProvider::class.java
                )
            }
        }

    }

    override fun getFrameworkType() = SmkFrameworkType()
}

