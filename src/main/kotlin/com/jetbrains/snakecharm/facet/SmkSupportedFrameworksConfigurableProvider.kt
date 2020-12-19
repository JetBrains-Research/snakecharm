package com.jetbrains.snakecharm.facet

import com.intellij.facet.ui.ValidationResult
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.openapi.project.Project
import com.intellij.util.io.exists
import com.intellij.util.io.isDirectory
import com.jetbrains.snakecharm.SnakemakeBundle
import java.nio.file.Paths
import javax.swing.JComponent

/**
 * Registers project settings tab for snakemake settings ('supported framework')
 */
class SmkSupportedFrameworksConfigurableProvider(
    val project: Project
) : Configurable, Configurable.NoScroll {

    private lateinit var projConfigurable: SearchableConfigurable

    override fun createComponent(): JComponent? {
        if (project.isDefault) {
            return null
        }

        projConfigurable = SmkProjectConfigurable(project)
        return projConfigurable.createComponent()
    }

    override fun isModified() = projConfigurable.isModified

    override fun apply() = projConfigurable.apply()
    override fun reset() = projConfigurable.reset()
    override fun getHelpTopic() = "snakemake_support"
    override fun getDisplayName() = SnakemakeBundle.message("smk.framework.configurable.display.name")


    override fun disposeUIResources() {
        projConfigurable.disposeUIResources()
    }

    companion object {
        fun validateWrappersPath(state: SmkSupportProjectSettings.State): ValidationResult {
            if (state.snakemakeSupportEnabled && !state.useBundledWrappersInfo) {
                val folderPathStr: String? = state.wrappersCustomSourcesFolder
                if (folderPathStr == null || folderPathStr.isBlank()) {
                    return ValidationResult(SnakemakeBundle.message("smk.framework.configurable.panel.wrappers.sources.path.is.blank"))
                }

                val path = Paths.get(folderPathStr)
                if (!path.exists()) {
                    return ValidationResult(SnakemakeBundle.message("smk.framework.configurable.panel.wrappers.sources.path.not.exist"))
                }
                if (!path.isDirectory()) {
                    return ValidationResult(SnakemakeBundle.message("smk.framework.configurable.panel.wrappers.sources.path.not.directory"))
                }
            }
            return ValidationResult.OK
        }
    }
}