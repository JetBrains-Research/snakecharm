package com.jetbrains.snakecharm.framework

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.DumbAwareRunnable
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupManager
import com.intellij.openapi.util.Computable
import com.intellij.openapi.util.Ref
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileVisitor
import com.intellij.platform.DirectoryProjectConfigurator
import com.intellij.util.ui.EDT
import com.jetbrains.snakecharm.SmkNotifier

/**
 * Is used when project is created from existing directory, especially for PyCharm. This class adds
 * snakemake support if 'Snakefile' exists in project. For some reason in PyCharm facet detector
 * doesn't work, so this workaround helps in existing projects.
 */
class SmkFrameworkSupportDirProjectConfigurator : DirectoryProjectConfigurator {
    override fun configureProject(
        project: Project,
        baseDir: VirtualFile,
        moduleRef: Ref<Module>,
        isProjectCreatedWithWizard: Boolean,
    ) {
        EDT.assertIsEdt()
        val module = moduleRef.get()
        if (module != null && SmkFrameworkType.isSuitableModuleType(module)) {
            if (detectSnakemake(baseDir)) {
                // XXX: on project created is required to configure smk facet, SnakemakeStartupActivity will not help
                @Suppress("UnstableApiUsage")
                StartupManager.getInstance(project).runAfterOpened(DumbAwareRunnable {
                    enableSnakemakeSupport(
                        module,
                        baseDir
                    )
                })
            }
        }
    }

    private fun detectSnakemake(baseDir: VirtualFile): Boolean {
        // TODO [romeo]: 1. think on SDK check, e.g. check that  OrderEntryUtil.findLibraryOrderEntry(model, 'snakemake') != null

        // TODO [romeo]: 2. maybe by naming conventions instead? several fixed paths
        // TODO [romeo]: 3. if detected maybe is good to install snakemake if is not installed in SDK to get all features
        //              (see DjangoFacetConfigurator)
        return containsSnakefile(baseDir, 3)
    }

    private fun enableSnakemakeSupport(module: Module, baseDir: VirtualFile) {
        val project = module.project
        val application = ApplicationManager.getApplication()
        val isSupportEnabled = application.runReadAction(Computable {
            SmkSupportProjectSettings.getInstance(project).snakemakeSupportEnabled
        })

        if (isSupportEnabled) {
            // already exists
            return
        }

        application.invokeLater(DumbAwareRunnable {
            // add support
            application.runWriteAction(DumbAwareRunnable {
                SmkSupportProjectSettings.addSupport(project)
            })
            // Notify
            SmkNotifier.notifySnakefileDetected(module)
        }, ModalityState.defaultModalityState(), module.disposed)
    }

    private fun containsSnakefile(root: VirtualFile, depth: Int): Boolean {
        var result = false
        VfsUtilCore.visitChildrenRecursively(root, object : VirtualFileVisitor<Void?>(limit(depth)) {
            override fun visitFileEx(file: VirtualFile): Result {
                if (file.isDirectory) {
                    if (containsSnakefileInRoot(file)) {
                        result = true
                    }
                }
                return CONTINUE
            }
        })
        return result
    }

    fun containsSnakefileInRoot(root: VirtualFile): Boolean {
        for (child in root.children) {
            if (!child.isDirectory && child.name == "Snakefile") {
                return true
            }
        }
        return false
    }
}