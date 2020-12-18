package com.jetbrains.snakecharm.facet

import com.intellij.facet.ui.*
import com.intellij.util.io.exists
import com.intellij.util.io.isDirectory
import com.jetbrains.snakecharm.SnakemakeBundle
import org.jetbrains.annotations.Nls
import java.nio.file.Paths
import javax.naming.ConfigurationException
import javax.swing.JComponent

class SmkFacetEditorTab(
    private val configuration: SmkFacetConfiguration,
    private val context: FacetEditorContext,
    private val validator: FacetValidatorsManager
) : FacetEditorTab() {

    lateinit var settingsPanel: SmkFacetSettingsPanel

    override fun isModified() = settingsPanel.isModified(configuration)

    @Nls(capitalization = Nls.Capitalization.Title)
    override fun getDisplayName(): String {
        return SnakemakeBundle.message("facet.configurable.display.name")
    }

    override fun createComponent(): JComponent {
        settingsPanel = SmkFacetSettingsPanel(context.project)

        validator.registerValidator(object : FacetEditorValidator() {
            override fun check(): ValidationResult {
                return validateWrappersPath(settingsPanel.uiState)
            }

        }, *settingsPanel.componentsToValidate)
        return settingsPanel
    }

    override fun apply() {
        try {
            settingsPanel.apply(configuration)
        } catch (e: ConfigurationException) {
            throw e
        } catch (e: Exception) {
            throw ConfigurationException(e.toString())
        }
    }

    override fun reset() {
        settingsPanel.reset(configuration)
    }

    companion object {
        fun validateWrappersPath(state: SmkFacetConfiguration.State): ValidationResult {
            if (!state.useBundledWrappersInfo) {
                val folderPathStr = state.wrappersCustomSourcesFolder
                if (folderPathStr!!.isBlank()) {
                    return ValidationResult(SnakemakeBundle.message("facet.settings.wrappers.sources.path.is.blank"))
                }

                val path = Paths.get(folderPathStr)
                if (!path.exists()) {
                    return ValidationResult(SnakemakeBundle.message("facet.settings.wrappers.sources.path.not.exist"))
                }
                if (!path.isDirectory()) {
                    return ValidationResult(SnakemakeBundle.message("facet.settings.wrappers.sources.path.not.directory"))
                }
            }
           return ValidationResult.OK
       }
    }
}