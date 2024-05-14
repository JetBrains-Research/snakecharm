package com.jetbrains.snakecharm.framework

import com.intellij.facet.ui.ValidationResult
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.jetbrains.snakecharm.SnakemakeBundle
import java.nio.file.Paths
import javax.swing.JComponent
import kotlin.io.path.exists
import kotlin.io.path.isDirectory

/**
 * Registers project settings tab for snakemake settings ('supported framework')
 */
class SmkFrameworkConfigurableProvider(
    val project: Project
) : Configurable, Configurable.NoScroll {

    private var projConfigurable: SearchableConfigurable? = null

    override fun createComponent(): JComponent? {
        if (project.isDefault) {
            return null
        }

        projConfigurable = SmkFrameworkConfigurable(project)
        return projConfigurable!!.createComponent()
    }

    override fun isModified() = projConfigurable!!.isModified

    override fun apply() = projConfigurable?.apply() ?: Unit
    override fun reset() = projConfigurable?.reset() ?: Unit
    override fun getHelpTopic() = "snakemake_support"
    override fun getDisplayName() = SnakemakeBundle.message("smk.framework.configurable.display.name")


    override fun disposeUIResources() {
        projConfigurable?.disposeUIResources()
    }

    companion object {
        fun validateWrappersPath(
            project: Project,
            state: SmkSupportProjectSettings.State
        ): ValidationResult {
            if (state.snakemakeSupportEnabled) {
                if (!state.useBundledWrappersInfo) {
                    val folderPathStr: String? = state.wrappersCustomSourcesFolder
                    if (folderPathStr.isNullOrBlank()) {
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

                val sdkName = state.pythonSdkName
                val sdk = SmkSupportProjectSettings.findPythonSdk(project, sdkName)
                if (sdk == null) {
                    if (sdkName.isNullOrBlank()) {
                        return ValidationResult(SnakemakeBundle.message("smk.framework.configurable.panel.sdk.project.not.valid"))
                    }
                    return ValidationResult(SnakemakeBundle.message("smk.framework.configurable.panel.sdk.not.valid", sdkName))
                }
            }
            return ValidationResult.OK
        }

        fun validateLanguageVersion(
            state: SmkSupportProjectSettings.State
        ): ValidationResult {
            if (state.snakemakeSupportEnabled) {

                val parts = state.snakemakeVersion?.split("\\.".toRegex())?.dropLastWhile { it.isEmpty() }?.toTypedArray()
                if (parts?.size != 3) {
                    return ValidationResult(SnakemakeBundle.message("smk.framework.configurable.panel.version.not.valid"))
                }
                for (part in parts) {
                    if (!StringUtil.isNumeric(part)) {
                        return ValidationResult(SnakemakeBundle.message("smk.framework.configurable.panel.version.not.valid"))
                    }
                }
            }
            return ValidationResult.OK
        }
    }
}